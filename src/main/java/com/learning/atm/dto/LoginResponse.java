package com.learning.atm.dto;

import java.math.BigDecimal;

/**
 * Returned after a successful login. The token must be sent as
 * {@code Authorization: Bearer <token>} on every following request.
 */
public record LoginResponse(
		String token,
		long accountId,
		String accountNumber,
		String ownerName,
		BigDecimal balance) {
}
