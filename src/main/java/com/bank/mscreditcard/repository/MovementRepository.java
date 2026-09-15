package com.bank.mscreditcard.repository;

import com.bank.mscreditcard.model.Movement;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * Repositorio para la entidad Movement en MongoDB.
 * No se permite @Query ni consultas dinamicas segun las reglas del proyecto.
 */
public interface MovementRepository extends MongoRepository<Movement, String> {

    /**
     * Lista los movimientos de una tarjeta del mas reciente al mas antiguo.
     *
     * @param cardId identificador de la tarjeta
     * @return lista de movimientos ordenada por fecha descendente
     */
    List<Movement> findByCardIdOrderByOccurredAtDesc(String cardId);
}
