---
name: dbn-localization-guide
description: Oracle© Database Navigator (DBN) localization and NLS guide. Use when adding, reviewing, renaming, sorting, or moving DBN resource keys; replacing hardcoded user-visible Java/UI/.form/action/progress/message/notification strings with txt(...); deciding app/cfg/msg/ntf/prc key families and element types; handling @Nls, @NonNls, @PropertyKey, DBNResources.properties, and localization validation.
---

# DBN Localization Guide

## Scope

Use this skill for localization-only or localization-heavy changes in DBN. Pair it with `dbn-codebase-guide` only when the task also needs broader architecture, service, UI, or persistence guidance.

Keep localization edits narrowly scoped. Do not refactor unrelated code while replacing hardcoded strings.

## Resource Access

- Resource bundle: `src/main/resources/messages/DBNResources.properties`.
- Access localized text with `import static com.dbn.nls.NlsResources.txt;`.
- Code should call `txt(...)` directly. Do not introduce wrapper helpers around `NlsResources`.
- Prefer passing translated text into constructors/APIs. Pass keys only when the callee explicitly expects keys.
- Use `@Nls` for localized text values and `@NonNls` for internal strings such as SQL, code, model prompts, file paths, IDs, log-only text, wire protocols, and generated syntax.
- Use `@PropertyKey` only for APIs that intentionally accept resource keys.
- For `.form` files, use `resource-bundle="messages/DBNResources"` and a `key`.
- Use DBN `Messages`, `Dialogs`, and `MessageBundle` helpers for user-facing notifications and dialogs.

## Key Families

Choose the family from the functional context, not from the Java class or form name:

- `app.<area>.*`: runtime UI text, actions, labels, columns, placeholders, titles, tooltips, constants.
- `cfg.<area>.*`: settings, configuration forms, setup wizards, configuration constants and hints.
- `msg.<area>.*`: dialogs, questions, errors, warnings, info messages, dialog buttons and titles.
- `ntf.<area>.*`: notification titles, bodies, links, and actions.
- `prc.<area>.*`: progress/process titles and body text.
- `log.<area>.*`: only for user-facing log output. Do not localize ordinary diagnostic logger strings unless requested.

Use shared groups (`app.shared.*`, `msg.shared.*`) only for genuinely reusable wording.

## Key Shape

Use `<family>.<area>.<element>.<Name>`.

- `area` is the functional area, for example `assistant`, `connection`, `debugger`, `data`, `java`, `mcp`, `languageParser`, or `vector`.
- `element` describes the role of the string, not the Swing class by itself.
- `Name` should be stable and concise. Do not encode an entire long sentence into the key token; use a short semantic token such as `ServerNameHint`.

Keep new keys in the associated resource group and preserve sorted order within that group.

## Element Types

- `action`: visible action text or menu item text.
- `tooltip`: action descriptions and UI tooltip text. IntelliJ action descriptions are tooltips.
- `button`: `JButton` text and dialog/action button captions.
- `label`: short JLabel/check box/radio button captions that behave like labels.
- `text`: sentence-like labels, explanatory text, status text, HTML bodies, and longer UI copy.
- `title`: dialog, popup, panel, group, message, or progress titles.
- `column`: `TableModel.getColumnName(...)` values.
- `placeholder`: placeholder/sentinel display values such as `[title]`, `<all>`, `[UNDEFINED]`.
- `const`: enum/presentable constants and option names.
- `token`: reusable sentence fragments or domain words inserted into other messages.
- `aria`: accessibility names/descriptions.
- `hint`, `link`, `code`, `question`, `info`, `warning`, `error`: use when the role is more specific, especially under `cfg.*` and `msg.*`.

If a label is a full sentence or instruction, prefer `text` over `label`. Check boxes and radio buttons can still use `label` when they are short control captions.

## Common Patterns

- Progress operations usually have paired keys:
  - `prc.<area>.title.<Operation>`
  - `prc.<area>.text.<Operation>`
- Parser display keys use the functional area `languageParser`, for example `msg.languageParser.token.*` and `msg.languageParser.tokenCategory.*`.
- Table model column names use `app.<area>.column.<Name>`.
- Presentable enums use `app.<area>.const.<EnumName_VALUE>` or `cfg.<area>.const.<EnumName_VALUE>`, depending on runtime versus configuration context.
- Runtime action keys use `app.<area>.action.<Name>`. For `AnAction` classes whose plugin XML text is only a keymap/context caption, set the template presentation from code using DBN keys and keep runtime update text unprefixed when needed.
- Do not touch `plugin.xml` unless explicitly requested. When action descriptions are localized in code, use `.tooltip.` keys.
- For strings inserted into composed sentences, use `.token.` rather than `.action.` or `.label.`.

## Text Values

- Preserve current visible wording unless the user asks for wording or capitalization changes.
- Prefer parameterized resource values over concatenation:
  - Good: `txt("prc.object.text.CreatingObjectDescription", description)`
  - Avoid: `"Creating " + description`
- Use `MessageFormat` choice patterns for visible singular/plural text when useful.
- Preserve mnemonics (`&`) in form labels and button text.
- Newline and HTML values belong in the properties file with escaped `\n` or existing HTML formatting.
- Product names, provider names, SQL keywords, API literals, generated code, and model prompts are often `@NonNls`, not resource keys.

## UI Writing Rules

- Use title capitalization for actions in buttons and menus, and for table, popup, message-box, dialog, and control-group headers.
- Use sentence capitalization for control labels, combo/list/tree/table items, links, notification bodies, error body text, tooltips, status descriptions, instructions, inspections, quick-fixes, and editor messages.
- Keep UI text short and clear: present tense, one idea per sentence, active voice, user-perspective wording, and no unnecessary generic words.
- Use ellipsis only for actions that open input-capable dialogs, truncated text without a scrollbar, or ongoing progress text.
- Do not end a single sentence or IDE action with a period. Use periods between multiple sentences.
- Use question marks only for confirmation alerts. Do not use exclamation points.
- Inspection NLS should be short, sentence-cased, problem-focused, and specific. Descriptions should start with a verb such as `Reports`.

## Workflow

1. Search with `rg` for the target class/API and nearby resource keys.
2. Inspect usages to decide whether each string is user-visible, internal, log-only, prompt/code, or a resource key.
3. Pick the functional area and family (`app`, `cfg`, `msg`, `ntf`, `prc`) from where the text appears.
4. Patch Java/.form/properties together, using static `txt(...)` imports in Java.
5. Re-scan the target files for remaining hardcoded user-visible strings.
6. Run `git diff --check`. Run the narrow compile/test command when useful; report unrelated existing blockers clearly.
