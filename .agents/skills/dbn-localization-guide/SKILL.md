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
- `intention`: IntelliJ intention and quick-fix names returned by APIs such as `EditorIntentionAction#getText()` or `LocalQuickFix#getName()`.
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
- Editor intention keys use `app.<area>.intention.<Name>`. Do not reuse `.action.` keys for `EditorIntentionAction#getText()` just because an intention invokes the same behavior as a menu or toolbar action.
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
- Use sentence capitalization for control labels, combo/list/tree/table items, links, notification bodies, error body text, tooltips, status descriptions, instructions, inspections, intentions, quick-fixes, and editor messages.
- Keep UI text short and clear: present tense, one idea per sentence, active voice, user-perspective wording, and no unnecessary generic words.
- Use ellipsis only for actions that open input-capable dialogs, truncated text without a scrollbar, or ongoing progress text.
- Do not end a single sentence or IDE action with a period. Use periods between multiple sentences.
- Use question marks only for confirmation alerts. Do not use exclamation points.
- Inspection NLS should be short, sentence-cased, problem-focused, and specific. Descriptions should start with a verb such as `Reports`.

## Verification Passes

Use verification passes for broad resource-file cleanup. Keep each pass focused on one rule, and re-scan after editing.

### Placeholder and MessageFormat Safety

- Preserve every `{0}` / `{1}` placeholder, MessageFormat choice/plural pattern, escaped newline (`\n`), and HTML boundary unless the caller is changed at the same time.
- Compare old and new placeholder sets for every changed resource value. Treat any mismatch as a blocker until the Java/.form caller is inspected.
- Do not add unnecessary escapes for double quotes in `.properties` values. Use `"{0}"`, not `\"{0}\"`, unless the visible text really needs a backslash.
- Be careful with apostrophes in MessageFormat values: a single quote starts/ends quoting, and a literal apostrophe usually needs to be doubled. SQL/code snippets may already use doubled quotes intentionally.
- For values ending with or containing `\n`, inspect the caller before removing or moving the newline. Some dialog text depends on resource-level spacing, while other code adds the newline itself.

Useful check:

```bash
rg -n '\{[0-9]' src/main/resources/messages/DBNResources.properties
```

After editing a broad set of values, derive changed keys from `git diff -U0` and compare placeholder sets for old and new values before compiling.

### Key Family and Usage Context

- Verify key family from the display context, not from the class name. The same concept can be `app.*` in runtime UI, `cfg.*` in settings, `msg.*` in a dialog body/title, `ntf.*` in a notification, and `prc.*` in progress text.
- Check exact usages before renaming or moving keys. Do not infer from the key name alone, especially when the value is used by an action, intention, notification, progress task, or form label.
- When renaming keys, update Java, `.form`, and resource references together, then re-run `rg` for every old key and old visible value.
- Keep moved or renamed keys sorted within their resource group after editing.
- Allow casing to differ by usage space: a menu/action can be title-cased while an intention, hint, notification body, or configuration label should usually be sentence-cased.

Useful check:

```bash
rg -n 'txt\("([^"]+)"' src/main/java
```

### Required and Missing Wording

- For validation messages, prefer direct remediation over passive failure labels: `Enter a credential name`, `Select a database file`, `Specify a JDBC driver`, `Configure a schema`.
- Use `Enter` for typed values, `Select` for choices/files/schemas, `Specify` for named configuration values, and `Configure` when the user must open or adjust settings.
- Avoid unnecessary `required`, `missing`, `not specified`, and `cannot be empty` wording when a direct action is clearer.
- Keep composite-message use cases in mind. Direct imperative messages work well in cumulative lists such as `Correct the following errors:\n - Enter a data type`, but sentence fragments may not.
- Keep genuine state descriptions when they are not field validation, for example missing privileges, missing prerequisites, missing database objects, or unavailable files.

Useful check:

