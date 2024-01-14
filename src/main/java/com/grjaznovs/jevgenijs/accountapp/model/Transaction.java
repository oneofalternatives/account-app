package com.grjaznovs.jevgenijs.accountapp.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Setter
@Getter
@EqualsAndHashCode
@ToString
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
