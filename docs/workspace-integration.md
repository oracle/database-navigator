# Workspace Integration

## Summary

Workspace Integration connects DBN database context with local project files. It covers DDL File Management for attaching project files to database objects, and Connection File Mapping for assigning project folders and files to a database connection, schema, and session.

- **DDL File Management:** Links local DDL files to database objects so generated source can be reviewed, tracked, and synchronized in the IDE project.
- **Connection File Mapping:** Associates local files or directories with a DBN connection, current schema, and current session.
- **Editor context:** Supplies SQL and PSQL files with the target database context used by execution, completion, and toolbar actions.
- **Workspace state:** Persists DDL file attachments and file connection mappings in project state.

> **Note:** Workspace Integration depends on project files, connection availability, database object type, DBN file type recognition, and connection-level settings. A local SQL file can use an explicit editor toolbar selection, an inherited directory mapping, or an attached DDL object context.

## When To Use It

Use Workspace Integration when you need to:

- Keep generated DDL files beside the rest of your project source.
- Attach an existing project DDL file to a database object.
- Create a new DDL file from a database object and populate it from the object source.
- Route SQL or PSQL files to the correct DBN connection and schema.
- Assign an entire project directory to a database environment.
- Review or change all file-to-connection mappings in one place.
- Detach a file from a database object without deleting the local file.

## Access Paths

- **DDL Files menu:** Program Editor or Java Editor toolbar -> `DDL Files`
- **Create DDL file:** Source editor toolbar -> `DDL Files` -> `Create New...`
- **Attach DDL files:** Source editor toolbar -> `DDL Files` -> `Attach Files`
- **Detach DDL files:** Source editor toolbar -> `DDL Files` -> `Detach Files`
- **DDL file settings:** Source editor toolbar -> `DDL Files` -> `DDL File Settings...`, or `DB Navigator` -> `Settings` -> `DDL Files`
- **File connection mappings:** `DB Navigator` -> `File Connection Mappings`
- **Folder connection mapping:** Project view folder context menu -> `Associate Connection...`
- **Folder schema mapping:** Project view folder context menu -> `Associate Schema...`
- **Remove folder mapping:** Project view folder context menu -> `Remove Connection Association`
- **Editor connection selector:** SQL/PSQL file toolbar -> `DB Connections`
- **Editor schema selector:** SQL/PSQL file toolbar -> `Current Schema`
- **Editor session selector:** SQL/PSQL file toolbar -> `Current Session`

## Workspace Context Model

DBN resolves database context for a file from several sources. Local project files usually receive context from file mappings or toolbar selections. Attached DDL files can inherit context from the linked database object while that database object is open.

| Context source | Applies to | Behavior |
| --- | --- | --- |
| Attached DDL object | Local DDL files linked to a database object | Uses the object's connection and schema when the object editor is open. |
| File or folder mapping | Local project files and directories | Uses the persisted connection, schema, and session assigned to the file or nearest mapped parent directory. |
| Editor toolbar selection | SQL and PSQL files | Lets users choose connection, schema, and session directly from the editor toolbar. |
| Database virtual file | SQL consoles and DBN object editors | Uses the owning connection and object context supplied by DBN. |

When a folder is mapped, files beneath it can inherit the folder context. A more specific file or subfolder mapping can override the inherited context.

## DDL File Management

DDL File Management links local files in the project to source-backed database objects. The linked file remains a normal project file, but DBN can attach it to an object, generate DDL into it, and reopen object editors when the attachment changes.

### Supported DDL Operations

- **Lookup:** Searches the project for DDL files whose name and extension match the database object.
- **Create:** Creates a new DDL file in a selected project folder and attaches it to the object.
- **Attach:** Links one or more existing project files to the selected database object.
- **Detach:** Removes the attachment while keeping the local file in the project.
- **Synchronize:** Updates attached DDL file content from the database object source when synchronization is enabled.
- **Configure:** Opens DDL Files settings for lookup, creation prompts, synchronization, and file extensions.

### File Lookup

DDL lookup searches project scope for files matching the adjusted database object name and the configured DDL extension. Object names are normalized to lower case for generated DDL file names. Java class object names use dot-separated file names instead of slash-separated database names.

- **File name pattern:** DBN searches for files containing the adjusted object file name and ending with the configured extension.
- **Multiple extensions:** If an object type has multiple configured DDL extensions, DBN can ask which DDL file type to use.
- **Existing attachments:** Already attached files are excluded when DBN looks for additional files to attach.
- **Missing association:** If no DDL file type is configured for the object type, DBN offers to open DDL File settings.

### Default DDL Extensions

| Database object | Default extension | DBN language file type |
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

DDL extension settings can add or change object-specific extensions. Extension values must be unique across DDL file types.

### Synchronization Behavior

When DDL synchronization is enabled, DBN writes generated DDL into attached files after the related database source is loaded or saved. This makes DDL files useful for review and source tracking, but it also means direct edits in the attached file can be overwritten.

