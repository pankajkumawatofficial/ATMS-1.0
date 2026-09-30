package com.learning.atm.exception;

/** Thrown when an account number is not in the expected 4-20 digit shape — mapped to HTTP 400. */
public class InvalidAccountNumberException extends RuntimeException {

	public InvalidAccountNumberException(String accountNumber) {
		super("Invalid account number: '" + accountNumber + "' — expected 4 to 20 digits");
	}
}
