package com.grjaznovs.jevgenijs.accountapp.util;

import com.grjaznovs.jevgenijs.accountapp.model.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.grjaznovs.jevgenijs.accountapp.util.MoneyConstants.ROUNDING_MODE;
import static com.grjaznovs.jevgenijs.accountapp.util.MoneyConstants.SCALE;

public class TransactionTestFactory {

    public static Transaction transactionWith(
        Integer transactionId,
        Integer senderAccountId,
        Integer receiverAccountId,
        double sourceAmount,
        double targetAmount,
        String transactionDate
    ) {
        var transaction = new Transaction();
        transaction.setId(transactionId);
        transaction.setSenderAccountId(senderAccountId);
        transaction.setReceiverAccountId(receiverAccountId);
        transaction.setSourceAmount(BigDecimal.valueOf(sourceAmount).setScale(SCALE, ROUNDING_MODE));
        transaction.setTargetAmount(BigDecimal.valueOf(targetAmount).setScale(SCALE, ROUNDING_MODE));
        transaction.setTransactionDate(LocalDateTime.parse(transactionDate));
        return transaction;
    }
}
