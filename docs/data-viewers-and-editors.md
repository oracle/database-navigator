# Data Viewers and Editors

## Summary

Data Viewers and Editors are the DBN surfaces for inspecting, filtering, editing, and exporting row data. They include editable dataset tabs for tables and views, JSON-focused editors for JSON views, and read-only result viewers for SQL and program execution results.

- **Table Editors:** Editable data grids for tables, views, and materialized views where data editing is supported.
- **JSON Editors:** JSON data editors for JSON views, with a result grid and optional JSON content editor.
- **Data Viewers:** Read-only grids and record viewers for SQL result sets, method cursor results, and Java cursor results.

> **Note:** Available actions depend on database type, object type, connection permissions, environment settings, transaction state, and whether the database supports updatable result sets.

## When To Use It

Use Data Viewers and Editors when you need to:

- Inspect table, view, materialized view, or JSON view data.
- Edit rows directly in a supported dataset.
- Insert, duplicate, or delete table records.
- Filter and sort large datasets before loading more rows.
- Open a record viewer for one-row-at-a-time inspection.
- Review SQL statement results without editing the source data.
- Export data from an editor or result grid.

## Access Paths

- **Table Editor:** Open a table, view, or materialized view from the Database Browser and choose the `Data` editor target.
- **JSON Editor:** Open a JSON view from the Database Browser and choose the JSON data editor target.
- **SQL Result Viewer:** Execute a SQL statement that returns a result set.
- **Method Cursor Viewer:** Execute a database method that returns a cursor result.
- **Java Cursor Viewer:** Execute a database Java method that returns a cursor result.
- **Record Viewer:** Use `View Record`, `Edit Record`, or the configured record navigation target from a grid.
- **Settings:** `DB Navigator` -> `Settings` -> `Data Editor` or `Data Grid`.

## Editor and Viewer Types

| Type | Backing data | Editing | Common actions |
| --- | --- | --- | --- |
| Table Editor | Table, view, or materialized view result set | Supported when the object, database, environment, and result set allow it | Find, fetch next records, reload, filter, sort, edit records, import, export, commit, rollback |
| JSON Editor | JSON view result set | Supported where DBN and the database expose writable JSON data | Find, fetch next records, reload, show content editor, lock or unlock editing, commit, rollback |
| SQL Result Viewer | Result set returned by statement execution | Read-only | View statement, view record, fetch next records, rerun, find, export, close |
| Cursor Result Viewer | Cursor returned by method or Java execution | Read-only | Find, fetch next records, view record, export |
| Record Viewer | Selected row from a grid | Read-only for result viewers; record editor for table editors when configured | Filter columns, sort columns, move between records |

## Table Editors

Table Editors are data-grid tabs opened for database datasets. A table opens directly to data, while views and materialized views can offer both code and data targets depending on database support and default editor settings.

### Toolbar Actions

- **`Lock / Unlock Editing`:** Toggles the editor's local editing lock.
- **`Find`:** Opens search for data visible in the grid.
- **`Edit Record`:** Opens the selected row in a record editor.
- **`Fetch Next Records`:** Loads another block of records from the result set.
- **`Reload`:** Reloads the dataset from the database.
- **`Select Filter`:** Switches between configured filters for the dataset.
- **`Create / Edit Filter`:** Opens filter configuration for the dataset.
- **`Duplicate Record`:** Copies the selected record into a new row. Default shortcut: `Ctrl+D`.
- **`Insert Record`:** Inserts a new row. Default shortcut: `Shift+Insert`.
- **`Delete Record`:** Marks selected records for deletion. Default shortcut: `Shift+Delete`.
- **`Commit` / `Rollback`:** Commits or rolls back pending changes on the active connection/session.
- **`Export`:** Exports grid data.
- **`Import`:** Imports data when supported by the database type.
- **`Options`:** Opens data sorting, column setup, and Data Editor settings.

### Editing Behavior

DBN tracks modified, inserted, and deleted rows in the grid until changes are committed, rolled back, or reloaded. A dataset can still be read-only if:

- The database object does not support data editing.
- The database connection cannot provide an updatable result set.
- The environment is configured as read-only for data.
- The editor is locally locked.
- The connection is disconnected or the main session is unavailable.

When an environment is read-only for data, DBN can show a notification explaining that the editor is protected from accidental changes.

### Loading And Fetching

Table Editors load data in blocks. The initial load and each `Fetch Next Records` operation use Data Editor fetch settings. If a query takes too long, DBN can cancel the load and report a timeout.

- **Loading:** DBN is retrieving rows from the database.
- **Loaded:** The current block of data is available in the grid.
- **Dirty:** Loading was canceled or changes are pending.
- **Modified:** One or more rows have uncommitted edits.
- **Disconnected:** The connection is unavailable; editing is disabled.

### Filters

Filters control which rows are loaded into a Table Editor.

- **No filter:** Load the dataset without a row predicate.
- **Basic filter:** Build simple column conditions from the UI.
- **Custom filter:** Enter a custom filter expression for advanced filtering.

Grid context actions can create filters from a column, from the current cell value, or from clipboard content when the value is suitable.

### Sorting And Columns

Table Editors support per-dataset sorting and column setup.

