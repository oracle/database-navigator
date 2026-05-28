# Vector Toolbox

## Summary

Vector Toolbox helps create and use vector embeddings from database content and local files. It is available for connections that support DBN vector embedding and vector search features, and the current workflow is powered by Oracle AI Vector Search operations such as `DBMS_VECTOR` and `DBMS_VECTOR_CHAIN`.

The toolbox connects three user workflows:

- **Embedding generation:** Create dense vector representations from files, database tables, or database queries.
- **Semantic search consoles:** Run similarity searches against an embeddings table from a dedicated `Search Console`.
- **Generic Database Assistant `RAG` mode:** Ask natural language questions over a selected embeddings table and let the generic assistant retrieve relevant passages before answering.

> **Note:** Vector Toolbox does not currently have a separate DBN settings page. Connection support, database privileges, assistant profiles, and assistant tool approvals are configured through the related feature areas.

## When To Use It

Use Vector Toolbox when you need to:

- Build an embeddings table from application data, reference documents, generated query output, or local files.
- Experiment with chunking behavior before creating embeddings.
- Run semantic searches against an embeddings table from the IDE.
- Open a generic Database Assistant chat in `RAG` mode using the embeddings table created by the toolbox.
- Keep vector search work tied to the same DBN connection, schema, and execution result context as the rest of your database work.

Use the generic Database Assistant `RAG` mode after you already have an embeddings table. Use Vector Toolbox when you need to create or refresh the embeddings that make `RAG` useful.

## Access Paths

- **Main menu:** `DB Navigator` -> `Open Vector Toolbox...`
- **Connection context menu:** Database Browser connection context menu -> `Open Vector Toolbox...`
- **Embedding result:** Vector embedding result toolbar -> `Open Vector Toolbox`
- **Similarity search:** Vector embedding result summary -> `Open Similarity Search`
- **Search Console:** SQL console picker or console context menu -> `New Search Console`, when vector search is supported.
- **RAG assistant:** `DB Assistant` tool window -> generic assistant profile -> `RAG` mode -> `Embedding Table`
- **RAG from result:** Vector embedding result summary -> `Open Database Assistant`

## Core Concepts

- **Data source:** The content DBN embeds. Supported sources are `File system`, `Database tables`, and `Database queries`.
- **Staging location:** A database table used for uploaded file content before chunking and embedding.
- **Chunking configuration:** Controls how large text is split before embedding, using `Chunk by`, `Split by`, `Max size`, and `Overlap`.
- **Embedding model:** The model used to convert text into vectors. DBN can use an `In-database model` or a configured `Third-party model`.
- **Embedding destination:** The table that stores generated chunks and vectors. It is the main repository used by Search Consoles and `RAG` assistant chats.
- **Vector table:** A table that matches the expected embeddings-table structure and is available for similarity search.

## Embedding Workflow

Vector Toolbox guides an embedding request through source selection, optional staging, chunking, model selection, and destination selection.

| Stage | What you choose | Why it matters |
| --- | --- | --- |
| Source | Files, database tables, or database queries | Defines which text content will be embedded. |
| Staging | Schema and table for file uploads | Stores file content before DBN extracts text, chunks it, and embeds it. |
| Chunking | Chunk unit, split rule, maximum size, and overlap | Balances retrieval precision with enough surrounding context. |
| Model | In-database model or third-party model details | Determines embedding shape and compatibility with existing destination data. |
| Destination | Schema and embeddings table | Stores `TEXT`, `EMBEDDING`, and `METADATA` rows for search and `RAG`. |

For table sources, DBN embeds selected table content in batches. For query sources, DBN embeds the text returned by the selected query. For file sources, DBN uploads file content into the staging table, extracts text, chunks it, and writes embeddings into the destination table.

## Embedding Results

Embedding output appears in the `DB Execution Console` as a vector embedding result. The summary shows the source type, source count, embedded rows, success rate, task duration, destination table, and embedding model when available.

From the result you can:

- **Open the embeddings table:** Navigate to the generated destination table.
- **Open the staging table:** Inspect uploaded file content when the source was `File system`.
- **Open Similarity Search:** Create or reuse a `Search Console` preselected to the destination table.
- **Open Database Assistant:** Start a generic assistant chat in `RAG` mode with the destination table selected as the embeddings table.
- **Open Vector Toolbox:** Reopen the toolbox with the same embedding request context.

## Semantic Search Consoles

A `Search Console` is a dedicated editor for similarity search over vector tables. It combines a plain-text search prompt, vector search toolbar controls, and a result grid.

Search Console controls include:

