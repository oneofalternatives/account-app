package com.grjaznovs.jevgenijs.accountapp.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionHistoryRecordProjection(
    int id,
    AccountBaseInfoProjection senderAccount,
    AccountBaseInfoProjection receiverAccount,
    BigDecimal amount,
    String currency,
    BigDecimal exchangeRate,
    LocalDateTime transactionDate
) {

    public record AccountBaseInfoProjection(
        int id,
        String number
    ) {
    }
}
