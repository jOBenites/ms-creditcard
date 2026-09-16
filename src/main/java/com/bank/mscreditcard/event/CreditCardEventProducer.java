package com.bank.mscreditcard.event;

import com.bank.mscreditcard.model.CreditCard;
import com.bank.mscreditcard.model.Movement;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Productor de eventos Kafka para el dominio creditcard.
 * Publica bank.creditcard.issued cuando se emite una nueva tarjeta y
 * bank.movement.recorded cuando se registra un consumo.
 * ms-account consume bank.creditcard.issued para la validacion de perfiles VIP/PYME.
 */
@Component
@RequiredArgsConstructor
public class CreditCardEventProducer {

    private static final Logger log = LoggerFactory.getLogger(CreditCardEventProducer.class);
    private static final String CREDITCARD_ISSUED_TOPIC = "bank.creditcard.issued";
    private static final String MOVEMENT_RECORDED_TOPIC = "bank.movement.recorded";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publica el evento bank.creditcard.issued con los datos minimos de la tarjeta.
     *
     * @param creditCard tarjeta recien emitida
     */
    public void publishCreditCardIssued(CreditCard creditCard) {
        Map<String, Object> payload = Map.of(
                "cardId", creditCard.getId(),
                "customerId", creditCard.getCustomerId(),
                "cardType", creditCard.getCardType(),
                "creditLimit", creditCard.getCreditLimit(),
                "occurredAt", LocalDateTime.now()
        );
        kafkaTemplate.send(CREDITCARD_ISSUED_TOPIC, creditCard.getId(), payload);
        log.info("Evento bank.creditcard.issued publicado para tarjeta {}", creditCard.getId());
    }

    /**
     * Publica el evento bank.movement.recorded con los datos del movimiento.
     *
     * @param movement movimiento recien registrado
     * @param productType tipo de tarjeta afectada (PERSONAL o BUSINESS)
     */
    public void publishMovementRecorded(Movement movement, String productType) {
        Map<String, Object> payload = Map.of(
                "movementId", movement.getId(),
                "productId", movement.getCardId(),
                "productType", productType,
                "movementType", movement.getMovementType(),
                "amount", movement.getAmount(),
                "occurredAt", movement.getOccurredAt()
        );
        kafkaTemplate.send(MOVEMENT_RECORDED_TOPIC, movement.getId(), payload);
        log.info("Evento bank.movement.recorded publicado para movimiento {} sobre tarjeta {}",
                movement.getId(), movement.getCardId());
    }
}
