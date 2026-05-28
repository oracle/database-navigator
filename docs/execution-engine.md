# Execution Engine

## Summary

The Execution Engine runs SQL statements, SQL script files, database methods, and database Java methods. It manages execution dialogs, target context, parameter input, result tabs, messages, script output, and the history of method execution inputs.

- **Script Execution:** Runs complete SQL files through a database command-line client such as `sqlplus`, `mysql`, `psql`, or `sqlite3`.
- **Statement Execution:** Runs the statement or selection from a DBN SQL or PSQL editor.
- **Method Execution:** Runs database procedures, functions, package methods, and type methods.
- **Method Execution History:** Stores recently used method execution inputs and parameter values.
- **Java Execution:** Runs executable database Java methods.
- **Java Execution History:** Stores recently used Java method execution inputs and parameter values.

> **Note:** Available execution actions depend on the selected editor, database type, database capabilities, connection status, user privileges, selected schema, selected session, and DBN settings.

## When To Use It

Use the Execution Engine when you need to:

- Execute the statement at the caret or the selected SQL text.
- Execute multiple statements from a SQL or PSQL file.
- Run a complete SQL script with the native database command-line client.
- Provide bind variable values before statement execution.
- Execute or debug a database method with input arguments.
- Execute or debug a database Java method with input parameters.
- Review execution messages, result sets, output arguments, database logging, and execution duration.
- Reuse or edit previous method and Java method execution inputs.

## Access Paths

- **Statement Execution:** SQL/PSQL editor toolbar -> `Execute Statement`, or press `Ctrl+Enter`.
- **Debug Statement:** SQL/PSQL editor toolbar -> `Debug Statement`, when debugging is supported.
- **Script Execution:** SQL/PSQL editor toolbar -> `Execute as Script`.
- **Method Execution:** Program editor toolbar -> `Execute Method`, database method context actions, or Method Execution History.
- **Method Debugging:** Program editor toolbar -> `Debug Method`, when debugging is supported.
- **Method Execution History:** `DB Navigator` -> `Method Execution History`, or press `Ctrl+Shift+M`.
- **Java Execution:** Java editor toolbar -> `Execute Method`, database Java method context actions, or Java Execution History.
- **Java Debugging:** Java editor toolbar -> `Debug Method`, when debugging is supported.
- **Java Execution History:** `DB Navigator` -> `Java Execution History`, or press `Ctrl+Shift+J`.
- **Execution settings:** `DB Navigator` -> `Settings` -> `Execution Engine`, or `Settings` from an execution result toolbar.

## Execution Console

Execution output is displayed in the `DB Execution Console` tool window.

- **Messages:** Shows statement execution messages, compiler messages, and explain-plan messages.
- **Statement result tabs:** Show query result grids, execution messages, and statement-specific result actions.
- **Method result tabs:** Show method input and output arguments, cursor outputs, large values, logging output, and execution duration.
- **Java result tabs:** Show Java method inputs, output values, arrays, cursor outputs, large values, logging output, and execution duration.
- **Vector embedding result tabs:** Show embedding status, destination tables, inserted rows, and handoff actions for similarity search and `RAG`.
- **Script output:** Shows command-line script output as log output.

The Execution Console is opened automatically when execution produces output.

## Script Execution

Script Execution runs a whole SQL file through a command-line database client. DBN prepares a temporary script file, invokes the selected client, streams process output to the Execution Console, and removes the temporary file after execution.

### Script Execution Dialog

- **`Connection`:** Target connection for the script. Local files can choose a connection from the project.
- **`Schema`:** Target schema used by the generated command-line execution input.
- **`Command-line interface`:** Database client executable used to run the script.
- **`Execution timeout`:** Maximum runtime for the script. `0` means no timeout.
- **`Clear output from previous executions`:** Clears previous script output before the new run starts.

### Command-Line Interfaces

DBN offers default executable names for supported database types and can also store custom command-line interfaces.

- **Oracle:** `sqlplus`
- **MySQL:** `mysql`
- **PostgreSQL:** `psql`
- **SQLite:** `sqlite3`
- **Generic JDBC:** `sql`

