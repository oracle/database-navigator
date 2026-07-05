---
name: dbn-user-docs-guide
description: DBN user docs guide. Use when Codex is creating, editing, normalizing, reviewing, or planning Markdown user documentation for DBN under docs/, especially feature guides, settings references, index updates, Confluence-style formatting, topic ordering, related documentation links, troubleshooting tables, database object icons, or table-vs-bullet formatting decisions.
---

# DBN User Docs Guide

## Product Context

Oracle© Database Navigator (DBN) is a database development and management plugin for JetBrains IDEs. User documentation should speak to developers, DBAs, data engineers, and IDE users who need to connect to databases, browse objects, edit code/data, run or debug database work, and configure DBN behavior.

Write user-facing documentation, not implementation notes. If a product fact is uncertain, inspect the existing docs and source code before stating it.

Keep topic-specific product behavior in the Markdown pages rather than this skill. This guide should capture reusable documentation structure, linking, and formatting conventions; for detailed product behavior, inspect the relevant docs pages and source code.

## Feature Code Map

Use this map as a starting point when validating product behavior from source before writing docs. Package roots are not exhaustive ownership boundaries; many workflows cross shared utilities, IntelliJ platform code, resources, and database-specific implementations.

| Documentation topic | Primary Java package roots to inspect |
| --- | --- |
| Connection Management | `com.dbn.connection`, `com.dbn.connection.config`, `com.dbn.connection.action`, `com.dbn.connection.info`, `com.dbn.connection.jdbc`, `com.dbn.connection.ssh`, `com.dbn.connection.ssl`, `com.dbn.driver`, `com.dbn.database` |
| Transaction Management | `com.dbn.connection.transaction`, `com.dbn.connection.transaction.options`, `com.dbn.connection.session`, `com.dbn.connection.resource`, `com.dbn.editor.session` |
| Database Events | `com.dbn.event`, `com.dbn.event.registration`, `com.dbn.event.notification`, `com.dbn.event.prerequisite`, `com.dbn.editor.data`, `com.dbn.object.action`, `com.dbn.database.interfaces` |
| Environment Types | `com.dbn.common.environment`, `com.dbn.common.environment.options`, `com.dbn.options.general`, `com.dbn.connection.config`; consumers also appear under `com.dbn.browser.ui`, `com.dbn.editor`, `com.dbn.data`, and `com.dbn.execution.common.ui` |
| Database Browser | `com.dbn.browser`, `com.dbn.browser.model`, `com.dbn.browser.options`, `com.dbn.browser.ui`, `com.dbn.object`, `com.dbn.object.common`, `com.dbn.object.impl`, `com.dbn.object.type`, `com.dbn.object.filter`, `com.dbn.object.dependency`, `com.dbn.object.properties`, `com.dbn.object.management`, `com.dbn.navigation` |
| Data Viewers and Editors | `com.dbn.editor.data`, `com.dbn.editor.json`, `com.dbn.data`, `com.dbn.data.grid`, `com.dbn.data.model`, `com.dbn.data.record`, `com.dbn.data.export`, `com.dbn.data.value`, `com.dbn.data.editor` |
| Code Editors | `com.dbn.editor.code`, `com.dbn.editor.console`, `com.dbn.editor.ddl`, `com.dbn.connection.console`, `com.dbn.language`, `com.dbn.language.sql`, `com.dbn.language.psql`, `com.dbn.code`, `com.dbn.code.sql`, `com.dbn.code.psql`, `com.dbn.vfs.file` |
| Workspace Integration | `com.dbn.ddl`, `com.dbn.ddl.action`, `com.dbn.ddl.options`, `com.dbn.ddl.ui`, `com.dbn.connection.mapping`, `com.dbn.connection.mapping.ui`, `com.dbn.connection.context.action`, `com.dbn.vfs`, `com.dbn.vfs.file` |
| Execution Engine | `com.dbn.execution`, `com.dbn.execution.common`, `com.dbn.execution.statement`, `com.dbn.execution.script`, `com.dbn.execution.method`, `com.dbn.execution.java`, `com.dbn.execution.compiler`, `com.dbn.execution.logging`, `com.dbn.execution.explain` |
| Debugging Engine | `com.dbn.debugger`, `com.dbn.debugger.common`, `com.dbn.debugger.jdbc`, `com.dbn.debugger.jdwp`, `com.dbn.debugger.options`, `com.dbn.debugger.prerequisite` |
| Database Assistant | `com.dbn.assistant`, `com.dbn.assistant.chat`, `com.dbn.assistant.settings`, `com.dbn.assistant.service.generic`, `com.dbn.assistant.service.selectai`, `com.dbn.assistant.tool`, `com.dbn.assistant.mcp`, `com.dbn.assistant.provider`, `com.dbn.assistant.profile`, `com.dbn.assistant.credential` |
| Vector Toolbox | `com.dbn.vector`, `com.dbn.vector.model`, `com.dbn.vector.pipeline`, `com.dbn.vector.service`, `com.dbn.vector.ui`, `com.dbn.vector.search`, `com.dbn.vector.prerequisite`, `com.dbn.editor.vector`, `com.dbn.database.interfaces`, `com.dbn.database.oracle` |
| MCP Server Builder | `com.dbn.mcp`, `com.dbn.mcp.build`, `com.dbn.mcp.model`, `com.dbn.mcp.ui`, `com.dbn.mcp.util`, `com.dbn.mcp.vfs`, `com.dbn.menu.action` |

