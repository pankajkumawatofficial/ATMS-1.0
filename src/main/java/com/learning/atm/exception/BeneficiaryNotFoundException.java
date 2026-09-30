package com.learning.atm.exception;

/** Thrown when a beneficiary id does not exist for the logged-in owner — mapped to HTTP 404. */
public class BeneficiaryNotFoundException extends RuntimeException {

	public BeneficiaryNotFoundException(long id) {
		super("Beneficiary not found: " + id);
	}
}
