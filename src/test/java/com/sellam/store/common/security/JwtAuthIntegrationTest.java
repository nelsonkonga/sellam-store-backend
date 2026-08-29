package com.sellam.store.common.security;

import com.sellam.store.auth.JwtProviderNew;
import com.sellam.store.identity.models.PersonEntity;
import com.sellam.store.identity.models.ShopMembershipEntity;
import com.sellam.store.identity.repositories.PersonRepository;
import com.sellam.store.identity.repositories.ShopMembershipRepository;
import com.sellam.store.shops.models.ShopEntity;
import com.sellam.store.users.models.RoleEnum;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Test d'intégration complet : génération de token → authentification via filtre.
 * 
 * Scénario du quotidien après le switch :
 * 1. Vrai JwtProviderNew.generateToken() génère un token JWT réel
 * 2. Token est passé dans une requête HTTP simulée
 * 3. Vrai JwtAuthFilterNew traite la requête
 * 4. Authentification résolue dans SecurityContextHolder
 * 
 * IMPORTANT : Utilise les VRAIS composants (JwtProviderNew, JwtAuthFilterNew) non mockés,
 * seule la base de données est mockée via repositories.
 */
@org.junit.jupiter.api.extension.ExtendWith(MockitoExtension.class)
class JwtAuthIntegrationTest
{

    private JwtProviderNew jwtProviderNew;
    private JwtAuthFilterNew jwtAuthFilterNew;

    @Mock
    private PersonRepository personRepository;

    @Mock
    private ShopMembershipRepository shopMembershipRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private UUID personId;
    private UUID shopId;
    private PersonEntity person;
    private ShopEntity shop;
    private ShopMembershipEntity membership;

    private static final String TEST_SECRET = "test-secret-key-test-secret-key-test-secret-key";

