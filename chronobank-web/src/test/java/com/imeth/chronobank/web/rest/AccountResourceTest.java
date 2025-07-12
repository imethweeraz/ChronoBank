package com.imeth.chronobank.web.rest;

import com.imeth.chronobank.common.dto.AccountDTO;
import com.imeth.chronobank.common.entity.Account;
import com.imeth.chronobank.common.entity.User;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the AccountResource class.
 */
public class AccountResourceTest {

    @Mock
    private EntityManager em;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Principal principal;

    @Mock
    private TypedQuery<Account> accountQuery;

    @Mock
    private TypedQuery<Long> countQuery;

    @InjectMocks
    private AccountResource accountResource;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Mock security context
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("admin");
        when(securityContext.isUserInRole("ADMIN")).thenReturn(true);
    }

    @Test
    public void testGetAllAccounts() {
        // Mock queries
        when(em.createQuery(anyString(), eq(Long.class))).thenReturn(countQuery);
        when(em.createQuery(anyString(), eq(Account.class))).thenReturn(accountQuery);
        
        when(countQuery.getSingleResult()).thenReturn(2L);
        
        // Create test accounts
        List<Account> accounts = new ArrayList<>();
        accounts.add(createTestAccount(1L, "1001", Account.Type.CHECKING));
        accounts.add(createTestAccount(2L, "1002", Account.Type.SAVINGS));
        
        when(accountQuery.setFirstResult(anyInt())).thenReturn(accountQuery);
        when(accountQuery.setMaxResults(anyInt())).thenReturn(accountQuery);
        when(accountQuery.getResultList()).thenReturn(accounts);
        
        // Execute the method
        Response response = accountResource.getAllAccounts(0, 10);
        
        // Verify the response
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertNotNull(response.getEntity());
        
        @SuppressWarnings("unchecked")
        List<AccountDTO> result = (List<AccountDTO>) response.getEntity();
        assertEquals(2, result.size());
    }

    @Test
    public void testGetAccountById() {
        // Mock find method
        Account account = createTestAccount(1L, "1001", Account.Type.CHECKING);
        when(em.find(Account.class, 1L)).thenReturn(account);
        
        // Execute the method
        Response response = accountResource.getAccountById(1L);
        
        // Verify the response
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertNotNull(response.getEntity());
        
        AccountDTO result = (AccountDTO) response.getEntity();
        assertEquals("1001", result.getAccountNumber());
    }

    @Test
    public void testGetAccountByIdNotFound() {
        // Mock find method to return null
        when(em.find(Account.class, 999L)).thenReturn(null);
        
        // Execute the method
        Response response = accountResource.getAccountById(999L);
        
        // Verify the response
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }
    
    private Account createTestAccount(Long id, String accountNumber, Account.Type type) {
        Account account = new Account();
        account.setId(id);
        account.setAccountNumber(accountNumber);
        account.setType(type);
        account.setBalance(BigDecimal.valueOf(1000));
        account.setAvailableBalance(BigDecimal.valueOf(1000));
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