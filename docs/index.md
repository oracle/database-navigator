# Oracle© Database Navigator

## Intro

Oracle© Database Navigator (DBN) is a database development, exploration, and administration plugin for JetBrains IDEs. It brings database connections, object browsing, data editing, SQL and source-code editing, execution, debugging, transaction control, assistant workflows, vector search, and MCP server generation into the IDE so developers and database users can work with database systems without leaving their project workspace.

## Contents

<!-- TOC -->
- [Connection Management](#connection-management)
- [Transaction Management](#transaction-management)
- [Environment Types](#environment-types)
- [Database Browser](#database-browser)
- [Data Viewers and Editors](#data-viewers-and-editors)
- [Code Editors](#code-editors)
- [Workspace Integration](#workspace-integration)
- [Execution Engine](#execution-engine)
- [Debugging Engine](#debugging-engine)
- [Database Assistant](#database-assistant)
- [Vector Toolbox](#vector-toolbox)
- [MCP Server Builder](#mcp-server-builder)
<!-- /TOC -->

## Connection Management

Connection Management is the starting point for connecting DBN to the databases you work with. It manages supported database types, JDBC drivers, authentication, SSH access, connection testing, and the lifecycle of saved connections. Saved definitions can be activated, duplicated, imported, reordered, and tested before use. Connection-level options shape browsing, execution, debugging, transactions, assistant workflows, and session behavior across the rest of the plugin.

Docs: [Connection Management](./connection-management.md) | [Settings](./connection-management-settings.md)

## Transaction Management

Transaction Management keeps database changes visible and controlled across DBN workflows. It manages auto-commit, commit and rollback decisions, pending changes, connection sessions, and live resource monitoring. Editors, data grids, and execution actions share the same transaction awareness, so users can see where changes originated before deciding what to do. Resource Monitor and Session Browser help inspect open work, transaction sources, and database-side sessions before committing, rolling back, or interrupting activity.

Docs: [Transaction Management](./transaction-management.md) | [Settings](./transaction-management-settings.md)

## Environment Types

Environment Types classify connections by database environment, such as development, integration, production, or custom project-defined categories. Environment colors make active targets easier to distinguish in browser, editor, and execution surfaces. Read-only data and code safeguards help reduce accidental changes on sensitive connections while still letting teams define their own environment model.

Docs: [Environment Types](./environment-types.md) | [Settings](./environment-types-settings.md)

## Database Browser

Database Browser is the main navigation surface for connected schemas and database objects. It organizes connections, schemas, object folders, object details, and supported object types. From the browser, users can open table data, inspect metadata, view source and DDL, navigate dependencies, and launch object-specific actions.

Docs: [Database Browser](./database-browser.md) | [Settings](./database-browser-settings.md)

## Data Viewers and Editors

Data Viewers and Editors provide the surfaces used to inspect and change data returned by browsing and execution workflows. They include table editors, SQL result viewers, cursor data viewers, JSON viewers, large value editors, filtering, sorting, export behavior, and editable result handling. Editable grids participate in transaction tracking, while focused viewers make structured values and large cell content easier to inspect.

Docs: [Data Viewers and Editors](./data-viewers-and-editors.md) | [Settings](./data-viewers-and-editors-settings.md)

## Code Editors

Code Editors are the DBN editing surfaces for writing, inspecting, executing, compiling, and debugging database code. They include SQL consoles for ad hoc work, SQL script files for saved scripts, DDL file editors for object definitions, program editors for stored database source, and Java editors for database Java code. Editors carry connection, schema, and session context into execution, debugging, assistant actions, and workspace mappings.

Docs: [Code Editors](./code-editors.md) | [Settings](./code-editors-settings.md)

## Workspace Integration

Workspace Integration connects project files with database context. DDL File Management creates, attaches, detaches, and synchronizes local DDL files with database objects, while Connection File Mapping assigns project files or folders to a connection, schema, and session. This keeps local scripts, generated DDL, and mapped folders aligned with the database targets they are meant to run against.

Docs: [Workspace Integration](./workspace-integration.md) | [Settings](./workspace-integration-settings.md)

## Execution Engine

Execution Engine runs database work from editors, object actions, and execution histories. It handles statement execution, full script execution, stored method calls, database Java method calls, execution inputs, target context, output handling, and reusable history entries. Results flow into grids, cursor viewers, output consoles, logging panels, and large value editors depending on what the execution returns.

Docs: [Execution Engine](./execution-engine.md) | [Settings](./execution-engine-settings.md)

## Debugging Engine

Debugging Engine lets users inspect supported database code while it runs. It supports Oracle statement debugging, stored method debugging, database Java debugging, breakpoint handling, dependency compilation, debug session startup, and the choice between JDWP and classic JDBC debugging. Debug sessions build on the same connection, editor, execution, and privilege context used by the rest of DBN.

Docs: [Debugging Engine](./debugging-engine.md) | [Settings](./debugging-engine-settings.md)

## Database Assistant

Database Assistant provides DBN's natural language assistance workflows. Oracle Select AI runs through Oracle database-native AI profiles and `DBMS_CLOUD_AI`, while the generic assistant runs through DBN-managed provider profiles, credentials, assistant modes, tools, and optional MCP servers. The generic assistant can use approved tools to work with metadata, source code, SQL consoles, data editors, and semantic-search results.

Docs: [Database Assistant](./database-assistant.md) | [Settings](./database-assistant-settings.md)

## Vector Toolbox

Vector Toolbox creates and uses vector embeddings from files, database tables, and database queries. It guides source selection, chunking, embedding model selection, staging, and destination table creation. Generated embeddings tables become semantic repositories for Search Consoles and the generic Database Assistant `RAG` mode.

Docs: [Vector Toolbox](./vector-toolbox.md)

## MCP Server Builder

MCP Server Builder is the fast path from useful SQL query to packaged MCP tool server. It turns selected SQL statements into named, typed MCP tools, verifies them against a supported database connection, and generates the runnable JAR, configuration, wallet, source project, README, and client snippets needed by consumer products, internal platforms, third-party AI clients, or another MCP-capable runtime. The generated server exposes only the SQL-backed tools you define, making it useful for bounded product integrations.

Docs: [MCP Server Builder](./mcp-server-builder.md)
