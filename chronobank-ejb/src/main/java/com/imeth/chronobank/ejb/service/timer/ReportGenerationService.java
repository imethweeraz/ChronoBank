package com.imeth.chronobank.ejb.service.timer;

import com.imeth.chronobank.common.entity.Account;
import com.imeth.chronobank.common.entity.Transaction;
import com.imeth.chronobank.common.entity.User;

import jakarta.annotation.Resource;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * EJB service that handles report generation.
 */
@Singleton
public class ReportGenerationService {

    private static final Logger LOGGER = Logger.getLogger(ReportGenerationService.class.getName());
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @PersistenceContext
    private EntityManager em;

    @Resource(name = "java:global/ChronoBank/ReportDirectory")
    private String reportDirectory;

    /**
     * Scheduled method that runs daily at 1:00 AM to generate daily reports.
     */
    @Schedule(hour = "1", minute = "0", second = "0", persistent = false)
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void generateDailyReports() {
        LOGGER.info("Starting daily report generation...");
        
        LocalDate yesterday = LocalDate.now().minusDays(1);
        String dateStr = yesterday.format(DATE_FORMATTER);
        
        try {
            // Create report directory if it doesn't exist
            File directory = new File(reportDirectory);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            
            // Generate transaction report
            generateTransactionReport(yesterday);
            
            // Generate account balance report
            generateAccountBalanceReport(yesterday);
            
            LOGGER.info("Daily report generation completed for " + dateStr);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating daily reports for " + dateStr, e);
        }
    }

    /**
     * Generate a report of all transactions for a specific date.
     *
     * @param date the date to generate the report for
     * @throws IOException if there is an error writing the report
     */
    private void generateTransactionReport(LocalDate date) throws IOException {
        String dateStr = date.format(DATE_FORMATTER);
        String fileName = reportDirectory + "/transaction_report_" + dateStr + ".csv";
        
        LOGGER.info("Generating transaction report for " + dateStr + " to " + fileName);
        
        // Get all transactions for the specified date
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay().minusSeconds(1);
        
        TypedQuery<Transaction> query = em.createQuery(
                "SELECT t FROM Transaction t WHERE t.transactionDate BETWEEN :startDate AND :endDate",
                Transaction.class);
        query.setParameter("startDate", startOfDay);
        query.setParameter("endDate", endOfDay);
        
        List<Transaction> transactions = query.getResultList();
        
        // Write transactions to CSV file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            // Write header
            writer.write("Transaction ID,Reference,Type,Amount,Status,Date,Source Account,Target Account,Description");
            writer.newLine();
            
            // Write transaction data
            for (Transaction transaction : transactions) {
                writer.write(String.join(",",
                        transaction.getId().toString(),
                        transaction.getTransactionReference(),
                        transaction.getType().toString(),
                        transaction.getAmount().toString(),
                        transaction.getStatus().toString(),
                        transaction.getTransactionDate().format(DATETIME_FORMATTER),
                        transaction.getAccount().getAccountNumber(),
                        transaction.getTargetAccount() != null ? transaction.getTargetAccount().getAccountNumber() : "",
                        transaction.getDescription() != null ? "\"" + transaction.getDescription() + "\"" : ""
                ));
                writer.newLine();
            }
        }
        
