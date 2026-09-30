package com.learning.atm.exception;

/** Thrown when a beneficiary is added twice for the same account — mapped to HTTP 409. */
public class DuplicateBeneficiaryException extends RuntimeException {

	public DuplicateBeneficiaryException(String accountNumber) {
		super("Account " + accountNumber + " is already in your beneficiary list");
	}
}
