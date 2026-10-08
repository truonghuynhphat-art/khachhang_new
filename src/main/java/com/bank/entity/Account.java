package com.bank.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "account")
@Getter
@Setter
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Account number is required")
    @Column(name = "account_number", nullable = false, unique = true)
    private String accountNumber;

    @NotNull(message = "Balance is required")
    @DecimalMin(value = "0", message = "Balance cannot be negative")
    @Column(nullable = false)
    private BigDecimal balance;

    @NotNull(message = "Owner (customer) is required")
    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer owner;

    @NotNull(message = "Transaction limit is required")
    @Positive(message = "Transaction limit must be greater than zero")
    @Column(name = "transaction_limit", nullable = false)
    private BigDecimal transactionLimit;

    @NotNull(message = "Opened date is required")
    @Column(name = "opened_date", nullable = false)
    private LocalDate openedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status = AccountStatus.ACTIVE;

    public Account() {}

    public Account(String accountNumber, BigDecimal balance, Customer owner,
                    BigDecimal transactionLimit, LocalDate openedDate) {
        this.accountNumber = accountNumber;
        this.balance = balance;
        this.owner = owner;
        this.transactionLimit = transactionLimit;
        this.openedDate = openedDate;
        this.status = AccountStatus.ACTIVE;
    }


    // public Long getId() { return id; }
    // public String getAccountNumber() { return accountNumber; }
    // public BigDecimal getBalance() { return balance; }
    // public void setBalance(BigDecimal balance) { this.balance = balance; }
    // public Customer getOwner() { return owner; }
    // public BigDecimal getTransactionLimit() { return transactionLimit; }
    // public void setTransactionLimit(BigDecimal transactionLimit) { this.transactionLimit = transactionLimit; }
    // public LocalDate getOpenedDate() { return openedDate; }
    // public AccountStatus getStatus() { return status; }
    // public void setStatus(AccountStatus status) { this.status = status; }
}