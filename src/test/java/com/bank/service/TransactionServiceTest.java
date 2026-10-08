package com.bank.service;

import com.bank.entity.*;
import com.bank.exception.BusinessException;
import com.bank.repository.AccountRepository;
import com.bank.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private AlertService alertService;

    @InjectMocks
    private TransactionService transactionService;

    private Account account;
    private CustomerType individualType;
    private Customer customer;

    @BeforeEach
    void setUp() {
        individualType = new CustomerType(CustomerTypeName.INDIVIDUAL, new BigDecimal("200000"));
        customer = new Customer("Ann", "Hanoi", individualType);
        account = new Account("100002", new BigDecimal("500000"), customer,
                new BigDecimal("300000"), LocalDate.now());

        lenient().when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void withdraw_success_whenWithinBothLimits() {
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));

        Transaction result = transactionService.withdraw(1L, new BigDecimal("100000"), "Hanoi");

        assertEquals("WITHDRAW", result.getType());
        assertEquals(new BigDecimal("1000.00"), result.getFee().setScale(2));
        assertEquals(new BigDecimal("399000.00"), account.getBalance().setScale(2)); 
        verify(alertService).checkAndFlag(result);
    }

    @Test
    void withdraw_throws_whenExceedsCustomerTypeLimit_evenIfWithinAccountLimit() {
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));

        // 250,000 < transactionLimit (300,000) NHƯNG > customerType limit (200,000)
        BusinessException ex = assertThrows(BusinessException.class,
                () -> transactionService.withdraw(1L, new BigDecimal("250000"), "Hanoi"));

        assertTrue(ex.getMessage().contains("200000"));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void withdraw_throws_whenInsufficientBalance() {
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));

        assertThrows(BusinessException.class,
                () -> transactionService.withdraw(1L, new BigDecimal("999999999"), "Hanoi"));
    }

    @Test
    void withdraw_throws_whenAmountIsZeroOrNegative() {
        // amount<=0 bị chặn TRƯỚC khi gọi tới repository (đã đổi thứ tự check trong patch) ->
        // không cần (và không được phép) stub accountRepository ở test này, nếu không sẽ bị
        // Mockito báo UnnecessaryStubbingException.
        assertThrows(BusinessException.class,
                () -> transactionService.withdraw(1L, BigDecimal.ZERO, "Hanoi"));

        verify(accountRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void withdraw_throws_whenAccountNotFound() {
        when(accountRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class,
                () -> transactionService.withdraw(99L, new BigDecimal("1000"), "Hanoi"));
    }

    @Test
    void deposit_success_increasesBalance() {
        // deposit() giờ chỉ cộng (amount - fee): 500000 + (50000 - 500) = 549500
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));

        Transaction result = transactionService.deposit(1L, new BigDecimal("50000"), "Hanoi");

        assertEquals("DEPOSIT", result.getType());
        assertEquals(new BigDecimal("549500.00"), account.getBalance().setScale(2));
        verify(alertService).checkAndFlag(result);
    }

    @Test
    void transfer_success_movesBalanceBetweenAccounts() {
        Account toAccount = new Account("100001", new BigDecimal("100000"), customer,
                new BigDecimal("500000"), LocalDate.now());

        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(toAccount));

        Transaction result = transactionService.transfer(1L, 2L, new BigDecimal("100000"), "Hanoi");

        assertEquals("TRANSFER_OUT", result.getType());
        assertEquals(new BigDecimal("399000.00"), account.getBalance().setScale(2));  // from chịu phí
        assertEquals(new BigDecimal("200000"), toAccount.getBalance());               // to nhận đủ
        verify(alertService, times(2)).checkAndFlag(any());
    }

    @Test
    void transfer_throws_whenSameAccount() {
        assertThrows(BusinessException.class,
                () -> transactionService.transfer(1L, 1L, new BigDecimal("1000"), "Hanoi"));
    }

    @Test
    void updateTransaction_throws_whenTryingToChangeAmount() {
        Transaction tx = new Transaction(account, "DEPOSIT", new BigDecimal("100000"), BigDecimal.ZERO, "Hanoi");
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(tx));

        Transaction updated = new Transaction(account, "DEPOSIT", new BigDecimal("999999"), BigDecimal.ZERO, "Hanoi");

        assertThrows(BusinessException.class, () -> transactionService.updateTransaction(1L, updated));
    }
}