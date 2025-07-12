package com.imeth.chronobank.ejb.service.timer;

import com.imeth.chronobank.common.constants.AppConstants;
import com.imeth.chronobank.common.entity.Account;
import com.imeth.chronobank.common.entity.Transaction;

import jakarta.annotation.Resource;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.Timeout;
import jakarta.ejb.Timer;
import jakarta.ejb.TimerConfig;
import jakarta.ejb.TimerService;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * EJB service that handles scheduled fund transfers.
 */
@Singleton
@Startup
public class ScheduledTransferService {

    private static final Logger LOGGER = Logger.getLogger(ScheduledTransferService.class.getName());

    @PersistenceContext
    private EntityManager em;

    @Resource
    private TimerService timerService;

    /**
     * Scheduled method that runs every hour to process scheduled transfers.
     */
    @Schedule(hour = "*", minute = "0", second = "0", persistent = false)
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void processScheduledTransfers() {
        LOGGER.info("Processing scheduled transfers...");
        
        try {
            // Find all scheduled transactions that are due
            TypedQuery<Transaction> query = em.createQuery(
                    "SELECT t FROM Transaction t WHERE t.status = :status AND t.scheduledDate <= CURRENT_TIMESTAMP",
                    Transaction.class);
            query.setParameter("status", Transaction.Status.SCHEDULED);
            
            List<Transaction> scheduledTransactions = query.getResultList();
            LOGGER.info("Found " + scheduledTransactions.size() + " scheduled transactions to process");
            
            for (Transaction transaction : scheduledTransactions) {
                processTransaction(transaction);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing scheduled transfers", e);
        }
    }

    /**
     * Process a single scheduled transaction.
     *
     * @param transaction the transaction to process
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    private void processTransaction(Transaction transaction) {
        try {
            LOGGER.info("Processing scheduled transaction: " + transaction.getTransactionReference());
            
            Account sourceAccount = transaction.getAccount();
            Account targetAccount = transaction.getTargetAccount();
            
            // Verify accounts are active
            if (sourceAccount.getStatus() != Account.Status.ACTIVE || 
                (targetAccount != null && targetAccount.getStatus() != Account.Status.ACTIVE)) {
                transaction.setStatus(Transaction.Status.FAILED);
                transaction.setDescription(transaction.getDescription() + " - Failed: Account inactive");
                em.merge(transaction);
                return;
            }
            
            // Verify sufficient funds
            if (sourceAccount.getAvailableBalance().compareTo(transaction.getAmount()) < 0) {
                transaction.setStatus(Transaction.Status.FAILED);
                transaction.setDescription(transaction.getDescription() + " - Failed: Insufficient funds");
                em.merge(transaction);
                return;
            }
            
            // Process the transfer
            sourceAccount.setBalance(sourceAccount.getBalance().subtract(transaction.getAmount()));
            sourceAccount.setAvailableBalance(sourceAccount.getAvailableBalance().subtract(transaction.getAmount()));
            em.merge(sourceAccount);
            
            if (targetAccount != null) {
                targetAccount.setBalance(targetAccount.getBalance().add(transaction.getAmount()));
                targetAccount.setAvailableBalance(targetAccount.getAvailableBalance().add(transaction.getAmount()));
                em.merge(targetAccount);
            }
            
            // Update transaction status
            transaction.setStatus(Transaction.Status.COMPLETED);
            em.merge(transaction);
            
            LOGGER.info("Successfully processed scheduled transaction: " + transaction.getTransactionReference());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing transaction: " + transaction.getTransactionReference(), e);
            transaction.setStatus(Transaction.Status.FAILED);
            transaction.setDescription(transaction.getDescription() + " - Failed: " + e.getMessage());
            em.merge(transaction);
        }
    }

    /**
     * Creates a timer for a specific scheduled transfer.
     *
     * @param transactionId the ID of the transaction to schedule
     * @param delay the delay in milliseconds
     */
    public void scheduleTransfer(Long transactionId, long delay) {
        TimerConfig config = new TimerConfig();
        config.setInfo(transactionId);
        config.setPersistent(true);
        timerService.createSingleActionTimer(delay, config);
        LOGGER.info("Scheduled transfer timer created for transaction ID: " + transactionId);
    }

    /**
     * Handles timer expiration events.
     *
     * @param timer the timer that expired
     */
    @Timeout
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void handleTimeout(Timer timer) {
        Long transactionId = (Long) timer.getInfo();
        LOGGER.info("Timer expired for transaction ID: " + transactionId);
        
        try {
            Transaction transaction = em.find(Transaction.class, transactionId);
            if (transaction != null && transaction.getStatus() == Transaction.Status.SCHEDULED) {
                processTransaction(transaction);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error handling timer for transaction ID: " + transactionId, e);
        }
    }
}