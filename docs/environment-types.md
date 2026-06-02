# Environment Types

## Summary

Environment Types classify DBN connections by purpose, risk, and visual identity. They help users distinguish development, integration, production, and other database environments while applying optional read-only safeguards for data and code editors.

- **Environment classification:** Labels a connection as Development, Integration, Production, Other, or a custom environment type.
- **Environment colors:** Applies connection colors to selected DBN tabs, headers, and result surfaces.
- **Readonly Data:** Protects data editors, JSON editors, and related data actions from accidental changes.
- **Readonly Code:** Protects source editors from accidental database code changes.
- **Connection assignment:** Applies one configured environment type to each DBN connection.

> **Note:** Environment Types are project-level definitions, but they affect connection-owned objects and editors. A database object inherits its environment from the connection it belongs to.

## Contents

<!-- TOC -->
- [When To Use It](#when-to-use-it)
- [Access Paths](#access-paths)
- [Environment Model](#environment-model)
- [Default Environment Types](#default-environment-types)
- [Environment Colors](#environment-colors)
- [Read-Only Safeguards](#read-only-safeguards)
  - [Temporary Edit Mode](#temporary-edit-mode)
- [Connection Assignment](#connection-assignment)
- [Common Workflows](#common-workflows)
  - [Mark A Connection As Production](#mark-a-connection-as-production)
  - [Create A Custom Environment Type](#create-a-custom-environment-type)
  - [Enable Color Cues For Script Editors](#enable-color-cues-for-script-editors)
  - [Temporarily Edit Read-Only Data](#temporarily-edit-read-only-data)
- [Troubleshooting](#troubleshooting)
- [Related Documentation](#related-documentation)
<!-- /TOC -->

## When To Use It

Use Environment Types when you need to:

- Visually separate development, test, staging, and production connections.
- Mark production or sensitive connections as read-only for data edits.
- Prevent accidental code edits on integration or production databases.
- Show environment color cues on browser tabs, editor tabs, dialog headers, or execution result tabs.
- Create organization-specific environment names beyond the built-in defaults.
- Make connection mappings and editor context easier to review by environment.

## Access Paths

- **Environment Types settings:** `DB Navigator` -> `Settings` -> `General` -> `Environment Types`
- **Environment color visibility:** `DB Navigator` -> `Settings` -> `General` -> `Environment Applicability`
- **Assign environment to connection:** `DB Navigator` -> `Settings` -> `Connections` -> selected connection -> `Details` -> `Environment type`
- **Read-only editor notification settings:** Data, JSON, or source editor notification -> `Settings`
- **Temporary data edit mode:** Read-only data or JSON editor notification -> `Edit mode`
- **Temporary code edit mode:** Read-only source editor notification -> `Edit mode`

## Environment Model

Environment Types are defined once per project and reused by connections. Each connection can select an environment type in its `Details` settings. Objects, editors, mappings, and result surfaces then resolve environment behavior from the active connection.

- **Environment type:** Named project-level classification with description, color, and read-only flags.
- **Connection environment:** The environment type selected for a DBN connection.
- **Object environment:** The environment inherited by database objects from their owning connection.
- **Editor environment:** The environment used by data, JSON, source, SQL, and result editors for color and read-only behavior.
- **Default fallback:** Neutral environment used when no configured environment type is applied.

## Default Environment Types

DBN starts with the following environment types. They can be edited, reordered, removed, or restored from Environment settings.

- **Development:** No read-only data or code restrictions.
- **Integration:** Read-only code is enabled; read-only data is disabled.
- **Production:** Read-only data and read-only code are enabled.
- **Other:** No read-only data or code restrictions.

DBN also has a neutral default fallback used when no environment type is selected or resolved.

## Environment Colors

Environment colors provide visual context for the active database target. They are especially useful when several connections are open in the same project.

- **Connection tabs:** The environment of the connection shown in a Database Browser tab.
- **Object editor tabs:** The environment of the connection owning the opened database object.
- **Script editor tabs:** The environment selected for the local SQL or PSQL file.
- **Dialog headers:** The environment of the connection or object shown in the dialog.
- **Execution result tabs:** The environment of the connection used by the execution result.

Color visibility is configurable. For example, script editor tabs can stay uncolored while connection tabs and object editor tabs remain colored.

## Read-Only Safeguards

Environment Types can prevent accidental edits in sensitive databases.

| Safeguard | Applies to | Behavior |
| --- | --- | --- |
| Readonly Data | Table Editors, JSON Editors, data import, row insert, duplicate, and delete actions | Keeps data editing locked for objects in that environment. |
| Readonly Code | Program Editors, Java Editors, and source-backed database editors | Keeps database source content read-only for objects in that environment. |

Read-only safeguards are not the same as database permissions. They are an IDE-side protection layer. Database permissions, object editability, connection status, and result-set capabilities still apply.

### Temporary Edit Mode

When an editor is read-only because of an environment type, DBN can show a notification with an `Edit mode` action. This allows an intentional, temporary override for the opened object and content type.

- **`Edit mode`:** Confirms and enables editing for the selected object content despite the environment read-only flag.
- **`Cancel editing`:** Returns the object content to environment-controlled read-only behavior.
- **`Settings`:** Opens General settings so the environment type or visibility options can be changed.

## Connection Assignment

Each connection selects its environment in the connection `Details` tab.

- **`Environment type`:** Chooses the environment classification for the connection.

The selected environment flows into the Database Browser, object editors, table and JSON editors, file mappings, execution results, and dialogs that carry connection context.

## Common Workflows

### Mark A Connection As Production

1. Open `DB Navigator` -> `Settings` -> `Connections`.
2. Select the production connection.
3. Open the `Details` tab.
4. Set `Environment type` to `Production`.
5. Apply settings and reopen or refresh affected editors if needed.

### Create A Custom Environment Type

1. Open `DB Navigator` -> `Settings` -> `General`.
2. In `Environment Types`, add a new row.
3. Enter a name and optional description.
4. Choose whether data, code, or both should be read-only.
5. Pick a color.
6. Assign the new environment type to one or more connections.

### Enable Color Cues For Script Editors

1. Open `DB Navigator` -> `Settings` -> `General`.
2. In `Environment Applicability`, enable `On script editor tabs`.
3. Open or map a SQL or PSQL file with DBN connection context.
4. Confirm the script editor tab reflects the selected connection environment.

### Temporarily Edit Read-Only Data

1. Open a table, view, or JSON view from a connection marked read-only for data.
2. Review the read-only notification.
3. Select `Edit mode`.
4. Confirm the prompt.
5. Make the intended edit, then use `Cancel editing` when finished.

## Troubleshooting

| Symptom | Resolution |
| --- | --- |
| Connection has no color | Confirm the connection has an environment type and the relevant color visibility option is enabled. |
| Script tab is not colored | Enable `On script editor tabs` and confirm the file has DBN connection context. |
| Table opens read-only | Check whether the connection environment has `Readonly Data` enabled. |
| Source editor is read-only | Check whether the connection environment has `Readonly Code` enabled. |
| Edit mode appears in a sensitive environment | The environment is read-only, but DBN allows a temporary intentional override from the notification. |
| Custom environment cannot be saved | Every environment type must have a name. |
| Color changes do not appear immediately | Reopen, refresh, or switch affected tabs if the current UI surface has not refreshed yet. |

## Related Documentation

- [Environment Types Settings](./environment-types-settings.md): Settings reference for environment definitions, colors, visibility, and connection assignment.
- [Connection Management](./connection-management.md): Guide to creating and configuring connections that receive environment types.
- [Data Viewers and Editors](./data-viewers-and-editors.md): Guide to data editors affected by read-only data safeguards.
- [Code Editors](./code-editors.md): Guide to source editors affected by read-only code safeguards.
- [Workspace Integration](./workspace-integration.md): Guide to file mappings that display the mapped connection environment.
