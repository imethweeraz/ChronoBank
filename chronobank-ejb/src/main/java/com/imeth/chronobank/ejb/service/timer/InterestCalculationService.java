package com.imeth.chronobank.ejb.service.timer;

import com.imeth.chronobank.common.constants.AppConstants;
import com.imeth.chronobank.common.entity.Account;
import com.imeth.chronobank.common.entity.Transaction;

import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * EJB service that handles interest calculations for accounts.
 */
@Singleton
public class InterestCalculationService {

    private static final Logger LOGGER = Logger.getLogger(InterestCalculationService.class.getName());
    
    // Daily interest calculation (APR / 365)
    private static final int DAYS_IN_YEAR = 365;
    private static final int SCALE = 6;

    @PersistenceContext
    private EntityManager em;

    /**
     * Scheduled method that runs daily at midnight to calculate interest.
     */
    @Schedule(hour = "0", minute = "0", second = "0", persistent = false)
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void calculateDailyInterest() {
        LOGGER.info("Calculating daily interest for accounts...");
        
        try {
            // Find all active savings and investment accounts with interest rates
            TypedQuery<Account> query = em.createQuery(
                    "SELECT a FROM Account a WHERE a.status = :status AND a.interestRate IS NOT NULL " +
                    "AND (a.type = :savingsType OR a.type = :investmentType)",
                    Account.class);
            query.setParameter("status", Account.Status.ACTIVE);
            query.setParameter("savingsType", Account.Type.SAVINGS);
            query.setParameter("investmentType", Account.Type.INVESTMENT);
            
            List<Account> accounts = query.getResultList();
            LOGGER.info("Found " + accounts.size() + " accounts for interest calculation");
            
            for (Account account : accounts) {
                calculateInterestForAccount(account);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error calculating daily interest", e);
        }
    }

    /**
     * Calculate and apply interest for a single account.
     *
     * @param account the account to calculate interest for
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    private void calculateInterestForAccount(Account account) {
        try {
            LOGGER.info("Calculating interest for account: " + account.getAccountNumber());
            
            // Calculate daily interest rate (annual rate / 365)
            BigDecimal dailyRate = account.getInterestRate()
                    .divide(BigDecimal.valueOf(DAYS_IN_YEAR), SCALE, RoundingMode.HALF_UP);
            
            // Calculate interest amount
            BigDecimal interestAmount = account.getBalance()
                    .multiply(dailyRate)
                    .setScale(2, RoundingMode.HALF_UP);
            
            // Only process if interest amount is greater than zero
            if (interestAmount.compareTo(BigDecimal.ZERO) > 0) {
                // Update account balance
                account.setBalance(account.getBalance().add(interestAmount));
                account.setAvailableBalance(account.getAvailableBalance().add(interestAmount));
                em.merge(account);
                
                // Create interest transaction record
                Transaction interestTransaction = new Transaction();
                interestTransaction.setTransactionReference(AppConstants.TRANSACTION_PREFIX + 
                        UUID.randomUUID().toString().substring(0, AppConstants.TRANSACTION_REFERENCE_LENGTH - 3));
                interestTransaction.setType(Transaction.Type.INTEREST);
                interestTransaction.setAmount(interestAmount);
                interestTransaction.setDescription("Daily interest accrual");
                interestTransaction.setStatus(Transaction.Status.COMPLETED);
                interestTransaction.setTransactionDate(LocalDateTime.now());
                interestTransaction.setAccount(account);
                
                em.persist(interestTransaction);
                
                LOGGER.info("Applied interest of " + interestAmount + " to account: " + account.getAccountNumber());
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error calculating interest for account: " + account.getAccountNumber(), e);
        }
    }

    /**
     * Calculate monthly compound interest for an account.
     * This method can be called manually or scheduled monthly.
     *
     * @param accountId the ID of the account to calculate interest for
     * @return the amount of interest applied
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public BigDecimal calculateMonthlyCompoundInterest(Long accountId) {
        try {
            Account account = em.find(Account.class, accountId);
            if (account == null || account.getStatus() != Account.Status.ACTIVE || account.getInterestRate() == null) {
                return BigDecimal.ZERO;
            }
            
            // Calculate monthly interest (annual rate / 12)
            BigDecimal monthlyRate = account.getInterestRate()
                    .divide(BigDecimal.valueOf(12), SCALE, RoundingMode.HALF_UP);
            
            // Calculate interest amount
            BigDecimal interestAmount = account.getBalance()
                    .multiply(monthlyRate)
                    .setScale(2, RoundingMode.HALF_UP);
            
            // Only process if interest amount is greater than zero
            if (interestAmount.compareTo(BigDecimal.ZERO) > 0) {
                // Update account balance
                account.setBalance(account.getBalance().add(interestAmount));
                account.setAvailableBalance(account.getAvailableBalance().add(interestAmount));
                em.merge(account);
                
                // Create interest transaction record
                Transaction interestTransaction = new Transaction();
                interestTransaction.setTransactionReference(AppConstants.TRANSACTION_PREFIX + 
                        UUID.randomUUID().toString().substring(0, AppConstants.TRANSACTION_REFERENCE_LENGTH - 3));
                interestTransaction.setType(Transaction.Type.INTEREST);
                interestTransaction.setAmount(interestAmount);
                interestTransaction.setDescription("Monthly compound interest");
                interestTransaction.setStatus(Transaction.Status.COMPLETED);
                interestTransaction.setTransactionDate(LocalDateTime.now());
                interestTransaction.setAccount(account);
                
                em.persist(interestTransaction);
                
                LOGGER.info("Applied monthly compound interest of " + interestAmount + 
                        " to account: " + account.getAccountNumber());
                
                return interestAmount;
            }
            
            return BigDecimal.ZERO;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error calculating monthly compound interest for account ID: " + accountId, e);
            return BigDecimal.ZERO;
        }
    }
}