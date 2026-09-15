package com.bank.mscreditcard.repository;

import com.bank.mscreditcard.model.CreditCard;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Repositorio para la entidad CreditCard en MongoDB.
 * No se permite @Query ni consultas dinamicas segun las reglas del proyecto.
 */
public interface CreditCardRepository extends MongoRepository<CreditCard, String> {
}