## Quick Workflow

1. Inspect the existing `docs/` pages and nearby topics before editing. Match their tone, heading style, link style, and level of detail.
2. For a new topic, normally create a feature page and a companion settings page: `topic-name.md` and `topic-name-settings.md`.
3. If the user or source confirms there are no settings yet, create only the feature page and note the absence of settings in the Summary section or overview.
4. If a settings reference grows too large, keep `topic-name-settings.md` as a settings overview and split details into focused settings pages such as `topic-name-area-settings.md`.
5. Update `docs/index.md` with a short paragraph overview and a `Docs: [Feature](./topic.md) | [Settings](./topic-settings.md)` line. For feature-only topics, use `Docs: [Feature](./topic.md)`.
6. Add or adjust `Related Documentation` links on the new pages and on adjacent pages when the relationship is useful.
7. Validate formatting, local links, troubleshooting tables, and stale formatting artifacts before finishing.

## Page Types

### Feature Guides

Use feature guides for daily workflows and concepts. Common shape:

```markdown
# Topic Name

## Summary

Short user-facing overview.

## When To Use It

## Access Paths

## Main Feature Sections

## Common Workflows

## Troubleshooting

| Symptom | Resolution |
| --- | --- |
| Example symptom | Example resolution. |

## Related Documentation

- [Topic Settings](./topic-settings.md): Short settings reference description.
```

Adapt sections to the topic, but keep the first screen useful. Do not create a marketing-style landing page.

Feature pages should normally include both `## When To Use It` and `## Access Paths` near the top so readers can quickly decide whether the page applies and where to start in the UI.

### Settings References

Use settings references for configuration details. Common shape:

```markdown
# Topic Settings

## Summary

| Setting area | Scope | Purpose |
| --- | --- | --- |
| Area | Project | Purpose. |

## Access Paths

## Settings Sections

## Validation Messages

| Message area | Common cause | Resolution |
| --- | --- | --- |
| Area | Cause. | Resolution. |

## Troubleshooting

| Symptom | Resolution |
| --- | --- |
| Example symptom | Example resolution. |

## Related Documentation

- [Topic](./topic.md): Short feature guide description.
```

Use `## Summary`, never `Page summary`.

Use `Setting area | Scope | Purpose` for settings summary tables. Include `## Troubleshooting` when the settings page has user-facing symptoms to resolve; otherwise a `## Validation Messages` table may be enough.

## Index Conventions

Keep `docs/index.md` lightweight because many more topics are expected. Use:

- `# Oracle© Database Navigator`
- `## Intro` with a short DBN product description, not a description of the index itself
- One `## Topic Name` section per topic
- One short paragraph describing what the feature does, not what the documentation page contains
- One `Docs: [Feature](./feature.md) | [Settings](./feature-settings.md)` line
- For feature-only topics with no settings page, one `Docs: [Feature](./feature.md)` line

Current topic order:

1. Connection Management
2. Transaction Management
3. Database Events
4. Environment Types
5. Database Browser
6. Data Viewers and Editors
7. Code Editors
8. Workspace Integration
9. Execution Engine
10. Debugging Engine
11. Database Assistant
12. Vector Toolbox
13. MCP Server Builder

Insert new topics where they naturally belong rather than always appending. For example, transaction material belongs near Connection Management, data viewers near Database Browser, and execution/debugging after editor and workspace topics.

