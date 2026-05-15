---
name: dbn-codebase-guide
description: DBN Database Navigator project coding guide. Use when Codex is changing, reviewing, explaining, or planning Java/Kotlin/Gradle/XML code in this repository, especially IntelliJ plugin services, DBN managers, actions, UI forms/dialogs, settings persistence, NLS resources, database object models, connection/session code, language/parser code, or when the user asks about DBN naming patterns, common practices, or shared/shard utilities.
---

# DBN Codebase Guide

## Quick Workflow

1. Inspect nearby code first. Match the existing package, base class, nullability annotations, Lombok usage, static imports, and registration style before adding a new pattern.
2. Prefer DBN wrappers over raw platform calls. Check `com.dbn.common.*` before using direct IntelliJ APIs for services, events, disposal, threading, dialogs, messages, settings XML, files, editors, and UI helpers.
3. Preserve the repository shape: Java 17, Gradle IntelliJ plugin, source under `src/main/java`, resources under `src/main/resources`, modules under `modules/dbn-api` and `modules/dbn-spi`.
4. Keep behavior defensive around disposed project/plugin objects. Use DBN refs and failsafe utilities where adjacent code does.
5. Validate with the narrowest useful Gradle task, normally `./gradlew test` for unit-level changes or `./gradlew build` for broader plugin/resource changes.

## References

Read [references/project-practices.md](references/project-practices.md) when making or reviewing code changes. It covers package layout, naming conventions, services, actions, UI forms/dialogs, settings, NLS, extension registration, and validation.

Read [references/shared-utilities.md](references/shared-utilities.md) when choosing helper APIs. It maps common needs to DBN utility classes and explains the "shared/shard utilities" layer. If the user says "shard utilities", treat it as DBN shared utilities unless the codebase contains a task-specific shard concept.

## Default Posture

- Keep edits localized and idiomatic. DBN has many small domain-specific base classes; use them.
- Do not introduce a generic helper if an equivalent exists under `com.dbn.common`.
- Do not bypass `txt(...)` for user-visible strings when surrounding code uses NLS keys.
- Do not hold strong references to long-lived project, connection, object, or UI objects when nearby code uses `ProjectRef`, `ConnectionRef`, `DBObjectRef`, or `WeakRef`.
- Do not use raw background, dispatch, progress, or write-action APIs without checking DBN wrappers first.