    @BeforeEach
    void setUp()
    {
        // Configurer le vrai JwtProviderNew avec le secret de test via reflection
        jwtProviderNew = new JwtProviderNew();
        try
        {
            java.lang.reflect.Field secretField = JwtProviderNew.class.getDeclaredField("secretkey");
            secretField.setAccessible(true);
            secretField.set(jwtProviderNew, TEST_SECRET);

            java.lang.reflect.Field expirationField = JwtProviderNew.class.getDeclaredField("expirationMs");
            expirationField.setAccessible(true);
            expirationField.set(jwtProviderNew, 86400000L);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to configure JwtProviderNew for test", e);
        }

        // Configurer le vrai JwtAuthFilterNew avec les repositories mockés
        jwtAuthFilterNew = new JwtAuthFilterNew(jwtProviderNew, personRepository, shopMembershipRepository);

        // Données de test
        personId = UUID.randomUUID();
        shopId = UUID.randomUUID();

        person = PersonEntity.builder()
                .id(personId)
                .name("Test Person")
                .phoneNumber("+33612345678")
                .email("test@example.com")
                .build();

        shop = ShopEntity.builder()
                .id(shopId)
                .name("Test Shop")
                .build();

        membership = ShopMembershipEntity.builder()
                .id(UUID.randomUUID())
                .person(person)
                .shop(shop)
                .role(RoleEnum.CASHIER)
                .active(true)
                .joinedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void realTokenGenerationAndAuthentication_ShouldWorkEndToEnd() throws Exception
    {
        // Given
        // 1. Générer un VRAI token avec le vrai JwtProviderNew.generateToken()
        String realToken = jwtProviderNew.generateToken(personId, "USER", shopId, "+33612345678");

        // Vérifier que le token est valide
        assertTrue(jwtProviderNew.validateToken(realToken), "Generated token should be valid");

        // 2. Configurer les repositories mockés pour simuler la base de données
        lenient().when(personRepository.findById(personId)).thenReturn(Optional.of(person));
        lenient().when(shopMembershipRepository.findActiveMembership(personId, shopId)).thenReturn(Optional.of(membership));

        // 3. Simuler une requête HTTP avec le token réel
        when(request.getHeader("Authorization")).thenReturn("Bearer " + realToken);

        // When
        // 4. Faire passer la requête dans le vrai JwtAuthFilterNew (non mocké)
        jwtAuthFilterNew.doFilterInternal(request, response, filterChain);

        // Then
        // 5. Vérifier que l'authentification est résolue dans SecurityContextHolder
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth, "Authentication should be present");
        
        // Le principal est l'objet AuthPrincipal (pas juste l'ID)
        Object principal = auth.getPrincipal();
        assertTrue(principal instanceof AuthPrincipal, "Principal should be AuthPrincipal");
        
        AuthPrincipal authPrincipal = (AuthPrincipal) principal;
        assertEquals(personId, authPrincipal.getId(), "Principal ID should be the person ID");
        assertEquals("USER", authPrincipal.getUserType(), "User type should be USER");
        assertEquals(shopId, authPrincipal.getShopId(), "Shop ID should match");

        // 6. Vérifier que les permissions sont correctes
        assertTrue(auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("VIEW_PRODUCTS")), 
                "Should have VIEW_PRODUCTS permission");
        assertTrue(auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("CREATE_INVOICE")), 
                "Should have CREATE_INVOICE permission");
        assertFalse(auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("PERM_ALL")), 
                "Should NOT have PERM_ALL (CASHIER role)");

        // 7. Vérifier que le filtre a continué la chaîne
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void realTokenGenerationWithoutShopId_ShouldWorkForAccountType() throws Exception
    {
        // Given
        // Générer un token pour un ACCOUNT (sans shopId)
        String realToken = jwtProviderNew.generateToken(personId, "ACCOUNT", null, "+33612345678");

        assertTrue(jwtProviderNew.validateToken(realToken), "Generated token should be valid");

        // Configurer pour un ACCOUNT sans memberships
        lenient().when(personRepository.findById(personId)).thenReturn(Optional.of(person));
        lenient().when(shopMembershipRepository.findByPersonId(personId)).thenReturn(List.of());

        when(request.getHeader("Authorization")).thenReturn("Bearer " + realToken);

        // When
        jwtAuthFilterNew.doFilterInternal(request, response, filterChain);

        // Then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        
        // Le principal est l'objet AuthPrincipal (pas juste l'ID)
        Object principal = auth.getPrincipal();
        assertTrue(principal instanceof AuthPrincipal, "Principal should be AuthPrincipal");
        
        AuthPrincipal authPrincipal = (AuthPrincipal) principal;
        assertEquals(personId, authPrincipal.getId(), "Principal ID should be the person ID");
        assertEquals("ACCOUNT", authPrincipal.getUserType(), "User type should be ACCOUNT");
        assertNull(authPrincipal.getShopId(), "Shop ID should be null for ACCOUNT");

        // ACCOUNT doit avoir PERM_ALL
        assertTrue(auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("PERM_ALL")), 
                "ACCOUNT should have PERM_ALL");

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void realTokenClaims_ShouldMatchOriginalValues()
    {
        // Given
        String realToken = jwtProviderNew.generateToken(personId, "USER", shopId, "+33612345678");

        // When
        AuthPrincipal principal = jwtProviderNew.getPrincipalFromToken(realToken);

        // Then
        assertNotNull(principal);
        assertEquals(personId, principal.getId());
        assertEquals("USER", principal.getUserType());
        assertEquals(shopId, principal.getShopId());
        assertEquals("+33612345678", principal.getPhoneNumber());
    }

    @Test
    void realTokenExpiration_ShouldBeValid()
    {
        // Given
        String realToken = jwtProviderNew.generateToken(personId, "USER", shopId, "+33612345678");

        // When & Then
        // Le token devrait être valide pour 24h (configuré dans setUp)
        assertTrue(jwtProviderNew.validateToken(realToken), "Token should be valid");
    }
}
