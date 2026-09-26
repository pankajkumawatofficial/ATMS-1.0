package com.learning.atm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * One line of an account statement (a ledger entry). Maps to the {@code transactions} table.
 *
 * <p>Learning notes:
 * <ul>
 *   <li>{@code @ManyToOne} links many transactions to one {@link Account}. It is loaded
 *       lazily so simply listing transactions never triggers extra queries.</li>
 *   <li>{@code @Enumerated(EnumType.STRING)} stores the enum name ("DEPOSIT") instead of
 *       its ordinal number, so reordering the enum cannot corrupt old rows.</li>
 *   <li>{@code balanceAfter} is a snapshot for auditing: the statement shows the balance
 *       after each movement without replaying history.</li>
 * </ul>
 */
@Entity
@Table(name = "transactions")
public class Transaction {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "account_id", nullable = false)
	private Account account;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TransactionType type;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	@Column(name = "balance_after", nullable = false, precision = 12, scale = 2)
	private BigDecimal balanceAfter;

	/** The other account number for transfers; {@code null} for deposit/withdrawal. */
	@Column(name = "counterparty_account", length = 20)
	private String counterpartyAccountNumber;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	protected Transaction() {
		// used by JPA
	}

	public Transaction(Account account, TransactionType type, BigDecimal amount,
			BigDecimal balanceAfter, String counterpartyAccountNumber) {
		this.account = account;
		this.type = type;
		this.amount = amount;
		this.balanceAfter = balanceAfter;
		this.counterpartyAccountNumber = counterpartyAccountNumber;
	}

	@PrePersist
	void prePersist() {
		if (createdAt == null) {
			createdAt = LocalDateTime.now();
		}
	}

	// -------------------------------------------------------------------------
	// Getters / setters
	// -------------------------------------------------------------------------

	public Long getId() {
		return id;
	}

	/** Only used so unit tests can simulate rows that already exist in the database. */
	public void setId(Long id) {
		this.id = id;
	}

	public Account getAccount() {
		return account;
	}

	public TransactionType getType() {
		return type;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public BigDecimal getBalanceAfter() {
		return balanceAfter;
	}

	public String getCounterpartyAccountNumber() {
		return counterpartyAccountNumber;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	/** Rounds money to 2 decimal places — one place every currency amount must be normalized. */
	public static BigDecimal money(BigDecimal value) {
		return value.setScale(2, RoundingMode.HALF_UP);
	}
}
