# Transaction Management Settings

## Summary

Transaction Management settings are spread across connection settings and operation settings. Connection settings define auto-commit and session-management defaults. Operation settings define what DBN should do when uncommitted changes are encountered and how the Session Browser behaves.

| Setting area | Scope | Purpose |
| --- | --- | --- |
| Connection Properties | Connection | Defines the default auto-commit state. |
| Connection Details | Connection | Enables session management and controls resource-related connection behavior. |
| Operations - Transaction Manager | Project | Controls prompts for uncommitted changes and multi-source transactions. |
| Operations - Session Browser | Project | Controls Session Browser reload and interruption defaults. |
| Execution dialogs | Per run | Can commit after statement, method, or Java execution when the option is selected. |

## Contents

<!-- TOC -->
- [Access Paths](#access-paths)
- [Auto-Commit Setting](#auto-commit-setting)
- [Session Management Settings](#session-management-settings)
- [Transaction Manager Settings](#transaction-manager-settings)
  - [Uncommitted Changes Handling](#uncommitted-changes-handling)
  - [Multiple Open Transactions Handling](#multiple-open-transactions-handling)
  - [Transaction Options](#transaction-options)
- [Session Browser Settings](#session-browser-settings)
  - [Session Interruption Options](#session-interruption-options)
- [Resource Monitor Settings](#resource-monitor-settings)
- [Execution Settings That Affect Transactions](#execution-settings-that-affect-transactions)
- [Validation and Prompts](#validation-and-prompts)
- [Troubleshooting](#troubleshooting)
- [Related Documentation](#related-documentation)
<!-- /TOC -->

## Access Paths

- **Auto-commit:** `DB Navigator` -> `Settings` -> `Connections` -> selected connection -> `Properties`.
- **Enable session management:** `DB Navigator` -> `Settings` -> `Connections` -> selected connection -> `Details`.
- **Transaction prompts:** `DB Navigator` -> `Settings` -> `Operations` -> `Transaction Manager`.
- **Session Browser behavior:** `DB Navigator` -> `Settings` -> `Operations` -> `Session Browser`.
- **Commit after execution:** Statement, method, and Java execution input dialogs or Execution Engine settings.

## Auto-Commit Setting

The connection `Properties` tab controls the default JDBC auto-commit state for the connection.

| Setting | Default | Description |
| --- | --- | --- |
| `Auto-commit` | Disabled | Enables JDBC auto-commit for the connection when selected. |

When auto-commit is disabled, DBN exposes commit and rollback actions in supported editors and data viewers. When auto-commit is enabled, the database commits each change immediately and manual commit or rollback usually does not apply.

## Session Management Settings

The connection `Details` tab controls whether DBN session selection is available for the connection.

| Setting | Default | Description |
| --- | --- | --- |
| `Enable session management` | Enabled | Shows DBN session selection in supported editors and allows custom DBN sessions for the connection. |
| `Idle time to disconnect` | 30 minutes | Disconnects idle main connections after the configured time. |
| `Idle time to disconnect pool` | 5 minutes | Disconnects idle pooled connections after the configured time. |
| `Maximum connection pool size` | 7 | Limits the number of pooled connections for the connection. |

If session management is disabled, the `Current Session` selector is hidden for that connection. DBN still uses internal sessions such as pool or debug sessions when required by plugin workflows.

## Transaction Manager Settings

Transaction Manager settings decide what DBN does when an action encounters uncommitted changes.

### Uncommitted Changes Handling

| Setting | Default | Available Saved Options | Description |
| --- | --- | --- | --- |
| `on Project Close` | Ask | Ask, Commit, Rollback, Review changes | Used when the project is closing and one or more connections have uncommitted changes. |
| `on Disconnect` | Ask | Ask, Commit, Rollback, Review changes | Used when disconnecting a connection that has uncommitted changes. |
| `on Auto-Commit Switch` | Ask | Ask, Commit, Rollback, Review changes | Used when switching auto-commit ON while changes are pending. |

### Multiple Open Transactions Handling

| Setting | Default | Available Saved Options | Description |
| --- | --- | --- | --- |
| `on Commit` | Ask | Ask, Commit, Review changes | Used when a commit action from one editor would affect other pending changes on the same connection/session. |
| `on Rollback` | Ask | Ask, Rollback, Review changes | Used when a rollback action from one editor would affect other pending changes on the same connection/session. |

Runtime prompts can also offer `Cancel` so the user can abandon the current action without changing the transaction.

### Transaction Options

- **Ask:** Show a prompt at the moment the transaction decision is needed.
- **Commit:** Commit the pending changes.
- **Rollback:** Roll back the pending changes.
- **Review changes:** Open the pending transaction review before committing or rolling back.
- **Cancel:** Runtime prompt option that leaves the transaction unchanged and cancels the triggering action.

## Session Browser Settings

Session Browser settings control reload behavior and the default prompt options for disconnecting or killing database sessions.

| Setting | Default | Available Saved Options | Description |
| --- | --- | --- | --- |
| `Reload sessions on filter change` | Disabled | Enabled/disabled | Reloads database sessions when user, host, or status filters change. When disabled, filtering updates the current table view until the user reloads. |
| `Session "Disconnect" option` | Ask | Ask, Immediate, Post Transaction | Default option used when disconnecting selected database sessions. |
| `Session "Kill" option` | Ask | Ask, Normal, Immediate | Default option used when killing selected database sessions. |

Runtime prompts can also offer `Cancel`.

### Session Interruption Options

| Option | Applies to | Meaning |
| --- | --- | --- |
| Ask | Disconnect, Kill | Prompt each time the action is used. |
| Normal | Kill | Requests normal session termination. |
| Immediate | Disconnect, Kill | Requests immediate interruption when the database supports it. |
| Post Transaction | Disconnect | Requests disconnect after the current transaction finishes when supported. |
| Cancel | Runtime prompt | Cancels the interruption action. |

If the database does not support session interruption timing, DBN falls back to the available session termination behavior.

## Resource Monitor Settings

Resource Monitor does not have a dedicated settings page. It uses the active connection state, DBN session list, pending transaction tracking, and Transaction Manager prompt settings.

- **Whether changes need commit or rollback:** Controlled by the connection `Auto-commit` setting and live JDBC transaction state.
- **Whether custom sessions can be used:** Controlled by the connection `Enable session management` setting.
- **Commit or rollback prompts:** Controlled by Operations -> `Transaction Manager`.
- **Session list and resource counters:** Controlled by live DBN connection/session state.

## Execution Settings That Affect Transactions

Execution workflows can participate in transaction handling.

| Setting | Location | Description |
| --- | --- | --- |
| `Commit after execution` | Statement, method, and Java execution input/settings | Commits automatically after execution when selected and auto-commit is not active. |
| `Session` | Statement, method, and Java execution input dialogs | Chooses the target session for the execution when session selection is available. |
| `Connection` | Execution input dialogs | Chooses the target connection for statement or script execution. |

For the full execution reference, see [Execution Engine Settings](./execution-engine-settings.md).

## Validation and Prompts

- **Project closes with uncommitted changes:** Uses the `on Project Close` Transaction Manager option.
- **Connection disconnects with uncommitted changes:** Uses the `on Disconnect` Transaction Manager option.
- **Auto-commit is switched ON with pending changes:** Uses the `on Auto-Commit Switch` Transaction Manager option.
- **Commit affects several pending sources:** Uses the `on Commit` multiple-open-transactions option.
- **Rollback affects several pending sources:** Uses the `on Rollback` multiple-open-transactions option.
- **Session Browser disconnects or kills sessions:** Uses Session Browser interruption settings when timing options are supported.

## Troubleshooting

| Symptom | Resolution |
| --- | --- |
| Commit prompt appears too often | Change the relevant Transaction Manager option from `Ask` to the desired saved behavior. |
| Review dialog appears before commit or rollback | Multiple pending sources exist for the same transaction; review them or change the multiple-open-transactions setting. |
| Auto-commit setting does not take effect | Confirm the JDBC driver and database support changing auto-commit for the active connection. |
| Session selector is hidden | Enable session management for the connection under the `Details` tab. |
| Session Browser filter changes do not query the database | Enable `Reload sessions on filter change`, or reload manually after changing filters. |
| Disconnect or kill option is unavailable | The database feature or user permissions may not support the requested session interruption. |

## Related Documentation

- [Transaction Management](./transaction-management.md): Manage auto-commit, pending transactions, Resource Monitor, and Session Browser workflows.
- [Connection Management Settings](./connection-management-settings.md): Configure connection properties, details, pooling, and session management.
- [Execution Engine Settings](./execution-engine-settings.md): Configure execution options that can commit after execution.
- [Data Viewers and Editors Settings](./data-viewers-and-editors-settings.md): Configure data editors that can create pending changes.
