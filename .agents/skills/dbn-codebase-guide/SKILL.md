---
name: dbn-codebase-guide
description: Oracle© Database Navigator (DBN) project coding guide. Use when Codex is changing, reviewing, explaining, or planning Java/Kotlin/Gradle/XML code in this repository, especially IntelliJ plugin services, DBN managers, actions, UI forms/dialogs, settings persistence, database object models, connection/session code, language/parser code, or when the user asks about DBN naming patterns, common practices, or shared/shard utilities. For localization/NLS-only work, use dbn-localization-guide instead.
---

# DBN Codebase Guide

## Product Context

Oracle© Database Navigator (DBN) is a database development and management plugin for JetBrains IDEs. It lets developers connect to relational databases, browse schemas and database objects, edit SQL/PLSQL, run scripts and programs, edit table data, manage sessions and transactions, compile and debug database programs, and use assistant, OCI, and vector-related integrations.

Primary supported databases are Oracle, MySQL, PostgreSQL, and SQLite, with experimental generic JDBC support. Treat DBN as an IDE-integrated developer tool, not as a standalone database client or backend service.

## Quick Workflow

1. Inspect nearby code first. Match the existing package, base class, nullability annotations, Lombok usage, static imports, and registration style before adding a new pattern.
2. Prefer DBN wrappers over raw platform calls. Check `com.dbn.common.*` before using direct IntelliJ APIs for services, events, disposal, threading, dialogs, messages, settings XML, files, editors, and UI helpers.
3. Use `Components.projectService(...)` and `Components.applicationService(...)` from `com.dbn.common.component.Components` in DBN service `getInstance` helpers, not direct `project.getService(...)` or `ApplicationManager` calls. For services registered only from optional plugin descriptors, use `Components.optionalProjectService(...)` or `Components.optionalApplicationService(...)`. Project services are historically named `*Manager`; avoid introducing interface/implementation service pairs unless the surrounding code already uses that pattern.
4. For name, class, or package refactorings, preserve Git history by moving/renaming existing files first, then editing contents. Check `git diff --find-renames` when rename detection matters.
5. Preserve the repository shape: Java 17, Gradle IntelliJ plugin, source under `src/main/java`, resources under `src/main/resources`, modules under `modules/dbn-api` and `modules/dbn-spi`.
6. Keep behavior defensive around disposed project/plugin objects. Use DBN refs and failsafe utilities where adjacent code does.
7. Validate with the narrowest useful Gradle task, normally `./gradlew test` for unit-level changes or `./gradlew build` for broader plugin/resource changes.

## References

Read [references/project-practices.md](references/project-practices.md) when making or reviewing code changes. It covers package layout, naming conventions, services, actions, UI forms/dialogs, settings, extension registration, and validation.

Read [references/shared-utilities.md](references/shared-utilities.md) when choosing helper APIs. It maps common needs to DBN utility classes and explains the "shared/shard utilities" layer. If the user says "shard utilities", treat it as DBN shared utilities unless the codebase contains a task-specific shard concept.

## Feature Package Map

Use this map as a source-discovery starting point. Package roots are not exhaustive ownership boundaries; many workflows cross shared utilities, IntelliJ platform code, resources, and database-specific implementations.

| Feature area | Primary Java package roots to inspect |
| --- | --- |
| Connection Management | `com.dbn.connection`, `com.dbn.connection.config`, `com.dbn.connection.action`, `com.dbn.connection.info`, `com.dbn.connection.jdbc`, `com.dbn.connection.ssh`, `com.dbn.connection.ssl`, `com.dbn.driver`, `com.dbn.database` |
| Transaction Management | `com.dbn.connection.transaction`, `com.dbn.connection.transaction.options`, `com.dbn.connection.session`, `com.dbn.connection.resource`, `com.dbn.editor.session` |
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

## Default Posture

- Keep edits localized and idiomatic. DBN has many small domain-specific base classes; use them.
- Do not introduce a generic helper if an equivalent exists under `com.dbn.common`.
- For localization/NLS work, use `dbn-localization-guide`.
- Do not hold strong references to long-lived project, connection, object, or UI objects when nearby code uses `ProjectRef`, `ConnectionRef`, `DBObjectRef`, or `WeakRef`.
- Do not use raw background, dispatch, progress, or write-action APIs without checking DBN wrappers first.
