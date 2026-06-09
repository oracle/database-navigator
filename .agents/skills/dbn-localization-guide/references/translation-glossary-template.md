# Translation Glossary Template

Use this template for locale translation passes that span more than a few related keys.

## Locale

- Locale:
- Source of truth: `src/main/resources/messages/DBNResources.properties`
- Maintainer/reviewer:
- Last updated:

## Core Terms

| Source term | Target term | Keep English? | Context / rule | Example keys |
| --- | --- | --- | --- | --- |
| connection |  | no | Database connection, not generic link | `app.connection.*`, `cfg.connection.*` |
| schema |  | no | Database schema | `app.objects.const.DBObjectType_SCHEMA` |
| statement |  | no | SQL statement | `app.execution.*`, `msg.execution.*` |
| source code |  | no | Program/source editor content | `app.codeEditor.*`, `msg.codeEditor.*` |
| source |  | varies | Record separate meanings: data source, event source, source code, source schema |  |
| type |  | varies | Record separate meanings: database object type, Java type, UI category |  |
| commit |  | no | Transaction commit action | `app.transactions.*`, `msg.transactions.*` |
| rollback |  | no | Transaction rollback action | `app.transactions.*`, `msg.transactions.*` |
| grant |  | no | Database privilege grant | `prc.prerequisite.*` |
| Wallet |  | yes | Oracle wallet product/domain term unless reviewer says otherwise | `cfg.oci.*`, `msg.oci.*` |
| Select AI | Select AI | yes | Oracle feature name | `*.assistant.*` |
| MCP | MCP | yes | Protocol acronym | `*.mcp.*` |
| LLM | LLM | yes | AI acronym | `*.assistant.*` |
| OCI | OCI | yes | Oracle Cloud acronym | `*.oci.*` |
| embedding |  | varies | Vector/AI term; record noun and verb forms separately | `app.vector.*`, `msg.vector.*` |
| chunking |  | varies | Vector text segmentation; record noun and verb forms separately | `app.vector.*`, `msg.vector.*` |

## Action Verbs

| Source verb | Target command form | Target progress form | Target noun/title form | Notes |
| --- | --- | --- | --- | --- |
| create |  |  |  |  |
| load |  |  |  |  |
| reload |  |  |  |  |
| refresh |  |  |  |  |
| upload |  |  |  |  |
| download |  |  |  |  |
| verify |  |  |  |  |
| grant |  |  |  |  |
| execute |  |  |  |  |
| compile |  |  |  |  |

## Capitalization Rules

- Standalone titles, buttons, actions, columns, notification titles, progress titles, and log/error/info lines:
- Sentence-like labels, tooltips, hints, validation messages, notification bodies, progress bodies:
- Inserted fragments, tokens, units, object type names:
- Acronyms and product names:

## Intentional English / Technical Literals

| Literal | Reason | Example keys |
| --- | --- | --- |
| SQL | Acronym / language |  |
| JDBC | Acronym / API |  |
| PL/SQL | Product/language name |  |
| Java | Language/platform name |  |
| JSON | Acronym / data format |  |
| XML | Acronym / data format |  |
| ONNX | Acronym / model format |  |
| `java.sql.Driver` | API class literal |  |

## Ambiguous Terms

| Source term | Context | Target term | Example keys |
| --- | --- | --- | --- |
| source | Source code |  |  |
| source | Data source |  |  |
| source | Event source |  |  |
| source | Source schema/object |  |  |
| view | Database view |  |  |
| view | UI view |  |  |
| model | AI/vector model |  |  |
| model | UI/model object |  |  |
| profile | AI profile |  |  |
| profile | User/config profile |  |  |

## Batch Notes

| Batch | Status | Notes / unresolved questions |
| --- | --- | --- |
| `cfg.connection.label` |  |  |
| `msg.connection.error` |  |  |
| `app.vector.*` |  |  |
