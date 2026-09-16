package com.bank.mscreditcard.service;

import com.bank.mscreditcard.dto.CreditCardResponse;
import com.bank.mscreditcard.event.CreditCardEventProducer;
import com.bank.mscreditcard.model.CreditCard;
import com.bank.mscreditcard.model.CustomerView;
import com.bank.mscreditcard.repository.CreditCardRepository;
import com.bank.mscreditcard.repository.CustomerViewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * Servicio reactivo de gestion de tarjetas de credito.
 * Expone CRUD completo y emision de tarjetas aplicando las reglas de consistencia:
 * la linea de credito debe ser mayor a cero y el tipo de tarjeta debe ser
 * consistente con el tipo de cliente. No hay limite de tarjetas por cliente.
 * La validacion del tipo de cliente usa la vista local customer_view
 * alimentada por eventos, sin llamadas REST a ms-customer.
 */
@Service
@RequiredArgsConstructor
public class CreditCardService {

    private final CreditCardRepository creditCardRepository;
    private final CustomerViewRepository customerViewRepository;
    private final CreditCardEventProducer creditCardEventProducer;

    /**
     * Emite una tarjeta de credito para un cliente.
     * Valida que el limite sea positivo, que el tipo de tarjeta sea consistente
     * con el tipo de cliente y que el cliente exista.
     *
     * @param customerId identificador del cliente
     * @param cardType tipo de tarjeta (PERSONAL o BUSINESS)
     * @param creditLimit linea de credito (mayor a cero)
     * @return Mono con la tarjeta emitida
     */
    public Mono<CreditCard> issueCreditCard(String customerId, String cardType, BigDecimal creditLimit) {
        if (creditLimit == null || creditLimit.signum() <= 0) {
            return Mono.error(new IllegalArgumentException("La linea de credito debe ser mayor a cero"));
        }
        if (cardType == null || (!CreditCard.TYPE_PERSONAL.equals(cardType)
                && !CreditCard.TYPE_BUSINESS.equals(cardType))) {
            return Mono.error(new IllegalArgumentException("Tipo de tarjeta invalido: " + cardType));
        }
        return requireCustomer(customerId)
                .flatMap(customer -> {
                    if (CreditCard.TYPE_PERSONAL.equals(cardType)
                            && !CreditCard.TYPE_PERSONAL.equals(customer.getCustomerType())) {
                        return Mono.error(new IllegalArgumentException(
                                "Una tarjeta personal requiere un cliente personal"));
                    }
                    if (CreditCard.TYPE_BUSINESS.equals(cardType)
                            && !CreditCard.TYPE_BUSINESS.equals(customer.getCustomerType())) {
                        return Mono.error(new IllegalArgumentException(
                                "Una tarjeta empresarial requiere un cliente empresarial"));
                    }
                    CreditCard card = new CreditCard(customerId, cardType, creditLimit);
                    return creditCardRepository.save(card)
                            .doOnNext(creditCardEventProducer::publishCreditCardIssued);
                });
    }

    /**
     * Busca una tarjeta por su ID.
     *
     * @param id identificador de la tarjeta
     * @return Mono con la tarjeta encontrada o vacio
     */
    public Mono<CreditCard> findById(String id) {
        return creditCardRepository.findById(id);
    }

    /**
     * Lista todas las tarjetas registradas.
     *
     * @return Flux con las tarjetas
     */
    public Flux<CreditCard> findAll() {
        return creditCardRepository.findAll();
    }

    /**
     * Actualiza la linea de credito de una tarjeta existente.
     *
     * @param id identificador de la tarjeta
     * @param creditLimit nueva linea de credito (nullable)
     * @return Mono con la tarjeta actualizada, o vacio si no se encontro
     */
    public Mono<CreditCard> update(String id, BigDecimal creditLimit) {
        return creditCardRepository.findById(id)
                .flatMap(card -> {
                    if (creditLimit != null) {
                        card.setCreditLimit(creditLimit);
                    }
                    return creditCardRepository.save(card);
                });
    }

    /**
     * Elimina una tarjeta por su ID.
     *
     * @param id identificador de la tarjeta
     * @return Mono con true si se elimino, false si no existia
     */
    public Mono<Boolean> delete(String id) {
        return creditCardRepository.existsById(id)
                .flatMap(exists -> {
                    if (exists) {
                        return creditCardRepository.deleteById(id).then(Mono.just(true));
                    }
                    return Mono.just(false);
                });
    }

    /**
     * Convierte una entidad CreditCard a su DTO de respuesta.
     *
     * @param creditCard entidad a convertir
     * @return DTO con los campos poblados
     */
    public CreditCardResponse toResponse(CreditCard creditCard) {
        CreditCardResponse response = new CreditCardResponse();
        response.setId(creditCard.getId());
        response.setCustomerId(creditCard.getCustomerId());
        response.setCardType(creditCard.getCardType());
        response.setCreditLimit(creditCard.getCreditLimit());
        response.setAvailableBalance(creditCard.getAvailableBalance());
        response.setStatus(creditCard.getStatus());
        return response;
    }

    /**
     * Convierte un Flux de entidades CreditCard a DTOs de respuesta.
     *
     * @param creditCards Flux de entidades
     * @return Flux de DTOs
     */
    public Flux<CreditCardResponse> toResponseList(Flux<CreditCard> creditCards) {
        return creditCards.map(this::toResponse);
    }

    private Mono<CustomerView> requireCustomer(String customerId) {
        return customerViewRepository.findById(customerId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                        "Cliente no encontrado o no sincronizado: " + customerId)));
    }
}
