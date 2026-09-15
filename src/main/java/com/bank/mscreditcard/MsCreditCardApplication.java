package com.bank.mscreditcard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * Microservicio de gestion de tarjetas de credito del sistema bancario.
 * Expone CRUD completo y emision de tarjetas personales y empresariales
 * con linea de credito, y publica el evento bank.creditcard.issued
 * al emitir una nueva tarjeta.
 */
@EnableMongoAuditing
@SpringBootApplication
public class MsCreditCardApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsCreditCardApplication.class, args);
    }
}
