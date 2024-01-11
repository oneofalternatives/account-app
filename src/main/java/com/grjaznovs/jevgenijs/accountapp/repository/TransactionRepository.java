package com.grjaznovs.jevgenijs.accountapp.repository;

import com.grjaznovs.jevgenijs.accountapp.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

    @Query("SELECT t FROM Transaction t WHERE t.senderAccountId = :accountId OR t.receiverAccountId = :accountId")
    Page<Transaction> findAllBySenderAccountIdOrReceiverAccountId(int accountId, Pageable paginationParameters);
}
