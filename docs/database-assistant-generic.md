# Database Assistant: Generic Assistant

## Summary

The generic Database Assistant is the DBN-managed assistant path. DBN sends prompts directly to a configured language model provider and can use approved DBN tools, assistant modes, MCP servers, and semantic search to work with database context from the IDE.

The generic assistant is separate from Oracle Select AI. It is not backed by Oracle `DBMS_CLOUD_AI`, and it does not create or require Oracle Select AI database profiles.

## When To Use It

Use the generic assistant when you need to:

- Ask questions about database metadata, source code, open editors, SQL consoles, or datasets.
- Generate or refine SQL using DBN-managed provider profiles.
- Open data editors, SQL consoles, source editors, and Java editors through approved assistant tools.
- Use `Development`, `Analytics`, or `RAG` assistant modes.
- Use external MCP servers in `Development` or `Analytics` chats.
- Ask `RAG` questions over an embeddings table created by Vector Toolbox or another vector-search workflow.

Use Oracle Select AI instead when prompts must run through Oracle database-native `DBMS_CLOUD_AI` profiles and credentials.

## Access Paths

- **DB Assistant tool window:** Open the `DB Assistant` tool window from the IDE tool window bar.
- **Generic assistant settings:** `DB Navigator` -> `Settings` -> `Assistant`, or `DB Assistant` toolbar -> `Settings...`.
- **Tool approvals:** `DB Assistant` prompt/tool approval dialog, `Tools`, or `Tool Approvals...`.
- **MCP servers:** `DB Assistant` toolbar -> `MCP Servers`, or `DB Navigator` -> `Settings` -> `Assistant` -> `MCP Servers`.
- **RAG mode:** `DB Assistant` tool window -> generic assistant profile -> `RAG` mode -> `Embedding Table`.
- **RAG from Vector Toolbox:** Vector embedding result summary -> `Open Database Assistant`.

## How It Works

- **Provider profile:** A DBN Assistant profile selects a language model provider, credential, temperature preset, and optional custom instructions.
- **Project credential:** DBN stores provider credentials in the project assistant settings and secret values in credential storage.
- **Assistant mode:** The active mode controls which DBN tools are relevant for the conversation.
- **Built-in tools:** DBN tools can inspect database metadata, source code, open editors, datasets, SQL consoles, and semantic search results when approved.
- **Tool approvals:** DBN asks before running assistant tools and can remember approvals or denials by tool type, category, or MCP server.
- **MCP servers:** Generic assistant chats can use configured HTTP, STDIO, or IDE-provided MCP servers when selected and approved.
- **Generated MCP servers:** MCP Server Builder can create SQL-backed MCP servers that are then registered as generic assistant MCP servers.

Treat the generic assistant as assisted generation. Review SQL, code, and tool outputs before applying changes or running generated work.

## Assistant Modes

| Mode | Focus | Common tool context |
| --- | --- | --- |
| `Development` | Database and code-focused work | Database, schema, table, view, program, Java metadata, source code, SQL consoles, data editors, and source editors. |
| `Analytics` | Data exploration and query shaping | Database, schema, table, view metadata, view source, dataset editors, and SQL consoles. |
| `RAG` | Retrieval-augmented answers | Semantic search over a selected embeddings table, typically produced by Vector Toolbox. Available when vector search support is present. |

## Generic Assistant Tools

The generic assistant uses DBN-provided tools to read database context, open IDE surfaces, ask for confirmation, and retrieve semantic-search results. Tool availability depends on the selected assistant mode, the connected database capabilities, the selected model's tool support, and the current tool approval settings.

Built-in tools are visible to the generic assistant only when they are supported and not blocked. Tools marked as `Prompted` ask for approval before each invocation; tools marked as `Approved` can run without another prompt; tools marked as `Blocked` are hidden from the language model.

