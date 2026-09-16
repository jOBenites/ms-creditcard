package com.bank.mscreditcard.repository;

import com.bank.mscreditcard.model.CreditCard;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

/**
 * Repositorio reactivo para la entidad CreditCard en MongoDB.
 * No se permite @Query ni consultas dinamicas segun las reglas del proyecto.
 */
public interface CreditCardRepository extends ReactiveMongoRepository<CreditCard, String> {
}
