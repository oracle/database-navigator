# Italian DBN Translation Glossary

Canonical terminology decisions for Italian DBN translation work. Keep this file compact and promote only stable, reusable terms here.

## Locale

- Locale: Italian (`it`)
- Source of truth: `src/main/resources/messages/DBNResources.properties` and base `*.html.ft` templates
- Maintainer/reviewer: Codex first pass; native reviewer recommended before release
- Last updated: 2026-06-10

## Core Terms

| Source term | Target term | Keep English? | Context / rule |
| --- | --- | --- | --- |
| action | azione | no | UI commands and action lists |
| assistant | assistente | no | Generic assistant; keep `DB Assistant` as product feature label |
| approval | approvazione | no | Assistant tool approval UI |
| browser | browser | yes | DBN UI browser panels |
| chunk | chunk | yes | Vector/embedding segmentation noun |
| chunking | chunking | yes | Vector/embedding segmentation process |
| code editor | editor codice | no | DBN code editor action/title prefix |
| column | colonna | no | Database column |
| command-line interface | interfaccia da riga di comando | no | External database CLI executable |
| connection | connessione | no | Database connection |
| credential | credenziale | no | Stored auth credential |
| certificate authority | autorità di certificazione | no | Certificate file labels; `CA` may remain as acronym |
| chat | chat | yes | Assistant chat UI |
| data grid | griglia dati | no | DBN data grid UI |
| data editor | editor dati | no | DBN data editor UI |
| database object | oggetto database | no | Generic DB object |
| dataset | dataset | yes | DBN data/table/view set |
| debug logging | log debug | no | Diagnostics logging option |
| debugger | debugger | yes | Debug tool/runtime UI |
| breakpoint | breakpoint | yes | Debugger breakpoint |
| driver | driver | yes | JDBC/database driver |
| embedding | embedding | yes | Vector/AI embedding |
| environment | ambiente | no | DBN environment type |
| execution | esecuzione | no | SQL/script/method execution |
| filter | filtro | no | UI/database filters |
| foreign key | chiave esterna | no | Database relationship key |
| grant | concessione | no | Noun; use `concedi` for command/action |
| log | log | yes | Logging output |
| metadata | metadati | no | Database/AI metadata |
| materialized view | vista materializzata | no | Database materialized view object |
| object | oggetto | no | UI/database object |
| package | package | yes | Database package object type |
| package body | corpo package | partly | Database package body |
| package spec | specifica package | partly | Database package specification |
| profile | profilo | no | AI/connection/config profile |
| prompt | prompt | yes | AI prompt UI |
| provider | provider | yes | AI/provider integration labels |
| record | record | yes | UI/database record; use `riga` only when the source says row |
| record viewer | visualizzatore record | no | Record viewing UI |
| runner | esecutore | no | Execution runner UI labels |
| reverse tunnel | tunnel inverso | no | JDWP/SSH tunnel direction |
| schema | schema | no | Database schema |
| semantic search | ricerca semantica | no | AI/vector search feature |
| source code | codice sorgente | no | Program/source editor content |
| source | origine | varies | Data/vector source; use `sorgente` for source code |
| statement | istruzione | no | SQL statement |
| table | tabella | no | Database table |
| tool | strumento | no | Assistant tool UI |
| transaction commit | commit | yes | Transaction action |
| transaction manager | gestore transazioni | no | DBN transaction manager UI |
| transaction rollback | rollback | yes | Transaction action |
| trigger | trigger | yes | Database trigger object |
| vector | vettore / vettoriale | no | Noun `vettore`; adjective `vettoriale` |
| view | vista | no | Database view |
| walletless | senza Wallet | partly | OCI TLS mode without wallet files |
| workspace | workspace | yes | IDE workspace |

## Protected Terms

Keep product, provider, acronym, protocol, code, and SQL literals unchanged unless the source context is plain prose and this glossary says otherwise.

| Literal | Reason |
| --- | --- |
| Oracle Database Navigator, Database Navigator, DBN | Product names |
| DB Assistant, Select AI | Feature names |
| Oracle, OCI, Oracle Cloud Infrastructure, Oracle Wallet, Wallet, Object Storage | Product/provider terms |
| tenancy | OCI account scope term |
| OpenAI, Azure OpenAI, Anthropic, Google, Ollama, LM Studio | Provider names |
| AI, API, DB, DDL, DML, IDE, JDBC, JSON, MCP, LLM, OCI, OCID, ONNX, PL/SQL, RAG, SQL, SSH, SSL, TLS, TNS, URL, URI, XML | Acronyms/protocols |
| Java, JVM, JDK, JRE | Platform names |
| Auto-Commit, JDWP, ResultSet | Technical UI/API labels |
| `java.sql.Driver`, `tnsnames.ora`, `~/.oci/config` | File/API literals |

## Action Verbs

| Source verb | Target command form | Target progress form | Target noun/title form |
| --- | --- | --- | --- |
| add | aggiungi | aggiunta | aggiunta |
| apply | applica | applicazione | applicazione |
| attach | allega | collegamento | collegamento |
| build | crea | creazione | creazione |
| cancel | annulla | annullamento | annullamento |
| close | chiudi | chiusura | chiusura |
| compile | compila | compilazione | compilazione |
| connect | connetti | connessione | connessione |
| create | crea | creazione | creazione |
| delete | elimina | eliminazione | eliminazione |
| detach | scollega | scollegamento | scollegamento |
| disable | disabilita | disabilitazione | disabilitazione |
| download | scarica | download / scaricamento | download |
| edit | modifica | modifica | modifica |
| enable | abilita | abilitazione | abilitazione |
| execute | esegui | esecuzione | esecuzione |
| export | esporta | esportazione | esportazione |
| generate | genera | generazione | generazione |
| import | importa | importazione | importazione |
| load | carica | caricamento | caricamento |
| open | apri | apertura | apertura |
| refresh | aggiorna | aggiornamento | aggiornamento |
| reload | ricarica | ricaricamento | ricaricamento |
| remove | rimuovi | rimozione | rimozione |
| rename | rinomina | ridenominazione | ridenominazione |
| reset | reimposta | reimpostazione | reimpostazione |
| restore | ripristina | ripristino | ripristino |
| save | salva | salvataggio | salvataggio |
| select | seleziona | selezione | selezione |
| upload | carica | caricamento | upload |
| verify | verifica | verifica | verifica |

## Ambiguous Terms

| Source term | Context | Target term |
| --- | --- | --- |
| source | Source code | codice sorgente / sorgente |
| source | Data/vector source | origine |
| source | Driver/source selection | origine |
| type | Database object type | tipo oggetto |
| type | Java/data type | tipo |
| type | UI category | tipo |
| view | Database object | vista |
| view | UI viewing action | visualizza / vista depending on context |
| model | AI model | modello |
| model | Data model | modello dati |
| profile | AI profile | profilo AI |
| profile | TNS/profile name | profilo |

## Capitalization Rules

- Actions, buttons, titles, and table columns: sentence-style Italian capitalization, not English title case.
- Sentence-like labels, tooltips, hints, messages, notifications, progress text: natural Italian sentence case.
- Acronyms and product names: preserve source casing.
- Mnemonic markers: keep exactly one `&` where the source has one, placed on a natural label word.
