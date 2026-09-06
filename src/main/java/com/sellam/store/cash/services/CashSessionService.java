package com.sellam.store.cash.services;

import com.sellam.store.cash.dto.CashDTO;
import com.sellam.store.cash.models.*;
import com.sellam.store.cash.repositories.*;
import com.sellam.store.common.exception.ResourceNotFoundException;
import com.sellam.store.identity.models.PersonEntity;
import com.sellam.store.identity.repositories.PersonRepository;
import com.sellam.store.invoices.models.InvoiceEntity;
import com.sellam.store.invoices.repositories.InvoiceRepository;
import com.sellam.store.shops.models.ShopEntity;
import com.sellam.store.shops.repositories.ShopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CashSessionService {

    private final CashRegisterRepository registerRepository;
    private final CashRegisterSessionRepository sessionRepository;
    private final CashMovementRepository movementRepository;
    private final PersonRepository personRepository;
    private final ShopRepository shopRepository;
    private final InvoiceRepository invoiceRepository;

    private static final List<CashSessionStatusEnum> ACTIVE_STATUSES = List.of(
            CashSessionStatusEnum.OPEN,
            CashSessionStatusEnum.PENDING_INITIAL_CASH
    );

    // ──────────────── Register Management ────────────────

    @Transactional
    public CashRegisterEntity getOrCreateDefaultRegister(UUID shopId) {
        return registerRepository.findFirstByShopIdAndActiveTrue(shopId)
                .orElseGet(() -> {
                    ShopEntity shop = shopRepository.findById(shopId)
                            .orElseThrow(() -> new ResourceNotFoundException("Boutique introuvable"));
                    CashRegisterEntity reg = CashRegisterEntity.builder()
                            .shop(shop)
                            .label("Caisse Principale")
                            .active(true)
                            .build();
                    return registerRepository.save(reg);
                });
    }

    public List<CashDTO.RegisterResponse> listRegisters(UUID shopId) {
        List<CashRegisterEntity> registers = registerRepository.findByShopIdAndActiveTrue(shopId);
        return registers.stream().map(r -> {
            Optional<CashRegisterSessionEntity> activeSession =
                    sessionRepository.findActiveSession(r.getId(), ACTIVE_STATUSES);
            return CashDTO.RegisterResponse.builder()
                    .id(r.getId())
                    .shopId(shopId)
                    .label(r.getLabel())
                    .active(r.isActive())
                    .activeSession(activeSession.map(this::toSessionSummary).orElse(null))
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional
    public CashDTO.RegisterResponse createRegister(UUID shopId, CashDTO.CreateRegisterRequest request) {
        ShopEntity shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Boutique introuvable"));
        CashRegisterEntity reg = CashRegisterEntity.builder()
                .shop(shop)
                .label(request.getLabel())
                .active(true)
                .build();
        reg = registerRepository.save(reg);
        return CashDTO.RegisterResponse.builder()
                .id(reg.getId())
                .shopId(shopId)
                .label(reg.getLabel())
                .active(reg.isActive())
                .build();
    }

    // ──────────────── Session Lifecycle ────────────────

    /**
     * Ouvre manuellement une session de caisse avec saisie du fonds initial.
     */
    @Transactional
    public CashDTO.SessionSummary openSession(UUID registerId, UUID personId, CashDTO.OpenSessionRequest request) {
        validateNonNegative(request.getOpeningCashAmount(), "fonds de caisse initial");
        PersonEntity person = findPerson(personId);
        CashRegisterEntity register = findRegister(registerId);

        // Vérifier pas de session active
        Optional<CashRegisterSessionEntity> existing = sessionRepository.findActiveSessionForUpdate(registerId, ACTIVE_STATUSES);
        if (existing.isPresent()) {
            CashRegisterSessionEntity existingSession = existing.get();
            // Si la session est d'un autre caissier -> relève d'équipe
            if (!existingSession.getOpenedBy().getId().equals(personId)) {
                return handleHandoverDetection(register, existingSession, person);
            }
            throw new IllegalStateException("Une session est déjà active sur cette caisse");
        }

        CashRegisterSessionEntity session = CashRegisterSessionEntity.builder()
                .register(register)
                .openedBy(person)
                .openedAt(LocalDateTime.now())
                .openingCashAmount(request.getOpeningCashAmount())
                .status(CashSessionStatusEnum.OPEN)
                .build();
        session = sessionRepository.save(session);
        return toSessionSummary(session);
    }

    /**
     * Clôture manuelle d'une session : l'utilisateur déclare le montant physique.
     */
    @Transactional
    public CashDTO.SessionSummary closeSession(UUID sessionId, UUID personId, CashDTO.CloseSessionRequest request) {
        validateNonNegative(request.getClosingDeclaredAmount(), "montant déclaré de clôture");
        CashRegisterSessionEntity session = findSession(sessionId);

        if (session.getStatus() != CashSessionStatusEnum.OPEN &&
            session.getStatus() != CashSessionStatusEnum.PENDING_INITIAL_CASH) {
            throw new IllegalStateException("Cette session n'est pas active et ne peut pas être clôturée");
        }

        PersonEntity person = findPerson(personId);
        BigDecimal netMovements = movementRepository.computeNetMovements(sessionId);
        BigDecimal computedAmount = session.getOpeningCashAmount().add(netMovements);
        BigDecimal discrepancy = request.getClosingDeclaredAmount().subtract(computedAmount);

        session.setClosedBy(person);
        session.setClosedAt(LocalDateTime.now());
        session.setClosingDeclaredAmount(request.getClosingDeclaredAmount());
        session.setClosingComputedAmount(computedAmount);
        session.setDiscrepancy(discrepancy);

        if (session.getStatus() == CashSessionStatusEnum.PENDING_INITIAL_CASH) {
            // Session jamais régularisée, clôture comme "réconciliation"
            session.setStatus(CashSessionStatusEnum.RESOLVED_BY_RECONCILIATION);
        } else {
            session.setStatus(CashSessionStatusEnum.CLOSED);
        }

        sessionRepository.save(session);
        return toSessionSummary(session);
    }

    /**
     * Régularise le fonds de caisse initial pour une session PENDING_INITIAL_CASH.
     */
    @Transactional
    public CashDTO.SessionSummary regularizeInitialCash(UUID sessionId, UUID personId, CashDTO.RegularizeInitialCashRequest request) {
        validateNonNegative(request.getActualOpeningCashAmount(), "fonds de caisse initial réel");
        CashRegisterSessionEntity session = findSession(sessionId);

        if (session.getStatus() != CashSessionStatusEnum.PENDING_INITIAL_CASH) {
            throw new IllegalStateException("Cette session n'est pas en attente de régularisation du fonds initial");
        }

        session.setOpeningCashAmount(request.getActualOpeningCashAmount());
        session.setStatus(CashSessionStatusEnum.OPEN);
        sessionRepository.save(session);

        log.info("Session {} régularisée : fonds initial réel = {}", sessionId, request.getActualOpeningCashAmount());
        return toSessionSummary(session);
    }

    // ──────────────── Frictionless Auto-Open & Cash Sale ────────────────

    /**
     * Appelé lors de la validation d'une facture CASH.
     * Crée automatiquement un CashMovement VENTE_CASH, en ouvrant une session si besoin.
     */
    @Transactional
    public void recordCashSale(UUID shopId, UUID personId, InvoiceEntity invoice) {
        CashRegisterEntity register = getOrCreateDefaultRegister(shopId);
        PersonEntity person = findPerson(personId);

        CashRegisterSessionEntity session = getOrCreateActiveSession(register, person);

        CashMovementEntity movement = CashMovementEntity.builder()
                .session(session)
                .type(CashMovementTypeEnum.VENTE_CASH)
                .amount(invoice.getTotalAmount())
                .referenceInvoice(invoice)
                .effectuePar(person)
                .timestamp(LocalDateTime.now())
                .build();
        movementRepository.save(movement);

        log.info("Mouvement VENTE_CASH enregistré : {} FCFA, facture {}, session {}",
                invoice.getTotalAmount(), invoice.getId(), session.getId());
    }

    /**
     * Obtient ou crée une session active pour le vendeur sur la caisse spécifiée.
     * Gère :
     * - Session existante du même vendeur -> réutiliser
     * - Session existante d'un autre vendeur -> relève d'équipe
     * - Pas de session -> auto-ouverture en PENDING_INITIAL_CASH
     * - Concurrence (deux ventes simultanées) -> capture DataIntegrityViolationException
     */
    private CashRegisterSessionEntity getOrCreateActiveSession(CashRegisterEntity register, PersonEntity person) {
        Optional<CashRegisterSessionEntity> existing =
                sessionRepository.findActiveSessionForUpdate(register.getId(), ACTIVE_STATUSES);

        if (existing.isPresent()) {
            CashRegisterSessionEntity session = existing.get();
            // Même caissier : réutiliser
            if (session.getOpenedBy().getId().equals(person.getId())) {
                return session;
            }
            // Autre caissier sur la MÊME caisse : relève d'équipe oubliée
            return performHandoverTransition(register, session, person);
        }

        // Pas de session active : auto-ouverture
        return autoOpenSession(register, person);
    }

    /**
     * Auto-ouverture frictionless : crée une session PENDING_INITIAL_CASH avec fonds initial à 0.
     * Si une DataIntegrityViolationException survient (concurrence), on rattache à la session existante.
     */
    private CashRegisterSessionEntity autoOpenSession(CashRegisterEntity register, PersonEntity person) {
        try {
            CashRegisterSessionEntity session = CashRegisterSessionEntity.builder()
                    .register(register)
                    .openedBy(person)
                    .openedAt(LocalDateTime.now())
                    .openingCashAmount(BigDecimal.ZERO)
                    .status(CashSessionStatusEnum.PENDING_INITIAL_CASH)
                    .build();
            session = sessionRepository.saveAndFlush(session);
            log.info("Session auto-ouverte (PENDING_INITIAL_CASH) sur caisse {} par {}",
                    register.getLabel(), person.getName());
            return session;
        } catch (DataIntegrityViolationException e) {
            // Concurrence : une autre vente a créé la session entre-temps
            log.warn("Conflit de concurrence à l'ouverture de session sur caisse {}. Rattachement à la session existante.",
                    register.getLabel());
            return sessionRepository.findActiveSession(register.getId(), ACTIVE_STATUSES)
                    .orElseThrow(() -> new IllegalStateException("Session introuvable après conflit de concurrence"));
        }
    }

    // ──────────────── Handover (Relève d'équipe) ────────────────

    /**
     * Détecte une relève oubliée et fait la transition atomique :
     * 1. Session A -> PENDING_HANDOVER_CLOSURE
     * 2. Nouvelle session B -> PENDING_INITIAL_CASH
     */
    @Transactional
    private CashRegisterSessionEntity performHandoverTransition(
            CashRegisterEntity register,
            CashRegisterSessionEntity previousSession,
            PersonEntity newPerson) {

        log.info("Relève d'équipe détectée sur caisse {} : {} remplace {}",
                register.getLabel(), newPerson.getName(), previousSession.getOpenedBy().getName());

        // 1. Basculer la session précédente en PENDING_HANDOVER_CLOSURE
        previousSession.setStatus(CashSessionStatusEnum.PENDING_HANDOVER_CLOSURE);
        sessionRepository.save(previousSession);

        // 2. Créer la nouvelle session
        CashRegisterSessionEntity newSession = CashRegisterSessionEntity.builder()
                .register(register)
                .openedBy(newPerson)
                .openedAt(LocalDateTime.now())
                .openingCashAmount(BigDecimal.ZERO)
                .status(CashSessionStatusEnum.PENDING_INITIAL_CASH)
                .build();
        newSession = sessionRepository.save(newSession);

        return newSession;
    }

    /**
     * Appelé quand l'UI envoie la relève manuelle d'ouverture.
     */
    private CashDTO.SessionSummary handleHandoverDetection(
            CashRegisterEntity register,
            CashRegisterSessionEntity previousSession,
            PersonEntity newPerson) {
        CashRegisterSessionEntity newSession = performHandoverTransition(register, previousSession, newPerson);
        return toSessionSummary(newSession);
    }

    /**
     * Résolution du comptage après relève d'équipe.
     * Après que B a compté le tiroir-caisse à l'instant countedAt :
     * - Calcule la somme nette des mouvements de B jusqu'à countedAt
     * - Déduit le montant déclaré de A
     * - Si négatif : bloque et alerte
     * - Si >= 0 : ferme A (CLOSED_BY_SYSTEM) et ouvre B (OPEN)
     */
    @Transactional
    public CashDTO.SessionSummary resolveHandover(UUID pendingSessionId, UUID currentSessionId, UUID personId, CashDTO.HandoverCountRequest request) {
        validateNonNegative(request.getCountedAmount(), "montant compté");

        CashRegisterSessionEntity pendingSession = findSession(pendingSessionId);
        CashRegisterSessionEntity currentSession = findSession(currentSessionId);

        if (pendingSession.getStatus() != CashSessionStatusEnum.PENDING_HANDOVER_CLOSURE) {
            throw new IllegalStateException("La session précédente n'est pas en attente de clôture de relève");
        }
        if (currentSession.getStatus() != CashSessionStatusEnum.PENDING_INITIAL_CASH) {
            throw new IllegalStateException("La session courante n'est pas en attente de régularisation");
        }

        LocalDateTime countedAt = request.getCountedAt();
        BigDecimal countedAmount = request.getCountedAmount();

        // Somme nette des mouvements de B jusqu'à countedAt strictement
        BigDecimal netMovementsOfB = movementRepository.computeNetMovementsUntil(currentSession.getId(), countedAt);

        // Montant déclaré pour A = montant compté - mouvements de B
        BigDecimal declaredForA = countedAmount.subtract(netMovementsOfB);

        // Vérification : impossible d'avoir un montant négatif dans un tiroir-caisse
        if (declaredForA.compareTo(BigDecimal.ZERO) < 0) {
            log.error("ALERTE HAUTE : Montant négatif calculé pour la session {} lors de la relève. " +
                      "Compté={}, Mouvements B={}, Résultat={}", 
                      pendingSessionId, countedAmount, netMovementsOfB, declaredForA);
            throw new IllegalStateException(
                    "Le calcul de résolution produit un montant négatif (" + declaredForA +
                    " FCFA). C'est physiquement impossible. Intervention manuelle d'un gérant requise.");
        }

        // Calcul du closingComputedAmount de A
        BigDecimal netMovementsOfA = movementRepository.computeNetMovements(pendingSession.getId());
        BigDecimal computedForA = pendingSession.getOpeningCashAmount().add(netMovementsOfA);
        BigDecimal discrepancyForA = declaredForA.subtract(computedForA);

        // Fermeture de A
        pendingSession.setClosingDeclaredAmount(declaredForA);
        pendingSession.setClosingComputedAmount(computedForA);
        pendingSession.setDiscrepancy(discrepancyForA);
        pendingSession.setClosedAt(LocalDateTime.now());
        pendingSession.setClosedBy(findPerson(personId));
        pendingSession.setStatus(CashSessionStatusEnum.CLOSED_BY_SYSTEM);
        sessionRepository.save(pendingSession);

        // Ouverture effective de B avec le fonds initial vérifié
        currentSession.setOpeningCashAmount(declaredForA);
        currentSession.setStatus(CashSessionStatusEnum.OPEN);
        sessionRepository.save(currentSession);

        log.info("Relève résolue : Session {} -> CLOSED_BY_SYSTEM (déclaré={}), Session {} -> OPEN (fonds initial={})",
                pendingSessionId, declaredForA, currentSessionId, declaredForA);

        return toSessionSummary(currentSession);
    }

    // ──────────────── Manual Movements ────────────────

    /**
     * Ajoute un mouvement manuel (RETRAIT, APPOINT, DEPENSE, CORRECTION, REMBOURSEMENT).
     */
    @Transactional
    public CashDTO.MovementResponse addMovement(UUID sessionId, UUID personId, CashDTO.CreateMovementRequest request) {
        CashRegisterSessionEntity session = findSession(sessionId);
        PersonEntity person = findPerson(personId);

        if (session.getStatus() != CashSessionStatusEnum.OPEN &&
            session.getStatus() != CashSessionStatusEnum.PENDING_INITIAL_CASH) {
            throw new IllegalStateException("Impossible d'ajouter un mouvement à une session non active");
        }

        // Validation du motif
        if (request.getType() == CashMovementTypeEnum.RETRAIT ||
            request.getType() == CashMovementTypeEnum.DEPENSE ||
            request.getType() == CashMovementTypeEnum.CORRECTION) {
            if (request.getReason() == null || request.getReason().isBlank()) {
                throw new IllegalArgumentException("Le motif est obligatoire pour les mouvements de type " + request.getType());
            }
        }

        // Validation de la référence facture pour les remboursements
        InvoiceEntity referenceInvoice = null;
        if (request.getType() == CashMovementTypeEnum.REMBOURSEMENT) {
            if (request.getReferenceInvoiceId() == null) {
                throw new IllegalArgumentException("La référence de la facture d'origine est obligatoire pour un remboursement");
            }
            referenceInvoice = invoiceRepository.findById(request.getReferenceInvoiceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Facture de référence introuvable"));
        }

        CashMovementEntity movement = CashMovementEntity.builder()
                .session(session)
                .type(request.getType())
                .amount(request.getAmount())
                .reason(request.getReason())
                .referenceInvoice(referenceInvoice)
                .effectuePar(person)
                .timestamp(LocalDateTime.now())
                .build();
        movement = movementRepository.save(movement);

        log.info("Mouvement {} enregistré : {} FCFA, session {}", request.getType(), request.getAmount(), sessionId);
        return toMovementResponse(movement);
    }

    /**
     * Enregistre un remboursement sur la session active de la caisse où il a lieu.
     */
    @Transactional
    public CashDTO.MovementResponse recordRefund(UUID shopId, UUID personId, UUID invoiceId, BigDecimal amount) {
        CashRegisterEntity register = getOrCreateDefaultRegister(shopId);
        PersonEntity person = findPerson(personId);
        InvoiceEntity invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Facture introuvable"));

        CashRegisterSessionEntity session = getOrCreateActiveSession(register, person);

        CashMovementEntity movement = CashMovementEntity.builder()
                .session(session)
                .type(CashMovementTypeEnum.REMBOURSEMENT)
                .amount(amount)
                .reason("Remboursement facture " + invoice.getInvoiceNumber())
                .referenceInvoice(invoice)
                .effectuePar(person)
                .timestamp(LocalDateTime.now())
                .build();
        movement = movementRepository.save(movement);

        log.info("REMBOURSEMENT enregistré : {} FCFA, facture {}, session {}",
                amount, invoiceId, session.getId());
        return toMovementResponse(movement);
    }

    // ──────────────── Queries ────────────────

    public CashDTO.SessionDetailResponse getSessionDetail(UUID sessionId) {
        CashRegisterSessionEntity session = findSession(sessionId);
        List<CashMovementEntity> movements = movementRepository.findBySessionIdOrderByTimestampAsc(sessionId);
        BigDecimal netMovements = movementRepository.computeNetMovements(sessionId);
        BigDecimal currentBalance = session.getOpeningCashAmount().add(netMovements);

        return CashDTO.SessionDetailResponse.builder()
                .session(toSessionSummary(session))
                .movements(movements.stream().map(this::toMovementResponse).collect(Collectors.toList()))
                .currentBalance(currentBalance)
                .build();
    }

    public List<CashDTO.SessionSummary> listSessions(UUID shopId) {
        return sessionRepository.findByShopIdOrderByOpenedAtDesc(shopId)
                .stream()
                .map(this::toSessionSummary)
                .collect(Collectors.toList());
    }

    /**
     * Statut de la caisse pour le dashboard/bandeau frontend.
     */
    public CashDTO.CashStatusResponse getCashStatus(UUID shopId) {
        CashRegisterEntity register = getOrCreateDefaultRegister(shopId);
        Optional<CashRegisterSessionEntity> activeSession =
                sessionRepository.findActiveSession(register.getId(), ACTIVE_STATUSES);

        List<CashRegisterSessionEntity> pendingHandovers =
                sessionRepository.findByRegisterIdAndStatus(register.getId(), CashSessionStatusEnum.PENDING_HANDOVER_CLOSURE);

        CashDTO.CashStatusResponse.CashStatusResponseBuilder builder = CashDTO.CashStatusResponse.builder()
                .hasActiveSession(activeSession.isPresent())
                .pendingInitialCash(activeSession.map(s -> s.getStatus() == CashSessionStatusEnum.PENDING_INITIAL_CASH).orElse(false))
                .hasPendingHandover(!pendingHandovers.isEmpty())
                .activeSession(activeSession.map(this::toSessionSummary).orElse(null));

        if (!pendingHandovers.isEmpty()) {
            CashRegisterSessionEntity pending = pendingHandovers.get(0);
            builder.pendingHandoverSessionId(pending.getId())
                   .pendingHandoverPreviousUserName(pending.getOpenedBy().getName());
        }

        return builder.build();
    }

    // ──────────────── Auto-Close (fin de journée) ────────────────

    /**
     * Clôture automatique des sessions restées ouvertes à la fin de la journée.
     * Appelé par un scheduler à 23h59.
     */
    @Transactional
    public void autoCloseEndOfDay() {
        List<CashSessionStatusEnum> toClose = List.of(
                CashSessionStatusEnum.OPEN,
                CashSessionStatusEnum.PENDING_INITIAL_CASH,
                CashSessionStatusEnum.PENDING_HANDOVER_CLOSURE
        );

        List<CashRegisterSessionEntity> sessions = sessionRepository.findByStatusIn(toClose);
        for (CashRegisterSessionEntity session : sessions) {
            autoCloseSession(session);
        }
    }

    private void autoCloseSession(CashRegisterSessionEntity session) {
        BigDecimal netMovements = movementRepository.computeNetMovements(session.getId());
        BigDecimal computedAmount = session.getOpeningCashAmount().add(netMovements);

        session.setClosingComputedAmount(computedAmount);
        session.setClosedAt(LocalDateTime.now());

        if (session.getStatus() == CashSessionStatusEnum.PENDING_INITIAL_CASH) {
            // Jamais vérifiée physiquement
            session.setDiscrepancy(null);
            session.setStatus(CashSessionStatusEnum.RESOLVED_BY_RECONCILIATION);
        } else if (session.getStatus() == CashSessionStatusEnum.PENDING_HANDOVER_CLOSURE) {
            session.setDiscrepancy(null);
            session.setStatus(CashSessionStatusEnum.CLOSED_BY_SYSTEM);
        } else {
            // OPEN -> clôture automatique sans montant déclaré
            session.setDiscrepancy(null);
            session.setStatus(CashSessionStatusEnum.CLOSED_BY_SYSTEM);
        }

        sessionRepository.save(session);
        log.info("Session {} clôturée automatiquement en fin de journée (status={})",
                session.getId(), session.getStatus());
    }

    // ──────────────── Helpers ────────────────

    private void validateNonNegative(BigDecimal amount, String field) {
        if (amount != null && amount.compareTo(BigDecimal.ZERO) < 0) {
            log.error("ALERTE HAUTE : tentative d'écriture d'un montant négatif pour '{}' : {}", field, amount);
            throw new IllegalArgumentException(
                    "Le montant '" + field + "' ne peut pas être négatif (" + amount + " FCFA). " +
                    "Résolution manuelle requise.");
        }
    }

    private PersonEntity findPerson(UUID id) {
        return personRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Personne introuvable"));
    }

    private CashRegisterEntity findRegister(UUID id) {
        return registerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Caisse introuvable"));
    }

    private CashRegisterSessionEntity findSession(UUID id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session de caisse introuvable"));
    }

    private CashDTO.SessionSummary toSessionSummary(CashRegisterSessionEntity s) {
        return CashDTO.SessionSummary.builder()
                .id(s.getId())
                .registerId(s.getRegister().getId())
                .registerLabel(s.getRegister().getLabel())
                .status(s.getStatus())
                .openedByName(s.getOpenedBy().getName())
                .openedAt(s.getOpenedAt())
                .openingCashAmount(s.getOpeningCashAmount())
                .closedByName(s.getClosedBy() != null ? s.getClosedBy().getName() : null)
                .closedAt(s.getClosedAt())
                .closingDeclaredAmount(s.getClosingDeclaredAmount())
                .closingComputedAmount(s.getClosingComputedAmount())
                .discrepancy(s.getDiscrepancy())
                .build();
    }

    private CashDTO.MovementResponse toMovementResponse(CashMovementEntity m) {
        return CashDTO.MovementResponse.builder()
                .id(m.getId())
                .sessionId(m.getSession().getId())
                .type(m.getType())
                .amount(m.getAmount())
                .reason(m.getReason())
                .referenceInvoiceId(m.getReferenceInvoice() != null ? m.getReferenceInvoice().getId() : null)
                .effectueParName(m.getEffectuePar().getName())
                .timestamp(m.getTimestamp())
                .build();
    }
}
