package com.learning.atm.repository;

import com.learning.atm.model.Account;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<Account, Long> {

	Optional<Account> findByAccountNumber(String accountNumber);

	/**
	 * Loads an account with a pessimistic write lock ({@code SELECT ... FOR UPDATE}).
	 *
	 * <p>Learning note: while the current transaction runs, no other transaction can read or
	 * update this row. That is what prevents two simultaneous withdrawals from spending the
	 * same money — the classic "lost update" race condition. The lock is released when the
	 * transaction ends (commit or rollback).
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select a from Account a where a.id = :id")
	Optional<Account> findByIdForUpdate(@Param("id") Long id);
}
