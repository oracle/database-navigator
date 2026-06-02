# Database Events

## Summary

Database Events lets DBN monitor Oracle table data-change notifications inside the IDE. It uses Oracle Continuous Query Notification to register listeners on supported tables and display received `INSERT`, `UPDATE`, and `DELETE` notifications in the `DB Events` tool window.

- **Primary UI:** `DB Events` tool window
- **Main menu:** `DB Navigator` -> `Data Events Monitor`
- **Main areas:** Event registrations and event notifications
- **Scope:** Oracle connections where data-change notification support is available
- **Settings:** There is no separate Database Events settings page yet. Behavior is controlled by connection support, database prerequisites, table registrations, and per-registration operation selections.

> **Note:** Database Events depends on Oracle database privileges and Oracle JDBC change-notification support. The menu entry and table actions are available only when DBN detects data-change notification support for the current project, connection, and table.

## Contents

<!-- TOC -->
- [When To Use It](#when-to-use-it)
- [Access Paths](#access-paths)
- [Availability And Prerequisites](#availability-and-prerequisites)
- [Event Lifecycle](#event-lifecycle)
- [Event Registrations](#event-registrations)
  - [Registration Columns](#registration-columns)
  - [Registration Actions](#registration-actions)
- [Event Notifications](#event-notifications)
  - [Notification Columns](#notification-columns)
  - [Notification Actions](#notification-actions)
- [Data Editor Integration](#data-editor-integration)
- [Common Workflows](#common-workflows)
  - [Start Listening For Table Changes](#start-listening-for-table-changes)
  - [Review Active Registrations](#review-active-registrations)
  - [Inspect Received Notifications](#inspect-received-notifications)
  - [Stop Listening For Table Changes](#stop-listening-for-table-changes)
- [Troubleshooting](#troubleshooting)
- [Related Documentation](#related-documentation)
<!-- /TOC -->

## When To Use It

Use Database Events when you need to:

- Monitor table changes made by another user, session, application, or tool.
- See whether a table editor is showing data that may have changed since it was loaded.
- Register interest in specific table operations: `INSERT`, `UPDATE`, `DELETE`, or a combination of them.
- Review active Oracle Continuous Query Notification registrations for a connection.
- Filter received notifications by table or operation.
- Stop a table listener without closing the connection or removing the table from the Database Browser.

## Access Paths

- **Open event monitor:** `DB Navigator` -> `Data Events Monitor`
- **Open tool window:** `DB Events` tool window, after it has been opened or activated
- **Enable table listener:** Database Browser -> table context menu -> `Notify Data Changes`
- **Disable table listener:** Database Browser -> table context menu -> `Stop Notifying Data Changes`
- **Show events from data editor:** Data editor warning banner -> `Show events`
- **Reload changed table data:** Data editor warning banner -> `Reload data`

## Availability And Prerequisites

Database Events is available for Oracle connections that support DBN's `Data change notification` feature. The event monitor lists Oracle connections and opens on demand when a listener is created or when the user opens `Data Events Monitor`.

The database user needs the privileges required for Oracle Continuous Query Notification:

- **`CHANGE NOTIFICATION`:** Allows the user to receive database change notifications.
- **`EXECUTE` on `SYS.DBMS_CHANGE_NOTIFICATION`:** Allows DBN to enable and disable database change notifications.

DBN checks these prerequisites when enabling notifications for a table. If the requirements are missing, use prerequisite verification or ask a database administrator to grant the required privileges.

## Event Lifecycle

1. The user chooses `Notify Data Changes` on a supported Oracle table.
2. DBN opens `Event Listener Registration` and asks which operations to monitor: `INSERT`, `UPDATE`, `DELETE`, or any combination of them.
3. DBN registers an Oracle Continuous Query Notification listener for the table.
4. Oracle sends notifications when matching row changes occur.
5. DBN records each notification with the table name, operation, timestamp, row id, and source registration id.
6. The `DB Events` tool window shows the registration and received notifications for the connection.
7. Data editors for monitored tables can warn when their loaded data is older than received notifications.
8. The user stops listening from the table context action or by deleting the registration from the event monitor.

## Event Registrations

The `Registrations` tab shows data-change notification registrations for the selected Oracle connection. It includes registrations known to the database metadata query and marks which registrations DBN is actively listening to in the current IDE session.

### Registration Columns

| Column | Description |
| --- | --- |
| `Registration Id` | Oracle registration identifier. |
| `User Name` | Database user that owns the registration. |
| `Table Name` | Table associated with the registration. |
| `Operations` | Operations monitored by the registration, such as `INSERT`, `UPDATE`, `DELETE`, or all operations. |
| `Timeout` | Registration timeout reported by the database. |
| `Change Lag` | Change lag reported by the database. |
| `Callback` | Oracle callback associated with the registration. |
| `Reg Flags` | Registration flags reported by the database. |

### Registration Actions

- **Reload:** Refreshes the registration list from the database.
- **Find:** Opens the registration search control when available.
- **User Filter:** Filters registrations by database user.
- **Table Filter:** Filters registrations by table name.
- **Status Filter:** Filters registrations by `Listening` or `Not Listening`.
- **Clear Filters:** Removes active registration filters.
- **Delete:** Deregisters selected registrations through `DBMS_CHANGE_NOTIFICATION`.

## Event Notifications

The `Notifications` tab shows notifications received by DBN for the selected Oracle connection during the current IDE session. Notifications are stored in memory for the project session and can be filtered for investigation.

### Notification Columns

| Column | Description |
| --- | --- |
| `Table Name` | Table that produced the notification. |
| `Operation` | Reported change operation, such as `INSERT`, `UPDATE`, or `DELETE`. |
| `Timestamp` | Time when DBN recorded the notification. |
| `Row Id` | Oracle row id reported with the notification. |
| `Source Registration Id` | Registration id that produced the notification. |

### Notification Actions

- **Reload:** Reloads notification data into the table.
- **Table Filter:** Filters notifications to a single table.
- **Operation Filter:** Filters notifications by operation.
- **Clear Filters:** Removes active notification filters.

## Data Editor Integration

When a data editor is open for a table with received notifications, DBN can detect that the table content changed after the editor loaded its data. The editor shows a warning banner with actions to reload data or open the event monitor filtered to the affected table.

Reloading data keeps the current filter and preserves local changes where possible. Use the event monitor first when you want to inspect which operations arrived before deciding whether to reload.

## Common Workflows

### Start Listening For Table Changes

1. Open the table in the Database Browser.
2. Right-click the table and choose `Notify Data Changes`.
3. In `Event Listener Registration`, select at least one operation: `INSERT`, `UPDATE`, or `DELETE`.
4. Confirm the dialog.
5. Open `DB Events` to review the new registration.

### Review Active Registrations

1. Open `DB Navigator` -> `Data Events Monitor`.
2. Select the Oracle connection.
3. Open the `Registrations` tab.
4. Use user, table, or status filters to narrow the list.
5. Click `Reload` if you need the latest database registration state.

### Inspect Received Notifications

1. Open the `DB Events` tool window.
2. Select the Oracle connection.
3. Open the `Notifications` tab.
4. Filter by table or operation if the list is large.
5. Use the timestamp, row id, and source registration id to correlate notifications with the table listener.

### Stop Listening For Table Changes

1. Use the Database Browser table context menu and choose `Stop Notifying Data Changes`.
2. Alternatively, open `DB Events` -> `Registrations`, select the registration, and click `Delete`.
3. Reload the registrations list to confirm the listener is no longer active.

## Troubleshooting

| Symptom | Resolution |
| --- | --- |
| `Data Events Monitor` is not visible | Confirm the project has an Oracle connection that supports data-change notifications. |
| `Notify Data Changes` is not available on a table | Confirm the selected object is a supported Oracle table and the connection supports `Data change notification`. |
| Registration fails because prerequisites are missing | Grant `CHANGE NOTIFICATION` and `EXECUTE` on `SYS.DBMS_CHANGE_NOTIFICATION`, then try again. |
| Event listener dialog cannot be confirmed | Select at least one operation: `INSERT`, `UPDATE`, or `DELETE`. |
| Registrations list looks stale | Click `Reload` in the `Registrations` tab. |
| Notifications do not appear | Confirm the listener is active, the changed table matches the registration, and the database change was committed when required by the database behavior. |
| Data editor reports changed content | Use `Show events` to inspect notifications or `Reload data` to refresh the table editor. |
| Delete registration fails | Confirm the current user can execute `SYS.DBMS_CHANGE_NOTIFICATION` and that the registration still exists in the database. |

## Related Documentation

- [Database Browser](./database-browser.md): Guide to browsing tables and opening table context actions.
- [Data Viewers and Editors](./data-viewers-and-editors.md): Guide to table editors, data reloads, and result viewers.
- [Connection Management](./connection-management.md): Guide to Oracle connection setup and connection lifecycle.
- [Transaction Management](./transaction-management.md): Guide to sessions, commits, rollbacks, and live database activity.