```bash
rg -n -i '\b(required|missing|not specified|cannot be empty|must be specified|not selected)\b' src/main/resources/messages/DBNResources.properties
```

### Ellipsis Usage

- Use ellipsis only for actions that open input-capable dialogs, truncated text without a scrollbar, or ongoing progress text.
- Do not add ellipsis simply because an action opens a popup, performs work, or shows a confirmation.
- Preserve ellipsis in progress/log values that intentionally show an operation is in progress, for example `Loading...`.
- Review action, button, link, progress, and title values separately; the same visual `...` can mean different things by element type.

Useful check:

```bash
rg -n '\.\.\.' src/main/resources/messages/DBNResources.properties
```

### Sentence vs. Title Case

- Use title capitalization for `action`, `button`, and true UI-header `title` values such as dialogs, message boxes, popups, table/tree/group headers, and menu/action presentation text.
- Use sentence capitalization for `intention`, `tooltip`, `error`, `warning`, `info`, `hint`, `message`, `text`, `label`, `link`, accessibility descriptions, editor messages, inspections, and quick-fixes.
- Do not mechanically title-case every `.title.` key. Progress/process titles, status titles, generated task names, and sentence-like titles can intentionally use sentence capitalization.
- Do not reuse `.action.` keys for `EditorIntentionAction#getText()` or quick-fix names just because the intention performs the same operation as an action. Add a matching `.intention.` key and keep it sentence-cased.
- Preserve mnemonics (`&`), placeholders (`{0}`), escaped newlines (`\n`), HTML markup, keyboard names, SQL keywords, object type names, acronyms, and product/provider names.
- Keep acronyms fully uppercase (`AI`, `API`, `DB`, `DDL`, `IDE`, `JDBC`, `JSON`, `MCP`, `OCI`, `OCID`, `SQL`, `SSH`, `URL`, `URI`, `XML`). Use normal capitalization for ordinary words next to them, for example `MCP server`, `JSON view`, and `Java source code`.
- Treat quoted UI labels as references to the actual visible label. If a button/action label changes from `Keep current` to `Keep Current`, update explanatory text that quotes that label.
- Defer ambiguous items instead of forcing them. In particular, review enum/presentable constants, product names, tree-root labels, progress text, and values containing quoted section names in context.

Useful checks:

```bash
rg -n '^[^#].*\\.(action|button|title|intention|tooltip|error|warning|info|hint|message|text|label|link)\\.[^=]+=.*' src/main/resources/messages/DBNResources.properties
rg -n 'txt\("app\.[^"]+\.action\.[^"]+"\)' src/main/java/com/dbn/code/common/intention src/main/java/com/dbn/assistant/service/selectai/editor/intention
```

For a safe candidate list, group keys by element type first. For `action`, `button`, and true header `title` values, flag non-small words that remain lowercase after the first word. For sentence-cased elements, flag non-acronym words capitalized after the first word. Review every hit in context before editing.

After editing, re-scan the touched packages for old key usages and old visible values. For intention migrations, expect zero `.action.` resource calls in intention packages and exact-key usages for every new `.intention.` key.

### Technical Casing

- Keep technical acronyms fully uppercase in visible values: `AI`, `API`, `DB`, `DDL`, `IDE`, `JAR`, `JDBC`, `JSON`, `MCP`, `OCI`, `OCID`, `ONNX`, `PL/SQL`, `SQL`, `SSH`, `URL`, `URI`, `XML`.
- Use normal capitalization around technical terms: `JSON view`, `MCP server`, `Java source code`, `JDBC driver`, `JAR file`.
- Prefer established product/protocol names such as `OpenSSH` and `PL/SQL`.
- Do not mechanically change key names, HTML tags, URLs, file paths, package/class names such as `java.sql.Driver`, SQL/code snippets, or file extensions such as `.onnx`.
- Run the scan against values, not key names, when checking visible text.

Useful checks:

