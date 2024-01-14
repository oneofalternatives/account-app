package com.grjaznovs.jevgenijs.accountapp.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.function.Consumer;

public record TransactionHistoryRecordProjection(
    Integer transactionId,
    AccountBaseInfoProjection peerAccount,
    Direction direction,
    BigDecimal amount,
    Currency currency,
    LocalDateTime transactionDate
) {

    public static Builder builder() {
        return new Builder();
    }

    public static TransactionHistoryRecordProjection buildWith(Consumer<Builder> builder) {
        var builderInstance = builder();
        builder.accept(builderInstance);
        return builderInstance.build();
    }

    public static class Builder {

        public Integer transactionId;
        public AccountBaseInfoProjection peerAccount;
        public Direction direction;
        public BigDecimal amount;
        public Currency currency;
        public LocalDateTime transactionDate;

        public Builder() { }

        public Builder with(Consumer<Builder> builder) {
            builder.accept(this);
            return this;
        }

        public TransactionHistoryRecordProjection build() {
            return new TransactionHistoryRecordProjection(
                transactionId,
                peerAccount,
                direction,
                amount,
                currency,
                transactionDate
            );
        }
    }

    public record AccountBaseInfoProjection(
        Integer id,
        String number
    ) {

        public static Builder builder() {
            return new Builder();
        }

        public static AccountBaseInfoProjection buildWith(Consumer<Builder> builder) {
            var builderInstance = builder();
            builder.accept(builderInstance);
            return builderInstance.build();
        }

        public static class Builder {

            public Integer id;
            public String number;

            public Builder() { }

            public Builder with(Consumer<Builder> builder) {
                builder.accept(this);
                return this;
            }

            public AccountBaseInfoProjection build() {
                return new AccountBaseInfoProjection(id, number);
            }
        }
    }

    public enum Direction {OUTBOUND, INBOUND}
}
