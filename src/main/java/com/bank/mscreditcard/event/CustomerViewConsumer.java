package com.bank.mscreditcard.event;

import com.bank.mscreditcard.model.CustomerView;
import com.bank.mscreditcard.repository.CustomerViewRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Consumidor de eventos Kafka del dominio customer.
 * Mantiene actualizada la vista de lectura local de clientes,
 * usada para validar el tipo de cliente al emitir tarjetas.
 */
@Component
@RequiredArgsConstructor
public class CustomerViewConsumer {

    private static final Logger log = LoggerFactory.getLogger(CustomerViewConsumer.class);

    private final CustomerViewRepository customerViewRepository;

    /**
     * Consume bank.customer.created y hace upsert de la vista local por customerId,
     * de modo que la reentrega del evento sea idempotente.
     *
     * @param payload datos del evento (customerId, customerType, profile, documentNumber)
     */
    @KafkaListener(topics = "bank.customer.created", groupId = "ms-creditcard")
    public void onCustomerCreated(Map<String, Object> payload) {
        String customerId = (String) payload.get("customerId");
        CustomerView view = customerViewRepository.findById(customerId)
                .orElseGet(CustomerView::new);
        view.setCustomerId(customerId);
        view.setCustomerType((String) payload.get("customerType"));
        view.setProfile((String) payload.get("profile"));
        view.setDocumentNumber((String) payload.get("documentNumber"));
        customerViewRepository.save(view);
        log.info("Vista local de cliente {} actualizada", customerId);
    }
}