```bash
rg -n 'Json|Jdbc|Mcp|Oci|Ocid|Onnx|PLSQL|Ssh|Sql|Url|Uri|Open SSH' src/main/resources/messages/DBNResources.properties
rg -n -i '\b(json|jdbc|mcp|oci|ocid|onnx|plsql|ssh|sql|url|uri)\b' src/main/resources/messages/DBNResources.properties
```

### Terminal Periods

- Remove a final `.` from standalone, single-sentence UI values, especially one-line `error`, `hint`, `info`, `label`, `message`, `text`, and `tooltip` values.
- Keep periods in multi-sentence values, multiline explanatory text, HTML bodies, code snippets, placeholder examples, values that intentionally end with `...`, and confirmation questions.
- Be cautious with values that are sentence fragments, status fragments, or are inserted into a larger message. A fragment may look like a single sentence but still need punctuation when composed.
- Before finalizing a broad period cleanup, audit every changed key for composition usage. Search exact key usages outside `DBNResources.properties`, and inspect any key used in string concatenation, `StringBuilder.append(...)`, `String.format(...)`, `MessageFormat.format(...)`, or another sentence-building call.
- Restore the period when the value is composed with following text but the code only adds spacing or a newline. Example pattern: `txt("some.key") + "\n"` where the next translated sentence follows.
- For fragment-like labels, manually inspect the caller even if no concatenation is present. Values such as execution-duration fragments may be inserted into a larger status format and should be judged by the final rendered text.

Useful checks:

```bash
rg -n '^[^#].*=.*\.$' src/main/resources/messages/DBNResources.properties
```

For a safe candidate list, filter out ellipses, multiline values, HTML, code keys, placeholders, questions, and values with multiple sentence-ending periods. After editing, rerun the same filter and expect zero remaining candidates for the chosen scope.

For composition safety, derive the period-only changed keys from `git diff -U0`, search exact usages outside the resource bundle, and flag same-line composition patterns such as `txt(...) +`, `+ txt(...)`, `append(txt(...))`, `String.format(...txt(...))`, and `MessageFormat.format(...txt(...))`. Treat flagged keys as mandatory manual review items.

### Courtesy and Professional Tone

- Avoid courtesy wrappers such as `Please verify`, `Please select`, and `Please contact` in product UI. Prefer direct, professional instructions: `Verify`, `Select`, `Contact`.
- Avoid `your` when the object is not meaningfully personal. Prefer `the JDBC driver`, `the selected database type`, `the current account`, or `the configured database client`.
- Keep user-context wording when it is genuinely useful, for example assistant profile copy about `your data model` or credentials that belong to the user.
- Preserve standard confirmation wording such as `Are you sure... ?` and `Do you want... ?` when it clearly asks for a decision.
- For security/trust prompts, keep direct wording such as `Only continue if you recognize...`; do not soften it with politeness.

Useful checks:

```bash
rg -n 'Please|please' src/main/resources/messages/DBNResources.properties
rg -n -i '\b(you|your|not allowed|cannot|must|may not|did not|provided invalid)\b' src/main/resources/messages/DBNResources.properties
```

### Shouty Warning Text

- Avoid all-caps emphasis in prose: use `Note:` instead of `NOTE:`, `active` instead of `ACTIVE`, and sentence-style warning bodies instead of banner text like `READ-ONLY DATA - ...`.
- Keep genuine acronyms, SQL keywords, placeholder tokens, enum-like constants, and log severity tokens uppercase when they are intentionally token-like, for example `[ERROR]`, `NOT NULL`, `ERROR`, `INFO`, `WARNING`.
- Keep warning copy serious without shouting. Prefer precise consequence wording over exclamation points or all-caps emphasis.
- Re-scan after editing and classify remaining uppercase matches as intentional tokens, acronyms, or candidates.

Useful check:

