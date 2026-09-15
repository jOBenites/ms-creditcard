package com.bank.mscreditcard.controller;

import com.bank.mscreditcard.dto.CreditCardResponse;
import com.bank.mscreditcard.dto.CreditCardUpdateRequest;
import com.bank.mscreditcard.dto.IssueCardRequest;
import com.bank.mscreditcard.model.CreditCard;
import com.bank.mscreditcard.service.CreditCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador REST para la gestion de tarjetas de credito.
 * Expone emision de tarjetas (personales y empresariales) y CRUD completo.
 */
@RestController
@RequestMapping("/credit-cards")
@RequiredArgsConstructor
public class CreditCardController {

    private final CreditCardService creditCardService;

    /**
     * Emite una nueva tarjeta de credito.
     *
     * @param request datos de la tarjeta (customerId, cardType, creditLimit)
     * @return la tarjeta emitida con codigo 201
     */
    @PostMapping
    public ResponseEntity<CreditCardResponse> issueCreditCard(@RequestBody IssueCardRequest request) {
        CreditCard card = creditCardService.issueCreditCard(
                request.getCustomerId(), request.getCardType(), request.getCreditLimit());
        return ResponseEntity.status(HttpStatus.CREATED).body(creditCardService.toResponse(card));
    }

    /**
     * Obtiene una tarjeta por su ID.
     *
     * @param id identificador de la tarjeta
     * @return la tarjeta encontrada o 404 si no existe
     */
    @GetMapping("/{id}")
    public ResponseEntity<CreditCardResponse> getCreditCardById(@PathVariable String id) {
        return creditCardService.findById(id)
                .map(card -> ResponseEntity.ok(creditCardService.toResponse(card)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Lista todas las tarjetas registradas.
     *
     * @return lista de tarjetas
     */
    @GetMapping
    public ResponseEntity<List<CreditCardResponse>> getAllCreditCards() {
        return ResponseEntity.ok(creditCardService.toResponseList(creditCardService.findAll()));
    }

    /**
     * Actualiza la linea de credito de una tarjeta existente.
     *
     * @param id identificador de la tarjeta
     * @param request campos a actualizar (creditLimit)
     * @return la tarjeta actualizada o 404 si no existe
     */
    @PutMapping("/{id}")
    public ResponseEntity<CreditCardResponse> updateCreditCard(
            @PathVariable String id,
            @RequestBody CreditCardUpdateRequest request) {
        return creditCardService.update(id, request.getCreditLimit())
                .map(card -> ResponseEntity.ok(creditCardService.toResponse(card)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Elimina una tarjeta por su ID.
     *
     * @param id identificador de la tarjeta
     * @return 204 si se elimino, 404 si no existe
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCreditCard(@PathVariable String id) {
        if (creditCardService.delete(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
