# Code Editors

## Summary

Code Editors are the DBN editing surfaces for SQL, database source code, DDL-linked files, and database Java source. They combine IDE editing features with DBN connection context, execution actions, database object metadata, and source synchronization.

- **SQL Consoles:** Connection-owned virtual editors for ad hoc SQL and database interaction.
- **SQL Script Files:** Project SQL/PSQL files with DBN connection context and execution toolbar actions.
- **DDL File Editors:** Project files attached to database objects and optionally synchronized from database source.
- **Program Editors:** Database source editors for procedures, functions, packages, types, triggers, views, and related source-backed objects.
- **Java Editors:** Database Java source editors for Java classes, resources, and executable Java methods.

> **Note:** Available actions depend on the selected editor, database type, connection permissions, object type, DBN settings, and whether the target database supports the feature.

## When To Use It

Use Code Editors when you need to:

- Run ad hoc SQL in a connection-specific SQL Console.
- Edit and execute project SQL or PSQL files.
- Execute a single statement, run a whole script, debug a statement, or view an explain plan.
- Edit database program source and save it back to the database.
- Compile database programs and compare local changes with the original or database version.
- Attach project DDL files to database objects for source tracking.
- Execute or debug database methods and database Java methods.

## Access Paths

- **SQL Console:** `DB Browser` toolbar -> `Open SQL Console`, connection context menu, or `DB Navigator` -> `Open SQL Console...`
- **SQL Script File:** Open a DBN SQL or PSQL file from the project, such as `.sql`, `.ddl`, `.psql`, `.plsql`, `.pkg`, or `.pks`.
- **DDL File Editor:** Open an attached DDL file from the project or from a database object's `DDL Files` action.
- **Program Editor:** Open a source-backed database object from the Database Browser, such as a procedure, function, package, type, trigger, or view.
- **Java Editor:** Open a database Java class or Java resource from the Database Browser.
- **Editor settings:** Editor toolbar -> `Settings`, or `DB Navigator` -> `Settings` -> relevant settings page.

## Editor Types

| Editor type | Backing content | Connection context | Common actions |
| --- | --- | --- | --- |
| SQL Console | DBN virtual console stored with the connection | Fixed to the console connection, schema, and session | Execute statement, explain plan, execute as script, commit, rollback, save to file, rename, delete |
| SQL Script File | Local project file | Selected from file context or editor toolbar | Execute statement, debug statement, explain plan, execute as script, commit, rollback |
| DDL File Editor | Local project file attached to a database object | Derived from the attached object or file context | Inspect generated DDL, detach mapping, execute as script when file type supports it |
| Program Editor | Database object source loaded through DBN | Object connection and schema | Save to database, revert, reload, compile, compare, execute/debug methods, manage DDL files |
| Java Editor | Database Java source loaded through DBN | Object connection and schema | Save, reload, compare, execute/debug Java methods, manage DDL files |

## Shared Editor Toolbar

SQL Consoles and SQL/PSQL project files use the DBN file editor toolbar. The available actions change by file type and context.

- **`Database Logging`:** Enables or disables supported database logging for execution output.
- **`DB Connections`:** Selects the target connection for local SQL/PSQL files.
- **`Current Schema`:** Selects the target schema for execution.
- **`Current Session`:** Selects the target session when session selection is allowed.
- **`Commit` / `Rollback`:** Commits or rolls back the selected connection/session transaction.
- **`Execute Statement`:** Executes the statement at the caret. Default shortcut: `Ctrl+Enter`.
- **`Debug Statement`:** Starts statement debugging where supported.
- **`Explain Plan`:** Generates an explain plan for the statement. Default shortcut: `Ctrl+Shift+Enter`.
- **`Execute as Script`:** Executes the current file through a configured command-line database client.
- **`Options`:** Opens console-specific actions, such as rename, delete, save to file, create console, and Code Editor settings.
- **`Settings`:** Opens Code Completion settings for SQL/PSQL files.

Program and Java editors use the source editor toolbar instead.

