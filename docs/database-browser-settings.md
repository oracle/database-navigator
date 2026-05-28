# Database Browser Settings

## Summary

Database Browser settings control how DBN displays database connections, which object types are visible, how object lists are sorted, which editor opens by default, and how object filters behave for each connection.

| Setting area | Scope | Purpose |
| --- | --- | --- |
| General | Project | Controls browser layout, tree behavior, quick filters, object details, and navigation history. |
| Filters | Project | Controls globally visible object types in the Database Browser. |
| Sorting | Project | Controls default sorting for selected object lists. |
| Default Editors | Project | Controls which editor opens for objects with multiple editor types. |
| Object Filters | Connection | Controls connection-specific object visibility, custom filters, and basic schema/column filters. |
| Browser toolbar state | Project workspace | Controls autoscroll behavior and whether object properties are visible. |

> **Note:** Settings can affect what appears in the `DB Browser` tool window immediately. If an object or object type is missing, check both project-level Database Browser filters and connection-level object filters.

## Contents

<!-- TOC -->
- [Access Paths](#access-paths)
- [Project-Level Settings](#project-level-settings)
  - [General](#general)
- [Filters](#filters)
  - [Visible Object Types](#visible-object-types)
- [Sorting](#sorting)
  - [Sorting Types](#sorting-types)
- [Default Editors](#default-editors)
  - [Editor Choices](#editor-choices)
- [Connection-Level Object Filters](#connection-level-object-filters)
  - [Object Type Filter Inheritance](#object-type-filter-inheritance)
  - [Basic Filters](#basic-filters)
  - [Custom Object Filters](#custom-object-filters)
- [Object List Filters](#object-list-filters)
- [Browser Toolbar State](#browser-toolbar-state)
  - [Autoscroll from Editor](#autoscroll-from-editor)
  - [Autoscroll to Editor](#autoscroll-to-editor)
  - [Show Object Properties](#show-object-properties)
- [Recommended Configurations](#recommended-configurations)
- [Troubleshooting](#troubleshooting)
- [Related Documentation](#related-documentation)
<!-- /TOC -->

## Access Paths

- **Database Browser settings:** `DB Navigator` -> `Settings` -> `Database Browser`
- **Browser display mode:** `DB Browser` toolbar -> `Options` -> `Display Mode`
- **Active connection object filters:** `DB Browser` toolbar -> `Options` -> `Object Filters...`
- **Object list filters:** Right-click an object list, such as `Tables` or `Columns`, then open `Filters`
- **Object properties panel:** `DB Browser` toolbar -> `Show Object Properties` or `Hide Object Properties`
- **Autoscroll toggles:** `DB Browser` toolbar -> `Options`

## Project-Level Settings

Project-level Database Browser settings are grouped into `General`, `Filters`, `Sorting`, and `Default Editors`.

### General

| Setting | Default | Description |
| --- | --- | --- |
| `Browser type` | `Connection Tabs` | Selects the layout used by the Database Browser. |
| `Enable sticky tree path` | Enabled | Keeps the current tree path visible while scrolling through deep trees. |
| `Enable quick object list filters` | Disabled | Enables quick, name-based filters on object list context menus. |
| `Show detailed object information` | Disabled | Adds extra conditional details to object labels when DBN can provide them. |
| `Navigation history size` | `100` items | Controls how many Database View selections are kept for `Back` and `Forward`. Valid range is `0` to `1000`. |

#### Browser Type

- **`Connection Tabs`:** Shows one tab per configured connection. This is the default.
- **`Single Tree`:** Shows all connections inside one shared tree.
- **`Connection Selector`:** Shows one connection at a time and switches connections with a selector.

Changing the browser type rebuilds the Database Browser view. It does not change connection definitions or database metadata.

#### Sticky Tree Path

Use `Enable sticky tree path` when working with deep object trees. The active path remains visible while you scroll, making it easier to keep context inside large schemas.

#### Quick Object List Filters

When `Enable quick object list filters` is enabled, object-list context menus can show `Quick Filter...`. Quick filters let you add temporary name conditions to a list, such as narrowing `Tables` to names that match a pattern.

Quick filters narrow the currently selected object list. They do not replace project-level visible object type filters or connection-level custom filters.

#### Detailed Object Information

When `Show detailed object information` is enabled, DBN appends additional object details to tree labels when available. This can make the tree more informative, but also wider.

Use it when you need more metadata while browsing. Leave it disabled when you prefer compact tree labels.

#### Navigation History Size

The Database Browser keeps a selection history for toolbar `Back` and `Forward` navigation.

- **`0`:** Disables useful selection history.
- **`100`:** Default history length.
- **Up to `1000`:** Keeps a longer navigation trail for large browsing sessions.

## Filters

The project-level `Filters` section controls which database object types are visible in the Database Browser. By default, all browsable object types are enabled.

These filters apply broadly. Connection-level filter settings can inherit them or override them for a specific connection.

### Visible Object Types

Use the object type checklist to show or hide entire categories of database objects.

Hiding an object type hides both the object list and objects of that type. For example, disabling `Column` hides column child nodes under tables and views.

For the object type catalog, see [Database Browser](./database-browser.md#object-types).

## Sorting

The `Sorting` section controls how selected object lists are sorted in the Database Browser. The available sorting types are `Name` and `Position`.

| Object type | Default sorting | Notes |
| --- | --- | --- |
| `Column` | `Name` | Sorts table, view, and materialized view columns by name unless changed. |
| `Function` | `Name` | Sorts function lists alphabetically. |
| `Procedure` | `Name` | Sorts procedure lists alphabetically. |
| `Argument` | `Position` | Preserves function and procedure argument order. |
| `Type attribute` | `Position` | Preserves attribute order inside object types. |

### Sorting Types

- **`Name`:** Sorts objects alphabetically by object name.
- **`Position`:** Sorts objects by their natural metadata position, such as argument order or attribute order.

Use `Position` for ordered structures where database order matters. Use `Name` when scanning long object lists alphabetically is more useful.

## Default Editors

The `Default Editors` section controls what opens when an object supports more than one editor target.

| Object type | Default | Available editor choices |
| --- | --- | --- |
| `View` | `Last Selection` | `Code`, `Data`, `Last Selection` |
| `Package` | `Last Selection` | `Spec`, `Body`, `Last Selection` |
| `Type` | `Last Selection` | `Spec`, `Body`, `Last Selection` |

### Editor Choices

- **`Code`:** Opens the object's code editor.
- **`Data`:** Opens the object's data editor.
- **`Spec`:** Opens the specification editor for package or type objects.
- **`Body`:** Opens the body editor for package or type objects.
- **`Last Selection`:** Reuses the last editor target DBN recorded for that object type when available.

Use an explicit editor choice when you consistently want one target. Use `Last Selection` when you switch between targets and want DBN to remember your recent preference.

## Connection-Level Object Filters

Connection-level object filters are opened from `DB Browser` toolbar -> `Options` -> `Object Filters...` for the active connection. They can also be edited from a connection's `Filters` settings tab.

Connection filters are useful when one database contains noisy schemas or object naming conventions that should not affect other connections.

### Object Type Filter Inheritance

Connection-level object type filters can inherit project-level filter settings.

| Setting | Default | Description |
| --- | --- | --- |
| `Use master settings` | Enabled | Uses the project-level Database Browser object type filters. |
| Object type checklist | All visible when not inheriting | Overrides visible object types for this connection. |

`Use master settings` appears only when editing connection-level filters. If you hide an object type from an object list context menu, DBN copies the master settings to the connection and then hides that type for the connection.

### Basic Filters

| Setting | Default | Description |
| --- | --- | --- |
| `Hide empty schemas` | Disabled | Hides schemas that have no visible objects. This appears only for databases that support empty schema evaluation. |
| `Hide audit columns` | Disabled | Hides audit or tracking columns from column lists. |
| `Hide pseudo columns` | Disabled | Hides pseudo or hidden columns from column lists. |

These basic filters combine with any active custom filters. For example, a column list can be narrowed by a custom column filter and also hide audit columns.

### Custom Object Filters

Custom object filters are connection-specific expressions for supported object types.

- **`Add Filter`:** Creates a custom filter for a supported object type.
- **`Activate Filter` / `Deactivate Filter`:** Turns an existing filter on or off without deleting it.
- **`View Filter`:** Shows a preview of the filter expression.
- **`Edit Filter`:** Changes the expression.
- **`Remove Filter`:** Removes the filter from the connection.

Custom filters can be created for:

- `Schema`
- `Table`
- `View`
- `Column`
- `Constraint`
- `Index`
- `Trigger`
- `Function`
- `Procedure`
- `Package`
- `Type`
- `Synonym`
- `Java class`
- `Database link`

Filter expressions use object attributes exposed by the filter editor. The editor shows supported attributes and a sample expression for the selected object type.

Examples of supported attributes include:

- **`Schema`:** `SCHEMA_NAME`, `IS_USER_SCHEMA`, `IS_PUBLIC_SCHEMA`, `IS_SYSTEM_SCHEMA`, `IS_EMPTY_SCHEMA`
- **`Table`:** `TABLE_NAME`, `IS_TEMPORARY_TABLE`
- **`View`:** `VIEW_NAME`, `IS_SYSTEM_VIEW`
- **`Column`:** `COLUM_NAME`, `DATA_TYPE`, `DATA_LENGTH`, `IS_AUDIT_COLUMN`, `IS_PSEUDO_COLUMN`, `IS_NULLABLE_COLUMN`, `IS_IDENTITY_COLUMN`, `IS_PRIMARY_KEY`, `IS_FOREIGN_KEY`
- **`Constraint`:** `CONSTRAINT_NAME`, `IS_PRIMARY_KEY`, `IS_FOREIGN_KEY`, `IS_UNIQUE_KEY`
- **`Index`:** `INDEX_NAME`, `IS_UNIQUE_INDEX`
- **Other supported types:** A default name attribute, such as `<OBJECT_TYPE>_NAME`.

> **Note:** The column name attribute is currently exposed as `COLUM_NAME` in the filter definition.

## Object List Filters

Object list filters are opened from an object list context menu in the Database View.

- **`Quick Filter...`:** Adds temporary name conditions when quick filters are enabled.
- **`Create Filter...`:** Creates a persistent filter for the selected object type and connection.
- **`Edit Filter...`:** Edits an existing persistent filter.
- **`Enable Filter` / `Disable Filter`:** Toggles an existing persistent filter.
- **`Remove Filter`:** Deletes the persistent filter.
- **`Hide Object Type`:** Hides this object type from the browser for the active filter scope.

Quick filters and persistent filters can both narrow a list. If a list is already filtered by connection settings, adding quick filter conditions narrows the already-filtered results.

## Browser Toolbar State

These related settings are stored as browser manager state rather than in the Database Browser settings page.

| Toolbar setting | Default | Description |
| --- | --- | --- |
| `Autoscroll from Editor` | Enabled | Selects the matching Database Browser node when a DBN database editor opens or becomes active. |
| `Autoscroll to Editor` | Disabled | Opens or navigates to the editor when a browser tree selection is made. |
| `Show Object Properties` | Enabled | Shows the Object Details View panel beside the Database View. |

### Autoscroll from Editor

Use `Autoscroll from Editor` when you want the Database Browser to follow your active DBN editor. This is useful when you navigate through several open database objects and want the corresponding browser node selected automatically.

### Autoscroll to Editor

Use `Autoscroll to Editor` when selecting objects in the tree should open or navigate to the corresponding editor. Disable it if you want to browse the tree without changing editor focus.

### Show Object Properties

Use `Show Object Properties` to keep the Object Details View visible. The panel updates when a single database object is selected in the Database View.

## Recommended Configurations

- **Many connections:** Use `Connection Tabs` or `Connection Selector`; enable environment colors if configured elsewhere.
- **Very large schemas:** Keep `Show detailed object information` disabled; use object type filters and custom filters.
- **Browsing ordered members:** Use `Position` sorting for arguments and type attributes.
- **Editing packages often:** Set package default editor to `Spec`, `Body`, or `Last Selection` depending on workflow.
- **Noisy metadata:** Hide unused object types globally, then add connection-level custom filters for database-specific noise.
- **Read-only browsing:** Disable `Autoscroll to Editor` so selection does not open editors unexpectedly.

## Troubleshooting

| Symptom | Resolution |
| --- | --- |
| An object type is missing everywhere | Project-level `Filters` may have that object type unchecked. |
| An object type is missing for one connection | Connection-level object type filters may override master settings. |
| Schemas disappear from one connection | `Hide empty schemas` or a custom schema filter may be active. |
| Columns disappear from tables or views | `Hide audit columns`, `Hide pseudo columns`, a custom column filter, or the `Column` object type filter may be active. |
| Selecting a tree node opens editors | Disable `Autoscroll to Editor`. |
| Browser does not follow open editors | Enable `Autoscroll from Editor`. |
| Object labels are too verbose | Disable `Show detailed object information`. |
| Back/Forward history is too short | Increase `Navigation history size`. |

## Related Documentation

- [Database Browser](./database-browser.md): Main Database Browser user guide.
- [Environment Types Settings](./environment-types-settings.md): Settings reference for environment colors used by browser tabs and headers.
- [Connection Management](./connection-management.md): Guide to creating and managing the connections shown in the Database Browser.
- [Transaction Management](./transaction-management.md): Guide to Resource Monitor and Session Browser workflows related to browser connections.
- [Connection Management Settings](./connection-management-settings.md): Settings reference for connection configuration, supported databases, drivers, and authentication.