```bash
rg -n '^[^#].*=(.*[^A-Za-z]|)(NOTE|NOT|ACTIVE|INACTIVE|MERGED|EDITABLE|READ-ONLY|WARNING|ERROR|INFO)([^A-Za-z].*|$)' src/main/resources/messages/DBNResources.properties
```

### Accusatory Phrasing

- Avoid phrasing that sounds like blame in error and warning bodies: `You did not`, `You provided invalid`, `You cannot`, `You are not allowed`, `You must`.
- Prefer neutral state plus recovery: `No schema selected. Select a schema to continue`, `Invalid or unsupported variable values. Correct the input and try again`.
- Use neutral capability wording for restrictions: `Statements cannot be executed against this connection`, `This operation may not be available`, `Object name and type cannot be changed`.
- Standard confirmation questions and explicit user decisions can keep `you`, especially `Are you sure... ?`, `Do you want... ?`, and trust prompts.
- In cumulative error lists, keep messages short and independently actionable.

Useful check:

```bash
rg -n -i '\b(you did not|you provided|you cannot|you are not allowed|you must|your input|your jdbc driver|you may not be able)\b' src/main/resources/messages/DBNResources.properties
```

### Mnemonics and `labelFor`

- Add mnemonics (`&`) to labels, buttons, check boxes, radio buttons, and other direct controls when they participate in keyboard navigation.
- Every `JLabel` that labels a focusable input in a `.form` file should have a `labelFor` pointing to that actual input component.
- `labelFor` targets must be non-empty and must reference an existing component id in the same `.form` file.
- Watch for copy/paste mistakes: a label often points to a nearby name field, the first combo box, or another row's input. If the label text describes a same-row field, point `labelFor` to that field.
- Prefer existing `*Mnemonic` keys when a plain label is reused in both focusable-input and non-input contexts, for example `Connection` versus `ConnectionMnemonic`. Add a separate mnemonic key instead of putting `&` into a shared read-only/status value.
- Keep mnemonic choices local to the containing panel or dialog when practical. Avoid obvious duplicates among neighboring controls, but do not force awkward letters just for global uniqueness.
- Do not add mnemonics or `labelFor` to status readouts, placeholder labels, unit labels (`ms`, `seconds`, `records`), preview/sample labels, decorative labels, section headers, table/list captions, or labels that only describe a read-only value group.
- Check boxes and radio buttons usually carry their own mnemonic in their `text` value and do not need a separate `JLabel`/`labelFor`.
- Disabled or read-only text fields can still have `labelFor` when the label directly names the field and keyboard focus is useful for inspection or copying.

Useful checks:

```bash
rg -n '<labelFor value=""' -g '*.form' src/main/java
rg -n 'class="javax.swing.JLabel"|<labelFor|<text resource-bundle="messages/DBNResources"' -g '*.form' src/main/java
```

For a broad audit, use a namespace-aware XML scan rather than grep only. Verify that all `labelFor` values resolve to component ids, that localized `JLabel` values with `labelFor` contain a mnemonic, and that same-row copied targets are reviewed manually. After editing, rerun the scan and expect zero empty `labelFor` values, zero missing target ids, zero labels-with-targets lacking mnemonics, and zero obvious copied targets.

Treat possible missing `labelFor` results as candidates, not automatic fixes. Manually exclude status/readout labels, units, preview labels, placeholders, headers, and labels in nested panels where grid row/column positions only look adjacent by coincidence.

## Workflow

1. Search with `rg` for the target class/API and nearby resource keys.
2. Inspect usages to decide whether each string is user-visible, internal, log-only, prompt/code, or a resource key.
3. Pick the functional area and family (`app`, `cfg`, `msg`, `ntf`, `prc`) from where the text appears.
4. Patch Java/.form/properties together, using static `txt(...)` imports in Java.
5. Re-scan the target files for remaining hardcoded user-visible strings.
6. Run `git diff --check`. Run the narrow compile/test command when useful; report unrelated existing blockers clearly.
