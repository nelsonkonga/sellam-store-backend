package com.sellam.store.cash.schedulers;

import com.sellam.store.cash.services.CashSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CashSessionScheduler {

    private final CashSessionService cashSessionService;

    /**
     * S'exécute tous les jours à 23h59 pour clôturer les sessions de caisse restées actives.
     */
    @Scheduled(cron = "0 59 23 * * *")
    public void autoCloseSessions() {
        log.info("Démarrage de la clôture automatique des sessions de caisse (fin de journée)...");
        try {
            cashSessionService.autoCloseEndOfDay();
            log.info("Clôture automatique terminée avec succès.");
        } catch (Exception e) {
            log.error("Erreur lors de la clôture automatique des sessions de caisse", e);
        }
    }
}
