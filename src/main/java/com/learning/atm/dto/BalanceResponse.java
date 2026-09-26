package com.learning.atm.dto;

import java.math.BigDecimal;

/** Result of {@code GET /api/accounts/{id}/balance}. */
public record BalanceResponse(long accountId, String accountNumber, BigDecimal balance) {
}
