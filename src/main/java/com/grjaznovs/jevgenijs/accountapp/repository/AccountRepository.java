package com.grjaznovs.jevgenijs.accountapp.repository;

import com.grjaznovs.jevgenijs.accountapp.model.Account;
import org.springframework.data.repository.CrudRepository;

public interface AccountRepository extends CrudRepository<Account, Integer> {

    Iterable<Account> findAllByClientId(Integer clientId);
}
