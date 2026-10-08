package com.bank.dto;

import com.bank.entity.AccountStatus;
import jakarta.validation.constraints.NotNull;

public class AccountStatusChangeRequest {
    @NotNull(message = "Status is required")
    private AccountStatus status;
    private String reason;

    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}

