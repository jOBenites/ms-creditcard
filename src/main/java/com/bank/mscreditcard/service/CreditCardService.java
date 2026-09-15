package com.bank.mscreditcard.service;

import com.bank.mscreditcard.dto.CreditCardResponse;
import com.bank.mscreditcard.event.CreditCardEventProducer;
import com.bank.mscreditcard.model.CreditCard;
import com.bank.mscreditcard.model.CustomerView;
import com.bank.mscreditcard.repository.CreditCardRepository;
import com.bank.mscreditcard.repository.CustomerViewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servicio de gestion de tarjetas de credito.
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
     * @return la tarjeta emitida
     * @throws IllegalArgumentException si el cliente no existe, el tipo es invalido
     *         o inconsistente, o el limite no es positivo
     */
    public CreditCard issueCreditCard(String customerId, String cardType, BigDecimal creditLimit) {
        if (creditLimit == null || creditLimit.signum() <= 0) {
            throw new IllegalArgumentException("La linea de credito debe ser mayor a cero");
        }
        CustomerView customer = requireCustomer(customerId);
        if (CreditCard.TYPE_PERSONAL.equals(cardType)) {
            if (!CreditCard.TYPE_PERSONAL.equals(customer.getCustomerType())) {
                throw new IllegalArgumentException("Una tarjeta personal requiere un cliente personal");
            }
        } else if (CreditCard.TYPE_BUSINESS.equals(cardType)) {
            if (!CreditCard.TYPE_BUSINESS.equals(customer.getCustomerType())) {
                throw new IllegalArgumentException("Una tarjeta empresarial requiere un cliente empresarial");
            }
        } else {
            throw new IllegalArgumentException("Tipo de tarjeta invalido: " + cardType);
        }
        CreditCard saved = creditCardRepository.save(new CreditCard(customerId, cardType, creditLimit));
        creditCardEventProducer.publishCreditCardIssued(saved);
        return saved;
    }

    /**
     * Busca una tarjeta por su ID.
     *
     * @param id identificador de la tarjeta
     * @return optional con la tarjeta encontrada
     */
    public Optional<CreditCard> findById(String id) {
        return creditCardRepository.findById(id);
    }

    /**
     * Lista todas las tarjetas registradas.
     *
     * @return lista de tarjetas
     */
    public List<CreditCard> findAll() {
        return creditCardRepository.findAll();
    }

    /**
     * Actualiza la linea de credito de una tarjeta existente.
     *
     * @param id identificador de la tarjeta
     * @param creditLimit nueva linea de credito (nullable, mantiene la actual)
     * @return la tarjeta actualizada, o empty si no se encontro
     */
    public Optional<CreditCard> update(String id, BigDecimal creditLimit) {
        return creditCardRepository.findById(id).map(card -> {
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
     * @return true si se elimino, false si no existia
     */
    public boolean delete(String id) {
        if (creditCardRepository.existsById(id)) {
            creditCardRepository.deleteById(id);
            return true;
        }
        return false;
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
     * Convierte una lista de entidades CreditCard a DTOs de respuesta.
     *
     * @param creditCards lista de entidades
     * @return lista de DTOs
     */
    public List<CreditCardResponse> toResponseList(List<CreditCard> creditCards) {
        return creditCards.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private CustomerView requireCustomer(String customerId) {
        return customerViewRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cliente no encontrado o no sincronizado: " + customerId));
    }
}
