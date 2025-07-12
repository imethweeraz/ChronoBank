# ChronoBank Banking System

A comprehensive Java EE banking system with EJB and Timer Services.

## Overview

ChronoBank is a multi-module Maven project that implements a banking system using Java EE technologies. It leverages Enterprise JavaBeans (EJB), Timer Services, and Java Persistence API (JPA) to provide a robust, secure, and scalable banking solution.

## Project Structure

The project is organized into the following modules:

1. **chronobank-common**: Shared entities, DTOs, utilities, and constants
2. **chronobank-ejb**: EJB components, timer services, transaction management, and business logic
3. **chronobank-web**: REST endpoints, web interface, and API documentation
4. **chronobank-ear**: Application packaging and deployment descriptors

## Key Features

- **Timer Services**: Scheduled fund transfers, interest calculations, daily balance updates, and report generation
- **Transaction Management**: ACID compliance, container-managed transactions, and rollback mechanisms
- **Security**: Role-based access control, JAAS integration, and audit logging
- **Database**: JPA persistence, connection pooling, and data integrity

## Requirements

- JDK 11
- GlassFish 7.0.23
- MySQL Database
- Maven 3.8+

## Getting Started

1. Clone the repository:
   ```
   git clone https://github.com/imethweeraz/ChronoBank.git
   ```

2. Build the project:
   ```
   cd ChronoBank
   mvn clean package
   ```

3. Set up the database (see [Technical Documentation](docs/technical-documentation.md) for details)

4. Deploy to GlassFish (see [Technical Documentation](docs/technical-documentation.md) for details)

## Documentation

For detailed technical documentation, see [Technical Documentation](docs/technical-documentation.md).

## License

This project is licensed under the MIT License - see the LICENSE file for details.
