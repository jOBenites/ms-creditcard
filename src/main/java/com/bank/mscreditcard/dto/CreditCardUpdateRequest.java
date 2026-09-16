package com.bank.mscreditcard.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO de solicitud para actualizar la linea de credito de una tarjeta.
 */
@Getter
@Setter
public class CreditCardUpdateRequest {

    private BigDecimal creditLimit;
}
