package com.bank.mscreditcard.controller;

import com.bank.mscreditcard.dto.CreditCardResponse;
import com.bank.mscreditcard.dto.CreditCardUpdateRequest;
import com.bank.mscreditcard.dto.IssueCardRequest;
import com.bank.mscreditcard.dto.MovementRequest;
import com.bank.mscreditcard.dto.MovementResponse;
import com.bank.mscreditcard.service.CreditCardService;
import com.bank.mscreditcard.service.MovementService;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Controlador REST reactivo para la gestion de tarjetas de credito.
 * Expone emision de tarjetas (personales y empresariales), CRUD completo,
 * registro de consumos y consulta de movimientos.
 */
@RestController
@RequestMapping("/credit-cards")
@RequiredArgsConstructor
public class CreditCardController {

    private final CreditCardService creditCardService;
    private final MovementService movementService;

    /**
     * Emite una nueva tarjeta de credito.
     *
     * @param request datos de la tarjeta
     * @return Mono con la tarjeta emitida y codigo 201
     */
    @PostMapping
    public Mono<ResponseEntity<CreditCardResponse>> issueCreditCard(@RequestBody IssueCardRequest request) {
        return creditCardService.issueCreditCard(
                        request.getCustomerId(), request.getCardType(), request.getCreditLimit())
                .map(card -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(creditCardService.toResponse(card)));
    }

    /**
     * Obtiene una tarjeta por su ID.
     *
     * @param id identificador de la tarjeta
     * @return Mono con la tarjeta encontrada o 404 si no existe
     */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<CreditCardResponse>> getCreditCardById(@PathVariable String id) {
        return creditCardService.findById(id)
                .map(card -> ResponseEntity.ok(creditCardService.toResponse(card)))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Lista todas las tarjetas registradas.
     *
     * @return Flux con las tarjetas
     */
    @GetMapping
    public Flux<CreditCardResponse> getAllCreditCards() {
        return creditCardService.toResponseList(creditCardService.findAll());
    }

    /**
     * Actualiza la linea de credito de una tarjeta existente.
     *
     * @param id identificador de la tarjeta
     * @param request campos a actualizar
     * @return Mono con la tarjeta actualizada o 404 si no existe
     */
    @PutMapping("/{id}")
    public Mono<ResponseEntity<CreditCardResponse>> updateCreditCard(
            @PathVariable String id,
            @RequestBody CreditCardUpdateRequest request) {
        return creditCardService.update(id, request.getCreditLimit())
                .map(card -> ResponseEntity.ok(creditCardService.toResponse(card)))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Elimina una tarjeta por su ID.
     *
     * @param id identificador de la tarjeta
     * @return Mono con 204 si se elimino, 404 si no existe
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteCreditCard(@PathVariable String id) {
        return creditCardService.delete(id)
                .flatMap(deleted -> {
                    if (deleted) {
                        return Mono.just(ResponseEntity.noContent().<Void>build());
                    }
                    return Mono.just(ResponseEntity.notFound().build());
                });
    }

    /**
     * Registra un consumo sobre una tarjeta de credito.
     *
     * @param id identificador de la tarjeta
     * @param request solicitud con el monto del consumo
     * @return Mono con el movimiento registrado y codigo 201, o 404 si la tarjeta no existe
     */
    @PostMapping("/{id}/charges")
    public Mono<ResponseEntity<MovementResponse>> charge(
            @PathVariable String id,
            @RequestBody MovementRequest request) {
        return movementService.charge(id, request.getAmount())
                .map(movement -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(movementService.toMovementResponse(movement)))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Lista los movimientos de una tarjeta del mas reciente al mas antiguo.
     *
     * @param id identificador de la tarjeta
     * @return Flux con los movimientos, o 404 si la tarjeta no existe
     */
    @GetMapping("/{id}/movements")
    public Mono<ResponseEntity<Flux<MovementResponse>>> getMovements(@PathVariable String id) {
        return creditCardService.findById(id)
                .map(card -> ResponseEntity.ok(
                        movementService.toMovementResponseList(
                                movementService.findMovements(id))))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
