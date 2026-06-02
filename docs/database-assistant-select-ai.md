# Database Assistant: Oracle Select AI

## Summary

Oracle Select AI is the database-native Database Assistant path for Oracle connections. DBN sends prompts through the connected Oracle database, and the database uses `DBMS_CLOUD_AI`, database AI profiles, database credentials, network ACLs, and package privileges to call the configured AI provider.

Oracle Select AI is separate from the generic DBN assistant. It does not use DBN provider profiles, generic tool approvals, MCP servers, or `RAG` mode.

## Contents

<!-- TOC -->
- [When To Use It](#when-to-use-it)
- [Access Paths](#access-paths)
- [How It Works](#how-it-works)
- [Select AI Prompt Actions](#select-ai-prompt-actions)
- [Editor Integration](#editor-integration)
- [Profile And Credential Management](#profile-and-credential-management)
- [Common Workflows](#common-workflows)
  - [Start Oracle Select AI](#start-oracle-select-ai)
  - [Generate SQL From An Editor Prompt](#generate-sql-from-an-editor-prompt)
  - [Review Select AI Setup](#review-select-ai-setup)
- [Troubleshooting](#troubleshooting)
- [Related Documentation](#related-documentation)
<!-- /TOC -->

## When To Use It

Use Oracle Select AI when you need to:

- Generate SQL through Oracle's database-native Select AI feature.
- Explain generated SQL using the selected database AI profile.
- Execute or narrate generated SQL through the database profile context.
- Manage database AI profiles and credentials from DBN.
- Use SQL or PSQL editor prompt actions backed by Select AI.
- Keep provider access and exposed database objects controlled by database-side configuration.

Use the generic assistant instead when you need DBN-managed provider profiles, DBN tools, MCP tools, editor and dataset actions, or vector-search `RAG`.

## Access Paths

- **DB Assistant tool window:** Open the `DB Assistant` tool window for an Oracle connection that supports Select AI.
- **Profile setup:** `DB Assistant` toolbar -> `Profile Setup...`
- **Select AI help:** `DB Assistant` toolbar -> `Help` when Select AI is active.
- **Editor actions:** SQL/PSQL editor context menu or Generate action -> `Select AI - Generate SQL` or `Select AI - Generate and Explain SQL`.
- **Editor notification:** Supported Oracle SQL files can show `Configure`, `Chat`, and `Dismiss`.
- **Database Browser:** AI profiles, AI models, and credentials appear as database objects where supported.
- **Settings reference:** `DB Navigator` -> `Settings` -> `Assistant`.

## How It Works

- **Database backend:** DBN calls Oracle assistant operations through the database assistant interface backed by `DBMS_CLOUD_AI`.
- **Database profiles:** AI profiles are database objects. They define provider, model, credential, enabled state, interaction mode, temperature, and exposed objects.
- **Database credentials:** Credentials are database objects used by Oracle Select AI to call the provider endpoint.
- **Profile context:** Before executing a Select AI statement, DBN sets the current database profile for the execution connection.
- **Assistant session:** Interactive assistant work uses the assistant connection/session when DBN needs a separate assistant context.

Oracle Select AI requires database-side setup. Unless your organization has already provisioned it, the database user needs execute privileges for `DBMS_CLOUD` and `DBMS_CLOUD_AI`, network access to the selected provider host, provider credentials, and at least one enabled AI profile.

## Select AI Prompt Actions

- **`Show SQL`:** Generates a SQL statement from a natural language prompt.
- **`Explain SQL`:** Generates SQL and explains the statement.
- **`Execute SQL`:** Generates and executes SQL from the prompt.
- **`Narrate`:** Generates SQL and explains the output in natural language.
- **`Chat`:** Sends the prompt directly to the model through the selected Select AI profile.

These actions are tied to the selected Select AI profile and model. If no profile is available, DBN prompts you to create one before continuing.

## Editor Integration

Select AI editor actions can use:

- **Selected text:** The selected editor text becomes the assistant prompt.
- **Prompt comment:** A sufficiently descriptive SQL or PSQL comment can become the prompt.
- **Select AI statement:** A parsed assistant statement can provide the prompt text for execution.

The editor action result is inserted or displayed in the editor context depending on the action. Select AI editor actions are Oracle Select AI features, not generic assistant features.

## Profile And Credential Management

DBN can help manage Select AI profiles and credentials when the connected user has the required database privileges.

- **Credentials:** Create, edit, enable, disable, delete, and reload database credentials used by Select AI profiles.
- **Profiles:** Create, edit, enable, disable, delete, reload, and mark default database AI profiles.
- **Associated objects:** Choose the tables and views exposed to a profile for natural language SQL generation.
- **Help and grants:** Review prerequisite grants and network ACL scripts from Select AI help where available.

The selected Select AI profile controls which database objects are visible to natural language SQL generation. If generated SQL ignores expected objects, check the profile's associated objects first.

## Common Workflows

### Start Oracle Select AI

1. Open the `DB Assistant` tool window for an Oracle connection that supports Select AI.
2. If prompted, review the Select AI introduction and prerequisites.
3. Open `Profile Setup...` when no profile is available or when profiles need changes.
4. Select the active AI profile and model.
5. Choose `Show SQL`, `Explain SQL`, `Execute SQL`, `Narrate`, or `Chat`.
6. Enter a natural language prompt and review the result.

### Generate SQL From An Editor Prompt

1. Open a DBN SQL or PSQL file mapped to an Oracle Select AI connection.
2. Select a natural language prompt or place the caret on a prompt comment.
3. Use `Select AI - Generate SQL` or `Select AI - Generate and Explain SQL`.
4. Review the generated SQL before running or saving it.

### Review Select AI Setup

1. Open `DB Assistant` for the Oracle connection.
2. Open `Profile Setup...` to review profiles and credentials.
3. Confirm the selected profile is enabled and has associated tables or views.
4. Use Select AI help to review package grants and network ACL requirements.
5. Reload profiles after database-side changes.

## Troubleshooting

| Symptom | Resolution |
| --- | --- |
| Oracle Select AI is unavailable | Confirm the connection is Oracle, the database supports Select AI, and the user can access the assistant packages. |
| Select AI reports missing profiles | Create at least one AI profile with `Profile Setup...`, then reload profiles. |
| Select AI profile setup is unavailable | Confirm the connection supports Select AI and that assistant availability has initialized successfully. |
| Select AI profile does not appear | Reload profiles and refresh the Database Browser AI profile list. |
| Select AI credential cannot be created | Confirm the connected user has privileges to create the credential and that all provider fields are valid. |
| Select AI invocation fails with privileges error | Grant execute privileges on `DBMS_CLOUD` and `DBMS_CLOUD_AI`, or ask a database administrator to provision them. |
| Select AI reports network access denied | Grant the database user network ACL access to the selected provider host. |
| Select AI provider call fails with `ORA-24247` | Grant network ACL access for the database user to the selected provider host. |
| Generated SQL ignores expected tables | Review the selected Select AI profile and confirm the required tables or views are exposed by that profile. |

## Related Documentation

- [Database Assistant](./database-assistant.md): Overview of Oracle Select AI and the generic assistant.
- [Database Assistant: Oracle Select AI Settings](./database-assistant-select-ai-settings.md): Settings reference for Select AI profiles, credentials, prerequisites, and database security.
- [Database Assistant Settings](./database-assistant-settings.md): Overview of Database Assistant settings areas.
- [Database Browser](./database-browser.md): Guide to AI profile, AI model, and credential objects visible in supported databases.
- [Code Editors](./code-editors.md): Guide to SQL and PSQL editors where Select AI prompt actions can appear.
- [Execution Engine](./execution-engine.md): Guide to statement execution and result viewers used with generated SQL.
- [Connection Management](./connection-management.md): Guide to Oracle connections that provide Select AI support detection.
