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
    private Integer id;

    private Integer senderAccountId;

    private Integer receiverAccountId;

    @Column(precision = 16, scale = 10)
    private BigDecimal sourceAmount;

    @Column(precision = 16, scale = 10)
    private BigDecimal targetAmount;

    private LocalDateTime transactionDate;
}