Custom clients can be added from Execution Engine settings or from the script execution dialog with `New Cmd-Line Interface...`.

### Script Output And Safety

- **Single active run per file:** A script file cannot be started again while it is already running.
- **User approval:** Custom command-line interfaces are tracked as user-approved executables.
- **Temporary script file:** DBN writes a temporary script file with restricted file permissions where supported.
- **Process output:** Standard process output is streamed into the Execution Console.
- **Timeout handling:** If execution times out, DBN offers retry or cancel.

## Statement Execution

Statement Execution runs the current statement, selected SQL text, or multiple statements from a DBN SQL or PSQL editor.

### Statement Selection

- **Selected text:** Executes the selected SQL text.
- **Statement at caret:** Executes the executable statement under the caret.
- **No statement at caret:** DBN can ask whether to execute all statements or all statements after the caret.

### Statement Input Dialog

DBN shows the statement input dialog when the statement has execution variables, when statement prompting is enabled, or when debugging requires execution context.

- **Variables:** Enter values for bind variables detected in the statement.
- **Preview:** Review the statement with provided variable values applied for preview.
- **Target context:** Review connection, schema, session, timeout, commit behavior, and logging.
- **Reuse variables:** Reuse variable values across a bulk statement execution.

Statement execution can produce:

- **Cursor result:** Query result grid with fetch, find, view record, rerun, export, and settings actions.
- **Basic result:** Non-query execution message and status.
- **Messages:** Execution messages grouped in the Messages tab.
- **Logging output:** Database log output when supported and enabled.

## Method Execution

Method Execution runs executable database methods, such as procedures, functions, package methods, and type methods.

### Method Input Dialog

- **Header:** Shows the selected method and its owning object.
- **Arguments:** Shows input arguments and editable values.
- **Target context:** Shows connection, schema, session, timeout, commit behavior, and logging.
- **Debugger version:** Shows debugger details during debug execution when available.

Before the dialog opens, DBN loads method arguments and declared type details in the background.

### Target Context

- **Connection:** Fixed to the method connection.
- **Schema:** Selectable when the database supports invoker-rights method execution.
- **Session:** Selectable for normal execution. Debug execution uses the debug session.
- **Commit after execution:** Enabled by default unless the connection is already auto-commit.
- **Enable logging:** Available when the database supports DBN database logging for method execution.

### Method Results

- **Argument tree:** Shows input and output argument values.
- **Output tabs:** Show cursor output, large object output, and large value output.
- **Logging tab:** Shows database logging output when supported.
- **Status bar:** Shows the connection, session type, and execution duration.

The method result toolbar provides `Close`, `Edit Method`, `Open Execution Dialog`, `Execute Again`, `Stop Execution`, and `Settings`.

## Method Execution History

Method Execution History stores execution inputs for database methods. It preserves method selection, target context, execution options, and argument value history.

- **Group by Program:** Toggles between a grouped program tree and a flat history tree.
- **Delete:** Removes selected history entries.
- **Edit inputs:** Edits stored argument values and execution context.
- **Execute:** Runs the selected history entry.
- **Debug:** Starts debugging for the selected entry when supported.
- **Save:** Saves changes made in the history dialog.
- **Settings:** Opens Execution Engine settings.

History entries are project state. Entries tied to removed connections are cleaned up.

## Java Execution

Java Execution runs executable database Java methods. It uses the database Java method metadata to build the input form and to render output values.

### Java Input Dialog

- **Header:** Shows the selected Java method.
- **Parameters:** Shows input parameters and editable values.
- **Target context:** Shows connection, schema, session, timeout, commit behavior, and logging.
- **Debugger version:** Shows debugger details during debug execution when available.

Before the dialog opens, DBN loads method parameters and nested Java class details needed by complex parameters.

### Java Results

- **Values tree:** Shows input values and output values.
- **Detail tabs:** Show array values, cursor output, large object output, large value output, and code-block inputs.
- **Logging tab:** Shows database logging output when supported.
- **Status bar:** Shows the connection, session type, and execution duration.

