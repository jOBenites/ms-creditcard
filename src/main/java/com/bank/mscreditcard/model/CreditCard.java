package com.bank.mscreditcard.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad que representa una tarjeta de credito del banco.
 * Tiene una linea de credito y un saldo disponible que se reduce con los consumos.
 * El tipo de tarjeta se registra explicitamente y debe ser consistente con el
 * tipo de cliente: PERSONAL o BUSINESS. Es prerrequisito para abrir cuentas
 * con perfil VIP o PYME (validado en ms-account via eventos).
 */
@Getter
@Setter
@NoArgsConstructor
@Document(collection = "credit_card")
public class CreditCard {

    /** Tipo de tarjeta personal. */
    public static final String TYPE_PERSONAL = "PERSONAL";
    /** Tipo de tarjeta empresarial. */
    public static final String TYPE_BUSINESS = "BUSINESS";
    /** Estado de tarjeta activa. */
    public static final String STATUS_ACTIVE = "ACTIVE";

    @Id
    private String id;

    @Indexed
    private String customerId;

    private String cardType;

    private BigDecimal creditLimit;

    private BigDecimal availableBalance;

    private String status;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    /**
     * Constructor para crear una tarjeta de credito nueva.
     * El saldo disponible inicia igual a la linea de credito y el estado en ACTIVE.
     *
     * @param customerId identificador del cliente
     * @param cardType tipo de tarjeta (PERSONAL o BUSINESS)
     * @param creditLimit linea de credito otorgada
     */
    public CreditCard(String customerId, String cardType, BigDecimal creditLimit) {
        this.customerId = customerId;
        this.cardType = cardType;
        this.creditLimit = creditLimit;
        this.availableBalance = creditLimit;
        this.status = STATUS_ACTIVE;
    }
}
