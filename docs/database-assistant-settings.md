# Database Assistant Settings

## Summary

Database Assistant settings are split between DBN project configuration for the generic assistant and Oracle database-native configuration for Select AI. This page is the settings overview; use the dedicated settings pages for detailed fields, validation messages, and troubleshooting.

| Setting area | Scope | Purpose |
| --- | --- | --- |
| Generic assistant settings | Project | Configure DBN-managed provider credentials, assistant profiles, MCP servers, and tool approvals. |
| Oracle Select AI settings | Database | Configure database-side Select AI profiles, credentials, prerequisites, and provider access. |

## Contents

<!-- TOC -->
- [Access Paths](#access-paths)
- [Settings Pages](#settings-pages)
- [Choosing A Settings Page](#choosing-a-settings-page)
- [Troubleshooting](#troubleshooting)
- [Related Documentation](#related-documentation)
<!-- /TOC -->

## Access Paths

- **Assistant project settings:** `DB Navigator` -> `Settings` -> `Assistant`.
- **Generic assistant settings:** `DB Assistant` toolbar -> `Settings...`.
- **Generic tool approvals:** `DB Assistant` prompt/tool approval dialog, `Tools`, `MCP Servers`, or `Tool Approvals...`.
- **Oracle Select AI profile setup:** `DB Assistant` toolbar -> `Profile Setup...`.
- **Oracle Select AI help:** `DB Assistant` toolbar -> `Help` when Select AI is active.
- **Oracle Select AI object management:** Database Browser -> AI profiles, credentials, and related object context actions where supported.

## Settings Pages

- **[Generic Assistant Settings](./database-assistant-generic-settings.md):** Use this page for DBN-managed assistant credentials, provider profiles, MCP servers, built-in tool approvals, and MCP tool approvals.
- **[Oracle Select AI Settings](./database-assistant-select-ai-settings.md):** Use this page for database AI profiles, database credentials, `DBMS_CLOUD_AI` prerequisites, network ACLs, and database-side validation.

## Choosing A Settings Page

- **Configure provider API access from DBN:** Use Generic Assistant Settings.
- **Configure external MCP tools:** Use Generic Assistant Settings.
- **Review or block assistant tools:** Use Generic Assistant Settings.
- **Configure Oracle database AI profiles:** Use Oracle Select AI Settings.
- **Configure Oracle database credentials for Select AI:** Use Oracle Select AI Settings.
- **Resolve `DBMS_CLOUD_AI` grants or network ACLs:** Use Oracle Select AI Settings.

## Troubleshooting

| Symptom | Resolution |
| --- | --- |
| Unsure which settings page applies | Use Generic Assistant Settings for DBN-managed provider profiles, MCP servers, and tools. Use Oracle Select AI Settings for database-native profiles, credentials, grants, and network ACLs. |
| Generic assistant has no profile | Create a DBN Assistant profile and credential in Generic Assistant Settings. |
| Select AI profile setup is unavailable | Review Oracle Select AI Settings and confirm the connection supports Select AI. |
| MCP tools do not appear | Review MCP server configuration and tool approvals in Generic Assistant Settings. |

## Related Documentation

- [Database Assistant: Generic Assistant Settings](./database-assistant-generic-settings.md): Settings reference for DBN-managed assistant profiles, credentials, tools, MCP servers, and `RAG` controls.
- [Database Assistant: Oracle Select AI Settings](./database-assistant-select-ai-settings.md): Settings reference for database-native Select AI profiles, credentials, prerequisites, and database security.
- [Database Assistant](./database-assistant.md): Overview of Oracle Select AI and the generic assistant.
- [Database Assistant: Generic Assistant](./database-assistant-generic.md): Guide to generic assistant profiles, tools, MCP support, and `RAG` mode.
- [Database Assistant: Oracle Select AI](./database-assistant-select-ai.md): Guide to database-native Select AI workflows, profiles, credentials, prerequisites, and editor actions.
- [MCP Server Builder](./mcp-server-builder.md): Guide to SQL-backed MCP server generation and registration handoff.
