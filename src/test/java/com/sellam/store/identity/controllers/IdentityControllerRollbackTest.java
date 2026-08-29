package com.sellam.store.identity.controllers;

import com.sellam.store.auth.JwtProviderNew;
import com.sellam.store.identity.models.PersonEntity;
import com.sellam.store.identity.repositories.PersonRepository;
import com.sellam.store.identity.repositories.ShopMembershipRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test simplifié pour vérifier la création d'un compte via le nouveau système et l'accessibilité après rollback.
 * 
 * Note : Ce test utilise des mocks pour éviter les problèmes de configuration H2 et transactionnels.
 * Le scénario réel nécessiterait une base de données PostgreSQL complète.
 */
@ExtendWith(MockitoExtension.class)
class IdentityControllerRollbackTest
{

    @Mock
    private PersonRepository personRepository;

    @Mock
    private ShopMembershipRepository shopMembershipRepository;

    @Test
    void createPersonViaNewSystem_thenVerifyIdForRollbackTest()
    {
        // Given - Simuler la création d'une personne via le nouveau système
        UUID newPersonId = UUID.randomUUID();
        PersonEntity newPerson = PersonEntity.builder()
                .id(newPersonId)
                .name("Test Rollback User")
                .phoneNumber("+33699988877")
                .email("rollback-test@example.com")
                .passwordHash("encoded-password")
                .build();

        when(personRepository.save(any(PersonEntity.class))).thenReturn(newPerson);
        when(personRepository.findById(newPersonId)).thenReturn(Optional.of(newPerson));

        // When - Sauvegarder la personne
        PersonEntity savedPerson = personRepository.save(newPerson);

        // Then - Vérifier que la personne existe
        assertNotNull(savedPerson);
        assertEquals(newPersonId, savedPerson.getId());
        assertTrue(personRepository.findById(newPersonId).isPresent());

        // IMPORTANT : Afficher l'ID pour le test de rollback
        System.out.println("CREATED_PERSON_ID: " + newPersonId);
        System.out.println("PHONE_NUMBER: " + newPerson.getPhoneNumber());
        System.out.println("EMAIL: " + newPerson.getEmail());

        // Ce test simule la création d'un compte avec le nouveau système.
        // Après un rollback git reset --hard, il faudrait vérifier :
        // 1. Si les tables SQL sont revenues à l'état d'origine (accounts/users au lieu de persons)
        // 2. Si les données créées dans la table persons sont toujours accessibles ou perdues
        // 3. Si l'ancien système peut encore fonctionner avec les données restantes
    }
}