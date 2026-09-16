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
        when(customerViewRepository.findById("cust-1")).thenReturn(Mono.just(personalView));
        when(creditCardRepository.save(any(CreditCard.class))).thenReturn(Mono.just(personalCard));

        StepVerifier.create(creditCardService.issueCreditCard("cust-1", CreditCard.TYPE_PERSONAL,
                        new BigDecimal("5000.00")))
                .assertNext(result -> {
                    assertNotNull(result);
                    assertEquals(CreditCard.TYPE_PERSONAL, result.getCardType());
                    assertEquals(CreditCard.STATUS_ACTIVE, result.getStatus());
                    assertEquals(result.getCreditLimit(), result.getAvailableBalance());
                })
                .verifyComplete();

        verify(creditCardEventProducer).publishCreditCardIssued(personalCard);
    }

    @Test
    void issueCreditCard_business_success() {
        when(customerViewRepository.findById("cust-2")).thenReturn(Mono.just(businessView));
        when(creditCardRepository.save(any(CreditCard.class))).thenReturn(Mono.just(businessCard));

        StepVerifier.create(creditCardService.issueCreditCard("cust-2", CreditCard.TYPE_BUSINESS,
                        new BigDecimal("20000.00")))
                .assertNext(result -> {
                    assertNotNull(result);
                    assertEquals(CreditCard.TYPE_BUSINESS, result.getCardType());
                })
                .verifyComplete();

        verify(creditCardEventProducer).publishCreditCardIssued(businessCard);
    }

    @Test
    void issueCreditCard_personalCardForBusinessCustomer_throws() {
        when(customerViewRepository.findById("cust-2")).thenReturn(Mono.just(businessView));

        StepVerifier.create(creditCardService.issueCreditCard("cust-2", CreditCard.TYPE_PERSONAL,
                        new BigDecimal("5000.00")))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void issueCreditCard_businessCardForPersonalCustomer_throws() {
        when(customerViewRepository.findById("cust-1")).thenReturn(Mono.just(personalView));

        StepVerifier.create(creditCardService.issueCreditCard("cust-1", CreditCard.TYPE_BUSINESS,
                        new BigDecimal("20000.00")))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void issueCreditCard_invalidType_throws() {
        StepVerifier.create(creditCardService.issueCreditCard("cust-1", "PREPAID",
                        new BigDecimal("5000.00")))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void issueCreditCard_nonPositiveLimit_throws() {
        StepVerifier.create(creditCardService.issueCreditCard("cust-1", CreditCard.TYPE_PERSONAL,
                        BigDecimal.ZERO))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void issueCreditCard_customerNotFound_throws() {
        when(customerViewRepository.findById("unknown")).thenReturn(Mono.empty());

        StepVerifier.create(creditCardService.issueCreditCard("unknown", CreditCard.TYPE_PERSONAL,
                        new BigDecimal("5000.00")))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void findById_found() {
        when(creditCardRepository.findById("cc-1")).thenReturn(Mono.just(personalCard));

        StepVerifier.create(creditCardService.findById("cc-1"))
                .assertNext(card -> assertEquals("cc-1", card.getId()))
                .verifyComplete();
    }

    @Test
    void findById_notFound() {
        when(creditCardRepository.findById("nonexistent")).thenReturn(Mono.empty());

        StepVerifier.create(creditCardService.findById("nonexistent"))
                .verifyComplete();
    }

    @Test
    void findAll_returnsFlux() {
        when(creditCardRepository.findAll()).thenReturn(Flux.fromIterable(
                java.util.Arrays.asList(personalCard, businessCard)));

        StepVerifier.create(creditCardService.findAll())
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void update_found_updatesLimit() {
        when(creditCardRepository.findById("cc-1")).thenReturn(Mono.just(personalCard));
        when(creditCardRepository.save(any(CreditCard.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(creditCardService.update("cc-1", new BigDecimal("8000.00")))
                .assertNext(card -> assertEquals(new BigDecimal("8000.00"), card.getCreditLimit()))
                .verifyComplete();
    }

    @Test
    void update_found_partialUpdate() {
        when(creditCardRepository.findById("cc-1")).thenReturn(Mono.just(personalCard));
        when(creditCardRepository.save(any(CreditCard.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(creditCardService.update("cc-1", null))
                .assertNext(card -> assertEquals(new BigDecimal("5000.00"), card.getCreditLimit()))
                .verifyComplete();
    }

    @Test
    void update_notFound() {
        when(creditCardRepository.findById("nonexistent")).thenReturn(Mono.empty());

        StepVerifier.create(creditCardService.update("nonexistent", new BigDecimal("8000.00")))
                .verifyComplete();
    }

    @Test
    void delete_found_returnsTrue() {
        when(creditCardRepository.existsById("cc-1")).thenReturn(Mono.just(true));
        when(creditCardRepository.deleteById(org.mockito.ArgumentMatchers.<String>any()))
                .thenReturn(Mono.empty());

        StepVerifier.create(creditCardService.delete("cc-1"))
                .assertNext(org.junit.jupiter.api.Assertions::assertTrue)
                .verifyComplete();
    }

    @Test
    void delete_notFound_returnsFalse() {
        when(creditCardRepository.existsById("nonexistent")).thenReturn(Mono.just(false));

        StepVerifier.create(creditCardService.delete("nonexistent"))
                .assertNext(org.junit.jupiter.api.Assertions::assertFalse)
                .verifyComplete();
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
        Flux<CreditCard> cards = Flux.fromIterable(
                java.util.Arrays.asList(personalCard, businessCard));

        StepVerifier.create(creditCardService.toResponseList(cards))
                .expectNextCount(2)
                .verifyComplete();
    }
}
