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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
                .thenReturn(personalCard);
        when(creditCardService.toResponse(personalCard)).thenReturn(personalResponse);

        IssueCardRequest request = new IssueCardRequest();
        request.setCustomerId("cust-1");
        request.setCardType(CreditCard.TYPE_PERSONAL);
        request.setCreditLimit(new BigDecimal("5000.00"));
        ResponseEntity<CreditCardResponse> response = creditCardController.issueCreditCard(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("cc-1", response.getBody().getId());
    }

    @Test
    void getCreditCardById_found() {
        when(creditCardService.findById("cc-1")).thenReturn(Optional.of(personalCard));
        when(creditCardService.toResponse(personalCard)).thenReturn(personalResponse);

        ResponseEntity<CreditCardResponse> response = creditCardController.getCreditCardById("cc-1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("cc-1", response.getBody().getId());
    }

    @Test
    void getCreditCardById_notFound() {
        when(creditCardService.findById("nonexistent")).thenReturn(Optional.empty());

        ResponseEntity<CreditCardResponse> response = creditCardController.getCreditCardById("nonexistent");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void getAllCreditCards_returnsList() {
        List<CreditCard> cards = Arrays.asList(personalCard, businessCard);
        when(creditCardService.findAll()).thenReturn(cards);
        when(creditCardService.toResponseList(cards))
                .thenReturn(Arrays.asList(personalResponse, businessResponse));

        ResponseEntity<List<CreditCardResponse>> response = creditCardController.getAllCreditCards();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
    }

    @Test
    void updateCreditCard_found() {
        when(creditCardService.update("cc-1", new BigDecimal("8000.00")))
                .thenReturn(Optional.of(personalCard));
        when(creditCardService.toResponse(personalCard)).thenReturn(personalResponse);

        CreditCardUpdateRequest request = new CreditCardUpdateRequest();
        request.setCreditLimit(new BigDecimal("8000.00"));
        ResponseEntity<CreditCardResponse> response = creditCardController.updateCreditCard("cc-1", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void updateCreditCard_notFound() {
        when(creditCardService.update("nonexistent", null)).thenReturn(Optional.empty());

        CreditCardUpdateRequest request = new CreditCardUpdateRequest();
        ResponseEntity<CreditCardResponse> response = creditCardController.updateCreditCard("nonexistent", request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void deleteCreditCard_found() {
        when(creditCardService.delete("cc-1")).thenReturn(true);

        ResponseEntity<Void> response = creditCardController.deleteCreditCard("cc-1");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void deleteCreditCard_notFound() {
        when(creditCardService.delete("nonexistent")).thenReturn(false);

        ResponseEntity<Void> response = creditCardController.deleteCreditCard("nonexistent");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void charge_returns201() {
        Movement movement = new Movement("cc-1", Movement.TYPE_CARD_CHARGE, new BigDecimal("200.00"));
        movement.setId("mov-1");
        when(movementService.charge("cc-1", new BigDecimal("200.00"))).thenReturn(Optional.of(movement));
        when(movementService.toMovementResponse(movement)).thenReturn(new MovementResponse());

        MovementRequest request = new MovementRequest();
        request.setAmount(new BigDecimal("200.00"));
        ResponseEntity<MovementResponse> response = creditCardController.charge("cc-1", request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void charge_cardNotFound_returns404() {
        when(movementService.charge("nonexistent", new BigDecimal("200.00"))).thenReturn(Optional.empty());

        MovementRequest request = new MovementRequest();
        request.setAmount(new BigDecimal("200.00"));
        ResponseEntity<MovementResponse> response = creditCardController.charge("nonexistent", request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getMovements_returns200() {
        Movement movement = new Movement("cc-1", Movement.TYPE_CARD_CHARGE, new BigDecimal("200.00"));
        when(movementService.findMovements("cc-1")).thenReturn(Optional.of(List.of(movement)));
        when(movementService.toMovementResponseList(List.of(movement)))
                .thenReturn(List.of(new MovementResponse()));

        ResponseEntity<List<MovementResponse>> response = creditCardController.getMovements("cc-1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getMovements_cardNotFound_returns404() {
        when(movementService.findMovements("nonexistent")).thenReturn(Optional.empty());

        ResponseEntity<List<MovementResponse>> response = creditCardController.getMovements("nonexistent");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
