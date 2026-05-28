# MCP Server Builder

## Summary

MCP Server Builder quickly turns selected SQL queries into SQL-backed MCP tools and packages them as a standalone Model Context Protocol server. Starting from a supported DBN database connection, it generates a runnable JAR, `mcp-config.yaml`, Oracle wallet, source project files, README, and MCP client snippets.

The main goal is integration: the generated server can be used by consumer products, internal platforms, agent frameworks, third-party AI clients, or any other MCP-capable runtime. DBN Generic Assistant can also consume the generated server, but it is only one possible client.

> **Note:** MCP Server Builder does not currently have a separate DBN settings page. Builder fields are part of the builder dialog, and generated runtime configuration is stored with the output package.

## Mission

The mission of MCP Server Builder is to make database knowledge available to external products and AI clients without requiring users to hand-code an MCP server. A user starts with SQL they already trust, gives each query a tool name, description, and typed inputs, verifies the query against the active connection, and lets DBN package the result as a runnable server.

This keeps the workflow intentionally focused:

- **Fast tool creation:** Turn a query into an MCP tool from inside DBN, using the connection, SQL editor behavior, and verification flow already available in the IDE.
- **Query-scoped access:** Expose only the SQL tools you define instead of giving a consuming product broad database access.
- **Portable packaging:** Generate the runtime JAR, configuration, wallet, source project, README, and client snippets needed to run the server outside DBN.
- **Product and client handoff:** Use the generated server from third-party AI clients, consumer applications, internal tools, agent frameworks, or the generic Database Assistant after registration and approval.

Think of MCP Server Builder as the bridge from `useful SQL query` to `usable MCP tool server`.

## When To Use It

Use MCP Server Builder when you need to:

- Expose a small set of database queries as callable MCP tools.
- Turn existing reporting, lookup, diagnostic, or metadata queries into reusable AI-accessible tools.
- Give a consumer product or AI client controlled access to query-based database capabilities.
- Build a repeatable MCP server package from a DBN connection and SQL definitions.
- Share a generated source project with developers who need to customize or rebuild the server.
- Use SQL-backed tools from third-party AI clients, internal platforms, product integrations, or another MCP-capable runtime.

Use Generic Assistant MCP server settings only when your task is to register an already built or already running MCP server for DBN Assistant chats.

## Access Paths

- **Main menu:** `DB Navigator` -> `Open MCP Server Builder...`
- **Connection selection:** If more than one supported connection exists, choose the target connection from `Select MCP Server Connection`.
- **Build result:** `MCP Build Complete` dialog after `Build Server` finishes.
- **Generated README:** `<project>/mcp-dist/<server-name>/README.md`
- **Generated client snippets:** Build result dialog or generated README.
- **Optional DBN Assistant registration:** `DB Navigator` -> `Settings` -> `Assistant` -> `MCP Servers`

The menu action is shown when the project has at least one connection that supports MCP Server Builder. The current implementation is available for Oracle connections that support the DBN `MCP server builder` feature.

## Consumer And Client Integration

MCP Server Builder is designed for consumers outside the builder itself. The generated server is a standalone Java process. It does not require DBN to be open at runtime, but it uses the connection URL, wallet, and tool definitions produced by DBN.

- **Consumer products:** Integrate the generated server as a local or service-side MCP process that exposes only the SQL-backed tools you packaged.
- **Internal platforms:** Provide approved lookup, reporting, metadata, or diagnostic queries to internal agents without rebuilding database connectivity from scratch.
- **Third-party AI clients:** Use generated snippets for clients such as Claude Desktop or Cline, or adapt the `STDIO` command and `HTTP` endpoint to another MCP-capable client.
- **Agent frameworks:** Register the generated server wherever the framework accepts MCP servers and tool schemas.
- **DBN Generic Assistant:** Optionally consume the generated server after registering it as an `HTTP` or `STDIO` MCP server and approving its tools.

Oracle Select AI does not use MCP Server Builder. Select AI runs through Oracle database-native `DBMS_CLOUD_AI` profiles.

