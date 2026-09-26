package com.learning.atm.repository;

import com.learning.atm.model.Transaction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

	/** Statement of one account, newest movement first. */
	List<Transaction> findByAccountIdOrderByCreatedAtDesc(Long accountId);
}
