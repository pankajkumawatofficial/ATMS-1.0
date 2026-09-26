package com.learning.atm.controller;

import com.learning.atm.dto.TransactionResponse;
import com.learning.atm.dto.TransferRequest;
import com.learning.atm.security.AuthInterceptor;
import com.learning.atm.service.AtmService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Money always leaves the logged-in account (taken from the token), never one supplied in
 * the body — otherwise a stolen token could drain someone else's account.
 */
@RestController
@RequestMapping("/api/transfers")
public class TransferController {

	private final AtmService atmService;

	public TransferController(AtmService atmService) {
		this.atmService = atmService;
	}

	@PostMapping
	public List<TransactionResponse> transfer(@Valid @RequestBody TransferRequest request,
			@RequestAttribute(AuthInterceptor.ACCOUNT_ID_ATTRIBUTE) long sessionAccountId) {
		return atmService.transfer(sessionAccountId, request.toAccountNumber(),
				request.amount(), request.pin());
	}
}
