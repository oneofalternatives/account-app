package com.grjaznovs.jevgenijs.accountapp.service;

import com.grjaznovs.jevgenijs.accountapp.api.FundTransferException;
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
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        return
            transactionRepository
                .findAll(
                    OffsetLimitPageRequest.of(offset, limit, Sort.by(DESC, "transactionDate"))
                );
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
                var direction =
                    tx.getSenderAccountId() == accountId
                        ? OUTBOUND
                        : INBOUND;

                var senderAccount = accountsById.get(tx.getSenderAccountId());
                var receiverAccount = accountsById.get(tx.getReceiverAccountId());

                var account =
                    direction == OUTBOUND
                        ? receiverAccount
                        : senderAccount;

                var amount =
                    direction == OUTBOUND
                        ? tx.getSourceAmount()
                        : tx.getTargetAmount();

                var currency =
                    direction == OUTBOUND
                        ? senderAccount.getCurrency()
                        : receiverAccount.getCurrency();

                return
                    new TransactionHistoryRecordProjection(
                        tx.getId(),
                        new AccountBaseInfoProjection(
                            account.getId(),
                            account.getNumber()
                        ),
                        direction,
                        amount,
                        currency,
                        tx.getTransactionDate()
                    );
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
    public Transaction transferFunds(
        Integer senderAccountId,
        Integer receiverAccountId,
        BigDecimal amount,
        LocalDateTime transactionDate
    ) {
        verifyAmountScale(amount);
        verifyAccountIds(senderAccountId, receiverAccountId);

        var accountsById =
            accountRepository
                .findAllById(Set.of(senderAccountId, receiverAccountId))
                .stream()
                .collect(toMap(Account::getId, Function.identity()));

        verifyAccountsArePresent(Set.of(senderAccountId, receiverAccountId), accountsById);

        var senderAccount = accountsById.get(senderAccountId);
        var receiverAccount = accountsById.get(receiverAccountId);

        var sourceCurrency = senderAccount.getCurrency();
        var targetCurrency = receiverAccount.getCurrency();

        verifyThatCurrenciesAreSupported(sourceCurrency, targetCurrency);

        var sourceAmount =
            currencyConversionClient
                .convert(amount, targetCurrency, sourceCurrency, transactionDate.toLocalDate())
                .setScale(moneySettings.scale(), moneySettings.roundingMode());

        var transaction = new Transaction();
        transaction.setSenderAccountId(senderAccountId);
        transaction.setReceiverAccountId(receiverAccountId);
        transaction.setSourceAmount(sourceAmount);
        transaction.setTargetAmount(amount);
        transaction.setTransactionDate(transactionDate);

        senderAccount.setBalance(senderAccount.getBalance().subtract(sourceAmount));
        receiverAccount.setBalance(receiverAccount.getBalance().add(amount));

        accountRepository.saveAllAndFlush(Set.of(senderAccount, receiverAccount));
        return transactionRepository.saveAndFlush(transaction);
    }

    private void verifyAmountScale(BigDecimal amount) {
        if (amount.scale() > moneySettings.scale()) {
            throw new FundTransferException("Amount scale must not be greater than " + moneySettings.scale());
        }
    }

    private void verifyAccountIds(Integer senderAccountId, Integer receiverAccountId) {
        if (senderAccountId == null || receiverAccountId == null) {
            throw new FundTransferException("Both sender and receiver accounts IDs must be passed");
        }

        if (senderAccountId.equals(receiverAccountId)) {
            throw new FundTransferException("Sender account and receiver account are the same");
        }
    }

    private void verifyAccountsArePresent(
        Set<Integer> requestedAccountIds,
        Map<Integer, Account> accounts
    ) {
        var nonExistingAccountIds =
            requestedAccountIds
                .stream()
                .filter(not(accounts.keySet()::contains))
                .map(String::valueOf)
                .collect(Collectors.toSet());

        if (isNotEmpty(nonExistingAccountIds)) {
            throw new FundTransferException(
                String.format(
                    "Accounts with these IDs do not exist: [%s]",
                    String.join(", ", nonExistingAccountIds)
                )
            );
        }
    }

    private void verifyThatCurrenciesAreSupported(String... currencies) {
        var supportedCurrencies = currencyConversionClient.getSupportedCurrencies();

        var unsupportedCurrencies =
            Arrays.stream(currencies)
                .filter(not(supportedCurrencies::contains))
                .collect(Collectors.toSet());

        if (isNotEmpty(unsupportedCurrencies)) {
            throw new FundTransferException(
                String.format(
                    "Conversion from/to any of these currencies is not supported: [%s]",
                    String.join(", ", unsupportedCurrencies)
                )
            );
        }
    }
}
