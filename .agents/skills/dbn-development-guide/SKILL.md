---
name: dbn-development-guide
description: Oracle© Database Navigator (DBN) development workflow guide. Use when Codex is planning or executing DBN repository work that involves BugDB tickets, build/test/compile/runIde validation choices, Gradle triggers, thread naming, Git/refactoring mechanics, repository shape, or deciding what validation to run or ask the developer to run.
---

# DBN Development Guide

## Scope

Use this skill for DBN development mechanics. Pair it with `dbn-codebase-guide` for Java/Kotlin/Gradle/XML implementation patterns, `dbn-localization-guide` for NLS/resource work, `dbn-translation-guide` for translation wording, and `dbn-user-docs-guide` for documentation changes.

This skill owns workflow rules, BugDB handling, validation command selection, repository shape, and Git/refactoring hygiene.

## Workflow Rules

1. Inspect nearby code first. Match the existing package, base class, nullability annotations, Lombok usage, static imports, registration style, and validation style before adding a new pattern.
2. Before changing code, clarify all open implementation points with the developer. Summarize the planned edits, including likely files/classes, behavior changes, resource or registration updates, and validation commands, then wait for confirmation before applying code changes.
3. Treat broad or architectural changes as a separate approval boundary even after a smaller fix was discussed. Stop and ask before changing transport/download mechanisms, class loading, persistence/state shape, threading/concurrency, security approval flow, external process/network behavior, plugin descriptors, Gradle/build configuration, or shared infrastructure. Present the minimal alternative first, then wait for explicit confirmation.
4. Do not run build, compile, test, Gradle, or IDE validation commands unless the developer explicitly asks. After edits, identify the narrow local validation command the developer should run in their workspace instead.
5. Preserve repository shape: Java 17, Gradle Kotlin DSL, IntelliJ Platform Gradle plugin, Lombok annotation processing, JUnit 4 tests, source under `src/main/java`, resources under `src/main/resources`, and public extension modules under `modules/dbn-api` and `modules/dbn-spi`.
6. Avoid hand-editing generated or bulky parser artifacts in language dialect packages unless the task explicitly targets generated parser output.

## BugDB Workflow

- When working on a BugDB ticket, name the Codex chat `Bug <number>` using the thread-title tool as soon as the bug number is known.
- Access bug details at `https://beat.oraclecorp.com/ords/bug/bugui/bug-details?bugnumber=<number>`.
- If the thread-title tool is unavailable, continue the work and mention that the chat title was not changed.
- Keep BugDB-derived implementation scope narrow: tie findings, planned edits, and validation suggestions back to the reported bug number.

## Validation Triggers

Do not execute these commands unless the developer explicitly asks. Use them to choose the correct handoff command after edits.

| Change type | Validation to ask for |
| --- | --- |
| Unit-level Java/Kotlin behavior | `./gradlew test` |
| Narrow compile check without full test execution | `./gradlew compileJava` |
| Broader plugin, resource, descriptor, module, or packaging changes | `./gradlew build` |
| UI behavior, IntelliJ plugin behavior, dialogs, actions, or IDE integration | `./gradlew runIde` |
| Rename/refactor where Git history matters | `git diff --find-renames` |
| Whitespace or patch hygiene after text/resource edits | `git diff --check` |

When the developer does ask to run validation, run the narrowest useful command first. Report the exact command, outcome, and any unrelated pre-existing blockers.

## Git And Refactoring Hygiene

- For name, class, or package refactorings, preserve Git history by moving or renaming existing files first, then editing contents.
- Check `git diff --find-renames` when rename detection matters.
- Never revert unrelated user changes. Work with dirty files if they affect the task, and ignore unrelated dirty files.
- Keep edits localized to the ownership boundary implied by the request.

## Final Handoff

- State whether build/test/Gradle/IDE validation was run. If it was not run because the developer did not ask, say so directly.
- Suggest the narrow validation command from the trigger table.
- Mention BugDB links, thread-title status, or rename-detection checks only when they are relevant to the completed work.
