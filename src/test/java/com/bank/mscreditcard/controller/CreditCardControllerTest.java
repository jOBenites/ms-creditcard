package com.bank.mscreditcard.controller;

import com.bank.mscreditcard.dto.CreditCardResponse;
import com.bank.mscreditcard.dto.CreditCardUpdateRequest;
import com.bank.mscreditcard.dto.IssueCardRequest;
import com.bank.mscreditcard.dto.MovementRequest;
import com.bank.mscreditcard.dto.MovementResponse;
import com.bank.mscreditcard.model.CreditCard;
import com.bank.mscreditcard.model.Movement;
import com.bank.mscreditcard.service.CreditCardService;
import com.bank.mscreditcard.service.MovementService;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias para {@link CreditCardController}.
 * Valida los endpoints REST y los codigos de respuesta HTTP.
 */
@ExtendWith(MockitoExtension.class)
class CreditCardControllerTest {

    @Mock
    private CreditCardService creditCardService;

    @Mock
    private MovementService movementService;

    @InjectMocks
    private CreditCardController creditCardController;

    private CreditCard personalCard;
    private CreditCard businessCard;
    private CreditCardResponse personalResponse;
    private CreditCardResponse businessResponse;

    @BeforeEach
    void setUp() {
        personalCard = new CreditCard("cust-1", CreditCard.TYPE_PERSONAL, new BigDecimal("5000.00"));
        personalCard.setId("cc-1");

        businessCard = new CreditCard("cust-2", CreditCard.TYPE_BUSINESS, new BigDecimal("20000.00"));
        businessCard.setId("cc-2");

        personalResponse = new CreditCardResponse();
        personalResponse.setId("cc-1");
        personalResponse.setCustomerId("cust-1");
        personalResponse.setCardType(CreditCard.TYPE_PERSONAL);
        personalResponse.setCreditLimit(new BigDecimal("5000.00"));
        personalResponse.setAvailableBalance(new BigDecimal("5000.00"));
        personalResponse.setStatus(CreditCard.STATUS_ACTIVE);

        businessResponse = new CreditCardResponse();
        businessResponse.setId("cc-2");
        businessResponse.setCustomerId("cust-2");
        businessResponse.setCardType(CreditCard.TYPE_BUSINESS);
        businessResponse.setCreditLimit(new BigDecimal("20000.00"));
        businessResponse.setAvailableBalance(new BigDecimal("20000.00"));
        businessResponse.setStatus(CreditCard.STATUS_ACTIVE);
    }

    @Test
    void issueCreditCard_returns201() {
        when(creditCardService.issueCreditCard("cust-1", CreditCard.TYPE_PERSONAL, new BigDecimal("5000.00")))
                .thenReturn(Mono.just(personalCard));
        when(creditCardService.toResponse(personalCard)).thenReturn(personalResponse);

        IssueCardRequest request = new IssueCardRequest();
        request.setCustomerId("cust-1");
        request.setCardType(CreditCard.TYPE_PERSONAL);
        request.setCreditLimit(new BigDecimal("5000.00"));

        StepVerifier.create(creditCardController.issueCreditCard(request))
                .assertNext(response -> {
                    assertEquals(201, response.getStatusCode().value());
                    assertNotNull(response.getBody());
                    assertEquals("cc-1", response.getBody().getId());
                })
                .verifyComplete();
    }

