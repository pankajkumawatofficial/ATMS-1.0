package com.learning.atm.model;

/**
 * The kinds of movements that can appear in an account statement.
 *
 * <p>A transfer produces <b>two</b> rows: {@link #TRANSFER_OUT} on the sending account and
 * {@link #TRANSFER_IN} on the receiving account — both written in one database transaction.
 */
public enum TransactionType {
	DEPOSIT,
	WITHDRAWAL,
	TRANSFER_OUT,
	TRANSFER_IN
}
