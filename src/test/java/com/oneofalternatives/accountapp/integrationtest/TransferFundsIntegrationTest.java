package com.oneofalternatives.accountapp.integrationtest;

import com.oneofalternatives.accountapp.api.AccountProjection;
import com.oneofalternatives.accountapp.api.TransactionHistoryRecordProjection;
import com.oneofalternatives.accountapp.api.TransactionHistoryRecordProjection.AccountBaseInfoProjection;
import com.oneofalternatives.accountapp.api.TransactionHistoryRecordProjection.Direction;
import com.oneofalternatives.accountapp.error.CurrencyExchangeServiceError;
import com.oneofalternatives.accountapp.integration.CurrencyConversionClient;
import com.oneofalternatives.accountapp.integration.CurrencyConverterMockSettings;
import com.oneofalternatives.accountapp.integrationtest.TestAccountAppRestClient.Paging;
import com.oneofalternatives.accountapp.model.Account;
import com.oneofalternatives.accountapp.util.TypeUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Set;

import static com.oneofalternatives.accountapp.util.CreateAccountProjectionFactory.createAccountProjection;
import static com.oneofalternatives.accountapp.util.Currencies.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-integrationtest.properties")
class TransferFundsIntegrationTest {

    @MockBean
    private CurrencyConversionClient currencyConversionClientMock;
    @Autowired
    private CurrencyConverterMockSettings currencyConverterMockSettings;
    @Autowired
    private TestAccountAppRestClient rest;

    @Test
    void shouldReturnEmptyTransactionHistoryWhenAccountDoesNotExist() {
        var nonExistingAccountId = getMaxAccountId() + 1;

        var transactionHistoryPage = rest.getTransactionHistoryFor(nonExistingAccountId, Paging.of(0, 10));

        Assertions.assertThat(transactionHistoryPage.content())
            .isEmpty();
    }

    @Test
    void shouldReturnEmptyTransactionHistoryWhenAccountHasNoTransactions() {
        var account = rest.putAccountSuccess(createAccountProjection(1, "ACC-0001", 1000.00, EUR));

        var transactionHistoryPage = rest.getTransactionHistoryFor(account.getId(), Paging.of(0, 10));

        Assertions.assertThat(transactionHistoryPage.content())
            .isEmpty();
    }

