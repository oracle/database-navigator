---
name: dbn-localization-guide
description: Oracle© Database Navigator (DBN) localization and NLS guide. Use when adding, reviewing, renaming, sorting, or moving DBN resource keys; replacing hardcoded user-visible Java/UI/.form/action/progress/message/notification strings with txt(...); deciding app/cfg/msg/ntf/prc key families and element types; handling @Nls, @NonNls, @PropertyKey, DBNResources.properties, and localization validation.
---

# DBN Localization Guide

## Scope

Use this skill for localization-only or localization-heavy changes in DBN. Pair it with `dbn-translation-guide` when translating or reviewing wording, maintaining locale glossaries, or checking terminology consistency. Pair it with `dbn-codebase-guide` only when the task also needs broader architecture, service, UI, or persistence guidance.

Keep localization edits narrowly scoped. Do not refactor unrelated code while replacing hardcoded strings.

Before changing Java, XML, `.form`, or resource files, inspect enough context to identify open points. Proceed directly for straightforward localized fixes. Ask for confirmation before broad NLS cleanup, key renames, visible wording changes, `.form` rewiring, or changes where callers/composition are unclear.

## Resource Access

- Resource bundle: `src/main/resources/messages/DBNResources.properties`.
- Access localized text with `import static com.dbn.nls.NlsResources.txt;`.
- Code should call `txt(...)` directly. Do not introduce wrapper helpers around `NlsResources`.
- Text templates live under `src/main/resources/textTemplates`, mirroring the Java package of the lookup class.
- Load user-visible, localizable templates with `TextResources.getLocalizable(...)`. Load internal prompts, model instructions, generated syntax, and other non-user-visible templates with `TextResources.getInternal(...)` and mark the resource name/content `@NonNls`.
- IntelliJ intention descriptions live under `src/main/resources/intentionDescriptions/<IntentionClassSimpleName>/description.html`; bundled translations use sibling files such as `description_de.html`, `description_ja.html`, and every standalone HTML file should declare a matching `<html lang="...">`.
- IntelliJ intention categories in `plugin.xml` should use `<categoryKey>` rather than literal `<category>` when they are plugin-provided. `categoryKey` resolves against the plugin-level `<resource-bundle>` by default. Prefer one DBN-style key per complete category path, using `app.<component>.category.<Title>`, and put any hierarchy slash in the translated value, for example `Database/Statement execution`; the platform resolves the key first, then splits the value into category path segments.
- IntelliJ intention before/after examples are code samples, not prose translations. Keep them only when they are real examples using the platform naming pattern `before.<language-extension>.template` / `after.<language-extension>.template`; otherwise set `<skipBeforeAfter>true</skipBeforeAfter>` on the `intentionAction` in `plugin.xml`.
- File-template description HTML lives under `src/main/resources/fileTemplates/*.html`. Treat visible description prose there as localizable content when translations are added, and keep standalone HTML metadata such as `lang` valid.
- Prefer passing translated text into constructors/APIs. Pass keys only when the callee explicitly expects keys.
- Use `@Nls` for localized text values and `@NonNls` for internal strings such as SQL, code, model prompts, file paths, IDs, log-only text, wire protocols, and generated syntax.
- Do not put executable code snippets, SQL/PLSQL blocks, generated syntax, or protocol payloads in resource bundles. Keep them as `@NonNls` code constants/helpers and set UI fields from code.
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
- `url`: localized hyperlink targets, documentation pages, API pages, and other URLs that may differ by locale.
- `hint`, `link`, `question`, `info`, `warning`, `error`: use when the role is more specific, especially under `cfg.*` and `msg.*`.

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
- Preserve mnemonic markers (`&`) in form labels and button text when the ampersand marks a keyboard mnemonic, for example `&Name` or `Na&me`.
- Do not treat textual ampersands as mnemonics. If the unqualified base value uses ` & ` to mean "and", replace it in the base value with `and` unless it is part of an official product name or literal. Localized values should translate the word naturally and should not gain a mnemonic marker.
- Newline and HTML values belong in the properties file with escaped `\n` or existing HTML formatting.
- Use separate `link` and `url` keys when a localized link caption can point to a locale-specific target, for example `cfg.assistant.link.SelectAiDocs` plus `cfg.assistant.url.SelectAiDocs`.
- Product names, provider names, SQL keywords, API literals, generated code, and model prompts are often `@NonNls`, not resource keys.

