package com.learning.atm.repository;

import com.learning.atm.model.Transaction;
import com.learning.atm.model.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository
		extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

	/** Statement of one account, newest movement first. */
	List<Transaction> findByAccountIdOrderByCreatedAtDesc(Long accountId);

	/**
	 * The {@code pageable.getPageSize()} most recent movements of one account, for the mini
	 * statement.
	 *
	 * <p>Learning note: a {@code Pageable} argument adds {@code LIMIT} to the generated SQL.
	 * A hard-coded {@code findTop5By...} finder would not let the caller ask for 3 or 10 rows.
	 */
	List<Transaction> findByAccountIdOrderByCreatedAtDesc(Long accountId, Pageable pageable);

	/**
	 * Total cash withdrawn by one account since {@code since} — the running total the daily
	 * limit is compared against.
	 *
	 * <p>{@code coalesce(..., 0)} matters: SQL {@code SUM} over zero rows returns NULL, and
	 * comparing a null BigDecimal would blow up instead of saying "nothing withdrawn yet".
	 */
	@Query("""
			select coalesce(sum(t.amount), 0)
			from Transaction t
			where t.account.id = :accountId
			  and t.type = :type
			  and t.createdAt >= :since
			""")
	BigDecimal sumAmountSince(@Param("accountId") Long accountId,
			@Param("type") TransactionType type,
			@Param("since") LocalDateTime since);

	/** Same as {@link #sumAmountSince} but for <b>incoming</b> money only. */
	@Query("""
			select coalesce(sum(t.amount), 0)
			from Transaction t
			where t.account.id = :accountId
			  and t.type in (com.learning.atm.model.TransactionType.DEPOSIT,
			                 com.learning.atm.model.TransactionType.TRANSFER_IN)
			  and t.createdAt >= :since
			""")
	BigDecimal sumInflowSince(@Param("accountId") Long accountId,
			@Param("since") LocalDateTime since);

	/** Outgoing money: cash withdrawals plus transfers sent away. */
	@Query("""
			select coalesce(sum(t.amount), 0)
			from Transaction t
			where t.account.id = :accountId
			  and t.type in (com.learning.atm.model.TransactionType.WITHDRAWAL,
			                 com.learning.atm.model.TransactionType.TRANSFER_OUT)
			  and t.createdAt >= :since
			""")
	BigDecimal sumOutflowSince(@Param("accountId") Long accountId,
			@Param("since") LocalDateTime since);

	Optional<Transaction> findFirstByAccountIdOrderByCreatedAtDesc(Long accountId);
}
