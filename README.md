# Oracle Database Navigator for IntelliJ IDEA

[![JetBrains Plugin](https://img.shields.io/jetbrains/plugin/v/1800-database-navigator.svg)](https://plugins.jetbrains.com/plugin/1800-database-navigator)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/1800-database-navigator.svg)](https://plugins.jetbrains.com/plugin/1800-database-navigator)

Oracle Database Navigator is a database development and management tool for IntelliJ-based IDEs, providing a comprehensive set of features for working with relational databases directly within the IDE.

This plugin is developed and actively maintained by Oracle as part of its developer tools ecosystem.

---

## Highlights

- Advanced code editors with deep IDE integration
- Rich database object navigation and intuitive browsing experience
- Integrated data editor with real-time result grids and inline editing
- Powerful execution engine with support for scripts, programs, and run configurations
- Built-in debugging and compiler support for database programs
- Intelligent workflows for managing database development directly within the IDE

---

## Features

- Advanced SQL and PL/SQL editor integrated with the IDE
- Database object browser (schemas, tables, views, procedures, etc.)
- Data editor with inline editing and result grids
- Script execution with run configurations and execution tracking  
- Integrated debugging and compiler support for database programs  
- Intelligent navigation across database objects
- Secure and flexible connection management
- Integrated AI-powered Database Assistant
- Oracle Cloud Infrastructure (OCI) integration (where applicable)


---

## Supported Databases

- Oracle
- MySQL
- PostgreSQL
- SQLite
- Any JDBC-compatible database (experimental)

---

## Getting Started

### Installation

1. Open IntelliJ IDEA (or any JetBrains IDE)
2. Go to **Settings / Preferences → Plugins**
3. Search for **Database Navigator**
4. Click **Install**
5. Restart the IDE

Or install directly from the JetBrains Marketplace:

https://plugins.jetbrains.com/plugin/1800-database-navigator

---

### Creating a Connection

1. Open the **DB Browser** tool window
2. Click the **+** icon to add a new connection
3. Select your database type
4. Enter connection details (e.g. host, port, credentials)
5. Test and save the connection

---

## Usage

### Browsing Database Objects

- Expand your connection in the DB Browser  
- Navigate through schemas, tables, views, indexes, and procedures

### Running Queries
 
- Open a SQL console
- Write your query
- Execute using **Ctrl/Cmd + Enter**

### Editing Data
 
- Right-click a table → **Edit Data**
- Modify data directly in the grid view
- Choose whether changes are auto-committed or require explicit commit

### Editing Code

- Right-click a program (procedure, function, package) → **Edit Code**
- Modify the code in the editor 
- Compare changes against the code version in the DB
- Save changes and verify execution results

### Executing and Debugging Programs

- Right-click a program (e.g. procedure) → **Execute...** or **Debug...**
- Provide values for the program arguments
- Execute and verify results in the DB Execution Console  
- Monitor and control execution using the debugger tool  

### Exploring Editor and Browser Features

- Right-click a database object in the DB Browser  
- Explore available actions for each object type  
- Open **DB Navigator** from the main menu to access additional features  

---

## Compatibility

- IntelliJ IDEA
- WebStorm
- PyCharm
- Other JetBrains IDEs

---

## Support

- Report issues: https://github.com/oracle/database-navigator/issues  
- Discussions: https://github.com/oracle/database-navigator/discussions  

---

## Contributing

This project welcomes contributions from the community. Before submitting a pull request, please [review our contribution guide](./CONTRIBUTING.md)

---

## Security

Please consult the [security guide](./SECURITY.md) for our responsible security vulnerability disclosure process

---

## License

This project is licensed under the Apache License 2.0.

See the [LICENSE](LICENSE.txt) file for details.

---

## Acknowledgements

- Original Database Navigator contributors
- Open-source community
- JetBrains Platform