    @Test
    void getCreditCardById_found() {
        when(creditCardService.findById("cc-1")).thenReturn(Mono.just(personalCard));
        when(creditCardService.toResponse(personalCard)).thenReturn(personalResponse);

        StepVerifier.create(creditCardController.getCreditCardById("cc-1"))
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCode().value());
                    assertEquals("cc-1", response.getBody().getId());
                })
                .verifyComplete();
    }

    @Test
    void getCreditCardById_notFound() {
        when(creditCardService.findById("nonexistent")).thenReturn(Mono.empty());

        StepVerifier.create(creditCardController.getCreditCardById("nonexistent"))
                .assertNext(response -> assertEquals(404, response.getStatusCode().value()))
                .verifyComplete();
    }

    @Test
    void getAllCreditCards_returnsFlux() {
        when(creditCardService.findAll()).thenReturn(Flux.just(personalCard, businessCard));
        when(creditCardService.toResponseList(any()))
                .thenReturn(Flux.just(personalResponse, businessResponse));

        StepVerifier.create(creditCardController.getAllCreditCards())
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void updateCreditCard_found() {
        when(creditCardService.update("cc-1", new BigDecimal("8000.00")))
                .thenReturn(Mono.just(personalCard));
        when(creditCardService.toResponse(personalCard)).thenReturn(personalResponse);

        CreditCardUpdateRequest request = new CreditCardUpdateRequest();
        request.setCreditLimit(new BigDecimal("8000.00"));

        StepVerifier.create(creditCardController.updateCreditCard("cc-1", request))
                .assertNext(response -> assertEquals(200, response.getStatusCode().value()))
                .verifyComplete();
    }

    @Test
    void updateCreditCard_notFound() {
        when(creditCardService.update("nonexistent", null)).thenReturn(Mono.empty());

        CreditCardUpdateRequest request = new CreditCardUpdateRequest();

        StepVerifier.create(creditCardController.updateCreditCard("nonexistent", request))
                .assertNext(response -> assertEquals(404, response.getStatusCode().value()))
                .verifyComplete();
    }

    @Test
    void deleteCreditCard_found() {
        when(creditCardService.delete("cc-1")).thenReturn(Mono.just(true));

        StepVerifier.create(creditCardController.deleteCreditCard("cc-1"))
                .assertNext(response -> assertEquals(204, response.getStatusCode().value()))
                .verifyComplete();
    }

    @Test
    void deleteCreditCard_notFound() {
        when(creditCardService.delete("nonexistent")).thenReturn(Mono.just(false));

        StepVerifier.create(creditCardController.deleteCreditCard("nonexistent"))
                .assertNext(response -> assertEquals(404, response.getStatusCode().value()))
                .verifyComplete();
    }

    @Test
    void charge_returns201() {
        Movement movement = new Movement("cc-1", Movement.TYPE_CARD_CHARGE, new BigDecimal("200.00"));
        movement.setId("mov-1");
        when(movementService.charge("cc-1", new BigDecimal("200.00"))).thenReturn(Mono.just(movement));
        when(movementService.toMovementResponse(movement)).thenReturn(new MovementResponse());

        MovementRequest request = new MovementRequest();
        request.setAmount(new BigDecimal("200.00"));

        StepVerifier.create(creditCardController.charge("cc-1", request))
                .assertNext(response -> assertEquals(201, response.getStatusCode().value()))
                .verifyComplete();
    }

    @Test
    void charge_cardNotFound_returns404() {
        when(movementService.charge("nonexistent", new BigDecimal("200.00"))).thenReturn(Mono.empty());

        MovementRequest request = new MovementRequest();
        request.setAmount(new BigDecimal("200.00"));

        StepVerifier.create(creditCardController.charge("nonexistent", request))
                .assertNext(response -> assertEquals(404, response.getStatusCode().value()))
                .verifyComplete();
    }

    @Test
    void getMovements_returns200() {
        Movement movement = new Movement("cc-1", Movement.TYPE_CARD_CHARGE, new BigDecimal("200.00"));
        when(creditCardService.findById("cc-1")).thenReturn(Mono.just(personalCard));
        when(movementService.findMovements("cc-1")).thenReturn(Flux.just(movement));
        when(movementService.toMovementResponseList(any()))
                .thenReturn(Flux.just(new MovementResponse()));

        StepVerifier.create(creditCardController.getMovements("cc-1"))
                .assertNext(response -> assertEquals(200, response.getStatusCode().value()))
                .verifyComplete();
    }

    @Test
    void getMovements_cardNotFound_returns404() {
        when(creditCardService.findById("nonexistent")).thenReturn(Mono.empty());

        StepVerifier.create(creditCardController.getMovements("nonexistent"))
                .assertNext(response -> assertEquals(404, response.getStatusCode().value()))
                .verifyComplete();
    }
}
