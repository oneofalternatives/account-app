package com.grjaznovs.jevgenijs.accountapp.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionHistoryRecordProjection(
    int transactionId,
    AccountBaseInfoProjection account,
    Direction direction,
    BigDecimal amount,
    String currency,
    LocalDateTime transactionDate
) {

    public record AccountBaseInfoProjection(
        int id,
        String number
    ) {
    }

    public enum Direction {OUTBOUND, INBOUND}
}
