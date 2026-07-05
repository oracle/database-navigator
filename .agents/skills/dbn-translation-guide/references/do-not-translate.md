# DBN Do-Not-Translate Inventory

Use this inventory for terms, literals, and patterns that should normally remain unchanged in every DBN locale. Locale-specific exceptions belong in `references/glossaries/dbn-glossary-<locale>.md`.

## Product And Feature Names

- Oracle
- Oracle Database
- Oracle Database Navigator
- Database Navigator
- DBN
- IntelliJ IDEA
- JetBrains
- Select AI
- Oracle Cloud Infrastructure
- OCI
- Oracle Wallet
- Wallet
- Java
- PL/SQL
- SQL
- JDBC
- MCP
- LLM
- RAG
- ONNX

## Providers And Services

- OpenAI
- Azure OpenAI
- Anthropic
- Google
- Ollama
- LM Studio

## Technical Acronyms And Formats

- AI
- API
- DB
- DDL
- DML
- IDE
- JSON
- JSONL
- HTML
- XML
- YAML
- URL
- URI
- UUID
- HTTP
- HTTPS
- SSH
- SSL
- TLS
- CSV
- TSV
- ZIP
- JAR
- JVM
- JDK
- JRE

## SQL And Database Literals

Keep SQL keywords, object syntax, and database literals unchanged when they appear as code, examples, generated syntax, protocol values, or quoted literals.

- SELECT
- INSERT
- UPDATE
- DELETE
- CREATE
- ALTER
- DROP
- MERGE
- TRUNCATE
- COMMIT
- ROLLBACK
- GRANT
- REVOKE
- TABLE
- VIEW
- INDEX
- SEQUENCE
- SYNONYM
- PACKAGE
- PROCEDURE
- FUNCTION
- TRIGGER
- TYPE
- SCHEMA
- USER
- ROLE
- TABLESPACE
- MATERIALIZED VIEW
- PRIMARY KEY
- FOREIGN KEY
- NOT NULL
- NULL

## Code, File, And Protocol Patterns

Preserve these categories exactly unless the source itself is being changed:

- Java package names, class names, method names, field names, enum constants, and annotation names
- SQL, PL/SQL, Java, JSON, XML, YAML, HTML, shell, and protocol snippets
- File names, directory names, file extensions, MIME types, URLs, URIs, JDBC URLs, connection strings, and environment variable names
- MessageFormat placeholders such as `{0}`, `{1}`, choice patterns, escaped newlines, HTML tags, and form mnemonic markers such as `&Name`
- Keyboard keys, shortcut literals, command names, executable names, and CLI flags

## Examples

| Literal | Reason |
| --- | --- |
| `java.sql.Driver` | Java API class literal |
| `DBNResources.properties` | Project file name |
| `TextResources.getLocalizable(...)` | Java API call |
| `JDBC_URL` | Environment/configuration key |
| `SELECT * FROM table_name` | SQL example |
| `{0}` | MessageFormat placeholder |