## Translation Handoff

For translation wording, glossary decisions, batch semantics, stale-English scans, and reviewer caveats, use `dbn-translation-guide`.

This localization skill still owns the DBN mechanics:

- Use only the unqualified base resources as the source of truth for locale variants.
- Prefer generic language locale files for broadly applicable translations, for example `DBNResources_de.properties` and `*_de.html.ft`. Use region-qualified variants such as `_de_DE` only for regional overrides.
- Translate only localizable text templates, currently user-visible `*.html.ft` files loaded through `TextResources.getLocalizable(...)`, IntelliJ intention `description.html` files, and user-visible file-template description HTML. Do not create locale variants for internal `*.md.ft` prompt/instruction templates loaded through `TextResources.getInternal(...)` or for intention before/after code examples.
- Preserve every resource key unless the task explicitly adds, removes, or renames keys.
- Preserve technical literals and placeholders exactly: `{0}`, MessageFormat choice patterns, escaped `\n`, HTML tags, mnemonic markers (`&Name`, `Na&me`), SQL/API/class names, file names, and protocol literals.
- Do not duplicate non-localizable code snippets into localized bundles. If a key family like `.code.` appears, inspect the caller and move the snippet to `@NonNls` code instead of translating it.

## UI Writing Rules

- Use casing from `Sentence vs. Title Case`: commands and true headers normally use title capitalization, while sentence-like labels, descriptions, tooltips, and messages normally use sentence capitalization.
- Keep UI text short and clear: present tense, one idea per sentence, active voice, user-perspective wording, and no unnecessary generic words.
- Use ellipsis only for actions that open input-capable dialogs, truncated text without a scrollbar, or ongoing progress text.
- Do not end a single sentence or IDE action with a period. Use periods between multiple sentences.
- Use question marks only for confirmation alerts. Do not use exclamation points.
- Inspection NLS should be short, sentence-cased, problem-focused, and specific. Descriptions should start with a verb such as `Reports`.

## Verification Passes

Use verification passes for broad resource-file cleanup. Keep each pass focused on one rule, and re-scan after editing.

### Batch Verification Gate

Run these checks for every translated batch before moving to the next batch.

- Key parity: the locale bundle must have the same key set as the unqualified source bundle unless the task explicitly adds or removes keys.
- Placeholder and marker parity: every `{0}`, `{1}`, MessageFormat choice/plural pattern, escaped newline, HTML boundary, mnemonic marker, and quoted technical literal must be preserved or intentionally changed with caller inspection. Do not count textual ` & ` as a mnemonic.
- Source capitalization check: compare each translated value against the unqualified source value. For standalone roles such as `action`, `button`, `title`, `column`, `error`, `warning`, `info`, `message`, `question`, `aria`, user-facing `text`, progress, notification, and log lines, review any value where the source starts with an uppercase letter but the translation starts lowercase. Treat inserted fragments, `token`, `const`, `unit`, `placeholder`, object type names, URLs, and code-like values as exceptions only after classification.
- Role capitalization check: independently scan translated `title`, `error`, `warning`, `info`, `question`, `message`, progress, notification, and log values for lowercase sentence starts. Fix full-sentence and standalone values; leave lowercase fragments only when they are intentionally composed into another sentence.
- Textual ampersand check: scan the unqualified base bundle for prose ` & `. Replace it with `and` unless it is part of an official product name or literal. Do not make localized values preserve a textual ampersand as a mnemonic marker.
- Diff check: run `git diff --check` for the touched locale files after each substantial batch.
- For stale-English, glossary consistency, semantic ambiguity, and UI role checks, use `dbn-translation-guide`.

