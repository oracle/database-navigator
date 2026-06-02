# Code Editors Settings

## Summary

Code Editors settings control DBN source editor behavior, code completion, execution defaults, command-line script clients, DDL file synchronization, and compiler behavior for database source objects.

| Setting area | Scope | Purpose |
| --- | --- | --- |
| Code Editor | Project | Controls source editor gutters, spellchecking, and close/save confirmations. |
| Code Completion | Project | Controls completion filters, sorting, and code-style casing. |
| Execution Engine | Project | Controls statement, script, method, and Java execution defaults. |
| DDL Files | Project | Controls DDL lookup, creation, synchronization, and object-specific extensions. |
| Operations - Compiler | Project | Controls compile type, dependency compilation, and compiler controls visibility. |

> **Note:** Some execution choices are selected per run from the execution dialog. Project settings provide defaults and validation rules; the active connection, schema, session, and database capabilities still determine what can run.

## Contents

<!-- TOC -->
- [Access Paths](#access-paths)
- [Code Editor Settings](#code-editor-settings)
  - [Confirmations](#confirmations)
- [Code Completion Settings](#code-completion-settings)
  - [Format](#format)
  - [Filters](#filters)
  - [Sorting](#sorting)
- [Execution Engine Settings](#execution-engine-settings)
  - [Statement Execution](#statement-execution)
  - [Script Execution](#script-execution)
  - [Method Execution](#method-execution)
  - [Java Execution](#java-execution)
  - [Per-Run Execution Options](#per-run-execution-options)
- [DDL Files Settings](#ddl-files-settings)
  - [Extensions](#extensions)
- [Compiler Settings](#compiler-settings)
- [Validation Messages](#validation-messages)
- [Related Documentation](#related-documentation)
<!-- /TOC -->

## Access Paths

- **Code Editor:** `DB Navigator` -> `Settings` -> `Code Editor`
- **Code Completion:** `DB Navigator` -> `Settings` -> `Code Completion`
- **Execution Engine:** `DB Navigator` -> `Settings` -> `Execution Engine`
- **DDL Files:** `DB Navigator` -> `Settings` -> `DDL Files`
- **Compiler settings:** `DB Navigator` -> `Settings` -> `Operations` -> `Compiler`
- **SQL/PSQL editor settings:** Editor toolbar -> `Settings`
- **SQL Console options:** SQL Console toolbar -> `Options`

## Code Editor Settings

Code Editor settings apply mainly to DBN source editors, including Program Editors and Java Editors. Some confirmation behavior also applies to SQL Consoles.

### General

| Setting | Default | Description |
| --- | --- | --- |
| `Show spec / declaration navigation gutter` | Enabled | Shows gutter navigation between related declarations and source sections. |
| `Show object navigation gutter` | Disabled | Shows gutter navigation for database objects referenced from source. |
| `Enable spellchecking` | Enabled | Enables spellchecking in DBN code editors. |
| `Include reference identifiers` | Disabled | Includes database reference identifiers in spellchecking when spellchecking is enabled. |

### Confirmations

| Setting | Default | Options | Description |
| --- | --- | --- | --- |
| `Confirm "Save" changes` | Disabled | Enabled/disabled | Asks before saving source changes to the database. |
| `Confirm "Revert" changes` | Enabled | Enabled/disabled | Asks before discarding local source changes and reloading from the database. |
| `Close editor with changes action` | `Ask` | `Ask`, `Save`, `Discard` | Controls what DBN does when a modified source editor is closed. |
| `Close temporary console action` | `Ask` | `Ask`, `Save`, `Discard` | Controls what DBN does when a temporary SQL Console is closed. |

## Code Completion Settings

Code Completion settings apply to DBN SQL and PSQL language files, SQL Consoles, Program Editors, Java-related database source where DBN language support applies, and DDL files using DBN file types.

### Format

| Setting | Default | Description |
| --- | --- | --- |
| `Enforce code style case` | Enabled | Applies configured code-style case to inserted completion items. |

### Filters

Completion filters define what appears in basic and extended completion lists.

- **`Basic`:** Used by standard code completion.
- **`Extended`:** Used by smart or extended completion.
- **`Root elements`:** Reserved words and top-level database objects, such as schemas, users, roles, and privileges.
- **`User schema`:** Objects in the current schema.
- **`Public schema`:** Objects from public schema scope.
- **`Any schema`:** Objects from other schemas.

Filterable items include reserved word categories and database object types such as tables, views, indexes, constraints, triggers, sequences, procedures, functions, packages, types, Java classes, Java resources, credentials, AI profiles, and AI models.

### Sorting

| Setting | Default | Description |
| --- | --- | --- |
| `Enable sorting by type` | Enabled | Uses the configured type order to sort completion suggestions. |
| Sorting order | Preconfigured | Move items up or down to prioritize reserved words and object types in completion results. |

The default sorting order starts with keywords and data types, then common object types such as columns, tables, views, indexes, constraints, triggers, synonyms, sequences, procedures, functions, packages, types, schemas, roles, users, and parameters.

## Execution Engine Settings

Execution Engine settings control defaults for running SQL statements, SQL scripts, database methods, and database Java methods.

### Statement Execution

| Setting | Default | Valid range | Description |
| --- | --- | --- | --- |
| `Execution timeout` | `20` seconds | `0` to `6000` | Maximum time for normal statement execution. `0` means no timeout. |
| `Debug execution timeout` | `600` seconds | `0` to `6000` | Maximum time for statement debugging. `0` means no timeout. |
| `Result-Set fetch block size` | `100` records | `1` to `10000` | Number of records fetched per block for statement result sets. |
| `Prompt execution dialog` | Disabled | Enabled/disabled | Shows the execution dialog before running statements. |
| `Focus result after execution` | Disabled | Enabled/disabled | Moves focus to the execution result after execution. |

### Script Execution

| Setting | Default | Valid range | Description |
| --- | --- | --- | --- |
| `Execution timeout` | `300` seconds | `0` to `6000` | Maximum time for SQL script execution. `0` means no timeout. |
| `Command line interfaces` | No custom entries | Named clients | Defines custom database client executables used by `Execute as Script`. Built-in defaults are also offered at execution time. |

Command-line interfaces are configured with:

- **`Database Type`:** Native database type for the client executable.
- **`Name`:** Display name shown in script execution dialogs. Must be unique.
- **`Executable Path`:** Path to the database command-line client executable. Required.

Default executable names suggested by DBN include:

- **Oracle:** `sqlplus`
- **MySQL:** `mysql`
- **PostgreSQL:** `psql`
- **SQLite:** `sqlite3`

### Method Execution

| Setting | Default | Valid range | Description |
| --- | --- | --- | --- |
| `Execution timeout` | `30` seconds | `0` to `6000` | Maximum time for database method execution. `0` means no timeout. |
| `Debug execution timeout` | `600` seconds | `0` to `6000` | Maximum time for method debugging. `0` means no timeout. |
| `Parameter history size` | `10` | `0` to `3000` | Number of recent parameter values retained for method execution input. |

### Java Execution

| Setting | Default | Valid range | Description |
| --- | --- | --- | --- |
| `Execution timeout` | `30` seconds | `0` to `6000` | Maximum time for database Java method execution. `0` means no timeout. |
| `Debug execution timeout` | `600` seconds | `0` to `6000` | Maximum time for Java method debugging. `0` means no timeout. |
| `Parameter history size` | `10` | `0` to `3000` | Number of recent parameter values retained for Java execution input. |

### Per-Run Execution Options

Execution dialogs can show additional options for a specific run.

- **`Connection`:** Target connection. For local files, this comes from file context or editor selection.
- **`Target schema`:** Schema used for statement, method, or script execution.
- **`Target session`:** Session used for execution when session selection is allowed.
- **`Execution timeout`:** Timeout for this execution.
- **`Commit after execution`:** Commits automatically after execution when selected and auto-commit is not already active.
- **`Enable logging`:** Enables database logging for supported databases and execution types.
- **`Remember execution variables`:** Reuses variable values for supported statement or method executions.
- **`Clear output from previous executions`:** Script execution option that clears previous script output.

## DDL Files Settings

DDL Files settings control project files attached to database objects.

### General

| Setting | Default | Description |
| --- | --- | --- |
| `Lookup DDL files when database object is opened` | Enabled | Searches project scope for matching DDL files when a database object opens. |
| `Suggest creating DDL file if not found` | Disabled | Offers to create a DDL file when lookup finds no matching file. |
| `Synchronize DDL files` | Enabled | Updates attached DDL files when database object source is saved. |
| `Make DDL scripts rerunnable` | Enabled | Generates rerunnable DDL, such as `create or replace` or `drop if exists`, where supported. |
| `Use qualified names` | Disabled | Generates qualified names such as `schema.object` where supported. |

When `Synchronize DDL files` is enabled, `Make DDL scripts rerunnable` and `Use qualified names` become active. Synchronization overwrites attached DDL file content with generated DDL from the database object.

### Extensions

| Database object | Default extension | Language file type |
| --- | --- | --- |
| View | `vw` | DBN-SQL |
| Trigger | `trg` | DBN-PSQL |
| Procedure | `prc` | DBN-PSQL |
| Function | `fnc` | DBN-PSQL |
| Package | `pkg` | DBN-PSQL |
| Package spec | `pks` | DBN-PSQL |
| Package body | `pkb` | DBN-PSQL |
| Type | `tpe` | DBN-PSQL |
| Type spec | `tps` | DBN-PSQL |
| Type body | `tpb` | DBN-PSQL |
| Java source | `sql` | DBN-SQL |

Each extension field accepts a comma-separated list. Extension values must be unique across DDL file types.

## Compiler Settings

Compiler settings are located under `Operations`, but they affect Program Editors when compiling database source.

| Setting | Default | Options | Description |
| --- | --- | --- | --- |
| `Show compiler controls` | When invalid | `Always`, `When invalid` | Controls whether compile controls are always visible or only shown when DBN considers them needed. |
| `Compile type` | `Keep existing` | `Normal`, `Debug`, `Keep existing`, `Ask` | Selects normal compile, debug compile, preserving current debug state, or asking at compile time. |
| `Compile with dependencies` | `Ask` | `Yes`, `No`, `Ask` | Controls whether dependent objects are compiled with the selected object. |

## Validation Messages

| Message area | Common cause | Resolution |
| --- | --- | --- |
| Statement execution settings | Timeout or fetch size outside allowed range | Enter values within the ranges shown in Execution Engine settings. |
| Script command-line interfaces | Missing name, duplicate name, or missing executable path | Provide a unique name and a valid executable path for each client. |
| Command-line interface creation | Executable does not match the selected database type | Select a matching client executable or verify the executable path. |
| Method or Java execution settings | Timeout or parameter history outside allowed range | Enter values within the ranges shown in Execution Engine settings. |
| DDL extensions | Duplicate extension value | Make each extension unique across all DDL file types. |
| DDL synchronization | Local DDL changes disappear | Disable synchronization or detach the DDL file before editing it independently. |

## Related Documentation

- [Code Editors](./code-editors.md): Feature guide for SQL Consoles, SQL Script Files, DDL File Editors, Program Editors, and Java Editors.
- [Environment Types Settings](./environment-types-settings.md): Settings reference for read-only code safeguards and environment colors.
- [Workspace Integration Settings](./workspace-integration-settings.md): Settings reference for DDL file behavior and file connection mappings.
- [Transaction Management Settings](./transaction-management-settings.md): Settings reference for auto-commit, transaction prompts, and editor session behavior.
- [Execution Engine Settings](./execution-engine-settings.md): Dedicated settings reference for statement, script, method, and Java execution.
- [Connection Management](./connection-management.md): Guide to creating and managing the connections used by code editors.
- [Database Browser](./database-browser.md): Guide to opening database objects and consoles from the Database Browser.
