package com.learning.atm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/**
 * A payee the account holder has saved, so a transfer is two taps instead of typing a number.
 * Maps to the {@code beneficiaries} table.
 *
 * <p>Learning notes:
 * <ul>
 *   <li>It is a <b>join entity</b>: the owner side ({@link Account}) and the target side
 *       (denormalized {@code accountNumber} + {@code ownerName}) are both stored, because
 *       storing only the owner plus a number column would have needed a second entity for
 *       the payee account.</li>
 *   <li>{@code ownerName} is a snapshot copied from the target account when the payee is saved,
 *       so the transfer screen can show a name without an extra query — and so a renamed
 *       account cannot silently rewrite old payee labels.</li>
 *   <li>{@link UniqueConstraint} on (owner, accountNumber) makes "add the same payee twice"
 *       impossible at the database level, not just in a service check. Concurrency means two
 *       requests can both pass a {@code findByOwnerIdAndAccountNumber} check and then race;
 *       the constraint is what actually holds the line.</li>
 * </ul>
 */
@Entity
@Table(name = "beneficiaries",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_beneficiary_owner_account",
				columnNames = {"owner_id", "account_number"}))
public class Beneficiary {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "owner_id", nullable = false)
	private Account owner;

	@Column(name = "account_number", nullable = false, length = 20)
	private String accountNumber;

	@Column(name = "owner_name", nullable = false, length = 100)
	private String ownerName;

	@Column(nullable = false, length = 40)
	private String nickname;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	protected Beneficiary() {
		// used by JPA
	}

	public Beneficiary(Account owner, String accountNumber, String ownerName, String nickname) {
		this.owner = owner;
		this.accountNumber = accountNumber;
		this.ownerName = ownerName;
		this.nickname = nickname;
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

	public Account getOwner() {
		return owner;
	}

	public String getAccountNumber() {
		return accountNumber;
	}

	public void setAccountNumber(String accountNumber) {
		this.accountNumber = accountNumber;
	}

	public String getOwnerName() {
		return ownerName;
	}

	public void setOwnerName(String ownerName) {
		this.ownerName = ownerName;
	}

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
}
