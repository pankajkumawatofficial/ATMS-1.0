package com.learning.atm.dto;

import com.learning.atm.model.Transaction;
import com.learning.atm.model.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One line of an account statement. */
public record TransactionResponse(
		long id,
		TransactionType type,
		BigDecimal amount,
		BigDecimal balanceAfter,
		String counterpartyAccountNumber,
		LocalDateTime createdAt) {

	public static TransactionResponse from(Transaction transaction) {
		return new TransactionResponse(
				transaction.getId(),
				transaction.getType(),
				transaction.getAmount(),
				transaction.getBalanceAfter(),
				transaction.getCounterpartyAccountNumber(),
				transaction.getCreatedAt());
	}
}
