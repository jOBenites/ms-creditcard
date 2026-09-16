package com.bank.mscreditcard.service;

import com.bank.mscreditcard.event.CreditCardEventProducer;
import com.bank.mscreditcard.model.CreditCard;
import com.bank.mscreditcard.model.Movement;
import com.bank.mscreditcard.repository.CreditCardRepository;
import com.bank.mscreditcard.repository.MovementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias para {@link MovementService} (tarjetas de credito).
 * Valida registro de consumos, validaciones de saldo y estado de la tarjeta.
 */
@ExtendWith(MockitoExtension.class)
class MovementServiceTest {

    @Mock
    private CreditCardRepository creditCardRepository;

    @Mock
    private MovementRepository movementRepository;

    @Mock
    private CreditCardEventProducer creditCardEventProducer;

    @InjectMocks
    private MovementService movementService;

    private CreditCard activeCard;
    private Movement chargeMovement;

    @BeforeEach
    void setUp() {
        activeCard = new CreditCard("cust-1", CreditCard.TYPE_PERSONAL, new BigDecimal("5000.00"));
        activeCard.setId("cc-1");
        activeCard.setAvailableBalance(new BigDecimal("5000.00"));

        chargeMovement = new Movement("cc-1", Movement.TYPE_CARD_CHARGE, new BigDecimal("200.00"));
        chargeMovement.setId("mov-1");
    }

    @Test
    void charge_success() {
        when(creditCardRepository.findById("cc-1")).thenReturn(Mono.just(activeCard));
        when(creditCardRepository.save(any(CreditCard.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(movementRepository.save(any(Movement.class))).thenReturn(Mono.just(chargeMovement));

        StepVerifier.create(movementService.charge("cc-1", new BigDecimal("200.00")))
                .assertNext(movement -> {
                    assertNotNull(movement);
                    assertEquals(new BigDecimal("4800.00"), activeCard.getAvailableBalance());
                })
                .verifyComplete();

        verify(creditCardEventProducer).publishMovementRecorded(chargeMovement, CreditCard.TYPE_PERSONAL);
    }

    @Test
    void charge_cardNotFound_returnsEmpty() {
        when(creditCardRepository.findById("nonexistent")).thenReturn(Mono.empty());

        StepVerifier.create(movementService.charge("nonexistent", new BigDecimal("200.00")))
                .verifyComplete();

        verify(movementRepository, never()).save(any());
    }

    @Test
    void charge_nonPositiveAmount_throws() {
        StepVerifier.create(movementService.charge("cc-1", BigDecimal.ZERO))
                .expectError(IllegalArgumentException.class)
                .verify();

        StepVerifier.create(movementService.charge("cc-1", new BigDecimal("-100")))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void charge_inactiveCard_throws() {
        activeCard.setStatus("CANCELLED");
        when(creditCardRepository.findById("cc-1")).thenReturn(Mono.just(activeCard));

        StepVerifier.create(movementService.charge("cc-1", new BigDecimal("200.00")))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void charge_amountExceedsAvailableBalance_throws() {
        when(creditCardRepository.findById("cc-1")).thenReturn(Mono.just(activeCard));

        StepVerifier.create(movementService.charge("cc-1", new BigDecimal("10000.00")))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void findMovements_cardNotFound_returnsEmpty() {
        when(creditCardRepository.existsById("nonexistent")).thenReturn(Mono.just(false));

        StepVerifier.create(movementService.findMovements("nonexistent"))
                .verifyComplete();
    }

    @Test
    void findMovements_cardExists_returnsMovements() {
        when(creditCardRepository.existsById("cc-1")).thenReturn(Mono.just(true));
        when(movementRepository.findByCardIdOrderByOccurredAtDesc("cc-1"))
                .thenReturn(Flux.just(chargeMovement));

        StepVerifier.create(movementService.findMovements("cc-1"))
                .assertNext(movement -> assertEquals("mov-1", movement.getId()))
                .verifyComplete();
    }

    @Test
    void toMovementResponse_mapsFields() {
        var response = movementService.toMovementResponse(chargeMovement);

        assertEquals("mov-1", response.getId());
        assertEquals("cc-1", response.getCardId());
        assertEquals(Movement.TYPE_CARD_CHARGE, response.getMovementType());
        assertEquals(new BigDecimal("200.00"), response.getAmount());
        assertNotNull(response.getOccurredAt());
    }
}
