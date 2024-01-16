package com.grjaznovs.jevgenijs.accountapp.integrationtest;

import com.grjaznovs.jevgenijs.accountapp.api.PageProjection;
import com.grjaznovs.jevgenijs.accountapp.api.TransactionHistoryRecordProjection;
import com.grjaznovs.jevgenijs.accountapp.error.CurrencyExchangeServiceError;
import com.grjaznovs.jevgenijs.accountapp.integration.CurrencyConversionClient;
import com.grjaznovs.jevgenijs.accountapp.integration.CurrencyConverterMockSettings;
import com.grjaznovs.jevgenijs.accountapp.model.Account;
import com.grjaznovs.jevgenijs.accountapp.model.Transaction;
import com.grjaznovs.jevgenijs.accountapp.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilderFactory;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Set;

import static com.grjaznovs.jevgenijs.accountapp.api.TransactionHistoryRecordProjection.Direction.INBOUND;
import static com.grjaznovs.jevgenijs.accountapp.api.TransactionHistoryRecordProjection.Direction.OUTBOUND;
import static com.grjaznovs.jevgenijs.accountapp.util.AccountTestFactory.accountWith;
import static com.grjaznovs.jevgenijs.accountapp.util.Currencies.*;
import static com.grjaznovs.jevgenijs.accountapp.util.TypeUtils.scaledBigDecimal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-integrationtest.properties")
class TransferFundsIntegrationTest {

    private static final UriBuilderFactory URI_BUILDER_FACTORY = new DefaultUriBuilderFactory();

    @MockBean
    private CurrencyConversionClient currencyConversionClientMock;
    @Autowired
    private CurrencyConverterMockSettings currencyConverterMockSettings;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private TestRestTemplate testRestTemplate;

    @Test
    void shouldReturnEmptyTransactionHistoryWhenAccountDoesNotExist() {
        var nonExistingAccountId = getMaxAccountId() + 1;

        var transactionHistoryPage = restGetTransactionHistoryForAccountId(nonExistingAccountId, Paging.of(0, 10));

        assertThat(transactionHistoryPage.content())
            .isEmpty();
    }

    @Test
    void shouldReturnEmptyTransactionHistoryWhenAccountHasNoTransactions() {
        var account = accountWith(1, "ACC-0001", 1000.00, EUR);

        accountRepository.saveAndFlush(account);

        var transactionHistoryPage = restGetTransactionHistoryForAccountId(account.getId(), Paging.of(0, 10));

        assertThat(transactionHistoryPage.content())
            .isEmpty();
    }