| Tool area | Available modes | What it allows | Notes |
| --- | --- | --- | --- |
| User prompts | `Development`, `Analytics`, `RAG` | Ask the user yes/no or multiple-choice questions from the assistant flow. | Interactive tools are always prompted. |
| Connection information | `Development` | Load connection, database, and authentication-mode details for the active database connection. | Useful when generated SQL or code depends on database type and connection context. |
| Database and schema metadata | `Development`, `Analytics` | Load database information and list schema names. | Forms the starting point for metadata-aware answers. |
| Table metadata | `Development`, `Analytics` | List table names and load table definitions. | Used for query generation, joins, and schema explanations. |
| View metadata and source | `Development`, `Analytics` | List views, materialized views, JSON views, definitions, and source. | Helps explain reporting layers and generated query targets. |
| Program metadata and source | `Development` | List and load source for functions, procedures, packages, and declared types. | Used for database code analysis and code-generation tasks. |
| Java metadata and source | `Development` | List and load OJVM Java classes, resources, and source. | Available when the connected database supports the Java virtual machine. |
| Dataset editors | `Development`, `Analytics` | Open table and view data editors in the IDE. | Uses DBN editor actions rather than returning table data directly. |
| SQL console editors | `Development`, `Analytics` | List SQL consoles, open consoles, create consoles, and update console content. | Allows the assistant to prepare SQL in a DBN console with approval. |
| Program and Java editor actions | `Development` | Open program, view, type, package, function, procedure, Java class, and Java resource editors. | Focused on navigating to source editors. |
| Semantic search | `RAG` | Run similarity search over a selected vector repository or embeddings table. | Requires vector search support and an embedding table selection, often created with Vector Toolbox. |

## MCP Support

MCP support extends the generic assistant with tools supplied by external Model Context Protocol servers. MCP tools are separate from DBN built-in tools and are not used by Oracle Select AI.

- **Server types:** DBN can connect to MCP servers over `HTTP` or start them through `STDIO` commands.
- **Workspace integration:** When the IDE MCP Server plugin is installed and enabled, DBN can expose IDE workspace MCP tools through the generic assistant.
- **Mode support:** MCP server selection is available for `Development` and `Analytics` chats. `RAG` mode focuses on the selected embeddings table and does not attach external MCP tool providers.
- **Trust prompt:** DBN asks you to trust an MCP endpoint before connecting to it. Continue only for MCP servers you recognize.
- **Tool filtering:** MCP tools can be approved or blocked per server and per tool. Blocked tools are not visible to the model.
- **IDE MCP safeguards:** DBN filters conflicting database-related IDE MCP tools so DBN's own database tools remain the source for database context.

MCP tools can expose capabilities outside DBN's built-in database surfaces. Review each MCP server endpoint, command, and tool list before allowing it.

## RAG Mode

`RAG` mode uses semantic search over a selected embeddings table. It is part of the generic assistant, not Oracle Select AI.

In `RAG` mode:

- The `Embedding Table` selector appears in the assistant toolbar.
- Recent embeddings tables are remembered per connection.
- The assistant can run semantic search against the selected table.
- Semantic search uses the active connection and a `COSINE` search over the selected embeddings table.
- External MCP servers are not attached to `RAG` chats.

Opening Database Assistant from a Vector Toolbox embedding result starts a generic `RAG` chat and preselects the result's embeddings table.

## Chat Controls

The `DB Assistant` tool window keeps the conversation tied to the selected connection and assistant mode.

- **`Select Profile`:** Chooses the generic DBN assistant profile.
- **`Select Model`:** Chooses the model available for the selected profile.
- **`Assistant Mode`:** Chooses `Development`, `Analytics`, or `RAG`.
- **`Embedding Table`:** Selects the vector embeddings table for `RAG` mode.
- **`Tools`:** Reviews built-in DBN tools available to the current chat.
- **`MCP Servers`:** Selects configured MCP servers for `Development` or `Analytics` chats.
- **`New Chat`:** Starts a new conversation.
- **`Clear Chat`:** Clears the current conversation.
- **`Chat History`:** Reopens saved assistant conversations.
- **`Submit Prompt`:** Sends the current prompt. Default shortcut: `Enter`.

## Common Workflows

### Start A Generic Assistant Chat

1. Open the `DB Assistant` tool window.
2. Select a database connection context.
3. Select a generic assistant profile.
4. Select a model if more than one model is available.
5. Choose `Development`, `Analytics`, or `RAG`.
6. Review tool approval prompts before allowing assistant access to metadata, source, data, or external tools.

### Review Generic Assistant Tools

