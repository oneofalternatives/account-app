package com.grjaznovs.jevgenijs.accountapp.service;

import com.grjaznovs.jevgenijs.accountapp.api.TransactionHistoryRecordProjection;
import com.grjaznovs.jevgenijs.accountapp.api.TransactionHistoryRecordProjection.AccountBaseInfoProjection;
import com.grjaznovs.jevgenijs.accountapp.model.Account;
import com.grjaznovs.jevgenijs.accountapp.model.Transaction;
import com.grjaznovs.jevgenijs.accountapp.repository.AccountRepository;
import com.grjaznovs.jevgenijs.accountapp.repository.OffsetLimitPageRequest;
import com.grjaznovs.jevgenijs.accountapp.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;
import static org.springframework.data.domain.Sort.Direction.DESC;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public TransactionService(
        TransactionRepository transactionRepository,
        AccountRepository accountRepository
    ) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    public Page<Transaction> getAllTransactions(
        int offset,
        int limit
    ) {
        return
            transactionRepository
                .findAll(
                    OffsetLimitPageRequest.of(offset, limit, Sort.by(DESC, "transactionDate"))
                );
    }

    public Page<TransactionHistoryRecordProjection> getTransactionHistoryByAccountId(
        int accountId,
        int offset,
        int limit
    ) {
        var transactionsPage =
            transactionRepository
                .findAllBySenderAccountIdOrReceiverAccountId(
                    accountId,
                    OffsetLimitPageRequest.of(
                        offset,
                        limit,
                        Sort.by(DESC, "transactionDate")
                    )
                );

        var transactions = transactionsPage.getContent();

        var otherAccountIds =
            transactions.stream()
                .flatMap(tx -> Stream.of(tx.getSenderAccountId(), tx.getReceiverAccountId()))
                .collect(Collectors.toSet());

        var accountsById =
            accountRepository
                .findAllById(otherAccountIds)
                .stream()
                .collect(toMap(Account::getId, Function.identity()));


        var transactionProjections = transactions.stream()
            .map(tx -> {
                var senderAccount = accountsById.get(tx.getSenderAccountId());
                var receiverAccount = accountsById.get(tx.getReceiverAccountId());

                return
                    new TransactionHistoryRecordProjection(
                        tx.getId(),
                        new AccountBaseInfoProjection(
                            senderAccount.getId(),
                            senderAccount.getNumber()
                        ),
                        new AccountBaseInfoProjection(
                            receiverAccount.getId(),
                            receiverAccount.getNumber()
                        ),
                        tx.getAmount(),
                        receiverAccount.getCurrency(),
                        tx.getExchangeRate(),
                        tx.getTransactionDate()
                    );
            })
            .toList();

        return new PageImpl<>(
            transactionProjections,
            transactionsPage.getPageable(),
            transactionsPage.getTotalElements()
        );
    }
}