    @Test
    void shouldNotRegisterFundTransferWhenAccountDoesNotExit() {
        var maxAccountId = (int) getMaxAccountId();

        var senderAccountId = maxAccountId + 1;
        var receiverAccountId = maxAccountId + 2;
        var responseEntity = restPostFundTransferFail(senderAccountId, receiverAccountId, 30.00);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseEntity.getBody()).startsWith("Accounts with these IDs do not exist:");
    }

    @Test
    void shouldNotRegisterFundTransferWhenCurrencyConversionClientReturnedError() {
        when(currencyConversionClientMock.getSupportedCurrencies())
            .thenReturn(currencyConverterMockSettings.supportedCurrencies());

        when(currencyConversionClientMock.getDirectRate(any(), any()))
            .thenThrow(new CurrencyExchangeServiceError("Error description"));

        var maxClientId = getMaxClientId();
        var clientOne = maxClientId + 1;
        var clientTwo = clientOne + 1;

        var eurAccount = accountWith(clientOne, "ACC-0001", 1000.00, EUR);
        var usdAccount = accountWith(clientTwo, "ACC-0002", 1000.00, USD);

        accountRepository.saveAllAndFlush(Set.of(eurAccount, usdAccount));

        var response = restPostFundTransferFail(eurAccount.getId(), usdAccount.getId(), 30.00);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isEqualTo("Error description");
    }

    @Test
    void shouldRegisterFundTransfer() {
        when(currencyConversionClientMock.getSupportedCurrencies())
            .thenReturn(currencyConverterMockSettings.supportedCurrencies());

        when(currencyConversionClientMock.getDirectRate(any(), any()))
            .thenAnswer((Answer<BigDecimal>) invocation -> {
                var fromCurrency = (Currency) invocation.getArgument(0);
                var toCurrency = (Currency) invocation.getArgument(1);
                return
                    currencyConverterMockSettings
                        .exchangeRates()
                        .get(fromCurrency.getCurrencyCode() + toCurrency.getCurrencyCode());
            });

        var clientOne = getMaxClientId() + 1;
        var clientTwo = clientOne + 1;

        var eurAccount = accountWith(clientOne, "ACC-0001", 1000.00, EUR);
        var usdAccount = accountWith(clientTwo, "ACC-0002", 1000.00, USD);
        var audAccount = accountWith(clientTwo, "ACC-0003", 1000.00, AUD);

        accountRepository.saveAllAndFlush(Set.of(eurAccount, usdAccount, audAccount));

        var eurUsdTransaction =
            restPostFundTransferSuccess(eurAccount.getId(), usdAccount.getId(), 30.00);
        var usdEurTransaction =
            restPostFundTransferSuccess(usdAccount.getId(), eurAccount.getId(), 50.00);
        var usdAudTransaction =
            restPostFundTransferSuccess(usdAccount.getId(), audAccount.getId(), 99.00);

        assertThat(Set.of(eurUsdTransaction, usdEurTransaction, usdAudTransaction))
            .allSatisfy(tx -> assertThat(tx.getId()).isNotNull());

        var eurAccountTransactionHistoryPage = restGetTransactionHistoryForAccountId(eurAccount.getId(), Paging.of(0, 10));
        var usdAccountTransactionHistoryPage = restGetTransactionHistoryForAccountId(usdAccount.getId(), Paging.of(0, 10));
        var audAccountTransactionHistoryPage = restGetTransactionHistoryForAccountId(audAccount.getId(), Paging.of(0, 10));

        // @formatter:off
        assertThat(eurAccountTransactionHistoryPage.content())
            .containsExactly(
                TransactionHistoryRecordProjection.buildWith($ -> {
                    $.transactionId     = usdEurTransaction.getId();
                    $.direction         = INBOUND;
                    $.peerAccount       = TransactionHistoryRecordProjection.AccountBaseInfoProjection.buildWith($$ -> {
                                            $$.id       = usdAccount.getId();
                                            $$.number   = usdAccount.getNumber();
                                        });
                    $.amount            = scaledBigDecimal(50.00);
                    $.currency          = eurAccount.getCurrency();
                    $.transactionDate   = usdEurTransaction.getTransactionDate();
                }),
                TransactionHistoryRecordProjection.buildWith($ -> {
                    $.transactionId     = eurUsdTransaction.getId();
                    $.direction         = OUTBOUND;
                    $.peerAccount       = TransactionHistoryRecordProjection.AccountBaseInfoProjection.buildWith($$ -> {
                                            $$.id       = usdAccount.getId();
                                            $$.number   = usdAccount.getNumber();
                                        });
                    $.amount            = scaledBigDecimal(33.0760749724);
                    $.currency          = eurAccount.getCurrency();
                    $.transactionDate   = eurUsdTransaction.getTransactionDate();
                })
            );
        // @formatter:on

        // @formatter:off
        assertThat(usdAccountTransactionHistoryPage.content())
            .containsExactly(
                TransactionHistoryRecordProjection.buildWith($ -> {
                    $.transactionId     = usdAudTransaction.getId();
                    $.direction         = OUTBOUND;
                    $.peerAccount       = TransactionHistoryRecordProjection.AccountBaseInfoProjection.buildWith($$ -> {
                                            $$.id       = audAccount.getId();
                                            $$.number   = audAccount.getNumber();
                                        });
                    $.amount            = scaledBigDecimal(66.00);
                    $.currency          = usdAccount.getCurrency();
                    $.transactionDate   = usdAudTransaction.getTransactionDate();
                }),
                TransactionHistoryRecordProjection.buildWith($ -> {
                    $.transactionId     = usdEurTransaction.getId();
                    $.direction         = OUTBOUND;
                    $.peerAccount       = TransactionHistoryRecordProjection.AccountBaseInfoProjection.buildWith($$ -> {
                                            $$.id       = eurAccount.getId();
                                            $$.number   = eurAccount.getNumber();
                                        });
                    $.amount            = scaledBigDecimal(45.4409921788);
                    $.currency          = usdAccount.getCurrency();
                    $.transactionDate   = usdEurTransaction.getTransactionDate();
                }),
                TransactionHistoryRecordProjection.buildWith($ -> {
                    $.transactionId     = eurUsdTransaction.getId();
                    $.direction         = INBOUND;
                    $.peerAccount       = TransactionHistoryRecordProjection.AccountBaseInfoProjection.buildWith($$ -> {
                                            $$.id       = eurAccount.getId();
                                            $$.number   = eurAccount.getNumber();
                                        });
                    $.amount            = scaledBigDecimal(30.00);
                    $.currency          = usdAccount.getCurrency();
                    $.transactionDate   = eurUsdTransaction.getTransactionDate();
                })
            );
        // @formatter:on

        // @formatter:off
        assertThat(audAccountTransactionHistoryPage.content())
            .containsExactly(
                TransactionHistoryRecordProjection.buildWith($ -> {
                    $.transactionId     = usdAudTransaction.getId();
                    $.direction         = INBOUND;
                    $.peerAccount       = TransactionHistoryRecordProjection.AccountBaseInfoProjection.buildWith($$ -> {
                                            $$.id       = usdAccount.getId();
                                            $$.number   = usdAccount.getNumber();
                                        });
                    $.amount            = scaledBigDecimal(99.00);
                    $.currency          = audAccount.getCurrency();
                    $.transactionDate   = usdAudTransaction.getTransactionDate();
                })
            );
        // @formatter:on

        var accountsForClientOne = restGetAccountsForClientId(clientOne);
        var accountsForClientTwo = restGetAccountsForClientId(clientTwo);

        assertThat(accountsForClientOne)
            .extracting(Account::getId, Account::getBalance)
            .containsExactly(tuple(eurAccount.getId(), scaledBigDecimal(1016.9239250276)));

        assertThat(accountsForClientTwo)
            .extracting(Account::getId, Account::getBalance)
            .containsExactlyInAnyOrder(
                tuple(usdAccount.getId(), scaledBigDecimal(918.5590078212)),
                tuple(audAccount.getId(), scaledBigDecimal(1099.00))
            );
    }

    private Integer getMaxAccountId() {
        return accountRepository
            .findAll()
            .stream()
            .map(Account::getId)
            .max(Integer::compareTo)
            .orElse(0);
    }

    private PageProjection<TransactionHistoryRecordProjection> restGetTransactionHistoryForAccountId(
        int accountId,
        Paging paging
    ) {
        var url =
            URI_BUILDER_FACTORY
                .uriString("/transaction/history")
                .queryParam("accountId", accountId)
                .queryParam("offset", paging.offset())
                .queryParam("limit", paging.limit())
                .build();

        return assertOkAndGetBody(testRestTemplate.exchange(url, GET, null, new ParameterizedTypeReference<>() { }));
    }

    private Transaction restPostFundTransferSuccess(
        int senderAccountId,
        int receiverAccountId,
        double amount
    ) {
        var responseEntity =
            restPostFundTransfer(
                senderAccountId,
                receiverAccountId,
                amount,
                Transaction.class
            );

        return assertOkAndGetBody(responseEntity);
    }

    private ResponseEntity<String> restPostFundTransferFail(
        int senderAccountId,
        int receiverAccountId,
        double amount
    ) {
        return
            restPostFundTransfer(
                senderAccountId,
                receiverAccountId,
                amount,
                String.class
            );
    }

    private <T> ResponseEntity<T> restPostFundTransfer(
        int senderAccountId,
        int receiverAccountId,
        double amount,
        Class<T> responseBodyType
    ) {
        var url =
            URI_BUILDER_FACTORY
                .uriString("/transaction/fund-transfer")
                .queryParam("senderAccountId", senderAccountId)
                .queryParam("receiverAccountId", receiverAccountId)
                .queryParam("amount", BigDecimal.valueOf(amount))
                .build();

        return testRestTemplate.exchange(url, POST, null, responseBodyType);
    }

    private List<Account> restGetAccountsForClientId(int clientId) {
        var url =
            URI_BUILDER_FACTORY
                .uriString("/account")
                .queryParam("clientId", clientId)
                .build();

        var responseEntity =
            testRestTemplate
                .exchange(
                    url,
                    GET,
                    null,
                    new ParameterizedTypeReference<List<Account>>() { }
                );

        return assertOkAndGetBody(responseEntity);
    }

    private <T> T assertOkAndGetBody(
        ResponseEntity<T> responseEntity
    ) {
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        var body = responseEntity.getBody();
        assertThat(body).isNotNull();

        return body;
    }

    private int getMaxClientId() {
        return
            accountRepository
                .findAll().stream()
                .map(Account::getClientId)
                .max(Integer::compareTo)
                .orElseThrow();
    }

    private record Paging(int offset, int limit) {

        public static Paging of(int offset, int limit) {
            return new Paging(offset, limit);
        }
    }
}