## Core Concepts

- **Server definition:** The builder form for server name, transport type, HTTP port, and tool definitions.
- **Transport:** `STDIO` starts the server from a command. `HTTP` starts a local streamable HTTP endpoint.
- **Tool definition:** A named SQL query with a description and optional typed parameters.
- **Named SQL parameters:** Bind variables written as `:name`, such as `:employee_id`; DBN derives the tool parameter list from these variables.
- **Verification:** A query preview step that runs the SQL with sample parameter values and confirms that it returns rows.
- **Generated package:** The runnable server distribution under `<project>/mcp-dist/<server-name>/`.
- **Wallet:** Oracle SEPS wallet files generated for the target connection credentials.

## Builder Controls

| Control | Purpose |
| --- | --- |
| `Server name` | Sets the generated output folder, JAR name, MCP client entry, and server identity. |
| `Transport` | Chooses `STDIO` or `HTTP` runtime behavior. |
| `HTTP port` | Sets the local endpoint port when `HTTP` transport is selected. |
| `Load Configuration` | Loads an XML builder definition created by MCP Server Builder. |
| `Save` / `Save As...` | Saves the current builder definition to an XML file. |
| `Reset Form` | Starts again from a default builder definition. |
| `Build Server` | Builds the standalone server package. |

## Tool Definitions

Each MCP tool is built from one SQL statement. The tool name and description help MCP clients decide when to call it, while the parameter metadata becomes the tool input schema.

Tool SQL should be query-oriented. The generated server executes tools with query semantics, so use `SELECT`-style statements that return a result set. DML, DDL, and anonymous PL/SQL blocks are not part of the generated server flow.

Named parameters are discovered from the SQL text, excluding SQL comments. Repeated parameters are kept once in the parameter list, but all occurrences are bound when the generated server executes the query.

Supported parameter types are:

| Builder type | Generated schema type | Generated format |
| --- | --- | --- |
| `STRING` | `string` | None |
| `INTEGER` | `integer` | None |
| `NUMBER` | `number` | None |
| `BOOLEAN` | `boolean` | None |
| `DATE` | `string` | `date` |

## Transport Options

| Transport | Runtime behavior | Common client use |
| --- | --- | --- |
| `STDIO` | MCP client starts the generated JAR with `java -jar`. | Claude Desktop-style local server configuration. |
| `HTTP` | You start the generated JAR and it serves `http://127.0.0.1:<port>/mcp`. | Streamable HTTP clients such as Cline, or Claude Desktop through `mcp-remote`. |

## Build Output

| Artifact | Location | Purpose |
| --- | --- | --- |
| Runnable JAR | `<project>/mcp-dist/<server-name>/<server-name>.jar` | Starts the generated MCP server. |
| Runtime configuration | `<project>/mcp-dist/<server-name>/mcp-config.yaml` | Contains transport, database URL, and tool definitions. |
| Oracle wallet | `<project>/mcp-dist/<server-name>/wallet/` | Stores generated Oracle wallet credentials for the server runtime. |
| Source project | `<project>/mcp-dist/<server-name>/source-project/` | Contains Maven project files and generated Java source for customization. |
| README | `<project>/mcp-dist/<server-name>/README.md` | Documents run commands, client snippets, credential behavior, and customization. |

DBN also writes a build-time `mcp-config.yaml` in the project base path before copying it into the generated server output folder.

## Generated Runtime Configuration

The generated `mcp-config.yaml` contains the runtime values used by the standalone server:

- **`transport`:** `stdio` or `http`.
- **`httpPort`:** Used when transport is `http`.
- **`dataSource.url`:** JDBC URL resolved from the DBN connection.
- **`dataSource.username` and `dataSource.password`:** Commented placeholders that can override wallet credentials if uncommented.
- **`tools`:** Tool descriptions, SQL statements, parameter metadata, and required flags.

By default, generated credentials are stored in `wallet/`. If direct credentials are added to `mcp-config.yaml`, treat the file as sensitive.

## Common Workflows

### Build A SQL-Backed MCP Server