The Java result toolbar provides `Close`, `Edit Method`, `Open Execution Dialog`, `Execute Again`, `Stop Execution`, and `Settings`.

## Java Execution History

Java Execution History stores execution inputs for database Java methods. It preserves Java method selection, target context, execution options, and parameter value history.

- **Group by Program:** Toggles between a grouped Java class tree and a flat history tree.
- **Delete:** Removes selected history entries.
- **Edit inputs:** Edits stored parameter values and execution context.
- **Execute:** Runs the selected history entry.
- **Debug:** Starts debugging for the selected entry when supported.
- **Save:** Saves changes made in the history dialog.
- **Settings:** Opens Execution Engine settings.

History entries are project state. Entries tied to removed connections are cleaned up.

## Common Workflows

### Execute A Statement

1. Open a DBN SQL or PSQL file.
2. Select the target connection, schema, and session in the editor toolbar.
3. Place the caret inside a statement or select SQL text.
4. Click `Execute Statement` or press `Ctrl+Enter`.
5. Enter execution variables if prompted.
6. Review the result tab or messages in the Execution Console.

### Run A Script

1. Open a SQL or PSQL script file.
2. Click `Execute as Script`.
3. Select the target connection, schema, command-line interface, and timeout.
4. Choose whether to clear previous output.
5. Start execution and review the script output in the Execution Console.

### Execute A Method

1. Open a database method or a source editor containing executable methods.
2. Click `Execute Method`.
3. Enter input argument values.
4. Review the target context and execution options.
5. Start execution and review output arguments, cursor output, large values, and logging output.

### Reuse A Method Input

1. Open `DB Navigator` -> `Method Execution History`.
2. Select a history entry.
3. Edit argument values or target context if needed.
4. Click `Execute`, `Debug`, or `Save`.

### Execute A Java Method

1. Open a database Java class or Java method.
2. Click `Execute Method`.
3. Enter parameter values.
4. Review the target context and execution options.
5. Start execution and review Java output values, cursor output, arrays, large values, and logging output.

## Troubleshooting

| Symptom | Resolution |
| --- | --- |
| `Execute as Script` cannot start | Configure a command-line interface for the database type and confirm the executable path is valid. |
| Script output is missing | Check the selected command-line interface, connection credentials, and whether previous output was cleared. |
| Script execution says the file is already running | Wait for the active script process for that file to finish or stop it from the Execution Console. |
| Statement execution dialog appears unexpectedly | Check `Prompt execution dialog` in Execution Engine settings or whether the statement contains execution variables. |
| Statement result did not take focus | Enable `Focus result after execution` in Statement Execution settings. |
| Method cannot be resolved | Refresh the Database Browser and confirm the method still exists on an active connection. |
| Debug action is disabled | Confirm the selected database, object, and connection support debugging. |
| History entry disappeared | Entries are removed when their connection is removed or the referenced method becomes obsolete. |
| Logging option is hidden | The database or execution target may not support DBN database logging. |

## Related Documentation

- [Execution Engine Settings](./execution-engine-settings.md): Settings reference for statement, script, method, and Java execution.
- [Workspace Integration](./workspace-integration.md): Guide to the file connection context used when executing local SQL and PSQL files.
- [Transaction Management](./transaction-management.md): Guide to auto-commit, commit after execution, and session-aware transaction handling.
- [Debugging Engine](./debugging-engine.md): Guide to statement, method, and database Java debugging workflows.
- [Code Editors](./code-editors.md): Guide to opening SQL, PSQL, program, and Java editors that start execution workflows.
- [Data Viewers and Editors](./data-viewers-and-editors.md): Guide to result grids, cursor output, record viewing, and exported result data.
- [Database Assistant](./database-assistant.md): Overview of assistant paths used for generated SQL and assistant-driven database work.
- [Database Assistant: Oracle Select AI](./database-assistant-select-ai.md): Guide to Select AI generated SQL and editor prompt actions.
- [Vector Toolbox](./vector-toolbox.md): Guide to vector embedding results and semantic-search handoffs.
