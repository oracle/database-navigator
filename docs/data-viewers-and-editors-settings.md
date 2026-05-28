# Data Viewers and Editors Settings

## Summary

Data Viewers and Editors settings control how DBN loads data, edits table and JSON records, displays grids, opens record views, handles large values, and applies dataset filters.

| Setting area | Scope | Purpose |
| --- | --- | --- |
| Data Editor | Project | Controls dataset loading, editing behavior, filters, large value editors, value-list popups, and record navigation. |
| Data Grid | Project | Controls grid behavior, sorting defaults, audit-column visibility, and audit-column editing. |
| Data Grid colors | IDE color scheme | Controls visual attributes for plain, modified, deleted, read-only, loading, key, selection, and caret-row data. |
| Execution Engine | Project | Controls SQL result viewer execution timeout and result-set fetch block size. |

> **Note:** Table Editors and JSON Editors use Data Editor and Data Grid settings. SQL result viewers also use Execution Engine settings for statement execution and result fetching.

## Access Paths

- **Data Editor:** `DB Navigator` -> `Settings` -> `Data Editor`
- **Data Grid:** `DB Navigator` -> `Settings` -> `Data Grid`
- **Data Editor from toolbar:** Table Editor toolbar -> `Options` -> `Settings`, or JSON Editor toolbar -> `Settings`
- **Result viewer settings:** SQL result viewer toolbar -> `Settings`
- **Data Grid colors:** IDE Settings -> `Editor` -> `Color Scheme` -> `Data Grid (DBN)`

## Data Editor Settings

Data Editor settings apply to Table Editors and JSON Editors. They also influence record viewers and large-value editing opened from data grids.

### General

| Setting | Default | Range | Description |
| --- | --- | --- | --- |
| `Fetch block size` | `100` records | `1` to `10000` | Number of records DBN loads per fetch operation. |
| `Fetch timeout` | `30` seconds | `0` to `300` | Maximum time for data loading. `0` means no timeout. |
| `Trim whitespaces` | Enabled | On or off | Trims whitespace from edited values before updating. |
| `Convert empty strings to null` | Enabled | On or off | Stores empty edited strings as `NULL`. |
| `Select content on cell edit` | Enabled | On or off | Selects the existing cell text when editing starts. |
| `Enable large value preview` | Enabled | On or off | Enables preview behavior for large cell values. |

### Basic Text Editor Popup

The Basic Text Editor popup is used for editing larger text values outside the inline grid cell editor.

| Setting | Default | Range | Description |
| --- | --- | --- | --- |
| `Pop up automatically` | Disabled | On or off | Opens the text editor popup automatically when configured thresholds are met. |
| `Pop up for empty value` | Disabled | On or off | Allows the popup to open for empty values when automatic popup is enabled. |
| `Length threshold to pop up` | `100` characters | `0` to `999999999` | Minimum value length that triggers automatic popup. |
| `Pop-up delay` | `1000` ms | `10` to `2000` | Delay before the popup opens. |

### Value List Popup

The Value List popup supports value selection for columns with list-like or discoverable values.

| Setting | Default | Range | Description |
| --- | --- | --- | --- |
| `Show popup button` | Enabled | On or off | Shows the value-list popup button in eligible cells. |
| `Element count threshold` | `1000` elements | `0` to `10000` | Maximum list size before DBN suppresses value-list behavior. |
| `Data length threshold` | `250` characters | `0` to `1000` | Maximum value length for value-list popup handling. |

### Filters

Filter settings control the default behavior when creating dataset filters from a Table Editor.

| Setting | Default | Options | Description |
| --- | --- | --- | --- |
| `Prompt filter dialog` | Enabled | On or off | Opens the filter dialog when creating a new filter. |
| `Default filter type` | `Basic` | `None`, `Basic`, `Custom` | Selects the default filter type offered by the filter dialog. |

### Qualified Text Editor

Qualified Text Editor settings control syntax-aware editors for large text, LOB-like values, and structured content.

| Setting | Default | Range | Description |
| --- | --- | --- | --- |
| `Text length threshold` | `300` characters | `0` to `999999999` | Minimum length for using a qualified text editor. |
| `Active content types` | All available detected types selected | Available IDE file types | Controls which syntax-aware content editors DBN can offer. |

Common available content types include `Text`, `Properties`, `XML`, `HTML`, `CSS`, `Java`, `SQL`, `PL/SQL`, `JavaScript`, `JSON`, `JSON5`, `YAML`, `C#`, `C++`, `Bash`, and `Manifest`. The exact list depends on file types available in the IDE.

