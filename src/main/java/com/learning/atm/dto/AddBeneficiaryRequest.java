package com.learning.atm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload for saving a payee ({@code POST /api/accounts/{id}/beneficiaries}).
 *
 * @param accountNumber whose account to save
 * @param nickname      the label to show in the UI, e.g. "Landlord" or "Mum"
 */
public record AddBeneficiaryRequest(
		@NotBlank(message = "accountNumber is required")
		@Pattern(regexp = "\\d{4,20}", message = "accountNumber must be 4-20 digits")
		String accountNumber,

		@NotBlank(message = "nickname is required")
		@Size(max = 40, message = "nickname must be 40 characters or fewer")
		String nickname) {
}