- **Create DDL file from object:** DBN creates the file, attaches it, and writes generated DDL from the object source.
- **Save source in a Program Editor:** DBN updates attached DDL files when synchronization is enabled.
- **Load source after initial open:** DBN can update attached DDL files when the source is refreshed.
- **Detach DDL file:** DBN removes the object attachment and can preserve the last known connection/schema context for the file.
- **Rename, move, or delete attached file:** DBN updates attachment state and can reopen the related object editor when needed.

## Connection File Mapping

Connection File Mapping assigns database context to files and directories in the IDE project. It is especially useful for SQL and PSQL files that live in the project instead of DBN virtual consoles.

### What A Mapping Stores

- **File:** The mapped file or directory path.
- **Connection:** The DBN connection assigned to the file or directory.
- **Schema:** The current schema used for execution and object resolution.
- **Session:** The DBN session used by execution and transaction actions.
- **Environment:** The environment type inherited from the mapped connection.

Mappings are persisted in project state. When a connection is removed, DBN removes mappings that point to that connection.

### Folder Mapping

Folder mapping is the fastest way to connect a group of scripts to the same database environment.

- **`Associate Connection...`:** Assigns or changes the DBN connection for a local project directory.
- **`Associate Schema...`:** Assigns or changes the schema for a directory that already has a live connection.
- **`Remove Connection Association`:** Removes the mapping from the selected directory.

Files under a mapped folder inherit the folder connection context. If a file needs a different database target, select a different connection or schema in that file's editor toolbar.

### File Connection Mappings Dialog

The File Connection Mappings dialog gives a project-wide view of mappings.

- **File column:** Shows the mapped file or directory path. Double-clicking a file entry opens it in the editor.
- **Connection column:** Opens a connection selector for the selected mapping. The selector includes configured and virtual connections.
- **Schema column:** Opens a schema selector when the mapped connection is live.
- **Session column:** Opens a session selector for main, pool, and custom sessions on the mapped connection.
- **Environment column:** Shows the mapped connection's environment type and color.

Use this dialog to audit mappings, correct wrong database targets, or find inherited context that affects opened files.

## Common Workflows

### Create A DDL File For A Database Object

1. Open a source-backed object from the Database Browser.
2. Open the source editor toolbar `DDL Files` menu.
3. Choose `Create New...`.
4. Select a project folder for the new file.
5. Review the generated DDL file content after DBN attaches and synchronizes it.

### Attach An Existing DDL File

1. Open the database object in a Program Editor or Java Editor.
2. Open `DDL Files` -> `Attach Files`.
3. Review the matching files found in the project.
4. Select the files to attach.
5. Confirm synchronization behavior before editing the attached files directly.

### Map A Folder To A Connection

1. Open the IDE Project view.
2. Right-click a local folder containing SQL or PSQL files.
3. Choose `Associate Connection...`.
4. Select the target DBN connection.
5. Choose `Associate Schema...` if the folder should use a specific schema.

### Use A Mapped SQL File

1. Open a SQL or PSQL file under a mapped folder.
2. Confirm the editor toolbar shows the expected connection, schema, and session.
3. Run a statement, debug a statement, or execute the file as a script.
4. Change the toolbar connection or schema if this file needs a more specific target.

### Review All File Mappings

1. Open `DB Navigator` -> `File Connection Mappings`.
2. Sort or scan the mapping table by file, connection, schema, session, or environment.
3. Click a connection, schema, or session cell to change the selected mapping.
4. Double-click a file cell to open the mapped file.

## Troubleshooting

| Symptom | Resolution |
| --- | --- |
| SQL file has no target connection | Map the file or a parent folder, or select a connection from the editor toolbar. |
| File inherits the wrong connection | Check the nearest mapped parent folder and any file-specific mapping in File Connection Mappings. |
| Schema selector is empty | Confirm the selected connection is live and can load schemas. |
| Session selector is hidden | Confirm session management is enabled for the connection. |
| DDL file was overwritten | DDL synchronization is enabled. Detach the file or disable synchronization before editing it independently. |
| DDL lookup finds no files | Check DDL lookup settings, the connection's project DDL lookup option, object name, project scope, and configured file extensions. |
| DDL file cannot be attached | Confirm the file extension is configured for the object type. |
| Connection mapping disappears | Check whether the mapped connection was removed from the project. |

## Related Documentation

- [Workspace Integration Settings](./workspace-integration-settings.md): Settings reference for DDL files, file connection mappings, and workspace context behavior.
- [Environment Types](./environment-types.md): Guide to the environment classification shown for mapped connections.
- [Code Editors](./code-editors.md): Guide to SQL files, DDL File Editors, Program Editors, and Java Editors.
- [Connection Management](./connection-management.md): Guide to creating and configuring the connections used by workspace mappings.
- [Execution Engine](./execution-engine.md): Guide to statement and script execution from mapped SQL and PSQL files.
- [Database Browser](./database-browser.md): Guide to opening source-backed objects that can create or attach DDL files.
