# Execution Engine Settings

## Summary

Execution Engine settings define the defaults for statement execution, script execution, method execution, and database Java execution. These settings are project-level defaults; execution dialogs can override some values for a specific run.

| Setting area | Scope | Purpose |
| --- | --- | --- |
| Statement Execution | Project | Controls timeouts, result fetch size, execution prompt behavior, and result focus. |
| Script Execution | Project | Controls script timeout and custom command-line database clients. |
| Method Execution | Project | Controls method execution timeouts and method parameter history size. |
| Java Execution | Project | Controls Java method execution timeouts and Java parameter history size. |

> **Note:** Execution settings provide defaults and validation rules. The active connection, selected schema, selected session, database features, object type, and user privileges still determine what can be executed.

## Access Paths

- **Execution Engine:** `DB Navigator` -> `Settings` -> `Execution Engine`
- **Statement result settings:** Statement result toolbar -> `Settings`
- **Method result settings:** Method result toolbar -> `Settings`
- **Java result settings:** Java result toolbar -> `Settings`
- **Messages settings:** Execution Console `Messages` toolbar -> `Settings`
- **Method history settings:** Method Execution History toolbar -> `Settings`
- **Java history settings:** Java Execution History toolbar -> `Settings`

## Statement Execution Settings

Statement Execution settings control single-statement execution, selected-text execution, bulk statement execution, statement debugging, and query result fetching.

| Setting | Default | Valid range | Description |
| --- | --- | --- | --- |
| `Execution timeout` | `20` seconds | `0` to `6000` | Maximum time for normal statement execution. `0` means no timeout. |
| `Debug execution timeout` | `600` seconds | `0` to `6000` | Maximum time for statement debugging. `0` means no timeout. |
| `Result-Set fetch block size` | `100` records | `1` to `10000` | Number of records fetched per block for query result sets. |
| `Prompt execution dialog` | Disabled | Enabled/disabled | Shows the statement execution dialog before running statements, even when no variables are required. |
| `Focus result after execution` | Disabled | Enabled/disabled | Moves focus to the result tab after statement execution. |

### Statement Dialog Behavior

- **Statement has execution variables:** DBN prompts for variable values.
- **`Prompt execution dialog` is enabled:** DBN prompts before normal statement execution.
- **Statement debugging is started:** DBN prompts for execution context and debug timeout.
- **Bulk execution has repeated variables:** DBN can reuse variable values across statements.

## Script Execution Settings

Script Execution settings control execution of complete SQL files through database command-line clients.

| Setting | Default | Valid range | Description |
| --- | --- | --- | --- |
| `Execution timeout` | `300` seconds | `0` to `6000` | Maximum time for SQL script execution. `0` means no timeout. |
| `Command line interfaces` | No custom entries | Named clients | Custom database client executables available in the script execution dialog. |

### Command-Line Interface Fields

| Field | Required | Description |
| --- | --- | --- |
| `Database Type` | Yes | Database type the client executable supports. |
| `Name` | Yes | Display name shown in settings and execution dialogs. Must be unique. |
| `Executable Path` | Yes | Path to the database command-line client executable. |

### Built-In Defaults

DBN offers built-in command-line client entries even when no custom clients are configured.

- **Oracle:** `sqlplus`
- **MySQL:** `mysql`
- **PostgreSQL:** `psql`
- **SQLite:** `sqlite3`
- **Generic JDBC:** `sql`

Custom entries are useful when the executable is not on `PATH`, when multiple client versions are installed, or when a database client has a non-standard location.

### User Approval

Custom command-line interfaces are tracked as approved executables. When the configured list changes, DBN updates the executable approvals associated with those entries.

## Method Execution Settings

Method Execution settings control database method execution and debugging.

| Setting | Default | Valid range | Description |
| --- | --- | --- | --- |
| `Execution timeout` | `30` seconds | `0` to `6000` | Maximum time for normal method execution. `0` means no timeout. |
| `Debug execution timeout` | `600` seconds | `0` to `6000` | Maximum time for method debugging. `0` means no timeout. |
| `Parameter history size` | `10` | `0` to `3000` | Number of recent values retained for method input arguments. |

