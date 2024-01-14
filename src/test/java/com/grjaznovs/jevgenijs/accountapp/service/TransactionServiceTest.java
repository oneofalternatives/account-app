package com.grjaznovs.jevgenijs.accountapp.service;

import com.grjaznovs.jevgenijs.accountapp.api.FundTransferException;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.grjaznovs.jevgenijs.accountapp.api.TransactionHistoryRecordProjection.Direction.INBOUND;
import static com.grjaznovs.jevgenijs.accountapp.api.TransactionHistoryRecordProjection.Direction.OUTBOUND;
import static com.grjaznovs.jevgenijs.accountapp.util.AccountTestFactory.accountWith;
import static com.grjaznovs.jevgenijs.accountapp.util.MoneyConstants.ROUNDING_MODE;
import static com.grjaznovs.jevgenijs.accountapp.util.MoneyConstants.SCALE;
import static com.grjaznovs.jevgenijs.accountapp.util.TransactionProjectionRequirementVerifier.require;
import static com.grjaznovs.jevgenijs.accountapp.util.TransactionProjectionRequirementVerifier.requirements;
import static com.grjaznovs.jevgenijs.accountapp.util.TransactionTestFactory.transactionWith;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

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
        // TODO verify pageable
        when(transactionRepository.findAllBySenderAccountIdOrReceiverAccountId(eq(1), any()))
            .thenReturn(
                new PageImpl<>(
                    List.of(
                        transactionWith(3, 1, 2, 25.00, 35.00, "2023-11-11T11:11"),
                        transactionWith(2, 2, 1, 77.00, 88.00, "2023-10-10T10:10"),
                        transactionWith(1, 3, 1, 10.00, 20.00, "2023-09-09T09:09")
                    ),
                    // TODO pageable
                    PageRequest.ofSize(5),
                    3
                )
            );

        when(accountRepository.findAllById(any()))
            .thenAnswer((Answer<List<Account>>) invocationOnMock -> {
                var accounts =
                    List.of(
                        accountWith(1, 1, "ACC-0001", 100.00, "EUR"),
                        accountWith(2, 1, "ACC-0002", 090.00, "USD"),
                        accountWith(3, 2, "ACC-0003", 080.00, "AUD")
                    );

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
            });

        var page = transactionService.getTransactionHistoryByAccountId(1, 0, 10);

        assertThat(page).isNotNull();
        // @formatter:off
        assertThat(page.content())
            .isNotEmpty()
            .satisfiesExactly(
                requirements(
                    require(    "transactionId",        3                                       ),
                    require(    "direction",            OUTBOUND                                ),
                    require(    "peerAccount.id",       2                                       ),
                    require(    "peerAccount.number",   "ACC-0002"                              ),
                    require(    "amount",               25.00                                   ),
                    require(    "currency",             "EUR"                                   ),
                    require(    "transactionDate",      LocalDateTime.parse("2023-11-11T11:11") )
                ),
                requirements(
                    require(    "transactionId",        2                                       ),
                    require(    "direction",            INBOUND                                 ),
                    require(    "peerAccount.id",       2                                       ),
                    require(    "peerAccount.number",   "ACC-0002"                              ),
                    require(    "amount",               88.00                                   ),
                    require(    "currency",             "EUR"                                   ),
                    require(    "transactionDate",      LocalDateTime.parse("2023-10-10T10:10") )
                ),
                requirements(
                    require(    "transactionId",        1                                       ),
                    require(    "direction",            INBOUND                                 ),
                    require(    "peerAccount.id",       3                                       ),
                    require(    "peerAccount.number",   "ACC-0003"                              ),
                    require(    "amount",               20.00                                   ),
                    require(    "currency",             "EUR"                                   ),
                    require(    "transactionDate",      LocalDateTime.parse("2023-09-09T09:09") )
                )
            );
        // @formatter:on
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
            .isInstanceOf(FundTransferException.class)
            .hasMessage("Amount scale must not be greater than 10");
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
            .isInstanceOf(FundTransferException.class)
            .hasMessage("Sender and receiver account must be different");
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
            .isInstanceOf(FundTransferException.class)
            .hasMessage(errorMessage);
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
            .thenReturn(Set.of("EUR", "USD", "AUD"));

        var exception = catchThrowable(() ->
            transactionService.transferFunds(
                1,
                2,
                BigDecimal.valueOf(0.0123456789),
                LocalDateTime.parse("2023-11-11T11:11")
            )
        );

        assertThat(exception)
            .isInstanceOf(FundTransferException.class)
            .hasMessage(errorMessage);
    }

    private static Stream<Arguments> parametersFor_transferFunds_shouldVerifyThatCurrencyIsSupported() {
        return
            Stream.of(
                arguments(
                    List.of(accountWith(1, "GBP"), accountWith(2, "GEL")),
                    "Conversion from/to any of these currencies is not supported: [GBP, GEL]"
                ),
                arguments(
                    List.of(accountWith(1, "AUD"), accountWith(2, "GEL")),
                    "Conversion from/to any of these currencies is not supported: [GEL]"
                )
            );
    }

    @Test
    void transferFunds_shouldRegisterTransaction() {
        when(moneySettings.scale())
            .thenReturn(SCALE);
        when(moneySettings.roundingMode())
            .thenReturn(RoundingMode.HALF_UP);

        when(accountRepository.findAllById(any()))
            .thenReturn(
                List.of(
                    accountWith(1, 10, "ACC-0001", 100.00, "EUR"),
                    accountWith(2, 11, "ACC-0002", 100.00, "USD")
                )
            );

        when(currencyConversionClient.getSupportedCurrencies())
            .thenReturn(Set.of("EUR", "USD", "AUD"));
        when(currencyConversionClient.convert(BigDecimal.valueOf(30.00), "USD", "EUR", LocalDate.parse("2023-11-11")))
            .thenReturn(BigDecimal.valueOf(60.00));

        when(transactionRepository.saveAndFlush(any()))
            .thenAnswer((Answer<Transaction>) invocation -> {
                    var transaction = (Transaction) invocation.getArgument(0);
                    transaction.setId(777);
                    return transaction;
                }
            );

        var transaction =
            transactionService.transferFunds(
                1,
                2,
                BigDecimal.valueOf(30.00),
                LocalDateTime.parse("2023-11-11T11:11")
            );

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(transaction.getId()).isEqualTo(777);
            softly.assertThat(transaction.getSenderAccountId()).isEqualTo(1);
            softly.assertThat(transaction.getReceiverAccountId()).isEqualTo(2);
            softly.assertThat(transaction.getSourceAmount()).isEqualTo(bigDecimal(60.0000000000));
            softly.assertThat(transaction.getTargetAmount()).isEqualTo(BigDecimal.valueOf(30.00));
        });
    }

    private BigDecimal bigDecimal(double value) {
        return BigDecimal.valueOf(value).setScale(SCALE, ROUNDING_MODE);
    }
}
