package com.learning.atm.exception;

/** Thrown when creating an account with a number that already exists — mapped to HTTP 409. */
public class DuplicateAccountNumberException extends RuntimeException {

	public DuplicateAccountNumberException(String accountNumber) {
		super("Account number already exists: " + accountNumber);
	}
}