### Method History Behavior

- **Stored execution inputs:** DBN stores method references, target schema, target session, execution options, and argument value history.
- **Grouping:** Method Execution History can be grouped by program.
- **Removed connections:** Entries tied to removed connections are cleaned up.
- **Recent methods:** Program editors can use history to list recently executed methods.

## Java Execution Settings

Java Execution settings control database Java method execution and debugging.

| Setting | Default | Valid range | Description |
| --- | --- | --- | --- |
| `Execution timeout` | `30` seconds | `0` to `6000` | Maximum time for normal Java method execution. `0` means no timeout. |
| `Debug execution timeout` | `600` seconds | `0` to `6000` | Maximum time for Java method debugging. `0` means no timeout. |
| `Parameter history size` | `10` | `0` to `3000` | Number of recent values retained for Java method input parameters. |

### Java History Behavior

- **Stored execution inputs:** DBN stores Java method references, target schema, target session, execution options, and input value history.
- **Grouping:** Java Execution History can be grouped by Java class.
- **Removed connections:** Entries tied to removed connections are cleaned up.
- **Recent methods:** Java editors can use history to list recently executed Java methods.

## Per-Run Execution Options

Execution dialogs can override default settings for one run.

| Option | Applies to | Description |
| --- | --- | --- |
| `Connection` | Statement, script, method, Java | Target connection. Method and Java execution are fixed to the owning object connection. |
| `Schema` | Statement, script, method, Java | Target schema where schema selection is available. |
| `Session` | Statement, method, Java | Target session for normal execution. Debug execution uses the debug session. |
| `Execution timeout` | Statement, script, method, Java | Runtime limit for this run. |
| `Debug execution timeout` | Statement, method, Java | Runtime limit for this debug run. |
| `Commit after execution` | Statement, method, Java | Commits automatically after execution when selected and auto-commit is not active. |
| `Enable logging` | Statement, method, Java | Enables database logging when the database and execution target support it. |
| `Remember execution variables` | Statement | Reuses variable values for supported bulk statement executions. |
| `Clear output from previous executions` | Script | Clears previous script output before starting the new script run. |

## Validation Messages

| Message area | Common cause | Resolution |
| --- | --- | --- |
| Statement Execution | Timeout or fetch block size is outside the allowed range | Enter values within the ranges shown in Statement Execution settings. |
| Script Execution | Timeout is outside the allowed range | Enter a value from `0` to `6000` seconds. |
| Command-line interfaces | Missing name, duplicate name, missing database type, or missing executable path | Provide a complete unique entry for each custom client. |
| Command-line executable | Selected executable does not match the selected database type | Select a client executable for the selected database type or adjust the database type. |
| Method Execution | Timeout or parameter history size is outside the allowed range | Use a timeout from `0` to `6000` seconds and history size from `0` to `3000`. |
| Java Execution | Timeout or parameter history size is outside the allowed range | Use a timeout from `0` to `6000` seconds and history size from `0` to `3000`. |

## Related Documentation

- [Execution Engine](./execution-engine.md): Feature guide for script, statement, method, and Java execution.
- [Environment Types Settings](./environment-types-settings.md): Settings reference for environment colors on execution result tabs.
- [Workspace Integration Settings](./workspace-integration-settings.md): Settings reference for file connection context used by local script and statement execution.
- [Transaction Management Settings](./transaction-management-settings.md): Settings reference for auto-commit, transaction prompts, and session behavior.
- [Debugging Engine Settings](./debugging-engine-settings.md): Settings reference for debugger type, JDWP tunnel behavior, and debug sessions.
- [Code Editors](./code-editors.md): Guide to editors and toolbar actions that start execution workflows.
- [Data Viewers and Editors](./data-viewers-and-editors.md): Guide to result grids, cursor output, record viewing, and exported result data.