- **`Save`:** Saves modified source back to the database.
- **`Revert Changes`:** Reloads the source from the database after confirmation.
- **`Reload`:** Reloads source when the editor is not modified.
- **`Compile`:** Compiles the current source object or source section when supported.
- **`Compare with Original`:** Compares local changes with the source loaded when the editor opened.
- **`Compare with Database`:** Compares local source with the current database version.
- **`Execute Method` / `Debug Method`:** Executes or debugs the current method, or a selected child method for program and Java class editors.
- **`DDL Files`:** Creates, attaches, detaches, or configures DDL files for the source object.
- **`Settings`:** Opens Code Editor settings.

## SQL Consoles

SQL Consoles are DBN virtual files owned by a connection. They are useful for exploratory work, short SQL snippets, and connection-specific database tasks.

### Console Types

- **`SQL Console`:** Standard ad hoc SQL console for a connection.
- **`Debug Console`:** Console used for debugging workflows where supported.
- **`Search Console`:** Console used for vector similarity search where vector search is supported.

Each connection has a default SQL Console. Additional consoles can be created and named from the console picker or console options.

### Console Management

- **`Create Console`:** Creates another console for the same connection and console type.
- **`Rename Console`:** Changes the console display name. Names must be unique within the connection.
- **`Delete Console`:** Removes the console from the connection.
- **`Save to File`:** Saves standard or debug console text to a local `.sql` file and applies the console's connection context to the new file.
- **Temporary console close action:** When closing a temporary console, DBN can ask whether to save or discard it.

Console content and connection metadata are persisted in the project state.

## SQL Script Files

SQL Script Files are local project files recognized by DBN SQL or PSQL file types. They keep normal IDE file behavior while adding DBN database context and execution actions.

### Supported File Types

| DBN file type | Default extensions | Purpose |
| --- | --- | --- |
| `DBN-SQL` | `.sql`, `.ddl`, `.vw` | SQL scripts and SQL DDL files. |
| `DBN-PSQL` | `.psql`, `.plsql`, `.trg`, `.prc`, `.fnc`, `.pkg`, `.pks`, `.pkb`, `.tpe`, `.tps`, `.tpb` | Procedural SQL and DDL files for program-style database objects. |

DDL File settings can add or change object-specific DDL extensions.

### Execution Modes

- **`Execute Statement`:** Executes the statement at the caret through DBN statement execution.
- **`Debug Statement`:** Starts statement debugging where supported by the database and connection.
- **`Explain Plan`:** Runs explain-plan workflow for the current statement.
- **`Execute as Script`:** Saves the editor document and executes the whole file with a configured command-line database client.

Script execution uses a database client executable, such as `sqlplus`, `mysql`, `psql`, or `sqlite3`, configured in Execution Engine settings.

## DDL File Editors

DDL File Editors are local project files attached to database objects. They are intended for source tracking and review, not as the primary source of truth when synchronization is enabled.

### What DDL Files Do

- **Lookup:** DBN can search project scope for DDL files matching a database object.
- **Create:** DBN can create a new DDL file for a database object in a selected project folder.
- **Attach:** DBN maps a project file to a database object.
- **Detach:** DBN removes the mapping and keeps the project file.
- **Synchronize:** When enabled, saving database source updates attached DDL files with generated DDL.

When a DDL file is attached and synchronization is enabled, DBN shows a warning that changes to the database object can overwrite edits made directly in the DDL file.

### Default DDL File Mappings

- **View:** `vw`
- **Trigger:** `trg`
- **Procedure:** `prc`
- **Function:** `fnc`
- **Package:** `pkg`
- **Package spec:** `pks`
- **Package body:** `pkb`
- **Type:** `tpe`
- **Type spec:** `tps`
- **Type body:** `tpb`
- **Java source:** `sql`

## Program Editors

Program Editors load source directly from database objects. They are used for editing, compiling, executing, debugging, and comparing database program source.

### Supported Program Source

Program Editors are available for source-backed objects supported by the database and DBN, including:

- **Standalone programs:** Procedures and functions.
- **Program containers:** Packages and package bodies.
- **Object types:** Type specs, type bodies, type functions, and type procedures.
- **Other source-backed objects:** Triggers, views, and related database source objects where supported.

For objects with multiple source sections, such as packages and types, DBN opens separate editor tabs for spec and body content.

### Source State

- **Loading:** The source toolbar shows loading state while DBN retrieves source from the database.
- **Modified:** `Save`, `Revert Changes`, and `Compare with Original` become available.
- **Outdated:** DBN can notify you when the database version changed after the editor was opened.
- **Read-only:** DBN can make source read-only based on environment settings.
- **Load error:** DBN shows a notification when source cannot be loaded.

