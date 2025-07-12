package com.imeth.chronobank.ejb.service.timer;

import com.imeth.chronobank.common.entity.Account;
import com.imeth.chronobank.common.entity.Transaction;
import com.imeth.chronobank.common.entity.User;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the InterestCalculationService class.
 */
public class InterestCalculationServiceTest {

    @Mock
    private EntityManager em;

    @Mock
    private TypedQuery<Account> accountQuery;

    @InjectMocks
    private InterestCalculationService interestCalculationService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testCalculateDailyInterest() {
        // Mock query
        when(em.createQuery(anyString(), eq(Account.class))).thenReturn(accountQuery);
        
        // Create test accounts
        List<Account> accounts = new ArrayList<>();
        accounts.add(createTestAccount("1001", Account.Type.SAVINGS, new BigDecimal("1000.00"), new BigDecimal("0.05")));
        accounts.add(createTestAccount("1002", Account.Type.INVESTMENT, new BigDecimal("5000.00"), new BigDecimal("0.07")));
        
        when(accountQuery.getResultList()).thenReturn(accounts);
        
        // Execute the method
        interestCalculationService.calculateDailyInterest();
        
        // Verify that the accounts were updated
        verify(em, times(2)).merge(any(Account.class));
        
        // Verify that transactions were created
        verify(em, times(2)).persist(any(Transaction.class));
    }
    
    private Account createTestAccount(String accountNumber, Account.Type type, BigDecimal balance, BigDecimal interestRate) {
        Account account = new Account();
        account.setAccountNumber(accountNumber);
        account.setType(type);
        account.setBalance(balance);
        account.setAvailableBalance(balance);
        account.setInterestRate(interestRate);
        account.setStatus(Account.Status.ACTIVE);
        
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail("test@example.com");
        
        account.setUser(user);
        
        return account;
    }
}