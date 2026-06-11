---
name: dbn-translation-guide
description: DBN translation guide and glossary workflow. Use when translating Oracle© Database Navigator (DBN) user-visible text into another language, reviewing translated resource values or HTML templates for meaning, maintaining locale glossaries, checking terminology consistency, or deciding how to handle ambiguous DBN/database/AI terms. Pair with dbn-localization-guide when changing DBN resource bundles, NLS keys, or localizable template files.
---

# DBN Translation Guide

## Scope

Use this skill for language work: translating, reviewing, and maintaining terminology for DBN UI text, notifications, messages, progress text, logs, docs, and localizable HTML templates.

Use `dbn-localization-guide` for codebase mechanics: NLS key families, `DBNResources.properties`, `.form` bindings, `TextResources.getLocalizable(...)`, and resource validation.

## Quick Workflow

1. Identify the source text and locale. Use only the unqualified source content as truth, such as `DBNResources.properties`, base `.html.ft` templates, IntelliJ intention `description.html` files, file-template description HTML, or English docs.
2. Load the global do-not-translate inventory from [references/do-not-translate.md](references/do-not-translate.md), then load the canonical glossary for the locale from `references/glossaries/` when one exists. Current glossaries use the `dbn-glossary-<locale>.md` pattern, for example [references/glossaries/dbn-glossary-ko.md](references/glossaries/dbn-glossary-ko.md).
3. Translate in batches small enough to review by context, normally by feature area and UI role.
4. Translate phrase by phrase from the source value and context, using Codex's own language knowledge plus the canonical glossary. Do not use external translation tools, dictionary substitution, regex replacement, or another localized file as the source.
5. Preserve placeholders, markup, escaped newlines, mnemonics, code literals, SQL/API/class names, URLs, product names, and technical acronyms exactly unless the task explicitly changes them. Do not translate IntelliJ intention `before.<extension>.template` / `after.<extension>.template` code examples.
6. Update the locale glossary when a reusable term, ambiguous term, or intentional English term appears.
7. Run structural checks supplied by the localization or document workflow, then run semantic checks from this skill.
8. If no native reviewer is available, report the result as structurally safe and contextually translated, not release-polished.

## Role-Aware Translation

- Labels, fields, columns, constants, placeholders, units, and tokens should usually be compact noun phrases.
- Actions, buttons, menu items, and intentions should read as commands.
- Titles should read as concise headers.
- Messages, warnings, errors, questions, hints, tooltips, notifications, progress text, logs, and docs should be natural full phrases or sentences.
- Inserted fragments must work grammatically with their callers; inspect composition when placeholders or tokens make the target language awkward.
- URLs are not prose. Use a verified locale-specific URL only when one is known; otherwise keep the source URL.

## Do-Not-Translate Inventory

- Treat [references/do-not-translate.md](references/do-not-translate.md) as the shared protected-term inventory for all locales.
- Preserve listed literals, patterns, and names exactly unless the user explicitly requests a change or a locale glossary records an accepted exception.
- Keep language-specific exceptions in the locale glossary, not in the global inventory.
- Add newly discovered global protected terms to the inventory when they are product names, APIs, protocols, SQL keywords, class names, file names, environment variables, or other literals that should remain unchanged across locales.

## Glossary Rules

- Keep canonical accepted terms in this skill under `references/glossaries/`.
- Keep temporary pass notes or unresolved reviewer questions near the work product only when they are not yet accepted terminology.
- Promote stable project glossary decisions into the skill glossary at the end of a translation pass.
- Record separate glossary entries for ambiguous terms instead of forcing one translation everywhere. Common examples: `source`, `type`, `view`, `model`, `profile`, `statement`, and `grant`.
- Record terms intentionally kept in English, including product names, protocols, acronyms, provider names, class names, package names, SQL keywords, file names, and UI brand names.
- When the glossary and source context conflict, prefer source context and add a note to the glossary.

Use [references/glossary-template.md](references/glossary-template.md) when starting a new locale glossary.

## Semantic Checks

Run these after each substantial batch:

- **Glossary consistency:** scan for inconsistent translations of accepted terms and for hybrid English/Korean wording that is not intentional.
- **Cross-file consistency:** compare terminology across the locale resource bundle and localized `*.html.ft` templates so labels, messages, notifications, and longer help text use the same accepted terms unless context requires an exception.
- **Ambiguous terms:** inspect source values containing `schema`, `view`, `grant`, `profile`, `credential`, `wallet`, `driver`, `statement`, `console`, `object`, `type`, `session`, `model`, and `source`.
- **Action versus noun:** review terms like `Refresh`, `Open`, `Load`, `Create`, `Grant`, `Verify`, `Apply`, `Compile`, `Commit`, and `Rollback` by UI role.
- **Database collocations:** check that terms read like database software, especially `schema`, `table`, `view`, `execution privilege`, `database connection`, `JDBC driver`, `SQL statement`, `result set`, and `database object`.
- **AI/vector collocations:** check terms like `LLM`, `prompt`, `profile`, `credential`, `embedding`, `chunking`, `vector`, `distance metric`, `semantic search`, and `RAG`.
- **Reviewer caveat:** leave an explicit note for anything uncertain instead of producing fluent-looking unsupported text.

## Cross-File Consistency Checks

- Check the locale bundle and localized templates together, for example `DBNResources_<locale>.properties` and matching `*_<locale>.html.ft` files.
- Verify accepted glossary terms are used consistently across short UI labels, action text, messages, notifications, progress text, and longer HTML help content.
- Compare protected terms from [references/do-not-translate.md](references/do-not-translate.md) across all translated files and preserve exact spelling, casing, punctuation, and code formatting.
- Review allowed differences by UI role before changing them. Compact labels, command-style actions, and explanatory paragraphs may need different grammar while still using the same domain terminology.
- Record intentional cross-file exceptions in the locale glossary with the source term, context, target wording, and example files.
- Include stale-English scans over both resource bundles and localized templates before marking a language batch complete.

## Useful Scans

```bash
rg -n -i '\b(schema|view|grant|profile|credential|wallet|driver|statement|console|object|type|session|model|source)\b' src/main/resources/messages/DBNResources.properties
rg -n -i '\b(workspace|dataset|tool|embedding|chunking|vector|wallet|select ai|mcp|llm|oci|rag)\b' src/main/resources/messages/DBNResources.properties
```
