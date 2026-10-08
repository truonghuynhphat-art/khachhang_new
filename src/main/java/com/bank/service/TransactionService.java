package com.bank.service;

import com.bank.entity.Account;
import com.bank.entity.Transaction;
import com.bank.exception.BusinessException;
import com.bank.repository.AccountRepository;
import com.bank.repository.TransactionRepository;
import com.bank.repository.spec.TransactionSpecification;
import com.bank.service.AlertService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionService {
    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final AlertService alertService;

    public TransactionService(TransactionRepository transactionRepository,
                               AccountRepository accountRepository,
                               AlertService alertService) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.alertService = alertService;
    }

    @Cacheable(value = "transactionHistory", key = "#accountId")
    public List<Transaction> getTransactionHistory(Long accountId) {
        return transactionRepository.findByAccountIdAndActiveTrue(accountId);
    }

    @Caching(evict = {
            @CacheEvict(value = "accounts", allEntries = true),
            @CacheEvict(value = "accountById", key = "#accountId"),
            @CacheEvict(value = "transactionHistory", key = "#accountId")
    })

    @Transactional
    public Transaction withdraw(Long accountId, BigDecimal amount, String location) {
        log.info("Withdraw requested: accountId={}, amount={}, location={}", accountId, amount, location);

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Amount must be greater than zero", HttpStatus.BAD_REQUEST);
        }

        // Khoá row ngay khi đọc — không còn khoảng hở giữa "đọc" và "ghi" để request khác chen vào
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new BusinessException("Account not found", HttpStatus.NOT_FOUND));

        checkTransactionLimit(account, amount);

        BigDecimal fee = amount.multiply(new BigDecimal("0.01"));
        BigDecimal totalDebit = amount.add(fee); // trừ cả phí, không chỉ trừ amount
        if (totalDebit.compareTo(account.getBalance()) > 0) {
            log.warn("Withdraw rejected - insufficient balance: accountId={}, balance={}, requested={}",
                    accountId, account.getBalance(), totalDebit);
            throw new BusinessException("Insufficient balance to cover amount and fee", HttpStatus.BAD_REQUEST);
        }

        BigDecimal balanceBefore = account.getBalance();
        account.setBalance(account.getBalance().subtract(totalDebit));
        accountRepository.save(account);

        Transaction tx = new Transaction(account, "WITHDRAW", amount, fee, location);
        Transaction saved = transactionRepository.save(tx);
        alertService.checkAndFlag(saved);

        log.info("Withdraw completed: txId={}, accountId={}, balanceBefore={}, balanceAfter={}",
                saved.getId(), accountId, balanceBefore, account.getBalance());        
        return saved;
    }

    @Caching(evict = {
            @CacheEvict(value = "accounts", allEntries = true),
            @CacheEvict(value = "accountById", key = "#accountId"),
            @CacheEvict(value = "transactionHistory", key = "#accountId")
    })
    
    @Transactional
    public Transaction deposit(Long accountId, BigDecimal amount, String location) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Amount must be greater than zero", HttpStatus.BAD_REQUEST);
        }

        // Khoá row ngay khi đọc — không còn khoảng hở giữa "đọc" và "ghi" để request khác chen vào
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new BusinessException("Account not found", HttpStatus.NOT_FOUND));

        BigDecimal fee = amount.multiply(new BigDecimal("0.01"));
        BigDecimal credited = amount.subtract(fee); // khách chịu phí, chỉ được cộng amount - fee

        account.setBalance(account.getBalance().add(credited));
        accountRepository.save(account);

        Transaction tx = new Transaction(account, "DEPOSIT", amount, fee, location);
        Transaction saved = transactionRepository.save(tx);
        alertService.checkAndFlag(saved);
        return saved;
    }

    @Caching(evict = {
            @CacheEvict(value = "accounts", allEntries = true),
            @CacheEvict(value = "accountById", key = "#fromAccountId"),
            @CacheEvict(value = "accountById", key = "#toAccountId"),
            @CacheEvict(value = "transactionHistory", key = "#fromAccountId"),
            @CacheEvict(value = "transactionHistory", key = "#toAccountId")
    })
    @Transactional
    public Transaction transfer(Long fromAccountId, Long toAccountId, BigDecimal amount, String location) {
        log.info("Transfer requested: from={}, to={}, amount={}", fromAccountId, toAccountId, amount);

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Amount must be greater than zero", HttpStatus.BAD_REQUEST);
        }
        if (fromAccountId.equals(toAccountId)) {
            throw new BusinessException("Cannot transfer to the same account", HttpStatus.BAD_REQUEST);
        }

        // LOCK ORDERING: luôn khoá account có id NHỎ HƠN trước, bất kể ai là "from"/"to".
        // Nếu không làm vậy: request A->B khoá A trước rồi chờ khoá B, trong khi
        // request B->A (chạy song song) khoá B trước rồi chờ khoá A => cả hai chờ
        // nhau mãi mãi => DEADLOCK. Khoá theo thứ tự id cố định đảm bảo mọi transaction
        // xin lock theo đúng 1 chiều duy nhất.
        Long firstId = fromAccountId < toAccountId ? fromAccountId : toAccountId;
        Long secondId = fromAccountId < toAccountId ? toAccountId : fromAccountId;

        Account firstLocked = accountRepository.findByIdForUpdate(firstId)
                .orElseThrow(() -> new BusinessException("Account not found: " + firstId, HttpStatus.NOT_FOUND));
        Account secondLocked = accountRepository.findByIdForUpdate(secondId)
                .orElseThrow(() -> new BusinessException("Account not found: " + secondId, HttpStatus.NOT_FOUND));

        Account fromAccount = firstId.equals(fromAccountId) ? firstLocked : secondLocked;
        Account toAccount = firstId.equals(fromAccountId) ? secondLocked : firstLocked;

        checkTransactionLimit(fromAccount, amount);

        BigDecimal fee = amount.multiply(new BigDecimal("0.01"));
        BigDecimal totalDebit = amount.add(fee); // người chuyển (from) chịu phí
        if (totalDebit.compareTo(fromAccount.getBalance()) > 0) {
            throw new BusinessException("Insufficient balance to cover amount and fee", HttpStatus.BAD_REQUEST);
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(totalDebit));
        toAccount.setBalance(toAccount.getBalance().add(amount)); // người nhận nhận đủ, không bị trừ phí
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        Transaction transferOut = new Transaction(fromAccount, "TRANSFER_OUT", amount, fee, location);
        Transaction transferIn = new Transaction(toAccount, "TRANSFER_IN", amount, BigDecimal.ZERO, location);
        Transaction savedIn = transactionRepository.save(transferIn);
        Transaction savedOut = transactionRepository.save(transferOut);
        alertService.checkAndFlag(savedIn);
        alertService.checkAndFlag(savedOut);

        log.info("Transfer completed: txOutId={}, from={}, to={}, amount={}, fee={}",
                savedOut.getId(), fromAccountId, toAccountId, amount, fee);
        return savedOut;
    }

    @Caching(evict = {
            @CacheEvict(value = "transactionHistory", allEntries = true),
            @CacheEvict(value = "accounts", allEntries = true)
    })

    /**
     * Số tiền/loại giao dịch một khi đã ghi nhận là BẤT BIẾN - không cho sửa qua API này,
     * vì sửa amount/type mà không đụng tới balance sẽ làm 2 nguồn dữ liệu lệch nhau.
     * Muốn "sửa" một giao dịch sai thì đúng nghiệp vụ kế toán là tạo giao dịch điều chỉnh
     * (reversal + giao dịch mới), đi qua withdraw/deposit/transfer để balance luôn đúng.
     * API này chỉ cho sửa location (đính chính dữ liệu ghi sai, không ảnh hưởng balance).
     */
    public Transaction updateTransaction(Long id, Transaction updated) {
        Transaction existing = transactionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Transaction not found", HttpStatus.NOT_FOUND));

        if (updated.getType() != null && !updated.getType().equals(existing.getType())) {
            throw new BusinessException(
                    "Cannot change transaction type after it has been recorded (would desync balance). " +
                    "Create an adjustment transaction instead.", HttpStatus.BAD_REQUEST);
        }
        if (updated.getAmount() != null && updated.getAmount().compareTo(existing.getAmount()) != 0) {
            throw new BusinessException(
                    "Cannot change transaction amount after it has been recorded (would desync balance). " +
                    "Create an adjustment transaction instead.", HttpStatus.BAD_REQUEST);
        }
        if (updated.getLocation() != null) {
            existing.setLocation(updated.getLocation());
        }
        return transactionRepository.save(existing);
    }

    public Page<Transaction> search(String type, BigDecimal amountFrom, BigDecimal amountTo,
                                     LocalDateTime createdFrom, LocalDateTime createdTo,
                                     Long ownerCustomerId, Pageable pageable) {
        Specification<Transaction> spec = Specification
                .where(TransactionSpecification.hasType(type))
                .and(TransactionSpecification.amountFrom(amountFrom))
                .and(TransactionSpecification.amountTo(amountTo))
                .and(TransactionSpecification.createdFrom(createdFrom))
                .and(TransactionSpecification.createdTo(createdTo))
                .and(ownerCustomerId == null ? null :
                        (root, query, cb) -> cb.equal(root.get("account").get("owner").get("id"), ownerCustomerId));
        return transactionRepository.findAll(spec, pageable);
    }

    public List<Transaction> findAll() {
        return transactionRepository.findAll();
    }

    // Bản phân trang - dùng cho API list chính thay vì load hết bảng.
    public Page<Transaction> findAll(Pageable pageable) {
        return transactionRepository.findAll(pageable);
    }

    public Page<Transaction> getTransactionHistory(Long accountId, Pageable pageable) {
        return transactionRepository.findByAccountIdAndActiveTrue(accountId, pageable);
    }

    public Page<Transaction> findByOwnerId(Long ownerId, Pageable pageable) {
        return transactionRepository.findByAccountOwnerIdAndActiveTrue(ownerId, pageable);
    }

    public Optional<Transaction> findById(Long id) {
        return transactionRepository.findById(id);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "transactionHistory", allEntries = true),
            @CacheEvict(value = "accounts", allEntries = true)
    })
    public void deleteById(Long id) {
        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Transaction not found", HttpStatus.NOT_FOUND));
        tx.setActive(false);
        transactionRepository.save(tx);
        log.info("Transaction soft-deleted: txId={}, accountId={}", id, tx.getAccount().getId());
    }

    private void checkTransactionLimit(Account account, BigDecimal amount) {
        BigDecimal accountLimit = account.getTransactionLimit();
        BigDecimal customerTypeLimit = account.getOwner().getCustomerType().getMaxTransactionLimit();
        BigDecimal effectiveLimit = accountLimit.min(customerTypeLimit);

        if (amount.compareTo(effectiveLimit) > 0) {
            throw new BusinessException(
                    "Transaction exceeds allowed limit (" + effectiveLimit + ")",
                    HttpStatus.BAD_REQUEST);
        }
    }
}