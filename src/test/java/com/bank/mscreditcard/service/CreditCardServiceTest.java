package com.bank.mscreditcard.service;

import com.bank.mscreditcard.dto.CreditCardResponse;
import com.bank.mscreditcard.event.CreditCardEventProducer;
import com.bank.mscreditcard.model.CreditCard;
import com.bank.mscreditcard.model.CustomerView;
import com.bank.mscreditcard.repository.CreditCardRepository;
import com.bank.mscreditcard.repository.CustomerViewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
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
 * Pruebas unitarias para {@link CreditCardService}.
 * Valida las reglas de consistencia de tipo, el CRUD completo
 * y la publicacion de eventos de dominio.
 */
@ExtendWith(MockitoExtension.class)
class CreditCardServiceTest {

    @Mock
    private CreditCardRepository creditCardRepository;

    @Mock
    private CustomerViewRepository customerViewRepository;

    @Mock
    private CreditCardEventProducer creditCardEventProducer;

    @InjectMocks
    private CreditCardService creditCardService;

    private CustomerView personalView;
    private CustomerView businessView;
    private CreditCard personalCard;
    private CreditCard businessCard;

    @BeforeEach
    void setUp() {
        personalView = new CustomerView("cust-1", "PERSONAL", "REGULAR", "12345678");
        businessView = new CustomerView("cust-2", "BUSINESS", "REGULAR", "87654321");

        personalCard = new CreditCard("cust-1", CreditCard.TYPE_PERSONAL, new BigDecimal("5000.00"));
        personalCard.setId("cc-1");

        businessCard = new CreditCard("cust-2", CreditCard.TYPE_BUSINESS, new BigDecimal("20000.00"));
        businessCard.setId("cc-2");
    }

    @Test
    void issueCreditCard_personal_success() {
        when(customerViewRepository.findById("cust-1")).thenReturn(Optional.of(personalView));
        when(creditCardRepository.save(any(CreditCard.class))).thenReturn(personalCard);

        CreditCard result = creditCardService.issueCreditCard("cust-1", CreditCard.TYPE_PERSONAL, new BigDecimal("5000.00"));

        assertNotNull(result);
        assertEquals(CreditCard.TYPE_PERSONAL, result.getCardType());
        assertEquals(CreditCard.STATUS_ACTIVE, result.getStatus());
        assertEquals(result.getCreditLimit(), result.getAvailableBalance());
        verify(creditCardEventProducer).publishCreditCardIssued(personalCard);
    }

    @Test
    void issueCreditCard_business_success() {
        when(customerViewRepository.findById("cust-2")).thenReturn(Optional.of(businessView));
        when(creditCardRepository.save(any(CreditCard.class))).thenReturn(businessCard);

        CreditCard result = creditCardService.issueCreditCard("cust-2", CreditCard.TYPE_BUSINESS, new BigDecimal("20000.00"));

        assertNotNull(result);
        assertEquals(CreditCard.TYPE_BUSINESS, result.getCardType());
        verify(creditCardEventProducer).publishCreditCardIssued(businessCard);
    }

    @Test
    void issueCreditCard_personalCardForBusinessCustomer_throws() {
        when(customerViewRepository.findById("cust-2")).thenReturn(Optional.of(businessView));

        assertThrows(IllegalArgumentException.class, () -> creditCardService.issueCreditCard("cust-2", CreditCard.TYPE_PERSONAL, new BigDecimal("5000.00")));
        verify(creditCardRepository, never()).save(any());
    }

    @Test
    void issueCreditCard_businessCardForPersonalCustomer_throws() {
        when(customerViewRepository.findById("cust-1")).thenReturn(Optional.of(personalView));

        assertThrows(IllegalArgumentException.class, () -> creditCardService.issueCreditCard("cust-1", CreditCard.TYPE_BUSINESS, new BigDecimal("20000.00")));
        verify(creditCardRepository, never()).save(any());
    }

