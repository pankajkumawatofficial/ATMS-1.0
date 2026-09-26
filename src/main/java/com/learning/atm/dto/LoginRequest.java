package com.learning.atm.dto;

import jakarta.validation.constraints.NotBlank;

/** Credentials sent by the ATM screen when a customer types account number + PIN. */
public record LoginRequest(
		@NotBlank(message = "accountNumber is required") String accountNumber,
		@NotBlank(message = "pin is required") String pin) {
}