- **`Schema`:** Selects the schema that owns the vector table.
- **`Table`:** Selects an embeddings table filtered by DBN's vector-table detection.
- **`Distance Metric`:** Selects the similarity metric, such as `COSINE`, `DOT`, `EUCLIDEAN`, `MANHATTAN`, or `HAMMING`.
- **`Run Similarity Search`:** Runs the search text against the selected table and displays the closest matches.
- **`Vector Search - Settings`:** Opens editor display options for the console surface.

When you open `Open Similarity Search` from an embedding result, DBN preselects the destination schema and table. If a matching search console already exists, DBN reopens it instead of creating another temporary console.

## RAG Database Assistant Mode

`RAG` mode belongs to the generic Database Assistant, not Oracle Select AI. It uses DBN's semantic-search tool to retrieve relevant content from a selected embeddings table, then asks the selected language model to answer using the retrieved material.

In `RAG` mode:

- The `Embedding Table` selector appears in the assistant toolbar.
- Recent embeddings tables are remembered per connection.
- The assistant can run semantic search against the selected table.
- Semantic search uses the active connection and a `COSINE` search over the selected embeddings table.
- External MCP servers are not attached to `RAG` chats; the mode focuses on retrieval from the selected vector repository.

Opening Database Assistant from a vector embedding result starts a generic `RAG` chat and preselects the result's embeddings table. Starting from the assistant directly is also possible, but you must choose an `Embedding Table` before the assistant can retrieve content.

## Common Workflows

### Create Embeddings For Database Content

1. Open `DB Navigator` -> `Open Vector Toolbox...`
2. Select a connection that supports vector embedding.
3. Choose `Database tables` or `Database queries` as the source type.
4. Add the source tables or queries that produce the text content to embed.
5. Review chunking, model, and destination settings.
6. Start embedding and review the vector embedding result in the Execution Console.

### Create Embeddings For Files

1. Open Vector Toolbox for a supported connection.
2. Choose `File system` as the source type.
3. Select one or more files.
4. Configure the staging table used for file upload and extraction.
5. Review chunking, model, and destination settings.
6. Start embedding and review uploaded, skipped, failed, and embedded file rows in the result.

### Search An Embeddings Table

1. Open a `Search Console`, or click `Open Similarity Search` from a vector embedding result.
2. Select the schema and embeddings table if they are not already selected.
3. Choose the distance metric.
4. Enter the search text.
5. Click `Run Similarity Search` and review the matching content rows.

### Ask Questions With RAG

1. Open the `DB Assistant` tool window with a generic assistant profile.
2. Choose `RAG` mode.
3. Select an `Embedding Table`, or start from `Open Database Assistant` in a vector embedding result.
4. Ask a question about the indexed content.
5. Review the assistant answer against the retrieved source material.

## Troubleshooting

| Symptom | Resolution |
| --- | --- |
| `Open Vector Toolbox...` is not visible | Confirm the selected connection supports vector embedding. For Oracle workflows, the database must support AI Vector Search features. |
| Vector embedding prerequisite checks fail | Confirm the database version is Oracle Database 23.1 or later, the user can create tables, and the user has execute privileges on `SYS.DBMS_VECTOR` and `CTXSYS.DBMS_VECTOR_CHAIN`. |
| Embedding fails with an inconsistent model message | The destination table already records a different embedding model. Use the same model as the existing table or choose a different destination table. |
| File embedding cannot stage content | Check the staging schema/table selection and confirm the user can create or write to the staging table. |
| Query embedding does not produce expected rows | Confirm the query is valid for the selected connection and returns the text content you want embedded. |
| `Search Console` is not available | Confirm the connection supports vector search. Search consoles are offered only for connections with vector search support. |
| Similarity search cannot run | Select a schema, vector table, distance metric, and non-empty search text before running the search. |
| `RAG` mode is unavailable | Confirm the active connection supports vector search. Connections without vector search support show only the non-RAG assistant modes. |
| `RAG` chat has no embedding table | Use the assistant toolbar `Embedding Table` selector, or start the chat from `Open Database Assistant` in a vector embedding result. |

## Related Documentation

- [Database Assistant: Generic Assistant](./database-assistant-generic.md): Guide to generic assistant `RAG` mode, semantic search tools, and MCP support.
- [Database Assistant](./database-assistant.md): Overview of Oracle Select AI and the generic assistant.
- [Database Assistant: Generic Assistant Settings](./database-assistant-generic-settings.md): Settings reference for generic assistant profiles, credentials, tools, MCP servers, and `RAG` controls.
- [Database Assistant Settings](./database-assistant-settings.md): Overview of Database Assistant settings areas.
- [Code Editors](./code-editors.md): Guide to SQL Consoles and Search Consoles.
- [Execution Engine](./execution-engine.md): Guide to the Execution Console where vector embedding results are shown.
- [Database Browser](./database-browser.md): Guide to opening connections, schemas, tables, AI models, and other database objects.
- [Connection Management](./connection-management.md): Guide to configuring the database connections used by vector workflows.
