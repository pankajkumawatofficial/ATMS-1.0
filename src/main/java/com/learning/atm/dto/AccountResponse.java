package com.learning.atm.dto;

import com.learning.atm.model.Account;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Account data returned to clients. Note that the PIN is <b>never</b> included —
 * entities are not returned directly, DTOs are, so secret columns cannot leak by accident.
 */
public record AccountResponse(
		long id,
		String accountNumber,
		String ownerName,
		BigDecimal balance,
		LocalDateTime createdAt) {

	/** Static factory: maps an entity to its client-facing shape. */
	public static AccountResponse from(Account account) {
		return new AccountResponse(
				account.getId(),
				account.getAccountNumber(),
				account.getOwnerName(),
				account.getBalance(),
				account.getCreatedAt());
	}
}
