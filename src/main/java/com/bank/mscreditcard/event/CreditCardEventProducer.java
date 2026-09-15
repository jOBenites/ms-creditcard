package com.bank.mscreditcard.event;

import com.bank.mscreditcard.model.CreditCard;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Productor de eventos Kafka para el dominio creditcard.
 * Publica bank.creditcard.issued cuando se emite una nueva tarjeta.
 * ms-account consume este evento para la validacion de perfiles VIP/PYME.
 */
@Component
@RequiredArgsConstructor
public class CreditCardEventProducer {

    private static final Logger log = LoggerFactory.getLogger(CreditCardEventProducer.class);
    private static final String CREDITCARD_ISSUED_TOPIC = "bank.creditcard.issued";

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
}
