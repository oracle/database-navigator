# Transaction Management

## Summary

Transaction Management covers how DBN handles commit and rollback operations, auto-commit state, pending data changes, resource monitoring, and database session selection. It sits directly after Connection Management because transaction behavior depends on the active connection, JDBC connection state, and the DBN session assigned to an editor or data viewer.

- **Auto-Commit:** Controls whether database changes are committed immediately or collected until an explicit commit or rollback.
- **Commit and Rollback:** Applies pending changes from editors, data viewers, execution workflows, and connection sessions.
- **Resource Monitor:** Shows connection sessions, open resources, pending transaction sources, and session-level commit or rollback actions.
- **Session Management:** Lets users choose built-in or custom DBN sessions for editors and inspect live database sessions in the Session Browser.
- **Pending Transactions:** Tracks uncommitted changes by connection, session, and source file so DBN can warn before risky actions.

> **Note:** Available transaction and session actions depend on database support, connection settings, JDBC driver behavior, session permissions, and whether auto-commit is enabled.

## Contents

<!-- TOC -->
- [When To Use It](#when-to-use-it)
- [Access Paths](#access-paths)
- [Transaction Model](#transaction-model)
- [Auto-Commit](#auto-commit)
- [Commit and Rollback](#commit-and-rollback)
- [Pending Transactions](#pending-transactions)
- [Resource Monitor](#resource-monitor)
- [Session Management](#session-management)
- [Session Browser](#session-browser)
- [Common Workflows](#common-workflows)
  - [Review Pending Changes Before Committing](#review-pending-changes-before-committing)
  - [Use A Separate Session For A File](#use-a-separate-session-for-a-file)
  - [Inspect Live Database Sessions](#inspect-live-database-sessions)
  - [Switch Auto-Commit Safely](#switch-auto-commit-safely)
- [Troubleshooting](#troubleshooting)
- [Related Documentation](#related-documentation)
<!-- /TOC -->

## When To Use It

Use Transaction Management when you need to:

- Control whether data changes are committed automatically or manually.
- Commit or roll back work from SQL editors, table editors, JSON editors, or execution workflows.
- Review pending changes before disconnecting, closing the project, or changing auto-commit.
- Monitor connection sessions, pooled resources, and open transactions.
- Assign editor work to a specific DBN session.
- Inspect or interrupt live database sessions when the database supports it.

## Access Paths

- **Auto-commit setting:** `DB Navigator` -> `Settings` -> `Connections` -> selected connection -> `Properties`.
- **Auto-commit toggle:** Connection context actions in DBN connection lists and browser surfaces.
- **Commit or rollback from editor:** SQL editor, table editor, and JSON editor toolbars.
- **Resource Monitor:** `DB Navigator` -> `Resource Monitor`.
- **Session Browser:** `DB Navigator` -> `Open Session Browser...` or Database Browser toolbar -> `Open Session Browser`.
- **Session selector:** File editor toolbar -> `Current Session`.
- **Transaction prompt settings:** `DB Navigator` -> `Settings` -> `Operations` -> `Transaction Manager`.
- **Session Browser settings:** `DB Navigator` -> `Settings` -> `Operations` -> `Session Browser`.

## Transaction Model

DBN transaction handling is connection-aware and session-aware. A single configured connection can have several runtime JDBC connections behind it, including the main session, custom sessions, pool connections, and debugger-related sessions when debugging is supported.

- **Connection:** The configured database connection shown in DBN settings and the Database Browser.
- **DBN session:** A named session target such as `Main`, `Pool`, `Debug`, `Debugger`, `Assistant`, or a custom user-created session.
- **JDBC connection:** The live database connection used for a specific DBN session or pooled operation.
- **Pending transaction:** A tracked source of uncommitted changes on a non-auto-commit JDBC connection.
- **Source file:** The editor, table, JSON view, or database file that produced tracked changes.

When auto-commit is disabled, DBN records data changes against the live connection and the source file that caused them. Commit and rollback actions apply at the active connection/session level, which means one action may affect several pending changes on the same transaction.

## Auto-Commit

Auto-Commit controls whether JDBC commits each change immediately. DBN connections default to auto-commit disabled, which gives users explicit control over commit and rollback.

- **Auto-Commit ON:** Changes are committed by the database immediately after each operation. Commit and rollback actions are hidden or inactive where they do not apply.
- **Auto-Commit OFF:** Changes remain pending until the user commits, rolls back, disconnects, switches auto-commit, or closes the project.

DBN shows auto-commit state in editor surfaces such as SQL consoles, table editors, and JSON editors. The label indicates whether changes will be committed automatically or require a manual commit.

Switching auto-commit ON while there are pending changes is treated as a transaction decision. Depending on settings, DBN can ask whether to commit, roll back, or review pending changes before enabling auto-commit.

## Commit and Rollback

Commit and rollback actions are available in places where DBN has an active connection/session and auto-commit is disabled.

- **SQL editor toolbar:** Commits or rolls back the transaction for the editor target connection/session.
- **Table editor toolbar:** Commits or rolls back pending data editor changes on the target session.
- **JSON editor toolbar:** Commits or rolls back pending JSON editor changes on the target session.
- **Connection actions:** Commit or roll back all tracked pending changes for the selected connection sessions.
- **Resource Monitor:** Commits or rolls back the selected session connection.
- **Project close or disconnect:** Prompts when DBN detects uncommitted changes.

If a commit or rollback from an editor would affect more than the current file, DBN can warn that several changes are involved and offer a review step.

## Pending Transactions

Pending transactions are tracked so DBN can explain what will be affected before a broad commit, rollback, auto-commit switch, disconnect, or project close.

- **Source:** Identifies the file or database editor that produced the uncommitted change.
- **Session:** Identifies the DBN session associated with the live connection.
- **Count:** Shows the number of tracked changes for that source/session pair.
- **Review:** Opens pending transaction dialogs so users can inspect affected sources before choosing commit or rollback.

Pending transaction review is especially useful when the same database session has changes from multiple editors or data viewers.

## Resource Monitor

The Resource Monitor is a non-modal dialog for reviewing DBN connection resources and open transactions. It lists configured connections on the left and shows details for the selected connection on the right.

- **Connection list:** Shows configured DBN connections and lets users switch between them.
- **Sessions table:** Lists DBN sessions for the selected connection, including status and resource counters.
- **Open transactions table:** Shows pending transaction sources and details for the selected session.
- **Commit and Rollback:** Applies the transaction action to the selected session connection.
- **Session actions:** Disconnect the selected session, or rename and delete custom sessions when allowed.

The sessions table includes session name, status, last access, open connection count and peak count, open cursors, and cached statements. The open transactions table shows the source and details for each pending transaction entry.

## Session Management

Session Management lets DBN route editor and execution work through named sessions. This is useful when users want separate transaction scopes for different files, long-running work, debugging, or background operations.

- **Main:** The default session for ordinary editor and data workflows.
- **Pool:** The session entry representing pooled connection activity.
- **Debug and Debugger:** Sessions shown when the connection supports debugging.
- **Assistant:** Assistant-related session shown when assistant features are supported.
- **Custom:** User-created sessions that can be selected from editor session controls and persisted in the project.

When session management is enabled for a connection, DBN shows a `Current Session` selector in supported file editors. The selector lets users choose an existing session, create a new custom session, or disable session support for the connection.

## Session Browser

The Session Browser displays live database sessions returned by the database metadata layer. It is separate from the Resource Monitor: Resource Monitor focuses on DBN-side connection resources, while Session Browser focuses on database-side sessions.

- **Reload:** Refreshes the database session list.
- **Timed reload:** Refreshes every 5, 10, 20, 30, or 60 seconds.
- **Filtering:** Narrows sessions by user, host, or status.
- **Search:** Opens search for the session table.
- **Details:** Shows selected session details and current SQL when supported.
- **Explain Plan:** Shows an explain-plan tab when the database supports explain plan.
- **Disconnect and Kill:** Interrupts selected database sessions when the database and permissions allow it.

Disconnect and kill actions can be immediate, normal, or post-transaction depending on database support and Session Browser settings.

## Common Workflows

### Review Pending Changes Before Committing

1. Open `DB Navigator` -> `Resource Monitor`.
2. Select the connection.
3. Select the session with pending changes.
4. Review the open transaction sources.
5. Click `Commit` or `Rollback`.

### Use A Separate Session For A File

1. Open a SQL file or console with a live DBN connection.
2. Open the `Current Session` selector in the editor toolbar.
3. Select an existing session or choose `New Session`.
4. Run statements or edit data using that session.
5. Commit or roll back that session when the work is complete.

### Inspect Live Database Sessions

1. Open `DB Navigator` -> `Open Session Browser...`.
2. Select the connection if DBN shows a connection chooser.
3. Use reload or timed reload to refresh the session list.
4. Filter by user, host, or status.
5. Select a session to inspect details and current SQL.

### Switch Auto-Commit Safely

1. Check the auto-commit label in the editor or data viewer.
2. If pending changes exist, open Resource Monitor or the pending transaction review dialog.
3. Commit or roll back the affected transaction.
4. Toggle auto-commit after the transaction is resolved.

## Troubleshooting

| Symptom | Resolution |
| --- | --- |
| Commit and rollback are hidden | Auto-commit may be enabled, or no supported transaction context is active. |
| Commit affects more than the current file | The same connection/session has pending changes from more than one source. Review pending transactions first. |
| Auto-commit switch prompts for action | The connection has uncommitted changes and DBN needs a commit, rollback, or review decision. |
| Session selector is missing | Confirm `Enable session management` is enabled for the connection. |
| Custom session cannot be deleted or renamed | The selected session may be built-in, or the session connection has pending changes. |
| Session Browser cannot disconnect sessions | The database may not support session disconnect, or the user may lack permission. |
| Session Browser filters do not reload data | Enable `Reload sessions on filter change` or reload manually after changing filters. |

## Related Documentation

- [Transaction Management Settings](./transaction-management-settings.md): Configure auto-commit, transaction prompts, Resource Monitor behavior, and Session Browser options.
- [Database Events](./database-events.md): Guide to monitoring table data-change notifications from Oracle sessions and applications.
- [Connection Management](./connection-management.md): Create and configure connections used by transaction and session workflows.
- [Connection Management Settings](./connection-management-settings.md): Configure connection properties, auto-commit defaults, session management, and pooling.
- [Execution Engine](./execution-engine.md): Run statements, scripts, methods, and Java methods that may participate in transactions.
- [Data Viewers and Editors](./data-viewers-and-editors.md): Edit table and JSON data that may require commit or rollback.
- [Database Assistant](./database-assistant.md): Guide to assistant workflows that use assistant sessions and generated SQL.
