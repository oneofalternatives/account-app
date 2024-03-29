package com.oneofalternatives.accountapp.service;

import com.oneofalternatives.accountapp.api.TransactionHistoryRecordProjection;
import com.oneofalternatives.accountapp.error.FundTransferValidationError;
import com.oneofalternatives.accountapp.integration.CurrencyConversionClient;
import com.oneofalternatives.accountapp.model.Account;
import com.oneofalternatives.accountapp.model.Transaction;
import com.oneofalternatives.accountapp.repository.AccountRepository;
import com.oneofalternatives.accountapp.repository.TransactionRepository;
import com.oneofalternatives.accountapp.settings.MoneySettings;
import com.oneofalternatives.accountapp.util.TransactionTestFactory;
import com.oneofalternatives.accountapp.util.TypeUtils;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static com.oneofalternatives.accountapp.util.AccountTestFactory.accountWith;
import static com.oneofalternatives.accountapp.util.Currencies.*;
import static com.oneofalternatives.accountapp.util.MoneyConstants.SCALE;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private CurrencyConversionClient currencyConversionClient;
    @Mock
    private MoneySettings moneySettings;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void getTransactionHistoryByAccountId_shouldMapTransactionAndAccountData() {
        var eurAccount = accountWith(1, 1, "ACC-0001", 100.00, EUR);
        var usdAccount = accountWith(2, 1, "ACC-0002", 090.00, USD);
        var audAccount = accountWith(3, 2, "ACC-0003", 080.00, AUD);

        when(transactionRepository.findAllBySenderAccountIdOrReceiverAccountId(eq(1), any()))
            .thenReturn(
                new PageImpl<>(
                    List.of(
                        TransactionTestFactory.transactionWith(3, eurAccount, usdAccount, 25.00, 35.00, "2023-11-11T11:11"),
                        TransactionTestFactory.transactionWith(2, usdAccount, eurAccount, 77.00, 88.00, "2023-10-10T10:10"),
                        TransactionTestFactory.transactionWith(1, audAccount, eurAccount, 10.00, 20.00, "2023-09-09T09:09")
                    ),
                    PageRequest.ofSize(5),
                    3
                )
            );

        var page = transactionService.getTransactionHistoryByAccountId(1, 0, 10);

        // @formatter:off
        Assertions.assertThat(page.content())
            .containsExactly(
                TransactionHistoryRecordProjection.buildWith($ -> {
                    $.transactionId =   3;
                    $.direction =       TransactionHistoryRecordProjection.Direction.OUTBOUND;
                    $.peerAccount =     TransactionHistoryRecordProjection.AccountBaseInfoProjection.buildWith($$ -> {
                                            $$.id = 2;
                                            $$.number = "ACC-0002";
                                        });
                    $.amount =          TypeUtils.scaledBigDecimal(25.00);
                    $.currency =        EUR;
                    $.transactionDate = LocalDateTime.parse("2023-11-11T11:11");
                }),
                TransactionHistoryRecordProjection.buildWith($ -> {
                    $.transactionId =   2;
                    $.direction =       TransactionHistoryRecordProjection.Direction.INBOUND;
                    $.peerAccount =     TransactionHistoryRecordProjection.AccountBaseInfoProjection.buildWith($$ -> {
                                            $$.id = 2;
                                            $$.number = "ACC-0002";
                                        });
                    $.amount =          TypeUtils.scaledBigDecimal(88.00);
                    $.currency =        EUR;
                    $.transactionDate = LocalDateTime.parse("2023-10-10T10:10");
                }),
                TransactionHistoryRecordProjection.buildWith($ -> {
                    $.transactionId =   1;
                    $.direction =       TransactionHistoryRecordProjection.Direction.INBOUND;
                    $.peerAccount =     TransactionHistoryRecordProjection.AccountBaseInfoProjection.buildWith($$ -> {
                                            $$.id = 3;
                                            $$.number = "ACC-0003";
                                        });
                    $.amount =          TypeUtils.scaledBigDecimal(20.00);
                    $.currency =        EUR;
                    $.transactionDate = LocalDateTime.parse("2023-09-09T09:09");
                })
            );
        // @formatter:on

        verifyNoMoreInteractions(accountRepository, transactionRepository);
    }

    @Test
    void transferFunds_shouldValidateAmountScale() {
        when(moneySettings.scale())
            .thenReturn(SCALE);

        var exception = catchThrowable(() -> transactionService.transferFunds(1, 1, BigDecimal.valueOf(0.01234567891)));

        assertThat(exception)
            .isInstanceOf(FundTransferValidationError.class)
            .hasMessage("Amount scale must not be greater than 10");

        verifyNoInteractions(accountRepository, transactionRepository);
    }

    @Test
    void transferFunds_shouldValidateAccountNumbers() {
        when(moneySettings.scale())
            .thenReturn(SCALE);

        var exception = catchThrowable(() -> transactionService.transferFunds(1, 1, BigDecimal.valueOf(0.0123456789)));

        assertThat(exception)
            .isInstanceOf(FundTransferValidationError.class)
            .hasMessage("Sender and receiver account must be different");

        verifyNoInteractions(accountRepository, currencyConversionClient, transactionRepository);
    }

    @ParameterizedTest
    @MethodSource("parametersFor_transferFunds_shouldVerifyThatAccountsExist")
    void transferFunds_shouldVerifyThatAccountsExist(
        List<Account> existingAccounts,
        String errorMessage
    ) {
        when(moneySettings.scale())
            .thenReturn(SCALE);

        when(accountRepository.findAllById(any()))
            .thenReturn(existingAccounts);

        var exception = catchThrowable(() -> transactionService.transferFunds(1, 2, BigDecimal.valueOf(0.0123456789)));

        assertThat(exception)
            .isInstanceOf(FundTransferValidationError.class)
            .hasMessage(errorMessage);

        verifyNoMoreInteractions(accountRepository);
        verifyNoInteractions(currencyConversionClient, transactionRepository);
    }

    private static Stream<Arguments> parametersFor_transferFunds_shouldVerifyThatAccountsExist() {
        return
            Stream.of(
                arguments(List.of(), "Accounts with these IDs do not exist: [1, 2]"),
                arguments(List.of(accountWith(1)), "Accounts with these IDs do not exist: [2]")
            );
    }

    @ParameterizedTest
    @MethodSource("parametersFor_transferFunds_shouldVerifyThatCurrencyIsSupported")
    void transferFunds_shouldVerifyThatCurrencyIsSupported(
        List<Account> accounts,
        String errorMessage
    ) {
        when(moneySettings.scale())
            .thenReturn(SCALE);

        when(accountRepository.findAllById(any()))
            .thenReturn(accounts);

        when(currencyConversionClient.getSupportedCurrencies())
            .thenReturn(Set.of(EUR, USD, AUD));

        var exception = catchThrowable(() -> transactionService.transferFunds(1, 2, BigDecimal.valueOf(0.0123456789)));

        assertThat(exception)
            .isInstanceOf(FundTransferValidationError.class)
            .hasMessage(errorMessage);

        verifyNoMoreInteractions(accountRepository, currencyConversionClient);
        verifyNoInteractions(transactionRepository);
    }

    private static Stream<Arguments> parametersFor_transferFunds_shouldVerifyThatCurrencyIsSupported() {
        return
            Stream.of(
                arguments(
                    List.of(accountWith(1, GBP), accountWith(2, GEL)),
                    "Conversion from/to any of these currencies is not supported: [GBP, GEL]"
                ),
                arguments(
                    List.of(accountWith(1, AUD), accountWith(2, GEL)),
                    "Conversion from/to any of these currencies is not supported: [GEL]"
                )
            );
    }

    @Test
    void transferFunds_shouldVerifyThatSourceAccountHasSufficientBalance() {
        when(moneySettings.scale())
            .thenReturn(SCALE);

        var eurAccount = accountWith(1, 10, "ACC-0001", 10.00, EUR);
        var usdAccount = accountWith(2, 11, "ACC-0002", 100.00, EUR);

        when(accountRepository.findAllById(any()))
            .thenReturn(List.of(eurAccount, usdAccount));

        var exception = catchThrowable(() -> transactionService.transferFunds(1, 2, BigDecimal.valueOf(10.0000000001)));

        assertThat(exception)
            .isInstanceOf(FundTransferValidationError.class)
            .hasMessage("Source account has insufficient balance");
    }

    @Test
    void transferFunds_shouldRegisterTransactionWithoutCurrencyConversion() {
        when(moneySettings.scale())
            .thenReturn(SCALE);

        var eurAccount = accountWith(1, 10, "ACC-0001", 100.00, EUR);
        var usdAccount = accountWith(2, 11, "ACC-0002", 100.00, EUR);

        when(accountRepository.findAllById(any()))
            .thenReturn(List.of(eurAccount, usdAccount));

        when(transactionRepository.save(any()))
            .thenAnswer((Answer<Transaction>) invocation -> {
                    var transaction = (Transaction) invocation.getArgument(0);
                    transaction.setId(777);
                    return transaction;
                }
            );

        var transaction = transactionService.transferFunds(1, 2, BigDecimal.valueOf(10.00));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(transaction.getId()).isEqualTo(777);
            softly.assertThat(transaction.getSenderAccount()).isEqualTo(eurAccount);
            softly.assertThat(transaction.getReceiverAccount()).isEqualTo(usdAccount);
            softly.assertThat(transaction.getSourceAmount()).isEqualTo(BigDecimal.valueOf(10.00));
            softly.assertThat(transaction.getTargetAmount()).isEqualTo(BigDecimal.valueOf(10.00));
            softly.assertThat(transaction.getTransactionDate()).isCloseTo(LocalDateTime.now(), within(1, SECONDS));
        });

        //noinspection unchecked
        ArgumentCaptor<Iterable<Account>> updatedAccountCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(accountRepository).saveAll(updatedAccountCaptor.capture());
        var updatedAccounts = updatedAccountCaptor.getValue();
        assertThat(updatedAccounts)
            .containsExactlyInAnyOrder(eurAccount, usdAccount);

        verifyNoMoreInteractions(accountRepository, transactionRepository);
        verifyNoInteractions(currencyConversionClient);
    }

    @Test
    void transferFunds_shouldRegisterTransactionWithCurrencyConversion() {
        var now = LocalDateTime.now();

        when(moneySettings.scale())
            .thenReturn(SCALE);
        when(moneySettings.roundingMode())
            .thenReturn(RoundingMode.HALF_UP);

        var eurAccount = accountWith(1, 10, "ACC-0001", 100.00, EUR);
        var usdAccount = accountWith(2, 11, "ACC-0002", 100.00, USD);

        when(accountRepository.findAllById(any()))
            .thenReturn(List.of(eurAccount, usdAccount));

        when(currencyConversionClient.getSupportedCurrencies())
            .thenReturn(Set.of(EUR, USD, AUD));
        when(currencyConversionClient.getDirectRate(EUR, USD))
            .thenReturn(BigDecimal.valueOf(1.12));

        when(transactionRepository.save(any()))
            .thenAnswer((Answer<Transaction>) invocation -> {
                    var transaction = (Transaction) invocation.getArgument(0);
                    transaction.setId(777);
                    return transaction;
                }
            );

        var transaction =
            transactionService
                .transferFunds(
                    eurAccount.getId(),
                    usdAccount.getId(),
                    BigDecimal.valueOf(10.00)
                );

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(transaction.getId()).isEqualTo(777);
            softly.assertThat(transaction.getSenderAccount()).isEqualTo(eurAccount);
            softly.assertThat(transaction.getReceiverAccount()).isEqualTo(usdAccount);
            softly.assertThat(transaction.getSourceAmount()).isEqualTo(TypeUtils.scaledBigDecimal(8.9285714286));
            softly.assertThat(transaction.getTargetAmount()).isEqualTo(BigDecimal.valueOf(10.00));
            softly.assertThat(transaction.getTransactionDate()).isCloseTo(now, within(1, SECONDS));
        });

        //noinspection unchecked
        ArgumentCaptor<Iterable<Account>> updatedAccountCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(accountRepository).saveAll(updatedAccountCaptor.capture());
        var updatedAccounts = updatedAccountCaptor.getValue();
        assertThat(updatedAccounts)
            .containsExactlyInAnyOrder(eurAccount, usdAccount);

        verifyNoMoreInteractions(accountRepository, currencyConversionClient, transactionRepository);
    }
}
