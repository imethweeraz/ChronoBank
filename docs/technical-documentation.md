# ChronoBank Banking System - Technical Documentation

## Overview

ChronoBank is a comprehensive banking system built on Java EE technology. It leverages Enterprise JavaBeans (EJB), Timer Services, and Java Persistence API (JPA) to provide a robust, secure, and scalable banking solution. The system is designed with a multi-module architecture to promote separation of concerns and maintainability.

## Architecture

ChronoBank follows a multi-tiered architecture:

1. **Presentation Tier**: Implemented in the `chronobank-web` module, providing REST endpoints and web interfaces.
2. **Business Tier**: Implemented in the `chronobank-ejb` module, containing EJBs, timer services, and business logic.
3. **Data Tier**: Implemented using JPA in the `chronobank-ejb` module, providing persistence and data access.
4. **Common Tier**: Implemented in the `chronobank-common` module, containing shared entities, DTOs, and utilities.

The system is packaged as an Enterprise Archive (EAR) in the `chronobank-ear` module, which includes all the other modules.

## Module Structure

### chronobank-common

This module contains shared components used across the application:

- **Entities**: JPA entities representing the domain model (User, Account, Transaction)
- **DTOs**: Data Transfer Objects for transferring data between layers
- **Utilities**: Helper classes for common operations
- **Constants**: Application-wide constants and configuration values

### chronobank-ejb

This module contains the business logic and data access layer:

- **EJB Components**: Stateless and stateful session beans implementing business logic
- **Timer Services**: Scheduled services for automated operations
- **Transaction Management**: Container-managed transaction handling
- **Security Implementations**: Role-based access control and authentication
- **Persistence Layer**: JPA entities and data access objects

### chronobank-web

This module provides the presentation layer:

- **REST Endpoints**: JAX-RS resources for API access
- **Web Interface**: HTML, CSS, and JavaScript files for the user interface
- **Authentication Filters**: Security filters for request authentication
- **API Documentation**: Swagger/OpenAPI documentation

### chronobank-ear

This module packages the application for deployment:

- **Application Packaging**: EAR assembly
- **Deployment Descriptors**: Configuration files for the application server
- **Resource Configurations**: JNDI resources and connection pools

## Key Features

### Timer Services

ChronoBank implements several timer services for automated operations:

1. **ScheduledTransferService**: Processes scheduled fund transfers at specified times.
2. **InterestCalculationService**: Calculates and applies interest to accounts on a daily or monthly basis.
3. **DailyBalanceUpdateService**: Updates and reconciles account balances at the end of each day.
4. **ReportGenerationService**: Generates daily reports for transactions and account balances.

These services use the EJB Timer Service to schedule and execute tasks at specified intervals.

### Transaction Management

ChronoBank ensures data integrity through robust transaction management:

- **Container-Managed Transactions**: EJBs use container-managed transactions to ensure ACID compliance.
- **Global Transaction Support**: Transactions span multiple resources and operations.
- **Rollback Mechanisms**: Automatic rollback on exceptions to maintain data consistency.

### Security

ChronoBank implements a comprehensive security model:

- **Role-Based Access Control**: Different user roles (ADMIN, MANAGER, CUSTOMER) with specific permissions.
- **JAAS Integration**: Java Authentication and Authorization Service for user authentication.
- **Secure Communication**: HTTPS for all sensitive operations.
- **Audit Logging**: Tracking of all security-related events.

### Database

ChronoBank uses JPA for database operations:

- **JPA Persistence**: Object-relational mapping for database access.
- **Connection Pooling**: Efficient database connection management.
- **Data Integrity**: Constraints and validations to ensure data consistency.
- **Backup Procedures**: Scheduled database backups for disaster recovery.

## Deployment

### Prerequisites

- JDK 11
- GlassFish 7.0.23
- MySQL Database
- Maven 3.8+

### Database Setup

1. Create a MySQL database named `chronobank`.
2. Create a database user `chronobank` with password `chronobank`.
3. Grant all privileges on the `chronobank` database to the `chronobank` user.

```sql
CREATE DATABASE chronobank;
CREATE USER 'chronobank'@'localhost' IDENTIFIED BY 'chronobank';
GRANT ALL PRIVILEGES ON chronobank.* TO 'chronobank'@'localhost';
FLUSH PRIVILEGES;
```

### GlassFish Configuration

1. Start GlassFish server:
   ```
   asadmin start-domain
   ```

2. Create JDBC Connection Pool:
   ```
   asadmin create-jdbc-connection-pool --datasourceclassname com.mysql.cj.jdbc.MysqlDataSource --restype javax.sql.DataSource --property user=chronobank:password=chronobank:URL=jdbc\\:mysql\\://localhost\\:3306/chronobank ChronoBankPool
   ```

3. Create JDBC Resource:
   ```
   asadmin create-jdbc-resource --connectionpoolid ChronoBankPool jdbc/ChronoBankDS
   ```

4. Create Security Realm:
   ```
   asadmin create-auth-realm --classname com.sun.enterprise.security.auth.realm.jdbc.JDBCRealm --property jaas-context=jdbcRealm:datasource-jndi=jdbc/ChronoBankDS:user-table=users:user-name-column=username:password-column=password_hash:group-table=user_groups:group-name-column=group_name:user-group-table=user_groups:group-table-user-name-column=username ChronoBankRealm
   ```

### Building and Deploying

1. Build the project:
   ```
   mvn clean package
   ```

2. Deploy the EAR file:
   ```
   asadmin deploy target/chronobank-ear-1.0.ear
   ```

## API Documentation

The REST API is documented using Swagger/OpenAPI. The documentation is available at:
```
http://localhost:8080/chronobank/api-docs
```

## Testing

ChronoBank includes comprehensive unit and integration tests:

- **Unit Tests**: Testing individual components in isolation.
- **Integration Tests**: Testing interactions between components.
- **System Tests**: Testing the entire system as a whole.

To run the tests:
```
mvn test
```

## Monitoring and Management

ChronoBank provides several monitoring and management features:

- **JMX Integration**: Exposing management beans for monitoring.
- **Logging**: Comprehensive logging for troubleshooting.
- **Performance Metrics**: Tracking system performance.
- **Health Checks**: Verifying system health.

## Conclusion

ChronoBank is a robust, secure, and scalable banking system built on Java EE technology. It provides a comprehensive set of features for banking operations, including account management, transaction processing, scheduled operations, and reporting. The multi-module architecture ensures separation of concerns and maintainability, while the use of EJB, Timer Services, and JPA provides a solid foundation for enterprise-grade functionality.