## Java Editors

Java Editors are source editors for database Java classes and Java resources. They use the DBN source editor toolbar and participate in database Java execution and debugging workflows where supported.

- **Source editing:** Opens database Java source from the Database Browser.
- **Save:** Saves Java source changes back to the database through DBN's Java-code operation flow.
- **Execute method:** Lists executable Java methods, including recently executed methods when available.
- **Debug method:** Starts Java method debugging where supported.
- **DDL files:** Java source can participate in DDL file attachment and synchronization.

Java method execution is focused on executable database Java methods, typically static methods exposed by the database Java class metadata.

## Common Workflows

### Execute A Statement From A SQL File

1. Open a DBN SQL or PSQL file.
2. Select the target connection, schema, and session in the editor toolbar.
3. Place the caret inside the statement.
4. Click `Execute Statement` or press `Ctrl+Enter`.
5. Review results and messages in the execution output.

### Run A SQL Script File

1. Open a DBN SQL or PSQL file.
2. Click `Execute as Script`.
3. Select the target connection, schema, command-line interface, and timeout.
4. Choose whether to clear previous output.
5. Start execution and review the script output.

### Save And Compile Program Source

1. Open a source-backed database object from the Database Browser.
2. Edit the source.
3. Click `Save` to write changes to the database.
4. Click `Compile` when compilation is available and needed.
5. Review compiler messages and object status.

### Attach A DDL File To A Program

1. Open the program in a Program Editor.
2. Open the source editor toolbar `DDL Files` menu.
3. Choose `Create New`, `Attach Files`, or `Detach Files`.
4. Review synchronization behavior before editing attached DDL files directly.

### Execute Or Debug A Program Method

1. Open a method, package, type, or Java class editor.
2. Click `Execute Method` or `Debug Method`.
3. Select a method when DBN shows a method list.
4. Enter parameter values and execution options when prompted.
5. Review results in the execution output.

## Troubleshooting

| Symptom | Resolution |
| --- | --- |
| SQL file toolbar has no connection | Associate the file or folder with a connection, or select a connection from the toolbar. |
| `Execute as Script` cannot start | Configure a command-line interface for the database type in Execution Engine settings. |
| Statement execution is disabled | Confirm the file is a DBN SQL/PSQL file and the caret is in executable SQL. |
| DDL file changes are overwritten | Attached DDL files are synchronized from database source when synchronization is enabled. Detach the file or disable synchronization if you want to edit it independently. |
| Compile action is hidden | The object may not be compilable, the database may not support invalidation, or the current editor is not a source-backed program. |
| Save is disabled | The source is unchanged, saving is already in progress, or the environment is read-only for code. |
| Debug action is hidden | The selected database, object, or connection may not support debugging. |

## Related Documentation

- [Code Editors Settings](./code-editors-settings.md): Settings reference for code editors, completion, execution, DDL files, and compiler behavior.
- [Environment Types](./environment-types.md): Guide to read-only code safeguards and environment color cues used by source editors.
- [Workspace Integration](./workspace-integration.md): Guide to DDL File Management and file-to-connection mappings used by local SQL and PSQL files.
- [Transaction Management](./transaction-management.md): Guide to editor commit, rollback, auto-commit, and session selection workflows.
- [Debugging Engine](./debugging-engine.md): Guide to debugging statements, database methods, and database Java methods from editors.
- [Database Assistant: Oracle Select AI](./database-assistant-select-ai.md): Guide to Select AI prompt actions from SQL and PSQL editor context.
- [Database Assistant: Generic Assistant](./database-assistant-generic.md): Guide to generic assistant workflows that use SQL consoles and source editors.
- [Database Assistant](./database-assistant.md): Overview of the two assistant paths.
- [Vector Toolbox](./vector-toolbox.md): Guide to Search Consoles used for vector similarity search.
- [Execution Engine](./execution-engine.md): Guide to statement, script, method, and Java execution workflows.
- [Connection Management](./connection-management.md): Guide to creating and managing the connections used by code editors.
- [Database Browser](./database-browser.md): Guide to opening database objects and consoles from the Database Browser.