- **`Data Sorting`:** Adds one or more sorting columns and directions.
- **Column header sort:** Sorts a sortable column from the grid.
- **`Column Setup`:** Shows, hides, and reorders columns for the editor.
- **`Hide Column`:** Removes one column from the current grid view.

The editor state preserves row count, sorting, column setup, readonly state, and selected content types with the editor state.

## JSON Editors

JSON Editors are data editors for JSON views. They show JSON records in a grid and can also display an adjacent JSON content editor for the selected record.

### Toolbar Actions

- **`Lock / Unlock Editing`:** Toggles local editing protection for JSON data.
- **`Show / Hide Content Editor`:** Shows or hides the JSON content editor panel.
- **`Find`:** Searches visible JSON data.
- **`Fetch Next Records`:** Loads more JSON records.
- **`Reload`:** Reloads JSON data from the database.
- **`Commit` / `Rollback`:** Commits or rolls back pending JSON changes.
- **`Settings`:** Opens Data Editor settings.

### Content Editor

The JSON content editor follows the selected JSON record. Use it when a JSON value is easier to read or edit as structured text than as a single grid cell.

The content editor participates in the same read-only checks as the JSON grid. If the JSON view is read-only, the environment is protected, or the editor is locked, DBN prevents accidental edits.

## Data Viewers

Data Viewers are read-only grids used for execution results. They use the same grid component family as Table Editors, but they do not modify source table data.

### SQL Result Viewers

SQL Result Viewers appear when statement execution returns a result set.

- **`Close`:** Closes the result tab.
- **`View Statement`:** Shows the SQL statement that produced the result.
- **`View Record`:** Opens the selected row in a record viewer.
- **`Open Variables Dialog`:** Reopens execution variables for rerunning the statement.
- **`Fetch Next Records`:** Loads the next result block.
- **`Rerun Statement`:** Runs the same statement again.
- **`Find Data`:** Searches the visible result grid.
- **`Export Data`:** Exports result data.
- **`Settings`:** Opens execution settings related to the result.

### Method And Java Cursor Viewers

Database method and Java method executions can return cursor results. Cursor viewers provide a compact toolbar focused on result inspection.

- **`Find Data`:** Searches the cursor grid.
- **`Fetch Next Records`:** Loads more rows from the cursor.
- **`View Record`:** Opens the selected row in a record viewer.
- **`Export`:** Exports cursor data.

### Record Viewer

The Record Viewer displays one row as a vertical list of columns and values. It is useful when a row has many columns or wide values.

- **Column filter:** Narrows the visible column list by name.
- **Sort columns alphabetically:** Switches between source column order and alphabetical order.
- **First / Previous / Next / Last:** Moves through rows in the source grid.
- **Data types:** Can be shown when the source viewer provides data type metadata.

## Common Workflows

### Open A Table Editor

1. Open `DB Browser`.
2. Expand a connection and schema.
3. Expand `Tables`, `Views`, or `Materialized Views`.
4. Open the object and choose the `Data` editor target.
5. Review the loaded rows in the grid.

### Edit And Commit Rows

1. Open a Table Editor.
2. Unlock editing if the editor is locked.
3. Edit cells, insert a row, duplicate a row, or delete selected rows.
4. Review modified row indicators in the grid.
5. Click `Commit` to save changes or `Rollback` to discard them.

### Filter A Dataset

1. Open a Table Editor.
2. Click `Create / Edit Filter`.
3. Choose a basic or custom filter.
4. Apply the filter and reload the data.
5. Use `Select Filter` to switch filters later.

### Inspect A JSON View

1. Open a JSON view from the Database Browser.
2. Select the JSON data editor.
3. Use `Show Content Editor` to inspect the selected JSON record as structured text.
4. Use `Find`, `Fetch Next Records`, or `Reload` as needed.

### View SQL Results

1. Execute a SQL statement that returns rows.
2. Review the result grid.
3. Use `Fetch Next Records` for more rows.
4. Use `View Record` for wide rows or `Export Data` to save the result.

## Troubleshooting

| Symptom | Resolution |
| --- | --- |
| Table opens read-only | Check object editability, environment read-only settings, connection permissions, and database support for updatable result sets. |
| Edits do not reach the database | Commit the active connection/session and check for validation or database errors. |
| Rows load slowly | Reduce the fetch block size or add a filter before loading more records. |
| Loading times out | Increase the Data Editor fetch timeout, or use a narrower filter. |
| Expected rows are missing | Check active dataset filters and whether more rows need to be fetched. |
| JSON content editor is hidden | Use `Show Content Editor` in the JSON Editor toolbar. |
| Record navigation opens the wrong target | Check the Record Navigation target in Data Editor settings. |
| Result grid is read-only | Execution result viewers are read-only by design; open the source table or view in a Table Editor to edit data. |

## Related Documentation

- [Data Viewers and Editors Settings](./data-viewers-and-editors-settings.md): Settings reference for Data Editor, Data Grid, record navigation, filters, popups, and grid behavior.
- [Environment Types](./environment-types.md): Guide to read-only data safeguards that affect Table Editors and JSON Editors.
- [Transaction Management](./transaction-management.md): Guide to committing, rolling back, and reviewing pending data changes.
- [Database Browser](./database-browser.md): Guide to opening table, view, materialized view, and JSON view data from the browser.
- [Code Editors](./code-editors.md): Guide to running SQL and opening result viewers from statement execution.
