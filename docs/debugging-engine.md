# Debugging Engine

## Summary

The Debugging Engine lets you debug Oracle database code from DBN editors and IDE debug sessions. It supports PL/SQL statement debugging, database method debugging, and database Java debugging through the debugger type configured on the connection.

Debugging is available for database connections that support the DBN debugging feature. In the current DBN implementation, this is an Oracle connection capability.

- **Debugger Types:** Choose between JDWP debugging over TCP and classic JDBC debugging over `DBMS_DEBUG`.
- **Statement Debugger:** Debug an executable statement from a SQL or PL/SQL editor.
- **Method Debugger:** Debug a procedure, function, package method, or type method.
- **Java Debugger:** Debug database Java methods through JDWP.
- **Breakpoints:** Use editor gutter breakpoints in database source files and debuggable SQL code.

## Contents

<!-- TOC -->
- [When To Use It](#when-to-use-it)
- [Access Paths](#access-paths)
- [Debugger Types](#debugger-types)
- [Debug Session Flow](#debug-session-flow)
- [Breakpoints](#breakpoints)
- [Statement Debugger](#statement-debugger)
- [Method Debugger](#method-debugger)
- [Java Debugger](#java-debugger)
- [Debug Controls](#debug-controls)
- [Prerequisites](#prerequisites)
- [Common Workflows](#common-workflows)
- [Troubleshooting](#troubleshooting)
- [Related Documentation](#related-documentation)
<!-- /TOC -->

## When To Use It

Use the Debugging Engine when you need to:

- Step through Oracle PL/SQL statements or stored database methods.
- Debug database Java methods with JDWP.
- Verify breakpoint behavior in database source editors.
- Reuse method or Java execution inputs while debugging.
- Diagnose debugger startup issues related to privileges, packages, tunnels, or network callbacks.

## Access Paths

- **Debug a SQL or PL/SQL statement:** Open a SQL editor and use the `Debug Statement` action from the editor toolbar.
- **Debug a database method:** Open a method or program editor and use the `Debug Method` action.
- **Debug a package or type method:** Open the package or type source and select the method to debug from the debug action menu.
- **Debug a database Java method:** Open database Java source and use the Java debug action for the method or class.
- **Reuse recent method inputs:** Open Method Execution History or Java Execution History, then debug from a saved input.
- **Configure debugger type and tunnels:** Open the connection settings and use the Debugger tab.
- **Configure debug execution timeouts:** Open the Execution Engine settings.

## Debugger Types

DBN can start statement and method debug sessions with either JDWP or JDBC, depending on the connection setting and IDE support.

| Debugger Type | Transport | Best For | Notes |
| --- | --- | --- | --- |
| JDWP | TCP | PL/SQL debugging with JDWP support, database Java debugging, cloud or tunneled debugging setups. | Requires IDE Java debugger support, `DBMS_DEBUG_JDWP`, and database network access back to the IDE or configured tunnel. |
| JDBC | JDBC connection | Classic PL/SQL statement and method debugging. | Uses `DBMS_DEBUG` and does not require a TCP callback from the database. |
| Ask | Interactive selection | Connections where both debugger types may be useful. | DBN prompts for JDWP, JDBC, or cancel when a debug session starts. |

Database Java debugging always uses JDWP. If JDWP support is not available in the IDE, Java debugging cannot fall back to the JDBC debugger.

## Debug Session Flow

When a debug action starts, DBN follows the same general flow for statements, methods, and Java methods:

1. Resolve the debugger type from the connection setting.
2. Verify that the database and IDE support the requested debugger.
3. Check the required database privileges and packages.
4. Optionally compile dependent database objects in debug mode.
5. Prompt for execution input such as arguments, target schema, session settings, and timeout.
6. Start the debug process and register applicable breakpoints.
7. Launch the target statement or method.
8. Clean up breakpoints, sessions, tunnels, and database connections when debugging stops.

## Breakpoints

DBN breakpoints are editor gutter breakpoints that belong to database source code or debuggable SQL code.

- **Database program source:** Supports breakpoints in methods, packages, package bodies, types, and type bodies.
- **SQL or PL/SQL editor code:** Supports breakpoints in debuggable executable code.
- **JDWP database Java code:** Can include database Java line breakpoints for the same connection.
- **Source loading:** DBN re-registers database breakpoints when source content is loaded into the editor.

Breakpoints work best when the target object and its dependencies are compiled with debug information. If DBN detects dependencies that are not compiled for debugging, it can prompt you to compile all, compile selected, or continue without compiling.

## Statement Debugger

The Statement Debugger starts from an executable statement in a SQL or PL/SQL editor. DBN identifies the debuggable statement at the caret, opens the statement execution input when needed, and starts the selected debugger type.

Use statement debugging when you want to inspect an anonymous block or executable statement without first opening a stored method editor.

- **Debug action:** Available from the SQL editor toolbar when the caret is on debuggable code.
- **Debugger types:** Supports JDWP and JDBC when the connection and IDE allow them.
- **Execution input:** Uses the statement execution dialog for target context, variables, timeout, commit behavior, and logging options.
- **Run configuration:** DBN creates a Database Statement debug configuration for the session.

## Method Debugger

The Method Debugger starts a debug session for stored database methods, including standalone procedures and functions, package methods, and type methods.

- **Method selection:** Select a method directly from source, from a program object, from the method browser, or from method execution history.
- **Arguments:** Uses the method execution input dialog for argument values and target context.
- **Debugger types:** Supports JDWP and JDBC when available.
- **Dependencies:** Can compile selected dependencies with debug information before the debug session starts.
- **Run configuration:** DBN creates a Database Method debug configuration and stores method selection history.

## Java Debugger

The Java Debugger starts JDWP debug sessions for database Java methods. It integrates with the IDE Java debugger and the DBN database execution wrappers required to invoke database Java code.

- **Method selection:** Select database Java methods from Java source, class actions, method browser, or Java execution history.
- **Debugger type:** Uses JDWP only. JDBC fallback is not available for Java debugging.
- **Execution input:** Uses the Java execution input dialog for method arguments, target context, timeout, and logging options.
- **Wrapper support:** DBN prepares database wrappers used to call the selected Java method during debugging.
- **Run configuration:** DBN creates a Database Java debug configuration for the session.

## Debug Controls

DBN debug sessions use the IDE debugger tool window and standard debug controls.

- **Resume:** Continue execution until the next breakpoint or completion.
- **Step Into:** Enter the next debuggable call when possible.
- **Step Over:** Execute the current line without entering nested calls.
- **Step Out:** Continue until the current routine returns.
- **Run to Position:** Continue execution to a selected source position.
- **Pause or Synchronize:** Ask the debugger to synchronize with the running target session.
- **Stop:** End the debug session and release DBN debug resources.

## Prerequisites

The exact grants depend on the debugger type and database environment. DBN checks these before it starts a session and reports missing prerequisites.

- **JDBC PL/SQL debugging:** Debug connect session, debug any procedure, and execute access to `SYS.DBMS_DEBUG`.
- **JDWP PL/SQL debugging:** JDBC PL/SQL requirements plus execute access to `SYS.DBMS_DEBUG_JDWP` and a JDWP host access control entry.
- **Database Java debugging:** JDWP requirements plus Java virtual machine support and wrapper creation privileges.

## Common Workflows

- **Debug a statement quickly:** Open a SQL editor, place the caret in the statement, add breakpoints if needed, then run `Debug Statement`.
- **Debug a stored method:** Open the method or containing program, choose `Debug Method`, provide argument values, and start the debug session.
- **Debug with JDWP over a tunnel:** Configure the connection Debugger tab for JDWP and the required tunnel type, then start the debug action.
- **Continue despite missing debug info:** When dependency compilation is offered, choose `Compile none` to continue without compiling dependencies.
- **Reuse a previous method input:** Open the method or Java execution history and start debugging from a saved execution entry.

## Troubleshooting

| Symptom | Resolution |
| --- | --- |
| Debug action is disabled | Confirm the connection supports debugging and the caret is on debuggable code or source. |
| JDWP is unavailable | Confirm the IDE has Java debugger support and the connection debugger type is not forcing JDWP in an unsupported environment. |
| Java debug cannot start | Java debugging requires JDWP; switch to an IDE edition or environment with Java debugger support. |
| Breakpoints are ignored | Compile the target object and dependencies with debug information. |
| Database cannot connect back to the IDE | Check the JDWP TCP host, port range, tunnel type, firewall rules, and Oracle host access control entry. |
| A second debug session cannot start | DBN supports one active debug session per connection at a time. Stop the existing session first. |
| Missing privilege warnings appear | Run the prerequisite verification and grant the missing debug package, debug session, or JDWP host permissions. |

## Related Documentation

- [Debugging Engine Settings](./debugging-engine-settings.md): Configure debugger type, JDWP tunnel behavior, reverse SSH settings, and debug timeouts.
- [Execution Engine](./execution-engine.md): Run statements, scripts, database methods, and database Java methods outside the debugger.
- [Execution Engine Settings](./execution-engine-settings.md): Configure execution inputs, histories, timeouts, and debug execution limits.
- [Code Editors](./code-editors.md): Edit SQL, PL/SQL, DDL, program source, and database Java source used by the debugger.
- [Connection Management Settings](./connection-management-settings.md): Configure connection-level options, including the Oracle Debugger tab.
