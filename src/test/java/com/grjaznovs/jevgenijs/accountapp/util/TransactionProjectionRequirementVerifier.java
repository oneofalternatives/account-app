package com.grjaznovs.jevgenijs.accountapp.util;

import com.grjaznovs.jevgenijs.accountapp.api.TransactionHistoryRecordProjection;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.ThrowingConsumer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

import static java.util.stream.Collectors.toMap;

public class TransactionProjectionRequirementVerifier {

    public static final String TRANSACTION_ID = "transactionId";

    public static final String DIRECTION = "direction";
    public static final String PEER_ACCOUNT_ID = "peerAccount.id";
    public static final String PEER_ACCOUNT_NUMBER = "peerAccount.number";
    public static final String AMOUNT = "amount";
    public static final String CURRENCY = "currency";
    public static final String TRANSACTION_DATE = "transactionDate";

    @SafeVarargs
    public static ThrowingConsumer<TransactionHistoryRecordProjection> requirements(
        Pair<String, Object>... requirements
    ) {
        var r =
            Arrays.stream(requirements)
                .collect(toMap(Pair::getKey, Pair::getValue));

        return tx ->
            SoftAssertions.assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(tx.transactionId())       .as(TRANSACTION_ID)     .isEqualTo(r.get(TRANSACTION_ID));
                softly.assertThat(tx.direction())           .as(DIRECTION)          .isEqualTo(r.get(DIRECTION));
                softly.assertThat(tx.peerAccount().id())    .as(PEER_ACCOUNT_ID)    .isEqualTo(r.get(PEER_ACCOUNT_ID));
                softly.assertThat(tx.peerAccount().number()).as(PEER_ACCOUNT_NUMBER).isEqualTo(r.get(PEER_ACCOUNT_NUMBER));
                softly.assertThat(tx.amount())              .as(AMOUNT)             .isEqualTo(
                                                                                        BigDecimal.valueOf((Double) r.get(AMOUNT))
                                                                                            .setScale(10, RoundingMode.HALF_UP));
                softly.assertThat(tx.currency())            .as(CURRENCY)           .isEqualTo(r.get(CURRENCY));
                softly.assertThat(tx.transactionDate())     .as(TRANSACTION_DATE)   .isEqualTo(r.get(TRANSACTION_DATE));
                // @formatter:on
            });
    }

    public static Pair<String, Object> require(String key, Object expectedValue) {
        return Pair.of(key, expectedValue);
    }

}
