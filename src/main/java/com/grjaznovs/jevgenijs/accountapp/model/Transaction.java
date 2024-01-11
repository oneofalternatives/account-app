package com.grjaznovs.jevgenijs.accountapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private int senderAccountId;

    private int receiverAccountId;

    @Column(precision = 16, scale = 10)
    private BigDecimal amount;

    @Column(precision = 16, scale = 10)
    private BigDecimal exchangeRate;

    private LocalDateTime transactionDate;
}
