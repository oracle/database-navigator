# Debugging Engine Settings

## Summary

Debugging settings are split between connection-level debugger options and Execution Engine timeout settings. The connection settings decide which debugger DBN uses and how JDWP connects. The Execution Engine settings control debug execution timeouts and input history behavior.

- **Connection Debugger tab:** Configure JDWP, JDBC, dependency compilation, TCP ports, and reverse SSH tunnel settings.
- **Execution Engine debug settings:** Configure statement, method, and Java debug timeouts.
- **Run/debug configurations:** Store custom statement, method, and Java debug launch configuration.
- **Prerequisite checks:** Validate required database packages, grants, Java support, and JDWP host access.

## Contents

<!-- TOC -->
- [Access Paths](#access-paths)
- [Connection Debugger Settings](#connection-debugger-settings)
  - [Debug Engine Type](#debug-engine-type)
- [JDWP Tunnel Settings](#jdwp-tunnel-settings)
  - [TCP Host and Port Range](#tcp-host-and-port-range)
- [Reverse SSH Tunnel Settings](#reverse-ssh-tunnel-settings)
- [Execution Engine Debug Settings](#execution-engine-debug-settings)
- [Run and Debug Configurations](#run-and-debug-configurations)
- [Prerequisite Checks](#prerequisite-checks)
- [Validation and Troubleshooting](#validation-and-troubleshooting)
- [Related Documentation](#related-documentation)
<!-- /TOC -->

## Access Paths

- **Debugger type:** Connection settings -> selected Oracle connection -> Debugger tab.
- **JDWP tunnel settings:** Connection settings -> selected Oracle connection -> Debugger tab.
- **Reverse SSH tunnel:** Connection settings -> selected Oracle connection -> Debugger tab -> JDWP Reverse Tunnel.
- **Debug execution timeout:** Execution Engine settings.
- **Statement, method, and Java debug configurations:** IDE Run/Debug Configurations.

## Connection Debugger Settings

The Debugger tab is available for Oracle connections. It controls how DBN starts PL/SQL and database Java debug sessions for the connection.

| Setting | Default | Description |
| --- | --- | --- |
| Compile dependencies before execution | Enabled | Prompts to compile dependent database objects with debug information before a supported debug session starts. |
| Debug engine type | Ask when JDWP is supported, otherwise Classic JDBC | Selects whether DBN uses JDWP, JDBC, or prompts at debug time. |
| JDWP tunnel type | None | Defines how the database reaches the IDE JDWP listener. |
| TCP host address | Empty | Host address passed to the database for JDWP callback. Empty lets DBN resolve the local address. |
| TCP port range | 4000 to 4999 | Port range DBN scans for a free JDWP listener port. |

### Debug Engine Type

- **Ask:** Prompts when a statement or method debug session starts.
- **JDWP:** Uses JDWP over TCP. Required for database Java debugging.
- **JDBC:** Uses the classic JDBC debugger over `DBMS_DEBUG`. Available for PL/SQL statement and method debugging.

When the setting is `Ask`, the runtime prompt also allows you to cancel the debug session.

## JDWP Tunnel Settings

JDWP debugging requires the database session to reach the IDE debugger listener. The tunnel type decides how that connection is made.

| Tunnel type | Description | Typical use |
| --- | --- | --- |
| None | The database connects directly to the configured TCP host and port. | Local or network environments where the database can reach the IDE machine. |
| TCP driver tunnel | Uses the JDBC driver tunnel support to provide the JDWP host and port. | Cloud database environments or drivers that expose a JDWP TCP tunnel. |
| SSH reverse tunnel | Opens a reverse SSH tunnel so the database can connect through the SSH host. | Restricted networks where direct database-to-IDE TCP access is not available. |

### TCP Host and Port Range

- **TCP host address:** Leave empty when DBN should resolve the local host address automatically. Set it explicitly when the database must connect to a specific reachable interface or hostname.
- **TCP port range:** DBN scans the configured range and uses the first free port for the JDWP listener. The fields must contain numeric values.

## Reverse SSH Tunnel Settings

These settings are used when the JDWP tunnel type is `SSH reverse tunnel`.

| Setting | Default | Description |
| --- | --- | --- |
| Host | Empty | SSH server used to create the reverse tunnel. |
| Port | 22 | SSH server port. |
| User | Empty | SSH username. |
| Authentication type | Password | Selects password authentication or key pair authentication. |
| Password | Empty secret | Password used for password authentication. Stored with the IDE password safe. |
| Key file | Empty | Private key file used for key pair authentication. |
| Key passphrase | Empty secret | Passphrase for the private key file. Stored with the IDE password safe. |
| Bind host | 127.0.0.1 | Host address bound on the remote side of the reverse tunnel. |
| Bind port | 0 | Remote bind port. `0` lets the tunnel implementation choose an available port. |

## Execution Engine Debug Settings

Debug execution limits are configured with the Execution Engine settings because debug sessions are launched through statement, method, and Java execution workflows.

| Setting | Default | Range | Description |
| --- | --- | --- | --- |
| Statement debug execution timeout | 600 seconds | 0 to 6000 seconds | Timeout used by statement debug sessions. |
| Method debug execution timeout | 600 seconds | 0 to 6000 seconds | Timeout used by method debug sessions. |
| Java debug execution timeout | 600 seconds | 0 to 6000 seconds | Timeout used by database Java debug sessions. |
| Method parameter history size | 10 entries | 0 to 3000 entries | Number of recent method input values kept for execution and debugging. |
| Java parameter history size | 10 entries | 0 to 3000 entries | Number of recent Java method input values kept for execution and debugging. |

Execution input dialogs can still override session-specific details such as target schema, timeout, commit behavior, and logging behavior when those options apply.

## Run and Debug Configurations

DBN registers statement, method, and Java run/debug configuration types with the IDE. These configurations are launched with the Debug executor.

- **Database Statement:** Created for statement debug sessions started from SQL or PL/SQL editors.
- **Database Method:** Stores a selected database method and method execution input. The method can be selected from the method browser or execution history.
- **Database Java:** Stores a selected database Java method and Java execution input. Java debugging uses JDWP.

DBN suppresses the standard build-before-launch behavior for database debug configurations because the target code is compiled and executed in the database.

## Prerequisite Checks

DBN verifies the database prerequisites before starting a debug session.

- **JDBC PL/SQL debugging:** `DEBUG CONNECT SESSION`, `DEBUG ANY PROCEDURE`, and execute access to `SYS.DBMS_DEBUG`.
- **JDWP PL/SQL debugging:** JDBC PL/SQL requirements plus execute access to `SYS.DBMS_DEBUG_JDWP` and JDWP host access control entry.
- **Database Java debugging:** JDWP requirements plus Java virtual machine support, `CREATE PROCEDURE`, and `CREATE TYPE`.

If prerequisites are missing, DBN reports the missing operation before the debug session is initialized.

## Validation and Troubleshooting

| Symptom | Resolution |
| --- | --- |
| TCP port range inputs must be numeric | Enter numeric values in both TCP port fields. |
| Debugging is not supported for the database | Use an Oracle connection with debugging support. |
| JDWP debugger is not available | Use JDBC for PL/SQL debugging, or enable an IDE environment with Java debugger support. |
| Java debugging is not available | Java debugging requires JDWP; JDBC cannot debug database Java methods. |
| Another debug session is active | Stop the existing debug session for the connection before starting another one. |
| Breakpoints are not hit | Compile the target object and dependencies with debug information. |
| JDWP connection fails | Check TCP host, port range, tunnel type, SSH settings, database network reachability, and Oracle JDWP host access permissions. |

## Related Documentation

- [Debugging Engine](./debugging-engine.md): Debug statements, database methods, and database Java methods.
- [Execution Engine Settings](./execution-engine-settings.md): Configure execution inputs, history sizes, execution timeouts, and debug timeouts.
- [Connection Management Settings](./connection-management-settings.md): Configure Oracle connection settings, including the Debugger tab.
- [Code Editors](./code-editors.md): Work with SQL, program source, and Java source files used during debugging.
