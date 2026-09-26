package com.learning.atm.exception;

/** Thrown when the PIN re-entered for a withdrawal/transfer is wrong — mapped to HTTP 401. */
public class InvalidPinException extends RuntimeException {

	public InvalidPinException() {
		super("Incorrect PIN");
	}
}
