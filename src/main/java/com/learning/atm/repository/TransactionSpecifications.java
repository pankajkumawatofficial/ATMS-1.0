package com.learning.atm.repository;

import com.learning.atm.model.Transaction;
import com.learning.atm.model.TransactionType;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/**
 * Turns optional statement filters into a JPA {@link Specification}.
 *
 * <p>Learning notes:
 * <ul>
 *   <li>Every filter is optional, so the specification is built by collecting
 *       {@link Predicate}s into a list and AND-ing them at the end. A {@code null} filter
 *       simply contributes nothing — that is the whole trick behind "filter by anything,
 *       omit anything".</li>
 *   <li>Dates are converted to half-open ranges: {@code from} is inclusive at midnight,
 *       {@code to} is <b>exclusive</b> at the next midnight. Using {@code <= to} would drop
 *       anything recorded after midnight on the last day.</li>
 *   <li>This is a plain interface with static factories, so it needs no bean and no
 *       {@code @Component} — nothing is injected into it.</li>
 * </ul>
 */
public final class TransactionSpecifications {

	private TransactionSpecifications() {
		// utility class
	}

	/**
	 * Builds the WHERE clause for one account's statement.
	 *
	 * @param accountId  always applied — a statement is only ever visible for the caller's own account
	 * @param type       filter to a single movement type, or {@code null} for all types
	 * @param from       inclusive start date, or {@code null}
	 * @param to         inclusive end date, or {@code null}
	 * @param minAmount  minimum absolute amount, or {@code null}
	 * @param maxAmount  maximum absolute amount, or {@code null}
	 * @param search     free text matched against the counterparty account number, or {@code null}
	 */
	public static Specification<Transaction> filter(Long accountId, TransactionType type,
			LocalDate from, LocalDate to, java.math.BigDecimal minAmount,
			java.math.BigDecimal maxAmount, String search) {

		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(cb.equal(root.get("account").get("id"), accountId));

			if (type != null) {
				predicates.add(cb.equal(root.get("type"), type));
			}
			if (from != null) {
				predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay()));
			}
			if (to != null) {
				predicates.add(cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay()));
			}
			if (minAmount != null) {
				predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), minAmount));
			}
			if (maxAmount != null) {
				predicates.add(cb.lessThanOrEqualTo(root.get("amount"), maxAmount));
			}
			if (search != null && !search.isBlank()) {
				// "%" + term + "%" is the SQL LIKE pattern; lower() makes it case-insensitive
				// because MySQL's default collation already is, but this does not depend on it.
				String pattern = "%" + search.trim().toLowerCase() + "%";
				predicates.add(cb.like(cb.lower(root.get("counterpartyAccountNumber")), pattern));
			}

			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}
}