1. Open `DB Navigator` -> `Open MCP Server Builder...`
2. Select the Oracle connection if prompted.
3. Enter a server name.
4. Choose `STDIO` or `HTTP`; for `HTTP`, confirm the port.
5. Add at least one MCP tool.
6. Verify tool SQL with sample parameter values.
7. Click `Build Server`.
8. Review the `MCP Build Complete` output paths and copy the client configuration snippet you need.

### Create And Verify A Tool

1. In MCP Server Builder, add a tool definition.
2. Enter a tool name and description.
3. Write a SQL query that returns rows.
4. Use named parameters such as `:department_id` where inputs are needed.
5. Set parameter types, descriptions, and required flags.
6. Click `Verify`, enter sample values, and run the query preview.
7. Save the tool after the statement is verified.

### Use The Generated Server From An MCP Client

1. Build the MCP server from MCP Server Builder.
2. Choose the generated `STDIO` command or `HTTP` endpoint that matches the consuming product.
3. Copy the client snippet from the build result dialog or generated README.
4. Adapt the snippet if the target product uses a different MCP configuration format.
5. Keep the generated `wallet/` folder beside the JAR unless direct credentials are configured.
6. Start or reconnect the consuming product so it reloads the generated tools.
7. Review tool behavior and access with the target client before making it available to users.

For DBN Generic Assistant, register the generated server in `DB Navigator` -> `Settings` -> `Assistant` -> `MCP Servers`, then select it from a generic `Development` or `Analytics` chat.

### Customize The Generated Server

1. Open the generated `README.md` in `<project>/mcp-dist/<server-name>/`.
2. Adjust `mcp-config.yaml` for SQL, descriptions, parameters, transport, or direct credentials.
3. Use `source-project/` if Java or build customization is needed.
4. Reconnect the MCP client after changing runtime configuration.

## Security Notes

- Keep the generated `wallet/` directory private.
- Do not commit generated wallet files to version control.
- Treat `mcp-config.yaml` as sensitive if direct `username` and `password` values are added.
- Review each generated tool before exposing it to a product, client, framework, or assistant.
- Prefer narrow, read-only query tools that return only the data needed by the client workflow.

## Troubleshooting

| Symptom | Resolution |
| --- | --- |
| `Open MCP Server Builder...` is not visible | Confirm the project has an Oracle connection that supports MCP Server Builder. |
| Builder asks for the Maven plugin | Install or enable the IDE Maven plugin `org.jetbrains.idea.maven`, then reopen the builder. |
| Builder says Maven runtime is unavailable | Check the IDE Maven settings and select a valid Maven runtime. |
| Build fails while creating the wallet | Add `oraclepki` to the selected Oracle driver bundle so DBN can create the Oracle SEPS wallet. |
| Build fails for a TNS connection | Confirm the connection has a TNS folder, a TNS profile, and a readable `tnsnames.ora` entry. |
| Parameter list is missing an expected value | Confirm the SQL uses named parameters like `:name`; parameters inside SQL comments are ignored. |
| Query verification fails with no result set | Change the tool SQL to a query that returns rows. |
| Generated HTTP server is not reachable | Start the generated JAR, confirm `transport: "http"` and `httpPort` in `mcp-config.yaml`, then reconnect the MCP client. |
| Generated tools do not appear in a client | Confirm the consuming product has the generated server configured, the server process or HTTP endpoint is reachable, and the client has reloaded its MCP tools. For DBN Generic Assistant, also verify server selection and tool approvals. |

## Related Documentation

- [Connection Management](./connection-management.md): Guide to Oracle connections and driver configuration used by the builder.
- [Code Editors](./code-editors.md): Guide to SQL editing surfaces and database-aware SQL behavior.
- [Database Assistant: Generic Assistant Settings](./database-assistant-generic-settings.md): Settings reference for registering generated MCP servers when DBN Assistant is one of the consumers.
- [Database Assistant: Generic Assistant](./database-assistant-generic.md): Guide to using external MCP servers from generic assistant chats.
