package com.bank.mscreditcard.repository;

import com.bank.mscreditcard.model.CustomerView;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Repositorio para la vista de lectura local de clientes.
 * Se alimenta exclusivamente de los eventos bank.customer.created.
 */
public interface CustomerViewRepository extends MongoRepository<CustomerView, String> {
}
