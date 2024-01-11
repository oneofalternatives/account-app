package com.grjaznovs.jevgenijs.accountapp.controller;

import com.grjaznovs.jevgenijs.accountapp.api.TransactionHistoryRecordProjection;
import com.grjaznovs.jevgenijs.accountapp.model.Transaction;
import com.grjaznovs.jevgenijs.accountapp.repository.TransactionRepository;
import com.grjaznovs.jevgenijs.accountapp.service.TransactionService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(
        TransactionRepository transactionRepository,
        TransactionService transactionService
    ) {
        this.transactionService = transactionService;
    }

    @GetMapping(path = "/transaction")
    public Page<Transaction> findAllTransactions(
        @RequestParam int offset,
        @RequestParam int limit
    ) {
        return transactionService.getAllTransactions(offset, limit);
    }

    @GetMapping(path = "/transaction/history")
    public Page<TransactionHistoryRecordProjection> findTransactionsByAccountId(
        @RequestParam Integer accountId,
        @RequestParam int offset,
        @RequestParam int limit
    ) {
        return
            transactionService.getTransactionHistoryByAccountId(accountId, offset, limit);
    }

    @PostMapping(path = "/transaction/fund-transfer")
    public Transaction transferFunds(
        int senderAccountId,
        int receiverAccountId,
        int amount
    ) {
        return transactionService.transferFunds(senderAccountId, receiverAccountId, amount);
    }
}
