# Database Assistant: Oracle Select AI Settings

## Summary

Oracle Select AI settings are database-side configuration for Oracle database-native assistant workflows. They cover Select AI profiles, database credentials, package privileges, network ACLs, and provider access used by `DBMS_CLOUD_AI`.

| Setting area | Scope | Purpose |
| --- | --- | --- |
| Oracle Select AI profiles | Database | Configure database-side profiles used by `DBMS_CLOUD_AI`. |
| Oracle Select AI credentials | Database | Configure database-side provider credentials used by Select AI profiles. |
| Oracle Select AI prerequisites | Database and DBA-managed security | Grant package execution and network ACL access needed by the database to call provider endpoints. |
| Associated objects | Database profile | Select the tables and views exposed to natural language SQL generation. |

## Access Paths

- **Oracle Select AI profile setup:** `DB Assistant` toolbar -> `Profile Setup...`.
- **Oracle Select AI help:** `DB Assistant` toolbar -> `Help` when Select AI is active.
- **Oracle Select AI object management:** Database Browser -> AI profiles, credentials, and related object context actions where supported.
- **Assistant settings overview:** `DB Navigator` -> `Settings` -> `Assistant`.

## Select AI Credentials

Select AI credentials are database credentials used by `DBMS_CLOUD_AI` to call the selected AI provider.

- **Credential name:** Database credential object name.
- **Credential type:** Provider-specific credential type, such as OCI, password, or token where supported.
- **Provider fields:** User, password, token, OCI user OCID, tenancy OCID, fingerprint, private key, and related provider values.
- **Status:** Credentials can be enabled or disabled.

DBN can create, edit, delete, enable, disable, and reload Select AI credentials when the connected user has the required database privileges.

## Select AI Profiles

Select AI profiles are database AI profile objects used by Oracle Select AI.

- **Profile name:** Database AI profile name.
- **Provider and model:** AI provider and model exposed by Oracle Select AI for the connected database.
- **Credential:** Database credential used by the profile.
- **Associated objects:** Tables and views exposed to the profile for prompt grounding.
- **Description/comments:** User-facing description or profile notes.
- **Status:** Profiles can be enabled, disabled, deleted, reloaded, or marked as the default profile.

The selected Select AI profile controls which database objects are visible to natural language SQL generation. If a generated query lacks required objects, check the profile's associated object list first.

## Select AI Prerequisites

Select AI runs from the database, so the database must be able to call the selected provider.

- **Package privileges:** The user needs execute privileges for `DBMS_CLOUD` and `DBMS_CLOUD_AI`.
- **Network ACL:** The database user needs network access to the selected provider host.
- **Provider credential:** The database must have a valid credential for the selected provider.
- **AI profile:** At least one enabled profile must exist before chat and prompt actions are available.

The Select AI help dialog can show grant and ACL scripts and can run supported grant actions where the connected user has permission.

## Validation Messages

| Message area | Common cause | Resolution |
| --- | --- | --- |
| Select AI credential name | Name is empty, already used, or contains unsupported characters. | Enter an alphanumeric credential name with underscores when needed. |
| Select AI OCI credential | User OCID, tenancy OCID, fingerprint, or private key is missing or malformed. | Correct the OCI credential fields. |
| Select AI profile name | Name is empty, already used, or contains unsupported characters. | Enter a valid unique profile name. |
| Select AI profile objects | No tables or views are associated with the profile. | Add at least one table or view to the profile. |

## Troubleshooting

| Symptom | Resolution |
| --- | --- |
| Select AI profile setup is unavailable | Confirm the connection is an Oracle connection that supports Select AI and that assistant availability has initialized successfully. |
| Select AI profile does not appear | Reload profiles and refresh the Database Browser AI profile list. |
| Select AI credential cannot be created | Confirm the connected user has privileges to create the credential and that all provider fields are valid. |
| Select AI provider call fails with `ORA-24247` | Grant network ACL access for the database user to the selected provider host. |
| Select AI execution fails with package privileges error | Grant execute privileges on `DBMS_CLOUD` and `DBMS_CLOUD_AI`. |
| Generated SQL ignores expected tables | Review the selected Select AI profile and confirm the required tables or views are exposed by that profile. |

## Related Documentation

- [Database Assistant Settings](./database-assistant-settings.md): Overview of Database Assistant settings areas.
- [Database Assistant: Oracle Select AI](./database-assistant-select-ai.md): Guide to database-native Select AI workflows, profiles, credentials, prerequisites, and editor actions.
- [Database Assistant](./database-assistant.md): Overview of Oracle Select AI and the generic assistant.
- [Database Browser](./database-browser.md): Guide to AI profile, AI model, and credential objects visible in supported databases.
- [Code Editors](./code-editors.md): Guide to SQL and PSQL editors where Select AI prompt actions can appear.
- [Connection Management](./connection-management.md): Guide to Oracle connections that provide Select AI support detection.
