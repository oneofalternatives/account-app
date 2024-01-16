package com.oneofalternatives.accountapp.util;

import com.oneofalternatives.accountapp.model.Account;
import com.oneofalternatives.accountapp.model.Transaction;

import java.time.LocalDateTime;

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
        transaction.setSourceAmount(TypeUtils.scaledBigDecimal(sourceAmount));
        transaction.setTargetAmount(TypeUtils.scaledBigDecimal(targetAmount));
        transaction.setTransactionDate(LocalDateTime.parse(transactionDate));
        return transaction;
    }

    public static Transaction transactionWith(
        Account senderAccount,
        Account receiverAccount,
        double sourceAmount,
        double targetAmount,
        LocalDateTime transactionDate
    ) {
        var transaction = new Transaction();
        transaction.setSenderAccount(senderAccount);
        transaction.setReceiverAccount(receiverAccount);
        transaction.setSourceAmount(TypeUtils.scaledBigDecimal(sourceAmount));
        transaction.setTargetAmount(TypeUtils.scaledBigDecimal(targetAmount));
        transaction.setTransactionDate(transactionDate);
        return transaction;
    }
}