Useful starting checks:

```bash
ruby -e 'loc=ARGV.fetch(0); base={}; File.readlines("src/main/resources/messages/DBNResources.properties", chomp:true).each{|l| next if l.strip.empty? || l.start_with?("#"); k,v=l.split("=",2); base[k]=v if v}; File.readlines(loc, chomp:true).each_with_index{|l,i| next if l.strip.empty? || l.start_with?("#"); k,v=l.split("=",2); next unless v && base[k]; b=base[k].sub(/^\\ /,"").sub(/^\\ - /,""); f=v.sub(/^\\ /,"").sub(/^\\ - /,""); next unless b =~ /\A[A-Z]/ && f =~ /\A[[:lower:]]/; role=k.split(".")[2] || ""; next if %w[const token unit placeholder].include?(role); puts "#{i+1}:#{k}=#{v} [base=#{base[k]}]"}' src/main/resources/messages/DBNResources_fr.properties
ruby -e 'def load(path); h={}; File.readlines(path, chomp:true).each{|l| next if l.strip.empty? || l.start_with?("#"); k,v=l.split("=",2); h[k]=v if v}; h; end; base=load("src/main/resources/messages/DBNResources.properties"); loc=load(ARGV.fetch(0)); missing=base.keys-loc.keys; extra=loc.keys-base.keys; bad=[]; base.each{|k,bv| next unless loc[k]; bp=bv.scan(/\{[0-9]+(?:,[^{}]+)?\}/).sort; fp=loc[k].scan(/\{[0-9]+(?:,[^{}]+)?\}/).sort; bad << [k,bp,fp] if bp!=fp}; puts "missing=#{missing.size} extra=#{extra.size} placeholder_mismatches=#{bad.size}"; bad.first(20).each{|k,bp,fp| puts "#{k}: #{bp.inspect} != #{fp.inspect}"}' src/main/resources/messages/DBNResources_fr.properties
```

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
- Preserve mnemonic markers, placeholders (`{0}`), escaped newlines (`\n`), HTML markup, keyboard names, SQL keywords, object type names, acronyms, and product/provider names.
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
- Count only actual mnemonic markers during parity checks. A practical heuristic is that `&` followed by a non-space character can be a mnemonic candidate, while ` & ` is textual prose and should normally be written as `and` in the base English value.
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

For a broad audit, warn the developer that a repo-wide XML scan can be token-expensive and ask for confirmation before running it. Prefer targeted checks on touched `.form` files unless the developer requested a full audit. If a full audit is needed, ask the developer to run the namespace-aware XML scan locally and share only failures or relevant excerpts. Verify that all `labelFor` values resolve to component ids, that localized `JLabel` values with `labelFor` contain a mnemonic, and that same-row copied targets are reviewed manually.

Treat possible missing `labelFor` results as candidates, not automatic fixes. Manually exclude status/readout labels, units, preview labels, placeholders, headers, and labels in nested panels where grid row/column positions only look adjacent by coincidence.

## Workflow

1. Search with `rg` for the target class/API and nearby resource keys.
2. Inspect usages to decide whether each string is user-visible, internal, log-only, prompt/code, or a resource key.
3. Pick the functional area and family (`app`, `cfg`, `msg`, `ntf`, `prc`) from where the text appears.
4. Patch Java/.form/properties together, using static `txt(...)` imports in Java.
5. Re-scan the target files for remaining hardcoded user-visible strings.
6. Run `git diff --check`. Keep scans targeted to touched files unless the developer asks for a broad audit. Do not run Gradle/build/test/IDE validation unless the developer explicitly asks; instead ask the developer to run the narrow compile/test command when useful.
