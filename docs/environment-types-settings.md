# Environment Types Settings

## Summary

Environment Types settings define project-level database environment classifications, their colors, and their read-only behavior. Connection settings then assign one environment type to each DBN connection. The same General settings page also controls regional formatting and native file chooser behavior.

| Setting area | Scope | Purpose |
| --- | --- | --- |
| Regional Settings | Project | Controls locale, number, date, and time formatting used by DBN displays. |
| Environment Types | Project | Defines environment names, descriptions, read-only flags, and colors. |
| Environment Applicability | Project | Controls where environment colors are displayed. |
| Environment Options | Project | Controls native file chooser usage. |
| Connection Details | Connection | Assigns an environment type to a connection. |
| Editor notifications | Object/editor | Allows temporary edit mode for read-only data or code. |

> **Note:** Environment Types settings live under General settings because they apply across Database Browser, editors, execution results, dialogs, and connection mappings.

## Contents

<!-- TOC -->
- [Access Paths](#access-paths)
- [Regional Settings](#regional-settings)
- [Environment Types Table](#environment-types-table)
  - [Table Actions](#table-actions)
- [Default Environment Types](#default-environment-types)
- [Environment Applicability](#environment-applicability)
- [Connection Details Setting](#connection-details-setting)
- [Read-Only Behavior](#read-only-behavior)
- [Environment Options](#environment-options)
- [Validation Messages](#validation-messages)
- [Troubleshooting](#troubleshooting)
- [Related Documentation](#related-documentation)
<!-- /TOC -->

## Access Paths

- **General settings:** `DB Navigator` -> `Settings` -> `General`
- **Regional Settings:** `DB Navigator` -> `Settings` -> `General` -> `Regional Settings`
- **Environment Types:** `DB Navigator` -> `Settings` -> `General` -> `Environment Types`
- **Environment Applicability:** `DB Navigator` -> `Settings` -> `General` -> `Environment Applicability`
- **Environment Options:** `DB Navigator` -> `Settings` -> `General` -> `Environment Options`
- **Connection environment assignment:** `DB Navigator` -> `Settings` -> `Connections` -> selected connection -> `Details` -> `Environment type`
- **Environment settings from editor notification:** Read-only data, JSON, or source editor notification -> `Settings`

![General settings page](https://raw.githubusercontent.com/oracle/database-navigator/master/docs/img/settings-general.png)

## Regional Settings

Regional Settings control how DBN formats numbers, dates, and times in its own UI surfaces.

![Regional settings](https://raw.githubusercontent.com/oracle/database-navigator/master/docs/img/settings-general-regional.png)

| Setting | Default | Description |
| --- | --- | --- |
| `Locale` | `System default` | Selects the locale used for DBN number, date, and time formatting. |
| `Use preset patterns` | Enabled | Uses predefined number and date formats for the selected locale. |
| `Number format` | `Ungrouped` | Selects the preset number format. |
| `Date format` | `Medium` | Selects the preset date format. |
| `Use custom patterns` | Disabled | Enables custom number, date, and time pattern fields. |
| Custom pattern fields | Disabled until custom patterns are selected | Define number, date, and time formats manually. |
| `Preview` | Live preview | Shows the effect of the selected regional settings before saving. |

## Environment Types Table

The Environment Types table defines the available environments for the project.

![Environment types and applicability settings](https://raw.githubusercontent.com/oracle/database-navigator/master/docs/img/settings-general-environments.png)

- **`Name`:** Display name used in connection settings and notifications. Required.
- **`Description`:** Optional explanation of the environment.
- **`Readonly Data`:** Makes data and JSON editing read-only for connections using this environment.
- **`Readonly Code`:** Makes database source editing read-only for connections using this environment.
- **`Color`:** Color used on enabled DBN UI surfaces.

### Table Actions

- **`Add`:** Adds a new custom environment type.
- **`Remove`:** Removes the selected environment type from the project configuration.
- **`Move Up` / `Move Down`:** Changes environment type order in selection lists.
- **`Revert Changes`:** Restores the default environment type list.

Every environment type must have a name before settings can be saved.

## Default Environment Types

| Environment type | Description | Readonly Data | Readonly Code |
| --- | --- | --- | --- |
| `Development` | Development environment. | Disabled | Disabled |
| `Integration` | Integration environment. | Disabled | Enabled |
| `Production` | Productive environment. | Enabled | Enabled |
| `Other` | General-purpose environment. | Disabled | Disabled |

DBN also uses a neutral default fallback when an environment type cannot be resolved.

## Environment Applicability

Environment Applicability controls where DBN displays environment colors.

| Setting | Default | Description |
| --- | --- | --- |
| `On connection tabs` | Enabled | Colors Database Browser connection tabs and selector headers. |
| `On object editor tabs` | Enabled | Colors tabs for database object editors. |
| `On script editor tabs` | Disabled | Colors local SQL and PSQL file editor tabs when they have DBN connection context. |
| `On dialog headers` | Enabled | Colors dialog headers that carry connection or object context. |
| `On execution result tabs` | Enabled | Colors execution result tabs by the result connection environment. |

Disabling a visibility option does not remove environment behavior. It only hides the color cue on that UI surface.

## Connection Details Setting

Each connection chooses an environment type from the connection `Details` tab.

| Setting | Default | Description |
| --- | --- | --- |
| `Environment type` | `Default` | Selects the environment classification used by DBN coloring and read-only behavior. |

The selected environment applies to the connection's browser nodes, object editors, data editors, source editors, mapped files, execution results, and dialogs.

## Read-Only Behavior

Read-only flags apply through the connection environment.

| Read-only flag | Affected content | Behavior |
| --- | --- | --- |
| `Readonly Data` | Data and JSON content | Prevents accidental edits in Table Editors and JSON Editors for objects in that environment. |
| `Readonly Code` | Source code, package specs, package bodies, type specs, type bodies, and related code content | Prevents accidental source edits in Program Editors and Java/source-backed editors for objects in that environment. |

When an editor is read-only because of an environment type, DBN can show a notification with `Edit mode`, `Cancel editing`, and `Settings` actions. `Edit mode` is a temporary per-object override; it does not change the environment type definition.

## Environment Options

Environment Options control cross-cutting General settings that are not tied to a single connection or editor.

| Setting | Default | Description |
| --- | --- | --- |
| `Use native file choosers` | Enabled | Uses operating-system native file chooser dialogs where available. |

## Validation Messages

| Message area | Common cause | Resolution |
| --- | --- | --- |
| Environment type names | One or more environment rows have no name | Enter a name for each environment type. |
| Unexpected read-only editor | Connection uses an environment with `Readonly Data` or `Readonly Code` enabled | Change the connection environment, change the environment flags, or use temporary `Edit mode`. |
| Missing color cue | Visibility option is disabled for that surface | Enable the relevant Environment Applicability option. |
| Script editor tab not colored | Script tab coloring is disabled or the file has no DBN connection context | Enable `On script editor tabs` and select or inherit a connection for the file. |

## Troubleshooting

| Symptom | Resolution |
| --- | --- |
| Production connection still allows data edits | Confirm the connection is assigned to `Production` and `Readonly Data` is enabled for that environment. |
| Integration source is editable | Confirm the connection is assigned to `Integration` and `Readonly Code` is enabled. |
| Environment type does not appear in connection settings | Save the General settings after adding the environment type. |
| Removed environment is still referenced | Reassign affected connections to an existing environment type. |
| Colors look different in dark theme | Environment types keep separate light and dark color values. Pick the color while using the theme you want to tune. |

## Related Documentation

- [Environment Types](./environment-types.md): Feature guide for environment classification, colors, and read-only safeguards.
- [Connection Management Settings](./connection-management-settings.md): Settings reference for assigning environment types to connections.
- [Data Viewers and Editors Settings](./data-viewers-and-editors-settings.md): Settings reference for data editor behavior affected by environment read-only data.
- [Code Editors Settings](./code-editors-settings.md): Settings reference for source editor behavior affected by environment read-only code.
- [Workspace Integration Settings](./workspace-integration-settings.md): Settings reference for file mappings that show connection environment context.
