package com.learning.atm.exception;

import java.math.BigDecimal;

/** Thrown when a withdrawal or transfer exceeds the available balance — mapped to HTTP 400. */
public class InsufficientFundsException extends RuntimeException {

	public InsufficientFundsException(BigDecimal balance, BigDecimal requested) {
		super("Insufficient funds: balance is " + balance + " but " + requested + " was requested");
	}
}
