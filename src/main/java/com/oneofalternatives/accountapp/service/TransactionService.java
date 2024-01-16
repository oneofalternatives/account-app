package com.oneofalternatives.accountapp.service;

import com.oneofalternatives.accountapp.error.FundTransferValidationError;
import com.oneofalternatives.accountapp.api.PageProjection;
import com.oneofalternatives.accountapp.api.TransactionHistoryRecordProjection;
import com.oneofalternatives.accountapp.integration.CurrencyConversionClient;
import com.oneofalternatives.accountapp.model.Account;
import com.oneofalternatives.accountapp.model.Transaction;
import com.oneofalternatives.accountapp.repository.AccountRepository;
import com.oneofalternatives.accountapp.repository.OffsetLimitPageRequest;
import com.oneofalternatives.accountapp.repository.TransactionRepository;
import com.oneofalternatives.accountapp.settings.MoneySettings;
import jakarta.annotation.Nonnull;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.springframework.data.domain.Sort.Direction.DESC;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CurrencyConversionClient currencyConversionClient;
    private final MoneySettings moneySettings;

    public TransactionService(
        TransactionRepository transactionRepository,
        AccountRepository accountRepository,
        CurrencyConversionClient currencyConversionClient,
        MoneySettings moneySettings
    ) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.currencyConversionClient = currencyConversionClient;
        this.moneySettings = moneySettings;
    }

    public Page<Transaction> getAllTransactions(
        int offset,
        int limit
    ) {
        return transactionRepository.findAll(OffsetLimitPageRequest.of(offset, limit, Sort.by(DESC, "transactionDate")));
    }

    public PageProjection<TransactionHistoryRecordProjection> getTransactionHistoryByAccountId(
        int accountId,
        int offset,
        int limit
    ) {
        var transactionsPage =
            transactionRepository
                .findAllBySenderAccountIdOrReceiverAccountId(
                    accountId,
                    OffsetLimitPageRequest.of(offset, limit, Sort.by(DESC, "transactionDate"))
                );

        var transactions = transactionsPage.getContent();

        var transactionProjections =
            transactions.stream()
                .map(tx -> {
                    var accountData = collectTransactionDataFromAccount(accountId, tx);

                    // @formatter:off
                    return
                        TransactionHistoryRecordProjection.buildWith($ -> {
                            $.transactionId     = tx.getId();
                            $.peerAccount       = TransactionHistoryRecordProjection.AccountBaseInfoProjection.buildWith($$ -> {
                                                    $$.id       = accountData.account().getId();
                                                    $$.number   = accountData.account().getNumber();
                                                });
                            $.direction         = accountData.direction();
                            $.amount            = accountData.amount();
                            $.currency          = accountData.currency();
                            $.transactionDate   = tx.getTransactionDate();
                        });
                    // @formatter:on
                })
                .toList();

        return
            new PageProjection<>(
                transactionProjections,
                transactionsPage.getPageable().getOffset(),
                transactionsPage.getSize(),
                transactionsPage.getNumber(),
                transactionsPage.getTotalPages(),
                transactionsPage.getNumberOfElements(),
                transactionsPage.getTotalElements(),
                transactionsPage.isFirst(),
                transactionsPage.isLast()
            );
    }

    @Transactional
    @Nonnull
    public Transaction transferFunds(
        @Nonnull Integer senderAccountId,
        @Nonnull Integer receiverAccountId,
        @Nonnull BigDecimal amount
    ) {
        var transactionDate = LocalDateTime.now();

        verifyAmountScale(amount);
        verifyAccountIds(senderAccountId, receiverAccountId);

        var accountsById =
            accountRepository
                .findAllById(Set.of(senderAccountId, receiverAccountId))
                .stream()
                .collect(toMap(Account::getId, Function.identity()));

        verifyAccountsExist(List.of(senderAccountId, receiverAccountId), accountsById);

        var senderAccount = accountsById.get(senderAccountId);
        var receiverAccount = accountsById.get(receiverAccountId);

        var sourceCurrency = senderAccount.getCurrency();
        var targetCurrency = receiverAccount.getCurrency();

        var sourceAmount =
            sourceCurrency.equals(targetCurrency)
                ? amount
                : convert(amount, sourceCurrency, targetCurrency);

        verifyThatBalanceIsSufficient(senderAccount.getBalance(), sourceAmount);

        var transaction = new Transaction();
        transaction.setSenderAccount(senderAccount);
        transaction.setReceiverAccount(receiverAccount);
        transaction.setSourceAmount(sourceAmount);
        transaction.setTargetAmount(amount);
        transaction.setTransactionDate(transactionDate);

        senderAccount.setBalance(senderAccount.getBalance().subtract(sourceAmount));
        receiverAccount.setBalance(receiverAccount.getBalance().add(amount));

        accountRepository.saveAll(Set.of(senderAccount, receiverAccount));
        return transactionRepository.save(transaction);
    }

    private void verifyAmountScale(BigDecimal amount) {
        if (amount.scale() > moneySettings.scale()) {
            throw new FundTransferValidationError("Amount scale must not be greater than " + moneySettings.scale());
        }
    }

    private void verifyAccountIds(Integer senderAccountId, Integer receiverAccountId) {
        if (senderAccountId.equals(receiverAccountId)) {
            throw new FundTransferValidationError("Sender and receiver account must be different");
        }
    }

    private void verifyAccountsExist(
        List<Integer> requestedAccountIds,
        Map<Integer, Account> accounts
    ) {
        var nonExistingAccountIds =
            requestedAccountIds
                .stream()
                .filter(not(accounts.keySet()::contains))
                .map(String::valueOf)
                .collect(Collectors.toList());

        if (isNotEmpty(nonExistingAccountIds)) {
            throw new FundTransferValidationError(
                String.format(
                    "Accounts with these IDs do not exist: [%s]",
                    String.join(", ", nonExistingAccountIds)
                )
            );
        }
    }

    private BigDecimal convert(BigDecimal amount, Currency sourceCurrency, Currency targetCurrency) {
        verifyThatCurrenciesAreSupported(sourceCurrency, targetCurrency);

        var directRate = currencyConversionClient.getDirectRate(sourceCurrency, targetCurrency);
        return amount.divide(directRate, moneySettings.scale(), moneySettings.roundingMode());
    }

    private void verifyThatCurrenciesAreSupported(Currency... currencies) {
        var supportedCurrencies = currencyConversionClient.getSupportedCurrencies();

        var unsupportedCurrencies =
            Arrays.stream(currencies)
                .filter(not(supportedCurrencies::contains))
                .map(Currency::getCurrencyCode)
                .distinct()
                .collect(Collectors.toList());

        if (isNotEmpty(unsupportedCurrencies)) {
            throw new FundTransferValidationError(
                String.format(
                    "Conversion from/to any of these currencies is not supported: [%s]",
                    String.join(", ", unsupportedCurrencies)
                )
            );
        }
    }

    private static void verifyThatBalanceIsSufficient(BigDecimal accountBalance, BigDecimal transactionAmount) {
        if (accountBalance.subtract(transactionAmount).compareTo(BigDecimal.ZERO) < 0) {
            throw new FundTransferValidationError("Source account has insufficient balance");
        }
    }

    private static TransactionDataFromAccount collectTransactionDataFromAccount(
        int accountId,
        Transaction tx
    ) {
        var direction =
            tx.getSenderAccount().getId() == accountId
                ? TransactionHistoryRecordProjection.Direction.OUTBOUND
                : TransactionHistoryRecordProjection.Direction.INBOUND;

        var account =
            direction == TransactionHistoryRecordProjection.Direction.OUTBOUND
                ? tx.getReceiverAccount()
                : tx.getSenderAccount();

        var amount =
            direction == TransactionHistoryRecordProjection.Direction.OUTBOUND
                ? tx.getSourceAmount()
                : tx.getTargetAmount();

        var currency =
            direction == TransactionHistoryRecordProjection.Direction.OUTBOUND
                ? tx.getSenderAccount().getCurrency()
                : tx.getReceiverAccount().getCurrency();

        return new TransactionDataFromAccount(direction, account, amount, currency);
    }

    private record TransactionDataFromAccount(
        TransactionHistoryRecordProjection.Direction direction,
        Account account,
        BigDecimal amount,
        Currency currency
    ) { }
}
