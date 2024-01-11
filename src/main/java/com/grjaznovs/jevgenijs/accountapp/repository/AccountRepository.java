package com.grjaznovs.jevgenijs.accountapp.repository;

import com.grjaznovs.jevgenijs.accountapp.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Integer> {

    Iterable<Account> findAllByClientId(Integer clientId);
}
