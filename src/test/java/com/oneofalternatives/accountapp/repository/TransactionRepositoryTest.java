package com.oneofalternatives.accountapp.repository;

import com.oneofalternatives.accountapp.model.Account;
import com.oneofalternatives.accountapp.model.Transaction;
import com.oneofalternatives.accountapp.util.TransactionTestFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.Currency;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.oneofalternatives.accountapp.util.AccountTestFactory.accountWith;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TransactionRepositoryTest {

    private final Account eurAccount = accountWith(1, "ACC-0001", 100.00, Currency.getInstance("EUR"));
    private final Account usdAccount = accountWith(2, "ACC-0002", 100.00, Currency.getInstance("USD"));
    private final Account audAccount = accountWith(2, "ACC-0003", 100.00, Currency.getInstance("AUD"));

    private final List<Transaction> eurToUsdTransactions =
        generateTransactionsForAccounts(eurAccount, usdAccount, 25, "2020-01-01T00:00");
    private final List<Transaction> usdToAudTransactions =
        generateTransactionsForAccounts(usdAccount, audAccount, 30, "2022-01-01T00:00");

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Autowired
    TransactionRepositoryTest(
        AccountRepository accountRepository,
        TransactionRepository transactionRepository
    ) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @BeforeEach
    void beforeEach() {
        accountRepository.saveAll(List.of(eurAccount, usdAccount, audAccount));
        transactionRepository.saveAll(eurToUsdTransactions);
        transactionRepository.saveAll(usdToAudTransactions);
    }

    @Test
    void shouldReturnAllTransactionsRelatedToAccount() {
        var expectedUsdAccountTransactions =
            Stream.of(eurToUsdTransactions, usdToAudTransactions)
                .flatMap(Collection::stream)
                .sorted(Comparator.comparing(Transaction::getTransactionDate))
                .toList();

        var pageRequest = (Pageable) OffsetLimitPageRequest.of(0, 100, Sort.by("transactionDate"));

        verifyPage(usdAccount.getId(), pageRequest, 55, 55, expectedUsdAccountTransactions);
    }

    @Test
    void shouldReturnPageableTransactionHistory() {
        var accId = eurAccount.getId();
        // expected transactions
        var txs = eurToUsdTransactions.reversed();
        var pageRequest = (Pageable) OffsetLimitPageRequest.of(0, 10, Sort.by("transactionDate").descending());

        // @formatter:off
        var firstPage  = verifyPage(accId, pageRequest,                   10, 25, txs.subList( 0, 10));
        var secondPage = verifyPage(accId, firstPage.next(),              10, 25, txs.subList(10, 20));
        var thirdPage  = verifyPage(accId, secondPage.next(),             5,  25, txs.subList(20, 25));

                         verifyPage(accId, thirdPage.previousOrFirst(),  10,  25, txs.subList(10, 20));
                         verifyPage(accId, secondPage.previousOrFirst(), 10,  25, txs.subList( 0, 10));
                         verifyPage(accId, thirdPage.first(),            10,  25, txs.subList( 0, 10));
                         verifyPage(accId, thirdPage.next(),              0,  25, List.of());
        // @formatter:on
    }

    private Pageable verifyPage(
        Integer accountId,
        Pageable pageable,
        int expectedNumberOfElementsOnPage,
        int expectedTotalElements,
        List<Transaction> expectedTransactions
    ) {
        var page = transactionRepository.findAllBySenderAccountIdOrReceiverAccountId(accountId, pageable);

        var transactions = page.getContent();
        Assertions.assertThat(transactions).containsExactlyElementsOf(expectedTransactions);

        Assertions.assertThat(page.getContent()).hasSize(expectedNumberOfElementsOnPage);
        assertThat(page.getNumberOfElements()).isEqualTo(expectedNumberOfElementsOnPage);

        assertThat(page.getSize()).isEqualTo(pageable.getPageSize());
        assertThat(page.getTotalElements()).isEqualTo(expectedTotalElements);
        assertThat(page.getNumber()).isEqualTo(pageable.getPageNumber());
        assertThat(page.getTotalPages()).isEqualTo((int) Math.ceil(expectedTotalElements / (double) pageable.getPageSize()));

        assertThat(page.getPageable()).isInstanceOf(OffsetLimitPageRequest.class);

        return pageable;
    }

    private static List<Transaction> generateTransactionsForAccounts(
        Account senderAccount,
        Account receiverAccount,
        int count,
        String initialTransactionDate
    ) {
        return
            IntStream.rangeClosed(1, count)
                .mapToObj(increment ->
                    TransactionTestFactory.transactionWith(
                        senderAccount,
                        receiverAccount,
                        increment,
                        increment,
                        LocalDateTime.parse(initialTransactionDate).plusHours(increment)
                    )
                )
                .collect(Collectors.toList());
    }
}
