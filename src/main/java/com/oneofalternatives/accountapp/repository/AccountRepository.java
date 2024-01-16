package com.oneofalternatives.accountapp.repository;

import com.oneofalternatives.accountapp.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Integer> {

    List<Account> findAllByClientId(Integer clientId);
}
