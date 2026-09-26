package com.learning.atm.exception;

/**
 * Thrown when a login attempt has the wrong account number or PIN. The message deliberately
 * does not say which of the two was wrong — that would help an attacker probe account numbers.
 * Mapped to HTTP 401.
 */
public class InvalidCredentialsException extends RuntimeException {

	public InvalidCredentialsException() {
		super("Invalid account number or PIN");
	}
}
