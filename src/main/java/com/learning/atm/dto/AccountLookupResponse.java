package com.learning.atm.dto;

import com.learning.atm.model.Account;

/**
 * Minimal account identity used to confirm a transfer target before money moves.
 *
 * <p>Deliberately carries no balance or transaction data: the lookup exists so a customer can
 * check "am I sending rent to the right Bob?", not so anyone can probe other people's money.
 */
public record AccountLookupResponse(
		String accountNumber,
		String ownerName,
		boolean exists) {

	public static AccountLookupResponse found(Account account) {
		return new AccountLookupResponse(account.getAccountNumber(), account.getOwnerName(), true);
	}

	public static AccountLookupResponse missing(String accountNumber) {
		return new AccountLookupResponse(accountNumber, null, false);
	}
}