    @Test
    void issueCreditCard_invalidType_throws() {
        when(customerViewRepository.findById("cust-1")).thenReturn(Optional.of(personalView));

        assertThrows(IllegalArgumentException.class, () -> creditCardService.issueCreditCard("cust-1", "PREPAID", new BigDecimal("5000.00")));
        verify(creditCardRepository, never()).save(any());
    }

    @Test
    void issueCreditCard_nonPositiveLimit_throws() {
        assertThrows(IllegalArgumentException.class, () -> creditCardService.issueCreditCard("cust-1", CreditCard.TYPE_PERSONAL, BigDecimal.ZERO));
        verify(customerViewRepository, never()).findById(anyString());
        verify(creditCardRepository, never()).save(any());
    }

    @Test
    void issueCreditCard_customerNotFound_throws() {
        when(customerViewRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> creditCardService.issueCreditCard("unknown", CreditCard.TYPE_PERSONAL, new BigDecimal("5000.00")));
        verify(creditCardRepository, never()).save(any());
    }

    @Test
    void findById_found() {
        when(creditCardRepository.findById("cc-1")).thenReturn(Optional.of(personalCard));

        Optional<CreditCard> result = creditCardService.findById("cc-1");

        assertTrue(result.isPresent());
        assertEquals("cc-1", result.get().getId());
    }

    @Test
    void findById_notFound() {
        when(creditCardRepository.findById("nonexistent")).thenReturn(Optional.empty());

        Optional<CreditCard> result = creditCardService.findById("nonexistent");

        assertFalse(result.isPresent());
    }

    @Test
    void findAll_returnsList() {
        when(creditCardRepository.findAll()).thenReturn(Arrays.asList(personalCard, businessCard));

        List<CreditCard> result = creditCardService.findAll();

        assertEquals(2, result.size());
    }

    @Test
    void update_found_updatesLimit() {
        when(creditCardRepository.findById("cc-1")).thenReturn(Optional.of(personalCard));
        when(creditCardRepository.save(any(CreditCard.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<CreditCard> result = creditCardService.update("cc-1", new BigDecimal("8000.00"));

        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("8000.00"), result.get().getCreditLimit());
    }

    @Test
    void update_found_partialUpdate() {
        when(creditCardRepository.findById("cc-1")).thenReturn(Optional.of(personalCard));
        when(creditCardRepository.save(any(CreditCard.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<CreditCard> result = creditCardService.update("cc-1", null);

        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("5000.00"), result.get().getCreditLimit());
    }

    @Test
    void update_notFound() {
        when(creditCardRepository.findById("nonexistent")).thenReturn(Optional.empty());

        Optional<CreditCard> result = creditCardService.update("nonexistent", new BigDecimal("8000.00"));

        assertFalse(result.isPresent());
        verify(creditCardRepository, never()).save(any());
    }

    @Test
    void delete_found_returnsTrue() {
        when(creditCardRepository.existsById("cc-1")).thenReturn(true);

        boolean result = creditCardService.delete("cc-1");

        assertTrue(result);
        verify(creditCardRepository).deleteById("cc-1");
    }

    @Test
    void delete_notFound_returnsFalse() {
        when(creditCardRepository.existsById("nonexistent")).thenReturn(false);

        boolean result = creditCardService.delete("nonexistent");

        assertFalse(result);
        verify(creditCardRepository, never()).deleteById(anyString());
    }

    @Test
    void toResponse_mapsAllFields() {
        CreditCardResponse response = creditCardService.toResponse(personalCard);

        assertEquals("cc-1", response.getId());
        assertEquals("cust-1", response.getCustomerId());
        assertEquals(CreditCard.TYPE_PERSONAL, response.getCardType());
        assertEquals(new BigDecimal("5000.00"), response.getCreditLimit());
        assertEquals(new BigDecimal("5000.00"), response.getAvailableBalance());
        assertEquals(CreditCard.STATUS_ACTIVE, response.getStatus());
    }

    @Test
    void toResponseList() {
        List<CreditCardResponse> responses = creditCardService.toResponseList(
                Arrays.asList(personalCard, businessCard));

        assertEquals(2, responses.size());
    }
}
