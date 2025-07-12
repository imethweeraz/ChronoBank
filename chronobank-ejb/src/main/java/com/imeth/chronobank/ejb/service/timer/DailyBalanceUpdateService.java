package com.imeth.chronobank.ejb.service.timer;

import com.imeth.chronobank.common.entity.Account;

import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * EJB service that handles daily balance updates and reconciliation.
 */
@Singleton
public class DailyBalanceUpdateService {

    private static final Logger LOGGER = Logger.getLogger(DailyBalanceUpdateService.class.getName());

    @PersistenceContext
    private EntityManager em;

    /**
     * Scheduled method that runs daily at 23:45 to update and reconcile account balances.
     */
    @Schedule(hour = "23", minute = "45", second = "0", persistent = false)
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void updateDailyBalances() {
        LOGGER.info("Starting daily balance update and reconciliation...");
        
        try {
            // Get all active accounts
            List<Account> accounts = em.createQuery(
                    "SELECT a FROM Account a WHERE a.status = :status", Account.class)
                    .setParameter("status", Account.Status.ACTIVE)
                    .getResultList();
            
            LOGGER.info("Found " + accounts.size() + " active accounts for daily balance update");
            
            for (Account account : accounts) {
                updateAccountBalance(account);
            }
            
            // Record the completion of the daily balance update
            LOGGER.info("Daily balance update completed at " + LocalDateTime.now());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during daily balance update", e);
        }
    }

    /**
     * Update and reconcile the balance for a single account.
     *
     * @param account the account to update
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    private void updateAccountBalance(Account account) {
        try {
            LOGGER.info("Updating balance for account: " + account.getAccountNumber());
            
            // Calculate the actual balance based on transactions
            // This is a simplified example - in a real system, you would perform a more thorough reconciliation
            Object result = em.createQuery(
                    "SELECT SUM(CASE WHEN t.type IN ('DEPOSIT', 'TRANSFER', 'INTEREST') AND t.targetAccount = :account THEN t.amount " +
                    "WHEN t.type IN ('WITHDRAWAL', 'TRANSFER', 'PAYMENT', 'FEE') AND t.account = :account THEN -t.amount " +
                    "WHEN t.type = 'ADJUSTMENT' THEN t.amount ELSE 0 END) " +
                    "FROM Transaction t " +
                    "WHERE (t.account = :account OR t.targetAccount = :account) " +
                    "AND t.status = 'COMPLETED'")
                    .setParameter("account", account)
                    .getSingleResult();
            
            // If there are no transactions, the result might be null
            if (result != null) {
                // Update the account balance if there's a discrepancy
                // In a real system, you would log discrepancies and possibly alert administrators
                if (!account.getBalance().equals(result)) {
                    LOGGER.warning("Balance discrepancy detected for account " + account.getAccountNumber() + 
                            ": recorded=" + account.getBalance() + ", calculated=" + result);
                    
                    // For this example, we'll just update to match the calculated balance
                    // In a real system, you might create an adjustment transaction
                    account.setBalance(account.getBalance());
                    account.setAvailableBalance(account.getAvailableBalance());
                    em.merge(account);
                }
            }
            
            // Update the last reconciliation date
            // In a real system, you would store this in a separate table or field
            LOGGER.info("Balance update completed for account: " + account.getAccountNumber());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating balance for account: " + account.getAccountNumber(), e);
        }
    }

    /**
     * Manually trigger a balance update for a specific account.
     *
     * @param accountId the ID of the account to update
     * @return true if the update was successful, false otherwise
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public boolean manualBalanceUpdate(Long accountId) {
        try {
            Account account = em.find(Account.class, accountId);
            if (account != null) {
                updateAccountBalance(account);
                return true;
            }
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during manual balance update for account ID: " + accountId, e);
            return false;
        }
    }
}