package com.bank.mscreditcard.service;

import com.bank.mscreditcard.dto.MovementResponse;
import com.bank.mscreditcard.event.CreditCardEventProducer;
import com.bank.mscreditcard.model.CreditCard;
import com.bank.mscreditcard.model.Movement;
import com.bank.mscreditcard.repository.CreditCardRepository;
import com.bank.mscreditcard.repository.MovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * Servicio reactivo de registro de movimientos sobre tarjetas de credito.
 * Permite registrar consumos (charges) y consultar el historial.
 * Cada consumo reduce el saldo disponible de la tarjeta.
 * Cada movimiento registrado se publica como evento bank.movement.recorded.
 */
@Service
@RequiredArgsConstructor
public class MovementService {

    private final CreditCardRepository creditCardRepository;
    private final MovementRepository movementRepository;
    private final CreditCardEventProducer creditCardEventProducer;

    /**
     * Registra un consumo sobre una tarjeta de credito activa.
     * Reduce el saldo disponible; rechaza si el monto supera el saldo.
     *
     * @param cardId identificador de la tarjeta
     * @param amount monto del consumo (mayor a cero)
     * @return Mono con el movimiento registrado, o vacio si la tarjeta no existe
     */
    public Mono<Movement> charge(String cardId, BigDecimal amount) {
        return validatePositiveAmount(amount)
                .then(Mono.defer(() -> creditCardRepository.findById(cardId)))
                .flatMap(card -> {
                    requireActiveCard(card);
                    if (amount.compareTo(card.getAvailableBalance()) > 0) {
                        return Mono.error(new IllegalArgumentException(
                                "El monto del consumo supera el saldo disponible de la tarjeta"));
                    }
                    card.setAvailableBalance(card.getAvailableBalance().subtract(amount));
                    return creditCardRepository.save(card)
                            .then(movementRepository.save(
                                    new Movement(cardId, Movement.TYPE_CARD_CHARGE, amount)))
                            .doOnNext(movement -> creditCardEventProducer.publishMovementRecorded(
                                    movement, card.getCardType()));
                });
    }

    /**
     * Lista los movimientos de una tarjeta del mas reciente al mas antiguo.
     *
     * @param cardId identificador de la tarjeta
     * @return Flux con los movimientos, o vacio si la tarjeta no existe
     */
    public Flux<Movement> findMovements(String cardId) {
        return creditCardRepository.existsById(cardId)
                .flatMapMany(exists -> {
                    if (!exists) {
                        return Flux.empty();
                    }
                    return movementRepository.findByCardIdOrderByOccurredAtDesc(cardId);
                });
    }

    /**
     * Convierte una entidad Movement a su DTO de respuesta.
     *
     * @param movement entidad a convertir
     * @return DTO con los campos poblados
     */
    public MovementResponse toMovementResponse(Movement movement) {
        MovementResponse response = new MovementResponse();
        response.setId(movement.getId());
        response.setCardId(movement.getCardId());
        response.setMovementType(movement.getMovementType());
        response.setAmount(movement.getAmount());
        response.setOccurredAt(movement.getOccurredAt());
        return response;
    }

    /**
     * Convierte un Flux de entidades Movement a DTOs de respuesta.
     *
     * @param movements Flux de entidades
     * @return Flux de DTOs
     */
    public Flux<MovementResponse> toMovementResponseList(Flux<Movement> movements) {
        return movements.map(this::toMovementResponse);
    }

    private Mono<Void> validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            return Mono.error(new IllegalArgumentException("El monto debe ser mayor a cero"));
        }
        return Mono.empty();
    }

    private void requireActiveCard(CreditCard card) {
        if (!CreditCard.STATUS_ACTIVE.equals(card.getStatus())) {
            throw new IllegalArgumentException("La tarjeta no esta activa");
        }
    }
}