## Formatting Rules

- Use Confluence-friendly Markdown.
- Add a `## Contents` section near the top of every documentation page, after `## Summary` for feature/settings pages or after `## Intro` for `docs/index.md`.
- Wrap generated TOC links in `<!-- TOC -->` and `<!-- /TOC -->` markers so the block is easy to regenerate or replace with a Confluence macro later.
- Build TOCs from H2 headings and useful H3 headings. Skip `Summary`, `Intro`, and `Contents`, and avoid linking duplicate page-local H3 anchors such as repeated `General` headings because Markdown and Confluence imports may resolve duplicates differently.
- Use Title Case headings such as `## When To Use It`, `## Access Paths`, and `## Related Documentation`.
- Use backticks for UI labels, settings, actions, shortcuts, file types, menu items, and code-like values.
- Use `->` for menu paths, for example `DB Navigator` -> `Settings` -> `Connections`.
- Use numbered lists for workflows.
- Use bullets for simple concept lists, access paths, and term-to-explanation material.
- Keep language concise and practical. Avoid over-describing related links or repeating the same product positioning on every page.
- Use ASCII unless the existing file clearly needs otherwise.

## Tables Vs Bullets

Use bullets when the content is a simple label plus one explanation:

```markdown
- **Connection:** The configured database connection shown in DBN settings.
```

Use tables when there are more than two information blocks or when users need to scan structured reference data. Keep tables for:

- Setting matrices such as `Setting | Default | Description`
- Option matrices such as `Setting | Default | Available Saved Options | Description`
- Validation matrices such as `Message area | Common cause | Resolution`
- Troubleshooting sections: always `Symptom | Resolution`
- Supported database matrices
- Default DDL extension matrices
- Object action or applicability matrices
- Database object type references with icons

Do not convert `Related Documentation` to a table. Use bullets there.

## Troubleshooting

Troubleshooting sections should be tables:

```markdown
## Troubleshooting

| Symptom | Resolution |
| --- | --- |
| Commit prompt appears too often | Change the relevant Transaction Manager option from `Ask` to the desired saved behavior. |
```

Do not use bullet lists, standalone bold symptom lines, or `<br/>` line-break workarounds. Confluence-style renderers may collapse plain Markdown newlines, so tables are the stable format.

## Related Documentation

Use bullet lists:

```markdown
## Related Documentation

- [Database Browser Settings](./database-browser-settings.md): Settings reference for browser layout, filters, sorting, default editors, and toolbar state.
- [Connection Management](./connection-management.md): Guide to creating and managing the connections shown in the Database Browser.
```

Keep descriptions short and action-oriented. Prefer feature/settings pairs and adjacent topic links. Use relative links like `./page.md`.

## Icons And Images

- Do not add action icons; they made the docs feel crowded.
- Only add object icons where object types are defined, currently in the `Database Browser` object types table.
- Use raw GitHub image URLs for object icons, not GitHub tree URLs:

```markdown
![](https://raw.githubusercontent.com/oracle/database-navigator/master/src/main/resources/img/object/table/Table.svg)
```

- Object type catalogs belong in the Database Browser feature page, not the settings page.

## Cross-Linking

When adding a topic, link both directions where useful:

- Feature page links to its settings page when one exists.
- Settings page links back to the feature page.
- Adjacent workflows link to each other, for example Connection Management, Transaction Management, Database Events, Environment Types, Database Browser, Data Viewers and Editors, Code Editors, Workspace Integration, Execution Engine, Debugging Engine, Database Assistant, and Vector Toolbox.
- Avoid link spam; include pages that help the reader choose the next relevant task or configuration reference.

## Validation Checklist

Before finishing documentation edits, run focused checks on touched docs. Warn the developer and ask for confirmation before repo-wide documentation scans, or ask the developer to run them locally and share only relevant failures.

```bash
rg -n 'Page summary|img/action|<br/>|What it controls:|Typical Use:|Applies To:' docs/<touched-file>.md
rg -n '^\\| Page \\| Description \\|' docs/<touched-file>.md
```

Also verify:

- `Related Documentation` sections are bullet lists.
- Troubleshooting sections use `Symptom | Resolution` tables.
- Local Markdown links point to existing files.
- New topic files are referenced from `docs/index.md`.
- No unrelated docs or source files were reformatted.
