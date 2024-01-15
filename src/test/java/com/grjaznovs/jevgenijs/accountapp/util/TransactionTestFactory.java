package com.grjaznovs.jevgenijs.accountapp.util;

import com.grjaznovs.jevgenijs.accountapp.model.Account;
import com.grjaznovs.jevgenijs.accountapp.model.Transaction;

import java.time.LocalDateTime;

import static com.grjaznovs.jevgenijs.accountapp.util.TypeUtils.scaledBigDecimal;

public class TransactionTestFactory {

    public static Transaction transactionWith(
        Integer transactionId,
        Account senderAccount,
        Account receiverAccount,
        double sourceAmount,
        double targetAmount,
        String transactionDate
    ) {
        var transaction = new Transaction();
        transaction.setId(transactionId);
        transaction.setSenderAccount(senderAccount);
        transaction.setReceiverAccount(receiverAccount);
        transaction.setSourceAmount(scaledBigDecimal(sourceAmount));
        transaction.setTargetAmount(scaledBigDecimal(targetAmount));
        transaction.setTransactionDate(LocalDateTime.parse(transactionDate));
        return transaction;
    }
}
