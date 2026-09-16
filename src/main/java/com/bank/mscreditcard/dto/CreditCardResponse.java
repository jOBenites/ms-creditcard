package com.bank.mscreditcard.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO de respuesta de una tarjeta de credito.
 */
@Getter
@Setter
public class CreditCardResponse {

    private String id;
    private String customerId;
    private String cardType;
    private BigDecimal creditLimit;
    private BigDecimal availableBalance;
    private String status;
}
