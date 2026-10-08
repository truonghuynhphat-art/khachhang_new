package com.bank.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "transactions")
@Getter
@Setter
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(nullable = false)
    private String type; // DEPOSIT, WITHDRAW, TRANSFER_IN, TRANSFER_OUT

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private BigDecimal fee;

    @Column(nullable = false)
    private String location;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Soft delete: transaction là dữ liệu tài chính/audit, không xoá vật lý ở môi trường thật.
    @Column(nullable = false)
    private boolean active = true;

    public Transaction() {}

    public Transaction(Account account, String type, BigDecimal amount,
                        BigDecimal fee, String location) {
        this.account = account;
        this.type = type;
        this.amount = amount;
        this.fee = fee;
        this.location = location;
        this.createdAt = LocalDateTime.now();
        this.active = true;
    }

    // public Long getId() { return id; }
    // public Account getAccount() { return account; }
    // public String getType() { return type; }
    // public void setType(String type) { this.type = type; }
    // public BigDecimal getAmount() { return amount; }
    // public void setAmount(BigDecimal amount) { this.amount = amount; }
    // public BigDecimal getFee() { return fee; }
    // public void setFee(BigDecimal fee) { this.fee = fee; }
    // public String getLocation() { return location; }
    // public void setLocation(String location) { this.location = location; }
    // public LocalDateTime getCreatedAt() { return createdAt; }
    // public boolean isActive() { return active; }
    // public void setActive(boolean active) { this.active = active; }
}