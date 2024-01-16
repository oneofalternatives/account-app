package com.grjaznovs.jevgenijs.accountapp.service;

import com.grjaznovs.jevgenijs.accountapp.api.TransactionHistoryRecordProjection;
import com.grjaznovs.jevgenijs.accountapp.api.TransactionHistoryRecordProjection.AccountBaseInfoProjection;
import com.grjaznovs.jevgenijs.accountapp.error.FundTransferValidationError;
import com.grjaznovs.jevgenijs.accountapp.integration.CurrencyConversionClient;
import com.grjaznovs.jevgenijs.accountapp.model.Account;
import com.grjaznovs.jevgenijs.accountapp.model.Transaction;
import com.grjaznovs.jevgenijs.accountapp.repository.AccountRepository;
import com.grjaznovs.jevgenijs.accountapp.repository.TransactionRepository;
import com.grjaznovs.jevgenijs.accountapp.settings.MoneySettings;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static com.grjaznovs.jevgenijs.accountapp.api.TransactionHistoryRecordProjection.Direction.INBOUND;
import static com.grjaznovs.jevgenijs.accountapp.api.TransactionHistoryRecordProjection.Direction.OUTBOUND;
import static com.grjaznovs.jevgenijs.accountapp.util.AccountTestFactory.accountWith;
import static com.grjaznovs.jevgenijs.accountapp.util.Currencies.*;
import static com.grjaznovs.jevgenijs.accountapp.util.MoneyConstants.SCALE;
import static com.grjaznovs.jevgenijs.accountapp.util.TransactionTestFactory.transactionWith;
import static com.grjaznovs.jevgenijs.accountapp.util.TypeUtils.scaledBigDecimal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
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

    // TODO verify pageable
    @Test
    void getTransactionHistoryByAccountId_shouldMapTransactionAndAccountData() {
        var eurAccount = accountWith(1, 1, "ACC-0001", 100.00, EUR);
        var usdAccount = accountWith(2, 1, "ACC-0002", 090.00, USD);
        var audAccount = accountWith(3, 2, "ACC-0003", 080.00, AUD);

        when(transactionRepository.findAllBySenderAccountIdOrReceiverAccountId(eq(1), any()))
            .thenReturn(
                new PageImpl<>(
                    List.of(
                        transactionWith(3, eurAccount, usdAccount, 25.00, 35.00, "2023-11-11T11:11"),
                        transactionWith(2, usdAccount, eurAccount, 77.00, 88.00, "2023-10-10T10:10"),
                        transactionWith(1, audAccount, eurAccount, 10.00, 20.00, "2023-09-09T09:09")
                    ),
                    PageRequest.ofSize(5),
                    3
                )
            );

/*        when(accountRepository.findAllById(any()))
            .thenAnswer((Answer<List<Account>>) invocationOnMock -> {
                var accounts = List.of(eurAccount, usdAccount, audAccount);

                //noinspection unchecked
                var accountIds = StreamSupport.stream(
                        ((Iterable<Integer>) invocationOnMock.getArgument(0)).spliterator(),
                        false
                    )
                    .collect(Collectors.toSet());

                return
                    accounts.stream()
                        .filter(account -> accountIds.contains(account.getId()))
                        .collect(Collectors.toList());
            });*/

        var page = transactionService.getTransactionHistoryByAccountId(1, 0, 10);

        // @formatter:off
        assertThat(page.content())
            .containsExactly(
                TransactionHistoryRecordProjection.buildWith($ -> {
                    $.transactionId =   3;
                    $.direction =       OUTBOUND;
                    $.peerAccount =     AccountBaseInfoProjection.buildWith($$ -> {
                                            $$.id = 2;
                                            $$.number = "ACC-0002";
                                        });
                    $.amount =          scaledBigDecimal(25.00);
                    $.currency =        EUR;
                    $.transactionDate = LocalDateTime.parse("2023-11-11T11:11");
                }),
                TransactionHistoryRecordProjection.buildWith($ -> {
                    $.transactionId =   2;
                    $.direction =       INBOUND;
                    $.peerAccount =     AccountBaseInfoProjection.buildWith($$ -> {
                                            $$.id = 2;
                                            $$.number = "ACC-0002";
                                        });
                    $.amount =          scaledBigDecimal(88.00);
                    $.currency =        EUR;
                    $.transactionDate = LocalDateTime.parse("2023-10-10T10:10");
                }),
                TransactionHistoryRecordProjection.buildWith($ -> {
                    $.transactionId =   1;
                    $.direction =       INBOUND;
                    $.peerAccount =     AccountBaseInfoProjection.buildWith($$ -> {
                                            $$.id = 3;
                                            $$.number = "ACC-0003";
                                        });
                    $.amount =          scaledBigDecimal(20.00);
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

        var exception = catchThrowable(() ->
            transactionService.transferFunds(
                1,
                1,
                BigDecimal.valueOf(0.01234567891),
                LocalDateTime.parse("2023-11-11T11:11")
            )
        );

        assertThat(exception)
            .isInstanceOf(FundTransferValidationError.class)
            .hasMessage("Amount scale must not be greater than 10");

        verifyNoInteractions(accountRepository, transactionRepository);
    }

    @Test
    void transferFunds_shouldValidateAccountNumbers() {
        when(moneySettings.scale())
            .thenReturn(SCALE);

        var exception = catchThrowable(() ->
            transactionService.transferFunds(
                1,
                1,
                BigDecimal.valueOf(0.0123456789),
                LocalDateTime.parse("2023-11-11T11:11")
            )
        );

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

        var exception = catchThrowable(() ->
            transactionService.transferFunds(
                1,
                2,
                BigDecimal.valueOf(0.0123456789),
                LocalDateTime.parse("2023-11-11T11:11")
            )
        );

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

        var exception = catchThrowable(() ->
            transactionService.transferFunds(
                1,
                2,
                BigDecimal.valueOf(0.0123456789),
                LocalDateTime.parse("2023-11-11T11:11")
            )
        );

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

        var transaction =
            transactionService.transferFunds(
                eurAccount.getId(),
                usdAccount.getId(),
                BigDecimal.valueOf(10.00),
                LocalDateTime.parse("2023-11-11T11:11")
            );

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(transaction.getId()).isEqualTo(777);
            softly.assertThat(transaction.getSenderAccount()).isEqualTo(eurAccount);
            softly.assertThat(transaction.getReceiverAccount()).isEqualTo(usdAccount);
            softly.assertThat(transaction.getSourceAmount()).isEqualTo(BigDecimal.valueOf(10.00));
            softly.assertThat(transaction.getTargetAmount()).isEqualTo(BigDecimal.valueOf(10.00));
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
        when(currencyConversionClient.getDirectRate(EUR, USD, LocalDate.parse("2023-11-11")))
            .thenReturn(BigDecimal.valueOf(1.12));

        when(transactionRepository.save(any()))
            .thenAnswer((Answer<Transaction>) invocation -> {
                    var transaction = (Transaction) invocation.getArgument(0);
                    transaction.setId(777);
                    return transaction;
                }
            );

        var transaction =
            transactionService.transferFunds(
                eurAccount.getId(),
                usdAccount.getId(),
                BigDecimal.valueOf(10.00),
                LocalDateTime.parse("2023-11-11T11:11")
            );

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(transaction.getId()).isEqualTo(777);
            softly.assertThat(transaction.getSenderAccount()).isEqualTo(eurAccount);
            softly.assertThat(transaction.getReceiverAccount()).isEqualTo(usdAccount);
            softly.assertThat(transaction.getSourceAmount()).isEqualTo(scaledBigDecimal(8.9285714286));
            softly.assertThat(transaction.getTargetAmount()).isEqualTo(BigDecimal.valueOf(10.00));
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