1. Open a generic assistant chat in the `DB Assistant` tool window.
2. Use `Tools` to review built-in tool availability for the selected mode.
3. Leave sensitive tools as `Prompted` or switch them to `Blocked`.
4. Allow only the tool categories needed for the current task.

### Use An MCP Server

1. Configure the MCP server in `DB Navigator` -> `Settings` -> `Assistant`.
2. Verify the server configuration and review the discovered tools.
3. In a generic `Development` or `Analytics` chat, open `MCP Servers`.
4. Select the MCP server for the current chat.
5. Trust the endpoint when prompted only if the server is expected and safe.
6. Review MCP tool approval prompts before allowing tool execution.

### Use A Generated SQL-Backed MCP Server

1. Build the server with MCP Server Builder.
2. Copy the generated `STDIO` command or `HTTP` endpoint from the build result or README.
3. Register the server in `DB Navigator` -> `Settings` -> `Assistant` -> `MCP Servers`.
4. Verify the server and review its tool list.
5. Select it in a generic `Development` or `Analytics` chat.

### Start A RAG Chat From Vector Toolbox

1. Create or open a vector embedding result.
2. Click `Open Database Assistant`.
3. Confirm the generic assistant is in `RAG` mode.
4. Check that the `Embedding Table` selector shows the destination embeddings table.
5. Ask questions about the indexed content and review the retrieved context behind the answer.

### Configure A Generic Assistant Profile

1. Open `DB Navigator` -> `Settings` -> `Assistant`.
2. Create or edit a credential for the LLM provider.
3. Create or edit a profile using that credential.
4. Choose the provider, temperature preset, and optional custom instructions.
5. Apply the settings and return to the `DB Assistant` tool window.

## Troubleshooting

| Symptom | Resolution |
| --- | --- |
| Generic assistant has no profile | Create a DBN Assistant profile and credential in Assistant settings. |
| Generic assistant cannot connect to provider | Check the selected profile, credential, provider API key, OCI configuration, IDE proxy settings, and network access. |
| Generic model list is empty | Confirm the selected profile has a provider and compatible credential, then reopen the model selector. |
| Tool approval prompt appears | Review the requested DBN or MCP tool and choose whether to allow it once, always allow it, deny it, or disable the tool. |
| Expected built-in tool is missing | Check the assistant mode, database capability support, selected model tool support, and whether the tool is blocked. |
| Generic assistant requests too many tools | Review `Tools` and `Tool Approvals...`, then block categories that should not be available. |
| MCP server is not listed in chat | MCP servers are selectable only in generic `Development` and `Analytics` chats. Check Assistant settings and whether the server was saved. |
| MCP tools do not appear in chat | Select the server from `MCP Servers`, confirm the active mode is not `RAG`, and check that the selected model supports tools. |
| MCP tool is hidden | Check the server-level and tool-level approvals. Blocked MCP tools are not visible to the model. |
| `RAG` mode is unavailable | Confirm the active connection supports vector search. Connections without vector search support show only the non-RAG assistant modes. |
| `RAG` mode has no embedding table | Select an embeddings table from the assistant toolbar, open the chat from a Vector Toolbox result, and confirm the connection supports vector search. |

## Related Documentation

- [Database Assistant](./database-assistant.md): Overview of Oracle Select AI and the generic assistant.
- [Database Assistant: Generic Assistant Settings](./database-assistant-generic-settings.md): Settings reference for generic assistant profiles, credentials, tools, MCP servers, and `RAG` controls.
- [Database Assistant Settings](./database-assistant-settings.md): Overview of Database Assistant settings areas.
- [MCP Server Builder](./mcp-server-builder.md): Guide to generating SQL-backed MCP servers from supported DBN connections.
- [Vector Toolbox](./vector-toolbox.md): Guide to creating embeddings tables and using them from Search Consoles and generic assistant `RAG` mode.
- [Code Editors](./code-editors.md): Guide to SQL consoles and source editors the generic assistant can open with approval.
- [Data Viewers and Editors](./data-viewers-and-editors.md): Guide to dataset editors and result viewers used by assistant workflows.
- [Connection Management](./connection-management.md): Guide to connections that provide assistant context.
