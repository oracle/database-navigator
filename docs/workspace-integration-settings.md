# Workspace Integration Settings

## Summary

Workspace Integration settings control how DBN connects local project files to database objects and database connections. The settings are split between DDL Files settings, connection-level DDL lookup, folder/file connection mappings, and editor-level context selectors.

| Setting area | Scope | Purpose |
| --- | --- | --- |
| DDL Files | Project | Controls DDL lookup, creation prompts, synchronization, and object-specific file extensions. |
| Connection Details | Connection | Enables or disables project DDL file lookup for a specific connection. |
| File Connection Mappings | Project | Stores file and folder mappings to connection, schema, and session context. |
| Editor context selectors | File | Allows local SQL and PSQL files to choose connection, schema, and session from the editor toolbar. |

> **Note:** File Connection Mappings is a project dialog rather than a traditional settings page. It still behaves as persisted project configuration because mappings are saved with the project state.

## Contents

<!-- TOC -->
- [Access Paths](#access-paths)
- [DDL Files Settings](#ddl-files-settings)
  - [General](#general)
  - [Extensions](#extensions)
- [Connection DDL Lookup](#connection-ddl-lookup)
- [File Connection Mappings](#file-connection-mappings)
  - [Mapping Table](#mapping-table)
  - [Mapping Inheritance](#mapping-inheritance)
- [Editor Context Selectors](#editor-context-selectors)
- [Validation and Prompts](#validation-and-prompts)
- [Troubleshooting](#troubleshooting)
- [Related Documentation](#related-documentation)
<!-- /TOC -->

## Access Paths

- **DDL Files settings:** `DB Navigator` -> `Settings` -> `DDL Files`
- **DDL Files settings from editor:** Program Editor or Java Editor toolbar -> `DDL Files` -> `DDL File Settings...`
- **Connection DDL lookup:** `DB Navigator` -> `Settings` -> `Connections` -> selected connection -> `Details`
- **File Connection Mappings dialog:** `DB Navigator` -> `File Connection Mappings`
- **Folder connection mapping:** Project view folder context menu -> `Associate Connection...`
- **Folder schema mapping:** Project view folder context menu -> `Associate Schema...`
- **Remove folder mapping:** Project view folder context menu -> `Remove Connection Association`
- **SQL file connection:** SQL/PSQL file toolbar -> `DB Connections`
- **SQL file schema:** SQL/PSQL file toolbar -> `Current Schema`
- **SQL file session:** SQL/PSQL file toolbar -> `Current Session`

## DDL Files Settings

DDL Files settings are project-level settings that control DDL attachment behavior and file type associations.

![DDL Files settings page](https://raw.githubusercontent.com/oracle/database-navigator/master/docs/img/settings-ddl-files.png)

### General

![DDL Files general settings](https://raw.githubusercontent.com/oracle/database-navigator/master/docs/img/settings-ddl-files-general.png)

| Setting | Default | Description |
| --- | --- | --- |
| `Lookup DDL files when database object is opened` | Enabled | Searches project scope for matching DDL files when a database object opens. |
| `Suggest creating DDL file if not found` | Disabled | Offers to create a DDL file when lookup finds no matching file. |
| `Synchronize DDL files` | Enabled | Updates attached DDL files with generated DDL when database source is loaded or saved. |
| `Make DDL scripts rerunnable` | Enabled | Generates rerunnable DDL where the database supports it. |
| `Use qualified names` | Disabled | Generates qualified object names where the database supports it. |

When `Synchronize DDL files` is enabled, DBN enables `Make DDL scripts rerunnable` and `Use qualified names`. Synchronization overwrites attached DDL file content with generated DDL from the linked database object.

### Extensions

![DDL Files extension settings](https://raw.githubusercontent.com/oracle/database-navigator/master/docs/img/settings-ddl-files-extensions.png)

| DDL file type | Default extension | Language file type | Content scope |
| --- | --- | --- | --- |
| View | `vw` | DBN-SQL | Code |
| Trigger | `trg` | DBN-PSQL | Code |
| Procedure | `prc` | DBN-PSQL | Code |
| Function | `fnc` | DBN-PSQL | Code |
| Package | `pkg` | DBN-PSQL | Spec and body |
| Package spec | `pks` | DBN-PSQL | Spec |
| Package body | `pkb` | DBN-PSQL | Body |
| Type | `tpe` | DBN-PSQL | Spec and body |
| Type spec | `tps` | DBN-PSQL | Spec |
| Type body | `tpb` | DBN-PSQL | Body |
| Java source | `sql` | DBN-SQL | Code |

Each extension field accepts a comma-separated list. Extension values must be unique across DDL file types. DBN registers configured extensions with the corresponding DBN language file type, and can restore those file associations if they are removed while still configured as DDL extensions.

## Connection DDL Lookup

Each connection has a detail setting that controls whether DBN can bind database objects from that connection to project DDL files.

| Setting | Default | Location | Description |
| --- | --- | --- | --- |
| `Enable project DDL file lookup` | Enabled | Connection `Details` tab | Allows DBN to look for and bind matching project DDL files when database objects are opened. |

This setting is useful when one connection should not prompt for project DDL file attachments. The attach dialog can also disable this option when the user chooses not to be prompted again for DDL lookup on that connection.

## File Connection Mappings

File Connection Mappings store database context for local files and directories.

- **File URL:** Persisted URL of the mapped file or directory.
- **Connection ID:** DBN connection selected for the file or directory.
- **Current schema:** Schema used for execution and object resolution.
- **Session ID:** DBN session used for execution and transaction actions.

Mappings can be created from folder context actions, editor toolbar selections, or DBN workflows that save a console to a file. Mappings can also be viewed and adjusted from the File Connection Mappings dialog.

### Mapping Table

- **`File`:** Local file or directory path. Double-click opens the mapped file when available.
- **`Connection`:** Selected DBN connection. Click or press Space to change it.
- **`Schema`:** Selected schema for live connections. Click or press Space to change it.
- **`Session`:** Selected DBN session. Click or press Space to change it.
- **`Environment`:** Environment type of the selected connection.

The mapping table is read and edited in place. It does not create new files or connections; it changes the database context associated with existing mappings.

### Mapping Inheritance

- **Parent directory inheritance:** A file can inherit database context from a mapped parent directory.
- **Specific override:** A more specific file or subdirectory mapping can override inherited context.
- **Redundant mapping cleanup:** DBN avoids keeping a child mapping when it is identical to the parent context.
- **Connection removal cleanup:** Mappings that reference a removed connection are cleaned up.

Inherited context can appear in editor notifications for non-SQL files, including an option to open mappings or remove the inherited association.

## Editor Context Selectors

Local SQL and PSQL files can choose database context directly from the editor toolbar.

| Selector | Availability | Behavior |
| --- | --- | --- |
| `DB Connections` | Local DBN SQL/PSQL files | Selects or clears the file connection. Hidden for SQL consoles. |
| `Current Schema` | Files with a live selected connection | Selects the schema used by execution and object resolution. |
| `Current Session` | Files with a live connection and session management enabled | Selects main, pool, custom, or other available DBN sessions. |

When a local file is an attached DDL file and the related database object is open, DBN can lock the connection and schema selectors to the attached object context.

## Validation and Prompts

- **No connection selected for execution:** DBN prompts for a target connection before continuing.
- **Selected connection is virtual:** DBN prompts for a real connection before statement execution.
- **No schema selected:** DBN asks whether to use the current schema or select a schema.
- **DDL file type is not configured:** DBN offers to open DDL File settings.
- **No DDL files found:** DBN can offer to create a new DDL file.
- **Attached DDL files exist:** DBN excludes already attached files when looking for additional matches.
- **Session deleted:** File mappings using that session fall back to the main session.

## Troubleshooting

| Symptom | Resolution |
| --- | --- |
| DDL lookup never prompts | Check `Lookup DDL files when database object is opened` and the connection's `Enable project DDL file lookup` setting. |
| DDL creation prompt never appears | Enable `Suggest creating DDL file if not found`. |
| DDL synchronization overwrites manual edits | Disable `Synchronize DDL files` or detach the file before editing it independently. |
| DDL file extension is not recognized | Add the extension to the correct DDL file type and ensure it is unique. |
| SQL file executes against the wrong database | Check editor toolbar context, file-specific mapping, and inherited folder mappings. |
| Folder schema action is unavailable | Associate a live connection with the folder before selecting a schema. |
| Session selector is not visible | Enable session management for the connection in Connection Details. |
| File mappings table is empty | Create a mapping from a folder context action or select a connection in a local SQL/PSQL file toolbar. |

## Related Documentation

- [Workspace Integration](./workspace-integration.md): Feature guide for DDL File Management and Connection File Mapping.
- [Environment Types Settings](./environment-types-settings.md): Settings reference for environment colors and read-only behavior on mapped connections.
- [Code Editors Settings](./code-editors-settings.md): Settings reference for code editors, DDL files, completion, execution, and compiler behavior.
- [Connection Management Settings](./connection-management-settings.md): Settings reference for connection details, session management, and project DDL lookup.
- [Execution Engine Settings](./execution-engine-settings.md): Settings reference for execution defaults that use file connection context.
- [Transaction Management Settings](./transaction-management-settings.md): Settings reference for session and transaction behavior used by mapped files.
