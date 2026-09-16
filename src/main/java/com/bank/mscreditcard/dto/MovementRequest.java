package com.bank.mscreditcard.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO de solicitud para registrar un consumo sobre una tarjeta de credito.
 */
@Getter
@Setter
public class MovementRequest {

    private BigDecimal amount;
}
