package com.bank.mscreditcard.service;

import com.bank.mscreditcard.dto.MovementResponse;
import com.bank.mscreditcard.event.CreditCardEventProducer;
import com.bank.mscreditcard.model.CreditCard;
import com.bank.mscreditcard.model.Movement;
import com.bank.mscreditcard.repository.CreditCardRepository;
import com.bank.mscreditcard.repository.MovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servicio de registro de movimientos sobre tarjetas de credito.
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
     * @return el movimiento registrado, o empty si la tarjeta no existe
     * @throws IllegalArgumentException si el monto no es positivo, la tarjeta
     *         no esta activa o el monto supera el saldo disponible
     */
    public Optional<Movement> charge(String cardId, BigDecimal amount) {
        requirePositiveAmount(amount);
        return creditCardRepository.findById(cardId).map(card -> {
            requireActiveCard(card);
            if (amount.compareTo(card.getAvailableBalance()) > 0) {
                throw new IllegalArgumentException(
                        "El monto del consumo supera el saldo disponible de la tarjeta");
            }
            card.setAvailableBalance(card.getAvailableBalance().subtract(amount));
            creditCardRepository.save(card);
            Movement movement = movementRepository.save(
                    new Movement(cardId, Movement.TYPE_CARD_CHARGE, amount));
            creditCardEventProducer.publishMovementRecorded(movement, card.getCardType());
            return movement;
        });
    }

    /**
     * Lista los movimientos de una tarjeta del mas reciente al mas antiguo.
     *
     * @param cardId identificador de la tarjeta
     * @return lista de movimientos, o empty si la tarjeta no existe
     */
    public Optional<List<Movement>> findMovements(String cardId) {
        if (!creditCardRepository.existsById(cardId)) {
            return Optional.empty();
        }
        return Optional.of(movementRepository.findByCardIdOrderByOccurredAtDesc(cardId));
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
     * Convierte una lista de entidades Movement a DTOs de respuesta.
     *
     * @param movements lista de entidades
     * @return lista de DTOs
     */
    public List<MovementResponse> toMovementResponseList(List<Movement> movements) {
        return movements.stream()
                .map(this::toMovementResponse)
                .collect(Collectors.toList());
    }

    private void requirePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero");
        }
    }

    private void requireActiveCard(CreditCard card) {
        if (!CreditCard.STATUS_ACTIVE.equals(card.getStatus())) {
            throw new IllegalArgumentException("La tarjeta no esta activa");
        }
    }
}