### Record Navigation

Record Navigation controls what opens when DBN navigates from a related record or when record navigation needs a target.

| Setting | Default | Options | Description |
| --- | --- | --- | --- |
| `Navigation target` | `Record Viewer` | `Table Editor`, `Record Viewer`, `Ask` | Chooses whether navigation opens the full table editor, opens a single-record viewer, or prompts each time. |

## Data Grid Settings

Data Grid settings affect DBN grid components, including Table Editors, JSON Editors, and result viewers.

### General

| Setting | Default | Description |
| --- | --- | --- |
| `Enable column info tooltips` | Enabled | Shows column metadata tooltips in grid headers or cells where available. |
| `Enable zooming (Ctrl+Mouse Wheel)` | Enabled | Allows grid zooming with `Ctrl+Mouse Wheel`. |

### Sorting

| Setting | Default | Range or options | Description |
| --- | --- | --- | --- |
| `Sorting columns limit` | `4` | `0` to `100`; `0` means unlimited | Limits the number of columns used in multi-column sorting. |
| `NULL values position` | `First` | `First`, `Last` | Controls whether null values sort before or after non-null values. |

### Audit Columns

Audit columns are columns DBN treats as operational or tracking columns, such as created-by, created-at, updated-by, or updated-at fields when configured.

| Setting | Default | Description |
| --- | --- | --- |
| `Audit column names` | Empty | Comma-separated names stored as configured audit columns. |
| `Show audit columns` | Enabled | Shows audit columns in grids. |
| `Enable editing on audit columns` | Disabled | Allows audit-column editing when audit columns are visible and the editor is otherwise editable. |

When audit columns are hidden, DBN reloads affected data grids so the visibility change is reflected in open editors.

## Data Grid Colors

Data Grid colors are configured through the IDE color scheme page `Data Grid (DBN)`.

- **`Plain data`:** Normal grid values.
- **`Audit data`:** Values in configured audit columns.
- **`Modified data`:** Edited values that are not yet committed.
- **`Deleted data`:** Rows marked for deletion.
- **`Error data`:** Values or rows with data errors.
- **`Readonly data`:** Values that cannot be edited.
- **`Loading data`:** Values while data is loading.
- **`Primary key`:** Primary key columns or cells.
- **`Foreign key`:** Foreign key columns or cells.
- **`Selection`:** Selected grid values.
- **`Caret row`:** The row containing the caret.

## Execution Result Settings

SQL result viewers are created by statement execution. The following related settings live under `Execution Engine` rather than `Data Editor`.

| Setting | Default | Location | Description |
| --- | --- | --- | --- |
| `Result set fetch block size` | `100` rows | `Execution Engine` -> `Statement Execution` | Number of rows fetched for statement result viewers. |
| `Execution timeout` | `20` seconds | `Execution Engine` -> `Statement Execution` | Maximum time for statement execution. |
| `Focus result` | Disabled | `Execution Engine` -> `Statement Execution` | Controls whether result tabs gain focus after execution. |

For the full execution reference, see [Execution Engine Settings](./execution-engine-settings.md).

## Validation Messages

| Message area | Common cause | Resolution |
| --- | --- | --- |
| General Data Editor settings | Fetch block size or timeout outside the allowed range | Enter values within the ranges shown in Data Editor settings. |
| Text editor popup | Delay or length threshold outside the allowed range | Use a valid threshold and delay value. |
| Value list popup | Element count or value length threshold outside the allowed range | Use values within the configured limits. |
| Qualified text editor | Text length threshold outside the allowed range | Use a non-negative value within the allowed range. |
| Data Grid sorting | Sorting column limit outside the allowed range | Use `0` for unlimited or a value up to `100`. |
| Audit columns | Audit columns are still visible | Confirm `Show audit columns` is disabled and reload affected grids if needed. |

## Related Documentation

- [Data Viewers and Editors](./data-viewers-and-editors.md): Feature guide for Table Editors, JSON Editors, Data Viewers, and Record Viewers.
- [Environment Types Settings](./environment-types-settings.md): Settings reference for read-only data safeguards and environment colors.
- [Transaction Management Settings](./transaction-management-settings.md): Settings reference for auto-commit, commit prompts, and pending data changes.
- [Database Browser](./database-browser.md): Guide to opening table, view, materialized view, and JSON view data.
- [Execution Engine Settings](./execution-engine-settings.md): Settings reference for SQL execution result behavior.
