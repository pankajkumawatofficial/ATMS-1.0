package com.learning.atm.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Payload for opening a new account ({@code POST /api/accounts}). */
public record CreateAccountRequest(
		@NotBlank(message = "accountNumber is required")
		@Pattern(regexp = "\\d{4,20}", message = "accountNumber must be 4-20 digits")
		String accountNumber,

		@NotBlank(message = "pin is required")
		@Size(min = 4, max = 6, message = "pin must be 4-6 characters")
		String pin,

		@NotBlank(message = "ownerName is required")
		String ownerName,

		/** Optional — when omitted the account starts at 0.00. */
		@DecimalMin(value = "0.00", message = "initialBalance cannot be negative")
		BigDecimal initialBalance) {
}
