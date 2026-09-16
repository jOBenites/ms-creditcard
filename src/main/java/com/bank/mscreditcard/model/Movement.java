package com.bank.mscreditcard.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Movimiento financiero registrado sobre una tarjeta de credito.
 * Tipo: consumo de tarjeta (CARD_CHARGE). Cada movimiento queda
 * asociado a su tarjeta y se publica como evento bank.movement.recorded.
 */
@Getter
@Setter
@NoArgsConstructor
@Document(collection = "movement")
public class Movement {

    /** Tipo de movimiento: consumo de tarjeta. */
    public static final String TYPE_CARD_CHARGE = "CARD_CHARGE";

    @Id
    private String id;

    @Indexed
    private String cardId;

    private String movementType;

    private BigDecimal amount;

    private LocalDateTime occurredAt;

    /**
     * Constructor para crear un movimiento nuevo.
     * La fecha de ocurrencia se fija al momento actual.
     *
     * @param cardId identificador de la tarjeta afectada
     * @param movementType tipo de movimiento (CARD_CHARGE)
     * @param amount monto del consumo (mayor a cero)
     */
    public Movement(String cardId, String movementType, BigDecimal amount) {
        this.cardId = cardId;
        this.movementType = movementType;
        this.amount = amount;
        this.occurredAt = LocalDateTime.now();
    }
}
