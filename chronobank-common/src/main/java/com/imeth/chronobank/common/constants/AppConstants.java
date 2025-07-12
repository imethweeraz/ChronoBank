package com.imeth.chronobank.common.constants;

/**
 * Constants used throughout the application.
 */
public final class AppConstants {

    // Security Constants
    public static final String SECURITY_REALM = "ChronoBankRealm";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_MANAGER = "MANAGER";
    public static final String ROLE_CUSTOMER = "CUSTOMER";
    
    // Transaction Constants
    public static final String TRANSACTION_PREFIX = "TRX";
    public static final int TRANSACTION_REFERENCE_LENGTH = 12;
    
    // Account Constants
    public static final String ACCOUNT_PREFIX = "CHB";
    public static final int ACCOUNT_NUMBER_LENGTH = 10;
    
    // Timer Service Constants
    public static final String DAILY_BALANCE_UPDATE_TIMER = "DailyBalanceUpdateTimer";
    public static final String INTEREST_CALCULATION_TIMER = "InterestCalculationTimer";
    public static final String SCHEDULED_TRANSFER_TIMER = "ScheduledTransferTimer";
    public static final String REPORT_GENERATION_TIMER = "ReportGenerationTimer";
    
    // API Paths
    public static final String API_BASE_PATH = "/api";
    public static final String API_VERSION = "/v1";
    public static final String API_ADMIN_PATH = "/admin";
    public static final String API_MANAGER_PATH = "/manager";
    public static final String API_CUSTOMER_PATH = "/customer";
    
    // Pagination Constants
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    
    private AppConstants() {
        // Private constructor to prevent instantiation
    }
}