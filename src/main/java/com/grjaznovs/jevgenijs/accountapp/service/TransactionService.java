package com.grjaznovs.jevgenijs.accountapp.service;

import com.grjaznovs.jevgenijs.accountapp.error.FundTransferValidationError;
import com.grjaznovs.jevgenijs.accountapp.api.PageProjection;
import com.grjaznovs.jevgenijs.accountapp.api.TransactionHistoryRecordProjection;
import com.grjaznovs.jevgenijs.accountapp.api.TransactionHistoryRecordProjection.AccountBaseInfoProjection;
import com.grjaznovs.jevgenijs.accountapp.integration.CurrencyConversionClient;
import com.grjaznovs.jevgenijs.accountapp.model.Account;
import com.grjaznovs.jevgenijs.accountapp.model.Transaction;
import com.grjaznovs.jevgenijs.accountapp.repository.AccountRepository;
import com.grjaznovs.jevgenijs.accountapp.repository.OffsetLimitPageRequest;
import com.grjaznovs.jevgenijs.accountapp.repository.TransactionRepository;
import com.grjaznovs.jevgenijs.accountapp.settings.MoneySettings;
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

import static com.grjaznovs.jevgenijs.accountapp.api.TransactionHistoryRecordProjection.Direction.INBOUND;
import static com.grjaznovs.jevgenijs.accountapp.api.TransactionHistoryRecordProjection.Direction.OUTBOUND;
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
                            $.peerAccount       = AccountBaseInfoProjection.buildWith($$ -> {
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
        @Nonnull BigDecimal amount,
        @Nonnull LocalDateTime transactionDate
    ) {
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
                : convert(amount, sourceCurrency, targetCurrency, transactionDate);

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

    private BigDecimal convert(BigDecimal amount, Currency sourceCurrency, Currency targetCurrency, LocalDateTime transactionDate) {
        verifyThatCurrenciesAreSupported(sourceCurrency, targetCurrency);

        var directRate = currencyConversionClient.getDirectRate(sourceCurrency, targetCurrency, transactionDate.toLocalDate());
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

    private static TransactionDataFromAccount collectTransactionDataFromAccount(
        int accountId,
        Transaction tx
    ) {
        var direction =
            tx.getSenderAccount().getId() == accountId
                ? OUTBOUND
                : INBOUND;

        var account =
            direction == OUTBOUND
                ? tx.getReceiverAccount()
                : tx.getSenderAccount();

        var amount =
            direction == OUTBOUND
                ? tx.getSourceAmount()
                : tx.getTargetAmount();

        var currency =
            direction == OUTBOUND
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
