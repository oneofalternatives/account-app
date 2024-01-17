package com.oneofalternatives.accountapp.repository;

import com.oneofalternatives.accountapp.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

    @Query("SELECT t FROM Transaction t WHERE t.senderAccount.id = :accountId OR t.receiverAccount.id = :accountId")
    Page<Transaction> findAllBySenderAccountIdOrReceiverAccountId(int accountId, Pageable paginationParameters);
}
