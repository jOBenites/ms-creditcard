package com.bank.mscreditcard.repository;

import com.bank.mscreditcard.model.Movement;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

/**
 * Repositorio reactivo para la entidad Movement en MongoDB.
 * No se permite @Query ni consultas dinamicas segun las reglas del proyecto.
 */
public interface MovementRepository extends ReactiveMongoRepository<Movement, String> {

    /**
     * Lista los movimientos de una tarjeta del mas reciente al mas antiguo.
     *
     * @param cardId identificador de la tarjeta
     * @return Flux de movimientos ordenados por fecha descendente
     */
    Flux<Movement> findByCardIdOrderByOccurredAtDesc(String cardId);
}