        LOGGER.info("Transaction report generated with " + transactions.size() + " transactions");
    }

    /**
     * Generate a report of all account balances as of a specific date.
     *
     * @param date the date to generate the report for
     * @throws IOException if there is an error writing the report
     */
    private void generateAccountBalanceReport(LocalDate date) throws IOException {
        String dateStr = date.format(DATE_FORMATTER);
        String fileName = reportDirectory + "/account_balance_report_" + dateStr + ".csv";
        
        LOGGER.info("Generating account balance report for " + dateStr + " to " + fileName);
        
        // Get all active accounts
        TypedQuery<Account> query = em.createQuery(
                "SELECT a FROM Account a WHERE a.status = :status",
                Account.class);
        query.setParameter("status", Account.Status.ACTIVE);
        
        List<Account> accounts = query.getResultList();
        
        // Write account balances to CSV file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            // Write header
            writer.write("Account ID,Account Number,Type,Balance,Available Balance,Status,Owner,Owner Email");
            writer.newLine();
            
            // Write account data
            for (Account account : accounts) {
                User owner = account.getUser();
                writer.write(String.join(",",
                        account.getId().toString(),
                        account.getAccountNumber(),
                        account.getType().toString(),
                        account.getBalance().toString(),
                        account.getAvailableBalance().toString(),
                        account.getStatus().toString(),
                        owner.getFirstName() + " " + owner.getLastName(),
                        owner.getEmail()
                ));
                writer.newLine();
            }
        }
        
        LOGGER.info("Account balance report generated with " + accounts.size() + " accounts");
    }

    /**
     * Generate a custom report for a specific user.
     *
     * @param userId the ID of the user to generate the report for
     * @param startDate the start date for the report period
     * @param endDate the end date for the report period
     * @return the path to the generated report file
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public String generateUserTransactionReport(Long userId, LocalDate startDate, LocalDate endDate) {
        String startDateStr = startDate.format(DATE_FORMATTER);
        String endDateStr = endDate.format(DATE_FORMATTER);
        String fileName = reportDirectory + "/user_" + userId + "_transactions_" + startDateStr + "_to_" + endDateStr + ".csv";
        
        LOGGER.info("Generating user transaction report for user ID " + userId + 
                " from " + startDateStr + " to " + endDateStr);
        
        try {
            // Create report directory if it doesn't exist
            File directory = new File(reportDirectory);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            
            // Get the user
            User user = em.find(User.class, userId);
            if (user == null) {
                LOGGER.warning("User not found with ID: " + userId);
                return null;
            }
            
            // Get all accounts for the user
            TypedQuery<Account> accountQuery = em.createQuery(
                    "SELECT a FROM Account a WHERE a.user = :user",
                    Account.class);
            accountQuery.setParameter("user", user);
            
            List<Account> userAccounts = accountQuery.getResultList();
            
            // Get all transactions for the user's accounts within the date range
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay().minusSeconds(1);
            
            TypedQuery<Transaction> transactionQuery = em.createQuery(
                    "SELECT t FROM Transaction t WHERE (t.account IN :accounts OR t.targetAccount IN :accounts) " +
                    "AND t.transactionDate BETWEEN :startDate AND :endDate " +
                    "ORDER BY t.transactionDate",
                    Transaction.class);
            transactionQuery.setParameter("accounts", userAccounts);
            transactionQuery.setParameter("startDate", startDateTime);
            transactionQuery.setParameter("endDate", endDateTime);
            
            List<Transaction> transactions = transactionQuery.getResultList();
            
            // Write transactions to CSV file
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
                // Write header
                writer.write("Transaction ID,Reference,Type,Amount,Status,Date,Source Account,Target Account,Description");
                writer.newLine();
                
                // Write transaction data
                for (Transaction transaction : transactions) {
                    writer.write(String.join(",",
                            transaction.getId().toString(),
                            transaction.getTransactionReference(),
                            transaction.getType().toString(),
                            transaction.getAmount().toString(),
                            transaction.getStatus().toString(),
                            transaction.getTransactionDate().format(DATETIME_FORMATTER),
                            transaction.getAccount().getAccountNumber(),
                            transaction.getTargetAccount() != null ? transaction.getTargetAccount().getAccountNumber() : "",
                            transaction.getDescription() != null ? "\"" + transaction.getDescription() + "\"" : ""
                    ));
                    writer.newLine();
                }
                
                // Write summary
                writer.newLine();
                writer.write("Summary for " + user.getFirstName() + " " + user.getLastName() + " (" + user.getEmail() + ")");
                writer.newLine();
                writer.write("Period: " + startDateStr + " to " + endDateStr);
                writer.newLine();
                writer.write("Total Transactions: " + transactions.size());
                writer.newLine();
                
                // Calculate totals by transaction type
                Map<Transaction.Type, BigDecimal> typeTotals = new HashMap<>();
                for (Transaction transaction : transactions) {
                    Transaction.Type type = transaction.getType();
                    BigDecimal amount = transaction.getAmount();
                    
                    // For outgoing transactions from user accounts, make the amount negative
                    if (transaction.getAccount() != null && userAccounts.contains(transaction.getAccount()) &&
                            (type == Transaction.Type.WITHDRAWAL || type == Transaction.Type.TRANSFER || 
                             type == Transaction.Type.PAYMENT || type == Transaction.Type.FEE)) {
                        amount = amount.negate();
                    }
                    
                    typeTotals.put(type, typeTotals.getOrDefault(type, BigDecimal.ZERO).add(amount));
                }
                
                // Write totals by type
                writer.newLine();
                writer.write("Totals by Transaction Type:");
                writer.newLine();
                for (Map.Entry<Transaction.Type, BigDecimal> entry : typeTotals.entrySet()) {
                    writer.write(entry.getKey() + ": " + entry.getValue());
                    writer.newLine();
                }
            }
            
            LOGGER.info("User transaction report generated with " + transactions.size() + " transactions");
            return fileName;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating user transaction report", e);
            return null;
        }
    }
}