package com.imeth.chronobank.web.rest;

import com.imeth.chronobank.common.constants.AppConstants;
import com.imeth.chronobank.common.dto.AccountDTO;
import com.imeth.chronobank.common.entity.Account;
import com.imeth.chronobank.common.entity.User;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * REST endpoint for managing accounts.
 */
@Path(AppConstants.API_BASE_PATH + AppConstants.API_VERSION + "/accounts")
@Stateless
public class AccountResource {

    private static final Logger LOGGER = Logger.getLogger(AccountResource.class.getName());

    @PersistenceContext
    private EntityManager em;

    @Context
    private SecurityContext securityContext;

    /**
     * Get all accounts.
     * Only accessible by administrators and managers.
     *
     * @param page the page number (0-based)
     * @param size the page size
     * @return a list of account DTOs
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({AppConstants.ROLE_ADMIN, AppConstants.ROLE_MANAGER})
    public Response getAllAccounts(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        
        try {
            // Validate pagination parameters
            if (page < 0) {
                page = 0;
            }
            if (size <= 0 || size > AppConstants.MAX_PAGE_SIZE) {
                size = AppConstants.DEFAULT_PAGE_SIZE;
            }
            
            // Get total count
            Long totalCount = em.createQuery("SELECT COUNT(a) FROM Account a", Long.class)
                    .getSingleResult();
            
            // Get paginated accounts
            TypedQuery<Account> query = em.createQuery("SELECT a FROM Account a ORDER BY a.id", Account.class);
            query.setFirstResult(page * size);
            query.setMaxResults(size);
            
            List<Account> accounts = query.getResultList();
            
            // Convert to DTOs
            List<AccountDTO> accountDTOs = accounts.stream()
                    .map(AccountDTO::new)
                    .collect(Collectors.toList());
            
            // Return response with pagination metadata
            return Response.ok(accountDTOs)
                    .header("X-Total-Count", totalCount)
                    .header("X-Page", page)
                    .header("X-Page-Size", size)
                    .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error retrieving accounts", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error retrieving accounts: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Get an account by ID.
     *
     * @param id the account ID
     * @return the account DTO
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({AppConstants.ROLE_ADMIN, AppConstants.ROLE_MANAGER, AppConstants.ROLE_CUSTOMER})
    public Response getAccountById(@PathParam("id") Long id) {
        try {
            Account account = em.find(Account.class, id);
            
            if (account == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Account not found with ID: " + id)
                        .build();
            }
            
            // Check if the user has permission to view this account
            String username = securityContext.getUserPrincipal().getName();
            boolean isAdmin = securityContext.isUserInRole(AppConstants.ROLE_ADMIN);
            boolean isManager = securityContext.isUserInRole(AppConstants.ROLE_MANAGER);
            
            if (!isAdmin && !isManager) {
                // For customers, check if the account belongs to them
                User user = em.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class)
                        .setParameter("username", username)
                        .getSingleResult();
                
                if (!account.getUser().getId().equals(user.getId())) {
                    return Response.status(Response.Status.FORBIDDEN)
                            .entity("You do not have permission to view this account")
                            .build();
                }
            }
            
            return Response.ok(new AccountDTO(account)).build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error retrieving account with ID: " + id, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error retrieving account: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Create a new account.
     *
     * @param accountDTO the account data
     * @return the created account
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({AppConstants.ROLE_ADMIN, AppConstants.ROLE_MANAGER})
    public Response createAccount(AccountDTO accountDTO) {
        try {
            // Validate input
            if (accountDTO.getUserId() == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("User ID is required")
                        .build();
            }
            
            // Find the user
            User user = em.find(User.class, accountDTO.getUserId());
            if (user == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("User not found with ID: " + accountDTO.getUserId())
                        .build();
            }
            
            // Create the account
            Account account = new Account();
            account.setAccountNumber(AppConstants.ACCOUNT_PREFIX + 
                    UUID.randomUUID().toString().substring(0, AppConstants.ACCOUNT_NUMBER_LENGTH - 3));
            account.setType(Account.Type.valueOf(accountDTO.getType()));
            account.setBalance(accountDTO.getBalance());
            account.setAvailableBalance(accountDTO.getAvailableBalance());
            account.setInterestRate(accountDTO.getInterestRate());
            account.setStatus(Account.Status.valueOf(accountDTO.getStatus()));
            account.setUser(user);
            
            em.persist(account);
            em.flush(); // Ensure the ID is generated
            
            return Response.status(Response.Status.CREATED)
                    .entity(new AccountDTO(account))
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid account data: " + e.getMessage())
                    .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating account", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error creating account: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Update an existing account.
     *
     * @param id the account ID
     * @param accountDTO the updated account data
     * @return the updated account
     */
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({AppConstants.ROLE_ADMIN, AppConstants.ROLE_MANAGER})
    public Response updateAccount(@PathParam("id") Long id, AccountDTO accountDTO) {
        try {
            Account account = em.find(Account.class, id);
            
            if (account == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Account not found with ID: " + id)
                        .build();
            }
            
            // Update account fields
            if (accountDTO.getInterestRate() != null) {
                account.setInterestRate(accountDTO.getInterestRate());
            }
            
            if (accountDTO.getStatus() != null) {
                account.setStatus(Account.Status.valueOf(accountDTO.getStatus()));
            }
            
            em.merge(account);
            
            return Response.ok(new AccountDTO(account)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid account data: " + e.getMessage())
                    .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating account with ID: " + id, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error updating account: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Delete an account.
     *
     * @param id the account ID
     * @return a success message
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({AppConstants.ROLE_ADMIN})
    public Response deleteAccount(@PathParam("id") Long id) {
        try {
            Account account = em.find(Account.class, id);
            
            if (account == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Account not found with ID: " + id)
                        .build();
            }
            
            // Instead of deleting, set status to CLOSED
            account.setStatus(Account.Status.CLOSED);
            em.merge(account);
            
            return Response.ok().entity("Account closed successfully").build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deleting account with ID: " + id, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error deleting account: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Get accounts for the current user.
     * Only accessible by customers.
     *
     * @return a list of account DTOs
     */
    @GET
    @Path("/my-accounts")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({AppConstants.ROLE_CUSTOMER})
    public Response getMyAccounts() {
        try {
            String username = securityContext.getUserPrincipal().getName();
            
            // Find the user
            User user = em.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class)
                    .setParameter("username", username)
                    .getSingleResult();
            
            // Get accounts for the user
            List<Account> accounts = em.createQuery(
                    "SELECT a FROM Account a WHERE a.user = :user ORDER BY a.id", Account.class)
                    .setParameter("user", user)
                    .getResultList();
            
            // Convert to DTOs
            List<AccountDTO> accountDTOs = accounts.stream()
                    .map(AccountDTO::new)
                    .collect(Collectors.toList());
            
            return Response.ok(accountDTOs).build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error retrieving accounts for current user", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error retrieving accounts: " + e.getMessage())
                    .build();
        }
    }
}