    @Test
    void shouldNotRegisterFundTransferWhenAccountDoesNotExit() {
        var maxAccountId = (int) getMaxAccountId();

        var senderAccountId = maxAccountId + 1;
        var receiverAccountId = maxAccountId + 2;
        var responseEntity = rest.postFundTransferFail(senderAccountId, receiverAccountId, 30.00);

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

        var eurAccount = rest.putAccountSuccess(createAccountProjection(clientOne, "ACC-0001", 1000.00, EUR));
        var usdAccount = rest.putAccountSuccess(createAccountProjection(clientTwo, "ACC-0002", 1000.00, USD));

        var response = rest.postFundTransferFail(eurAccount.getId(), usdAccount.getId(), 30.00);

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

        var eurAccount = rest.putAccountSuccess(createAccountProjection(clientOne, "ACC-0001", 1000.00, EUR));
        var usdAccount = rest.putAccountSuccess(createAccountProjection(clientTwo, "ACC-0002", 1000.00, USD));
        var audAccount = rest.putAccountSuccess(createAccountProjection(clientTwo, "ACC-0003", 1000.00, AUD));

        var eurUsdTransaction = rest.postFundTransferSuccess(eurAccount.getId(), usdAccount.getId(), 30.00);
        var usdEurTransaction = rest.postFundTransferSuccess(usdAccount.getId(), eurAccount.getId(), 50.00);
        var usdAudTransaction = rest.postFundTransferSuccess(usdAccount.getId(), audAccount.getId(), 99.00);

        assertThat(Set.of(eurUsdTransaction, usdEurTransaction, usdAudTransaction))
            .allSatisfy(tx -> Assertions.assertThat(tx.getId()).isNotNull());

        var eurAccountTransactionHistoryPage = rest.getTransactionHistoryFor(eurAccount.getId(), Paging.of(0, 10));
        var usdAccountTransactionHistoryPage = rest.getTransactionHistoryFor(usdAccount.getId(), Paging.of(0, 10));
        var audAccountTransactionHistoryPage = rest.getTransactionHistoryFor(audAccount.getId(), Paging.of(0, 10));

        // @formatter:off
        Assertions.assertThat(eurAccountTransactionHistoryPage.content())
            .containsExactly(
                TransactionHistoryRecordProjection.buildWith($ -> {
                    $.transactionId     = usdEurTransaction.getId();
                    $.direction         = Direction.INBOUND;
                    $.peerAccount       = AccountBaseInfoProjection.buildWith($$ -> {
                                            $$.id       = usdAccount.getId();
                                            $$.number   = usdAccount.getNumber();
                                        });
                    $.amount            = TypeUtils.scaledBigDecimal(50.00);
                    $.currency          = eurAccount.getCurrency();
                    $.transactionDate   = usdEurTransaction.getTransactionDate();
                }),
                TransactionHistoryRecordProjection.buildWith($ -> {
                    $.transactionId     = eurUsdTransaction.getId();
                    $.direction         = Direction.OUTBOUND;
                    $.peerAccount       = AccountBaseInfoProjection.buildWith($$ -> {
                                            $$.id       = usdAccount.getId();
                                            $$.number   = usdAccount.getNumber();
                                        });
                    $.amount            = TypeUtils.scaledBigDecimal(33.0760749724);
                    $.currency          = eurAccount.getCurrency();
                    $.transactionDate   = eurUsdTransaction.getTransactionDate();
                })
            );
        // @formatter:on

        // @formatter:off
        Assertions.assertThat(usdAccountTransactionHistoryPage.content())
            .containsExactly(
                TransactionHistoryRecordProjection.buildWith($ -> {
                    $.transactionId     = usdAudTransaction.getId();
                    $.direction         = Direction.OUTBOUND;
                    $.peerAccount       = AccountBaseInfoProjection.buildWith($$ -> {
                                            $$.id       = audAccount.getId();
                                            $$.number   = audAccount.getNumber();
                                        });
                    $.amount            = TypeUtils.scaledBigDecimal(66.00);
                    $.currency          = usdAccount.getCurrency();
                    $.transactionDate   = usdAudTransaction.getTransactionDate();
                }),
                TransactionHistoryRecordProjection.buildWith($ -> {
                    $.transactionId     = usdEurTransaction.getId();
                    $.direction         = Direction.OUTBOUND;
                    $.peerAccount       = AccountBaseInfoProjection.buildWith($$ -> {
                                            $$.id       = eurAccount.getId();
                                            $$.number   = eurAccount.getNumber();
                                        });
                    $.amount            = TypeUtils.scaledBigDecimal(45.4409921788);
                    $.currency          = usdAccount.getCurrency();
                    $.transactionDate   = usdEurTransaction.getTransactionDate();
                }),
                TransactionHistoryRecordProjection.buildWith($ -> {
                    $.transactionId     = eurUsdTransaction.getId();
                    $.direction         = Direction.INBOUND;
                    $.peerAccount       = AccountBaseInfoProjection.buildWith($$ -> {
                                            $$.id       = eurAccount.getId();
                                            $$.number   = eurAccount.getNumber();
                                        });
                    $.amount            = TypeUtils.scaledBigDecimal(30.00);
                    $.currency          = usdAccount.getCurrency();
                    $.transactionDate   = eurUsdTransaction.getTransactionDate();
                })
            );
        // @formatter:on

        // @formatter:off
        Assertions.assertThat(audAccountTransactionHistoryPage.content())
            .containsExactly(
                TransactionHistoryRecordProjection.buildWith($ -> {
                    $.transactionId     = usdAudTransaction.getId();
                    $.direction         = Direction.INBOUND;
                    $.peerAccount       = AccountBaseInfoProjection.buildWith($$ -> {
                                            $$.id       = usdAccount.getId();
                                            $$.number   = usdAccount.getNumber();
                                        });
                    $.amount            = TypeUtils.scaledBigDecimal(99.00);
                    $.currency          = audAccount.getCurrency();
                    $.transactionDate   = usdAudTransaction.getTransactionDate();
                })
            );
        // @formatter:on

        var accountsForClientOne = rest.getAccountsFor(clientOne);
        var accountsForClientTwo = rest.getAccountsFor(clientTwo);

        Assertions.assertThat(accountsForClientOne)
            .extracting(AccountProjection::id, AccountProjection::balance)
            .containsExactly(tuple(eurAccount.getId(), TypeUtils.scaledBigDecimal(1016.9239250276)));

        Assertions.assertThat(accountsForClientTwo)
            .extracting(AccountProjection::id, AccountProjection::balance)
            .containsExactlyInAnyOrder(
                tuple(usdAccount.getId(), TypeUtils.scaledBigDecimal(918.5590078212)),
                tuple(audAccount.getId(), TypeUtils.scaledBigDecimal(1099.00))
            );
    }

    private Integer getMaxAccountId() {
        return
            rest
                .getAllAccounts().stream()
                .map(Account::getId)
                .max(Integer::compareTo)
                .orElse(0);
    }

    private int getMaxClientId() {
        return
            rest
                .getAllAccounts().stream()
                .map(Account::getClientId)
                .max(Integer::compareTo)
                .orElse(0);
    }
}
