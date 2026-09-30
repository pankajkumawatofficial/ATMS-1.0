package com.learning.atm.dto;

import com.learning.atm.model.Beneficiary;
import java.time.LocalDateTime;

/**
 * A saved payee.
 *
 * @param ownerName the account holder's real name, resolved at save time so the transfer
 *                  screen can show "Bob Verma" without a second lookup
 */
public record BeneficiaryResponse(
		long id,
		String accountNumber,
		String ownerName,
		String nickname,
		LocalDateTime createdAt) {

	public static BeneficiaryResponse from(Beneficiary beneficiary) {
		return new BeneficiaryResponse(
				beneficiary.getId(),
				beneficiary.getAccountNumber(),
				beneficiary.getOwnerName(),
				beneficiary.getNickname(),
				beneficiary.getCreatedAt());
	}
}
