package com.learning.atm.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Payload for {@code POST /api/accounts/{id}/deposit}. */
public record DepositRequest(
		@NotNull(message = "amount is required")
		@DecimalMin(value = "0.01", message = "amount must be at least 0.01")
		@Digits(integer = 10, fraction = 2, message = "amount can have at most 2 decimal places")
		BigDecimal amount) {
}
