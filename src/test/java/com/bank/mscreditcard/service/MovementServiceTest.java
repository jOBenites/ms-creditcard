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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
        when(creditCardRepository.findById("cc-1")).thenReturn(Optional.of(activeCard));
        when(creditCardRepository.save(any(CreditCard.class))).thenAnswer(inv -> inv.getArgument(0));
        when(movementRepository.save(any(Movement.class))).thenReturn(chargeMovement);

        Optional<Movement> result = movementService.charge("cc-1", new BigDecimal("200.00"));

        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("4800.00"), activeCard.getAvailableBalance());
        verify(creditCardEventProducer).publishMovementRecorded(chargeMovement, CreditCard.TYPE_PERSONAL);
    }

    @Test
    void charge_cardNotFound_returnsEmpty() {
        when(creditCardRepository.findById("nonexistent")).thenReturn(Optional.empty());

        Optional<Movement> result = movementService.charge("nonexistent", new BigDecimal("200.00"));

        assertFalse(result.isPresent());
        verify(movementRepository, never()).save(any());
    }

    @Test
    void charge_nonPositiveAmount_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> movementService.charge("cc-1", BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> movementService.charge("cc-1", new BigDecimal("-100")));
        verify(creditCardRepository, never()).findById(anyString());
    }

    @Test
    void charge_inactiveCard_throws() {
        activeCard.setStatus("CANCELLED");
        when(creditCardRepository.findById("cc-1")).thenReturn(Optional.of(activeCard));

        assertThrows(IllegalArgumentException.class,
                () -> movementService.charge("cc-1", new BigDecimal("200.00")));
        verify(movementRepository, never()).save(any());
    }

    @Test
    void charge_amountExceedsAvailableBalance_throws() {
        when(creditCardRepository.findById("cc-1")).thenReturn(Optional.of(activeCard));

        assertThrows(IllegalArgumentException.class,
                () -> movementService.charge("cc-1", new BigDecimal("10000.00")));
        verify(movementRepository, never()).save(any());
    }

    @Test
    void findMovements_cardNotFound_returnsEmpty() {
        when(creditCardRepository.existsById("nonexistent")).thenReturn(false);

        Optional<List<Movement>> result = movementService.findMovements("nonexistent");

        assertFalse(result.isPresent());
        verify(movementRepository, never()).findByCardIdOrderByOccurredAtDesc(anyString());
    }

    @Test
    void findMovements_cardExists_returnsMovements() {
        when(creditCardRepository.existsById("cc-1")).thenReturn(true);
        when(movementRepository.findByCardIdOrderByOccurredAtDesc("cc-1"))
                .thenReturn(List.of(chargeMovement));

        Optional<List<Movement>> result = movementService.findMovements("cc-1");

        assertTrue(result.isPresent());
        assertEquals(1, result.get().size());
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
