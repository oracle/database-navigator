# Database Browser

## Summary

The Database Browser is the DBN tool window for exploring configured database connections and working with database objects from inside the IDE. Use it to inspect schemas, browse object lists, open data and code editors, launch consoles, apply filters, and view object metadata without leaving the project workspace.

- **Primary UI:** `DB Browser` tool window
- **Main menu:** `DB Navigator` -> `Database Browser`
- **Main areas:** Database View and Object Details View
- **Common users:** Developers, DBAs, data engineers, and anyone navigating database metadata in the IDE

> **Note:** Available objects and actions depend on the database type, connection permissions, enabled DBN features, and the object type selected in the tree.

## Contents

<!-- TOC -->
- [When To Use It](#when-to-use-it)
- [Access Paths](#access-paths)
- [Database View](#database-view)
  - [Open The Database View](#open-the-database-view)
  - [Tree Structure](#tree-structure)
  - [Display Modes](#display-modes)
  - [Toolbar Actions](#toolbar-actions)
  - [Navigation Behavior](#navigation-behavior)
  - [Autoscroll Options](#autoscroll-options)
  - [Database View Settings](#database-view-settings)
- [Object Details View](#object-details-view)
  - [Show Or Hide Object Details](#show-or-hide-object-details)
  - [What The Panel Shows](#what-the-panel-shows)
  - [Common Properties](#common-properties)
  - [Navigate From Details](#navigate-from-details)
- [Database Objects](#database-objects)
  - [Common Object Categories](#common-object-categories)
  - [Object Types](#object-types)
  - [Object Hierarchy](#object-hierarchy)
  - [Object Actions](#object-actions)
  - [Object List Actions](#object-list-actions)
  - [Browser-Level Object Filters](#browser-level-object-filters)
- [Common Workflows](#common-workflows)
  - [Create And Browse A Connection](#create-and-browse-a-connection)
  - [Open A Table Or View](#open-a-table-or-view)
  - [Open Source Code](#open-source-code)
  - [Filter A Large Object List](#filter-a-large-object-list)
  - [Inspect Object Details](#inspect-object-details)
  - [Find The Browser Node For An Open Editor](#find-the-browser-node-for-an-open-editor)
- [Troubleshooting](#troubleshooting)
- [Related Documentation](#related-documentation)
<!-- /TOC -->

## When To Use It

Use the Database Browser when you need to:

- Find a schema, table, view, procedure, function, package, type, or other database object.
- Open an object's data editor or source editor.
- Open a SQL console for the active connection.
- Run object-specific actions, such as compile, execute, debug, drop, or view dependencies.
- Filter large object lists so only relevant database objects are visible.
- Inspect the selected object's properties and related objects.

## Access Paths

- **Database Browser tool window:** `DB Browser` tool window.
- **Main menu:** `DB Navigator` -> `Database Browser`.
- **New connection:** `DB Browser` toolbar -> `New Connection`.
- **SQL console:** `DB Browser` toolbar -> `Open SQL Console`, or connection context menu -> `Open SQL Console`.
- **Object details:** `DB Browser` toolbar -> `Show Object Properties`.
- **Browser options:** `DB Browser` toolbar -> `Options`.

## Database View

The Database View is the tree area of the Database Browser. It shows database connections and the object hierarchy available under each connection.

### Open The Database View

1. Open the `DB Browser` tool window from the IDE tool window bar.
2. If the tool window is not visible, use `DB Navigator` -> `Database Browser`.
3. Expand a connection to load its database object groups.
4. Expand schemas and object lists to browse individual objects.

### Tree Structure

The tree is loaded on demand. Expanding a node retrieves the metadata needed for that level, so large schemas do not need to load completely before you start working.

- **Connection:** Database-specific root for schemas, consoles, and other top-level metadata.
- **Schema:** Schema-owned object lists, such as tables, views, packages, types, and procedures.
- **Object list:** Objects of one type, such as `Tables`, `Views`, `Functions`, or `Packages`.
- **Database object:** A concrete object, such as a table, column, constraint, procedure, package function, or Java method.

When a node is loading, the tree can show a `Loading...` placeholder. If a list appears empty, check connection status, database privileges, and active filters.

### Display Modes

The Database Browser supports three display modes.

- **`Connection Tabs`:** You work with multiple connections and want one tab per connection. This is the default browser mode.
- **`Single Tree`:** You want all connections displayed in one shared tree.
- **`Connection Selector`:** You prefer a compact selector that switches the visible tree between connections.

To change the display mode:

1. Open the Database Browser toolbar `Options` menu.
2. Select `Display Mode`.
3. Choose `Single Tree`, `Connection Tabs`, or `Connection Selector`.

You can also change the browser type from Database Browser settings.

### Toolbar Actions

The toolbar at the top of the Database Browser provides the most common navigation and workspace actions.

- **`New Connection`:** Create a new database connection.
- **`Back` / `Forward`:** Move through recent Database View selections.
- **`Expand All` / `Collapse All`:** Expand or collapse the active browser tree.
- **`Show Object Properties` / `Hide Object Properties`:** Show or hide the Object Details View.
- **`Open Session Browser`:** Open session monitoring for the selected or active connection.
- **`Open SQL Console`:** Open a SQL console for the selected or active connection.
- **`Options`:** Access autoscroll, object filters, display mode, and browser settings.

### Navigation Behavior

- **Expand node:** Loads and displays the next metadata level.
- **Double-click object:** Opens the default editor or navigates to the default target when available.
- **Press `Enter` on object:** Opens the default editor or navigates to the default target when available.
- **Start typing while the tree is focused:** Searches visible tree nodes using speed search.
- **Right-click connection, object list, or object:** Opens context actions for the selected node.

### Autoscroll Options

Autoscroll settings are available from the Database Browser toolbar `Options` menu.

- **`Autoscroll from Editor`:** Selects the related Database View node when you open or switch to a DBN database editor.
- **`Autoscroll to Editor`:** Opens the related editor when you select items in the Database View.

> **Tip:** If selecting objects opens editors unexpectedly, check whether `Autoscroll to Editor` is enabled.

### Database View Settings

Open Database Browser settings from DBN project settings. Related display and filter actions are also available from the Database Browser toolbar `Options` menu. The general settings include:

- **`Browser type`:** Selects the display mode used by the Database Browser.
- **`Enable sticky tree path`:** Keeps the current tree path visible while scrolling through deep object hierarchies.
- **`Enable quick object list filters`:** Enables the quick filter action on object lists.
- **`Show detailed object information`:** Adds extra details to object labels in the tree when available.
- **`Navigation history size`:** Controls how many tree selections are kept for `Back` and `Forward`.

For the full settings reference, see [Database Browser Settings](./database-browser-settings.md).

## Object Details View

The Object Details View is the secondary panel shown by the Database Browser `Show Object Properties` action. It displays properties for the currently selected database object.

The UI label uses `Object Properties`; this documentation uses `Object Details View` to describe the user-facing panel.

### Show Or Hide Object Details

1. Open the `DB Browser` tool window.
2. Select a database object in the Database View.
3. Click `Show Object Properties` in the Database Browser toolbar.
4. Click `Hide Object Properties` or the close control in the details panel to hide it.

### What The Panel Shows

- **Header:** Shows the selected object's type and name. When environment coloring is enabled, the header follows the object's connection environment color.
- **Property table:** Shows object-specific properties in `Property` and `Value` columns.
- **Navigable values:** Some values link to related objects, such as a connection, parent schema, declared type, foreign key target, credential, or synonym target.

The panel updates when the Database View selection changes. If no object or multiple objects are selected, the panel shows no object-specific details.

### Common Properties

All objects can show hierarchy and connection information. Additional details are shown when DBN has a specialized property provider for the selected object type.

- **Any database object:** Parent objects and connection.
- **Table:** Temporary table attribute when applicable.
- **Column:** Data type, identity or key attributes, nullable status, and foreign key target.
- **Argument:** Argument direction and data type.
- **Constraint:** Constraint type, check condition, or referenced foreign key constraint.
- **Synonym:** Underlying object when resolved.
- **Trigger:** Trigger timing/type and triggering events.
- **Type:** Collection element type when applicable.
- **Type attribute:** Attribute data type.
- **Credential:** Credential type, user name, and enabled status.
- **AI profile:** Provider, model, enabled state, interactive state, temperature, and credential.
- **AI model:** Mining function, algorithm, size, partitioning, in-memory, and external data attributes.

### Navigate From Details

Some property values are navigable. To open a related object from the details table:

1. Select the value cell in the `Value` column.
2. Double-click the cell, use the platform navigation gesture, or press `Enter`.
3. DBN navigates to the related object when a target is available.

> **Tip:** Navigable values are useful for jumping from a column to its declared type, from a foreign key to the referenced column or constraint, or from a synonym to its underlying object.

## Database Objects

Database objects are represented as typed nodes in the Database View. DBN groups them by connection, schema, object list, and object hierarchy so you can browse from broad database areas down to individual members such as columns and arguments.

### Common Object Categories

- **Data objects:** Tables, views, JSON views, materialized views, nested tables, columns, indexes, and constraints.
- **Program objects:** Procedures, functions, packages, package procedures, package functions, types, type procedures, and type functions.
- **Execution members:** Arguments, package members, type attributes, Java methods, and Java fields.
- **Database structure:** Schemas, sequences, synonyms, database links, clusters, dimensions, roles, privileges, users, and credentials.
- **Java objects:** Java classes, Java resources, Java fields, Java methods, and inner classes.
- **AI objects:** AI profiles and AI models where supported by the connected database.
- **Consoles:** SQL, debug, and search consoles available for a connection.

### Object Types

The Database Browser can show the following object types when the connected database supports them and they are not hidden by filters.

| Object type | What it represents |
| --- | --- |
| ![](https://raw.githubusercontent.com/oracle/database-navigator/master/src/main/resources/img/object/role/Role.svg) `Role` | Database roles visible in browser trees. |
| ![](https://raw.githubusercontent.com/oracle/database-navigator/master/src/main/resources/img/object/privilege/Privilege.svg) `Privilege` | Privilege nodes. |
| `Charset` | Character set metadata. |
| ![](https://raw.githubusercontent.com/oracle/database-navigator/master/src/main/resources/img/object/table/Table.svg) `Table` | Table lists and table objects. |
| ![](https://raw.githubusercontent.com/oracle/database-navigator/master/src/main/resources/img/object/view/View.svg) `View` | View lists and view objects. |
| ![](https://raw.githubusercontent.com/oracle/database-navigator/master/src/main/resources/img/object/view/JsonView.svg) `JSON view` | JSON view lists and objects. |
| ![](https://raw.githubusercontent.com/oracle/database-navigator/master/src/main/resources/img/object/view/MaterializedView.svg) `Materialized view` | Materialized view lists and objects. |
| ![](https://raw.githubusercontent.com/oracle/database-navigator/master/src/main/resources/img/object/NestedTable.png) `Nested table` | Nested table objects. |
| ![](https://raw.githubusercontent.com/oracle/database-navigator/master/src/main/resources/img/object/column/Column.svg) `Column` | Column lists and column nodes. |
| ![](https://raw.githubusercontent.com/oracle/database-navigator/master/src/main/resources/img/object/index/Index.svg) `Index` | Index lists and index nodes. |
| ![](https://raw.githubusercontent.com/oracle/database-navigator/master/src/main/resources/img/object/constraint/Constraint.svg) `Constraint` | Constraint lists and constraint nodes. |
| ![](https://raw.githubusercontent.com/oracle/database-navigator/master/src/main/resources/img/object/trigger/Trigger.svg) `Dataset trigger` | Table, view, and materialized view trigger nodes. |
| ![](https://raw.githubusercontent.com/oracle/database-navigator/master/src/main/resources/img/object/trigger/DatabaseTrigger.svg) `Database trigger` | Database trigger nodes. |
| ![](https://raw.githubusercontent.com/oracle/database-navigator/master/src/main/resources/img/object/synonym/Synonym.svg) `Synonym` | Synonym lists and synonym objects. |
| ![](https://raw.githubusercontent.com/oracle/database-navigator/master/src/main/resources/img/object/sequence/Sequence.svg) `Sequence` | Sequence lists and sequence objects. |
| ![](https://raw.githubusercontent.com/oracle/database-navigator/master/src/main/resources/img/object/Procedure.png) `Procedure` | Procedure lists and procedure objects. |
| ![](https://raw.githubusercontent.com/oracle/database-navigator/master/src/main/resources/img/object/function/Function.svg) `Function` | Function lists and function objects. |
| ![](https://raw.githubusercontent.com/oracle/database-navigator/master/src/main/resources/img/object/package/Package.svg) `Package` | Package lists and package objects. |
| ![](https://raw.githubusercontent.com/oracle/database-navigator/master/src/main/resources/img/object/type/Type.svg) `Type` | Type lists and type objects. |
| ![](https://raw.githubusercontent.com/oracle/database-navigator/master/src/main/resources/img/object/Attribute.png) `Type attribute` | Type attribute nodes. |
| ![](https://raw.githubusercontent.com/oracle/database-navigator/master/src/main/resources/img/object/Argument.png) `Argument` | Function and procedure argument nodes. |
| ![](https://raw.githubusercontent.com/oracle/database-navigator/master/src/main/resources/img/object/java/JavaClass.svg) `Java class` | Java class objects. |
| ![](https://raw.githubusercontent.com/oracle/database-navigator/master/src/main/resources/img/object/java/JavaField.svg) `Java field` | Java field nodes. |
| ![](https://raw.githubusercontent.com/oracle/database-navigator/master/src/main/resources/img/object/java/JavaMethod.svg) `Java method` | Java method nodes. |
| ![](https://raw.githubusercontent.com/oracle/database-navigator/master/src/main/resources/img/object/java/JavaResource.svg) `Java resource` | Java resource objects. |
| ![](https://raw.githubusercontent.com/oracle/database-navigator/master/src/main/resources/img/object/Dimension.png) `Dimension` | Dimension objects. |
| ![](https://raw.githubusercontent.com/oracle/database-navigator/master/src/main/resources/img/object/Cluster.png) `Cluster` | Cluster objects. |
| ![](https://raw.githubusercontent.com/oracle/database-navigator/master/src/main/resources/img/object/DatabaseLink.png) `Database link` | Database link objects. |
| ![](https://raw.githubusercontent.com/oracle/database-navigator/master/src/main/resources/img/object/credential/Credential.svg) `Credential` | Credential objects. |
| ![](https://raw.githubusercontent.com/oracle/database-navigator/master/src/main/resources/img/object/profile/AIProfile.svg) `AI profile` | AI profile objects. |
| ![](https://raw.githubusercontent.com/oracle/database-navigator/master/src/main/resources/img/object/model/AIModel.svg) `AI model` | AI model objects. |

### Object Hierarchy

The exact hierarchy depends on database support, but common relationships include:

- **Table:** Columns, constraints, indexes, triggers, and nested tables.
- **View:** Columns, constraints, and triggers.
- **Materialized view:** Columns, constraints, and triggers.
- **Package:** Package types, package functions, and package procedures.
- **Type:** Type types, type attributes, type functions, and type procedures.
- **Function or procedure:** Arguments.
- **Java class:** Java fields, Java methods, and inner classes.
- **User or role:** Granted roles and granted privileges.

### Object Actions

Right-click a database object to see actions available for that object. Actions are context-sensitive and appear only when the object type and database connection support them.

| Action | Applies to | Result |
| --- | --- | --- |
| `Edit Data` | Data objects such as tables and supported views | Opens a data editor. |
| `Edit Code` | Source-backed objects such as procedures, functions, packages, types, triggers, and Java sources | Opens the code editor. |
| `Compile` | Compilable objects | Compiles the selected program or source object. |
| `Execute` / `Debug` | Methods, programs, and supported Java objects | Opens execution or debugging workflow. |
| `Drop` | User schema objects where supported | Drops the selected object after confirmation. |
| `Dependencies` / `Dependency Tree` | Referenceable schema objects where supported | Shows referenced and referencing objects. |
| `Extract SQL Statement` | Supported objects | Generates SELECT, INSERT, or DDL statements for the object. |
| `Reload` / `Refresh` | Object lists and objects | Reloads metadata from the database. |

> **Note:** DBN hides destructive or unsupported actions for objects where they do not apply, such as system schema objects, public schema objects, or object types unsupported by the connected database.

Use the toolbar `Show Object Properties` action when you want metadata for the currently selected object.

### Object List Actions

Right-click an object list, such as `Tables` or `Columns`, to manage that list.

- **`Reload`:** Reload the list from the database.
- **`Filters`:** Open filtering actions for the list.
- **`Quick Filter`:** Add simple name-based filter conditions when quick filters are enabled.
- **`Create Filter` / `Edit Filter`:** Configure a persistent object filter.
- **`Enable Filter` / `Disable Filter`:** Enable or disable an existing object filter.
- **`Remove Filter`:** Remove the configured filter.
- **`Hide Object Type`:** Hide that object type from the browser.
- **`Hide Empty Schemas`:** Hide schemas with no visible objects.
- **`Hide Pseudo Columns`:** Hide pseudo columns from column lists.
- **`Hide Audit Columns`:** Hide audit columns from column lists.
- **`Create Object`:** Create a new object of the selected list type when creation is supported.

### Browser-Level Object Filters

Use `Options` -> `Object Filters...` from the Database Browser toolbar to control which object types are visible in the browser. These filters affect the Database View globally for the relevant connection or configuration.

Object-list filters narrow one list at a time. For example, you can filter a `Tables` list by name without hiding all table objects from the browser.

> **Tip:** If an expected object type or object is missing, check both browser-level object filters and object-list filters.

## Common Workflows

### Create And Browse A Connection

1. Open `DB Browser`.
2. Click `New Connection`.
3. Enter connection details and save the connection.
4. Expand the connection.
5. Expand a schema and the object list you want to inspect.

### Open A Table Or View

1. Expand the connection and schema.
2. Expand `Tables`, `Views`, or another supported data object list.
3. Double-click the object or right-click it.
4. Choose `Edit Data` to inspect or edit rows.

### Open Source Code

1. Expand the schema.
2. Find a source-backed object, such as a procedure, function, package, type, trigger, Java class, or Java resource.
3. Double-click the object or right-click it.
4. Choose `Edit Code`.

### Filter A Large Object List

1. Right-click the object list you want to narrow, such as `Tables`.
2. Open `Filters`.
3. Use `Quick Filter` for temporary name conditions, or `Edit Filter` for a configured list filter.
4. Reload the list if needed.

### Inspect Object Details

1. Click `Show Object Properties` in the Database Browser toolbar.
2. Select one object in the Database View.
3. Review the `Property` and `Value` table.
4. Navigate from linked values when you need related objects.

### Find The Browser Node For An Open Editor

1. Open the Database Browser toolbar `Options` menu.
2. Enable `Autoscroll from Editor`.
3. Switch to a DBN database editor.
4. DBN selects the related connection or object in the Database View.

## Troubleshooting

| Symptom | Resolution |
| --- | --- |
| Object list is empty | Confirm the connection is active, the database user has privileges, and filters are not hiding the objects. |
| Expected object type is missing | Check `Object Filters...` and whether the connected database supports that object type. |
| Details panel says no object is selected | Select exactly one database object. Connection roots and object lists do not always provide object-specific details. |
| Selecting objects opens editors unexpectedly | Disable `Autoscroll to Editor` from the Database Browser `Options` menu. |
| Browser does not follow the active editor | Enable `Autoscroll from Editor`. |
| Tree labels do not show enough metadata | Enable `Show detailed object information` in Database Browser settings. |
| Navigation history feels too short | Increase `Navigation history size` in Database Browser settings. |

## Related Documentation

- [Database Browser Settings](./database-browser-settings.md): Settings reference for browser layout, filters, sorting, default editors, and toolbar state.
- [Database Events](./database-events.md): Guide to enabling table data-change notifications from supported table actions.
- [Environment Types](./environment-types.md): Guide to environment colors shown on browser tabs and object-related UI.
- [Workspace Integration](./workspace-integration.md): Guide to creating and attaching DDL files from source-backed database objects.
- [Connection Management](./connection-management.md): Guide to creating and managing the connections shown in the Database Browser.
- [Transaction Management](./transaction-management.md): Guide to Resource Monitor and Session Browser workflows opened from DBN navigation.
- [Database Assistant: Oracle Select AI](./database-assistant-select-ai.md): Guide to Select AI profiles, models, credentials, and database-native assistant workflows.
- [Database Assistant](./database-assistant.md): Overview of Oracle Select AI and the generic assistant.
- [Vector Toolbox](./vector-toolbox.md): Guide to embeddings tables, AI models, and vector-search workflows tied to browser connections.
- [Connection Management Settings](./connection-management-settings.md): Settings reference for connection configuration, supported databases, drivers, and authentication.
