package com.grjaznovs.jevgenijs.accountapp.repository;

import com.grjaznovs.jevgenijs.accountapp.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Integer> {

    List<Account> findAllByClientId(Integer clientId);
}
