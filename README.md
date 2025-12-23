# 🏦 Bank-Management-System

This Bank Management System is designed to replicate core banking workflows in a structured and modular manner. The application focuses on clarity, maintainability, and correctness by separating concerns such as user interaction, business logic, and database access.

The system follows a layered approach:

Presentation Layer using Java Swing for user interaction

Business Logic Layer handling banking rules and validations

Data Access Layer using JDBC for SQL database operations

This design makes the project easy to understand, extend, and refactor for future enhancements like web or API-based architectures.


🔐 Authentication & Security Flow


Separate Admin and User login workflows

Input validation to prevent invalid operations

Controlled access to sensitive operations using role-based authorization

Centralized exception handling to prevent application crashes


🗃️ Database Design Overview


The system uses a relational SQL database to store persistent data such as:

User account information

Account balances

Transaction records

Admin credentials


Key database principles followed:

Primary key–based identification

Proper normalization to reduce redundancy

Consistent use of JDBC prepared statements


🧩 Core Functional Modules


🔹 Login Module


Handles authentication for both Admin and Users

Redirects users based on role

Validates credentials using database lookup


🔹 Banking Operations Module


Deposit money into account

Withdraw money with balance checks

View current balance and account details

Enforces business rules (e.g., insufficient balance prevention)


🔹 Admin Control Module


View and manage registered user accounts

Monitor system activity

Maintain overall system integrity


🧱 Object-Oriented Design Principles Applied


Encapsulation: Sensitive data protected using private fields and public methods

Modularity: Separate classes for GUI, logic, and database operations

Reusability: Common functions reused across modules

Maintainability: Clean class structure and meaningful naming conventions


🛠️ Tech Stack


Programming Language: Java

GUI Framework: Java Swing

Database: SQL

Connectivity: JDBC

IDE: VS Code

📂 Project Structure

Bank-Management-System/

│

├── LoginScreen.java        # Authentication module

├── AdminPanel.java         # Admin dashboard

├── BankGUI.java            # Main user interface

├── BankAccount.java        # Banking logic

├── DatabaseConnection.java # JDBC connection handling

├── ErrorHandler.java       # Exception handling

└── README.md
