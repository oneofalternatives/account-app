package com.grjaznovs.jevgenijs.accountapp.controller;

import com.grjaznovs.jevgenijs.accountapp.api.PageProjection;
import com.grjaznovs.jevgenijs.accountapp.api.TransactionHistoryRecordProjection;
import com.grjaznovs.jevgenijs.accountapp.model.Transaction;
import com.grjaznovs.jevgenijs.accountapp.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(
        TransactionService transactionService
    ) {
        this.transactionService = transactionService;
    }

    @Operation(
        summary = "List transaction history (for testing purposes)",
        description = "Returns plain transaction objects with nested accounts. Supports pagination.")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", description = "Page with transactions, may be empty if no transactions",
            content = @Content(mediaType = "application/json", array = @ArraySchema(items = @Schema(implementation = List.class))))
    })
    @GetMapping(path = "/transaction")
    public Page<Transaction> findAllTransactions(
        @RequestParam int offset,
        @RequestParam int limit
    ) {
        return transactionService.getAllTransactions(offset, limit);
    }

    @Operation(
        summary = "List transaction history by account ID",
        description = "Also returns the info of the other account participated in a transaction. Supports pagination." +
            "Transactions are sorted descending (latest first)")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", description = "Page with transactions, may be empty if account doesn't have any transactions",
            content = @Content(mediaType = "application/json", array = @ArraySchema(items = @Schema(implementation = PageProjection.class))))
    })
    @GetMapping(path = "/transaction/history")
    public PageProjection<TransactionHistoryRecordProjection> findTransactionsByAccountId(
        Integer accountId,
        int offset,
        int limit
    ) {
        return transactionService.getTransactionHistoryByAccountId(accountId, offset, limit);
    }

    @Operation(
        summary = "Transfer funds between two accounts",
        description =
            "Registers a transaction and updates account balances. " +
                "If accounts have different currencies, uses currency conversion service.")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", description = "Funds transferred successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Transaction.class))),
        @ApiResponse(
            responseCode = "400", description = "Input validation failed, details are given in the response",
            content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class))),
        @ApiResponse(
            responseCode = "503", description = "Currency conversion service error, details are given in the response",
            content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class)))
    })
    @PostMapping(path = "/transaction/fund-transfer")
    public Transaction transferFunds(
        int senderAccountId,
        int receiverAccountId,
        BigDecimal amount,
        LocalDateTime transactionDate
    ) {
        return transactionService.transferFunds(senderAccountId, receiverAccountId, amount, transactionDate);
    }
}
