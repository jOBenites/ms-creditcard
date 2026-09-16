package com.bank.mscreditcard.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO de solicitud para la emision de una tarjeta de credito.
 * El tipo de tarjeta debe ser consistente con el tipo de cliente.
 */
@Getter
@Setter
public class IssueCardRequest {

    private String customerId;
    private String cardType;
    private BigDecimal creditLimit;
}
