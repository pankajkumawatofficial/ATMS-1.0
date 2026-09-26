package com.learning.atm.exception;

/** Thrown when an account id/number does not exist — mapped to HTTP 404. */
public class AccountNotFoundException extends RuntimeException {

	public AccountNotFoundException(String account) {
		super("Account not found: " + account);
	}
}
