package com.grjaznovs.jevgenijs.accountapp.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionHistoryRecordProjection(
    Integer transactionId,
    AccountBaseInfoProjection peerAccount,
    Direction direction,
    BigDecimal amount,
    String currency,
    LocalDateTime transactionDate
) {

    public record AccountBaseInfoProjection(
        Integer id,
        String number
    ) { }

    public enum Direction {OUTBOUND, INBOUND}
}
