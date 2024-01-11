package com.grjaznovs.jevgenijs.accountapp.controller;

import com.grjaznovs.jevgenijs.accountapp.model.Transaction;
import com.grjaznovs.jevgenijs.accountapp.repository.OffsetLimitPageRequest;
import com.grjaznovs.jevgenijs.accountapp.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.data.domain.Sort.Direction.DESC;

@RestController
@RequestMapping("/transaction")
public class TransactionController {

    private final TransactionRepository transactionRepository;

    public TransactionController(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @GetMapping
    public Page<Transaction> findTransactionsByAccountId(
        @RequestParam(required = false) Integer accountId,
        @RequestParam int offset,
        @RequestParam int limit
    ) {
        if (accountId == null) {
            return transactionRepository.findAll(
                OffsetLimitPageRequest.of(offset, limit, Sort.by(DESC, "transactionDate"))
            );
        } else {
            return transactionRepository.findAllBySenderAccountIdOrReceiverAccountId(
                accountId,
                OffsetLimitPageRequest.of(offset, limit, Sort.by(DESC, "transactionDate"))
//                PageRequest.of(offset, limit, Sort.by(DESC, "transactionDate"))
            );
        }
    }
}
