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
- Text templates live under `src/main/resources/textTemplates`, mirroring the Java package of the lookup class.
- Load user-visible, localizable templates with `TextResources.getLocalizable(...)`. Load internal prompts, model instructions, generated syntax, and other non-user-visible templates with `TextResources.getInternal(...)` and mark the resource name/content `@NonNls`.
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

## Locale Translation Workflow

When translating an entire resource bundle or a locale-specific `DBNResources_*.properties` file, prioritize trustable contextual translation over broad automated coverage.

- Use only the unqualified base resources as the translation source of truth: `DBNResources.properties` for bundles and non-localized `.html.ft` templates for template variants. Never translate from another localized file.
- Prefer generic language locale files for broadly applicable translations, for example `DBNResources_de.properties` and `*_de.html.ft`. Use region-qualified variants such as `_de_DE` only for regional overrides.
- Translate only localizable text templates, currently user-visible `*.html.ft` files loaded through `TextResources.getLocalizable(...)`. Do not create locale variants for internal `*.md.ft` prompt/instruction templates loaded through `TextResources.getInternal(...)`.
- Translate key by key from the unqualified base bundle/template. Do not translate by dictionary substitution, regex word replacement, or word-by-word fallback.
- Use the resource key as context:
  - `label`, `column`, `field`, `placeholder`, `const`, and `token` values are usually noun phrases or compact domain terms.
  - `action`, `button`, `link`, and menu-like values are usually action phrases; prefer the target language's standard imperative or command style for UI controls.
  - `title` values are concise dialog, pane, popup, or process titles; translate as headers, not explanatory sentences.
  - `url` values are not translated as prose; choose a verified locale-specific target when available, otherwise keep the base URL.
  - `error`, `warning`, `info`, `message`, and `question` values are full user-facing messages; translate the whole sentence or paragraph in context.
  - `hint`, `text`, `tooltip`, and accessibility values often need natural sentence-level translation, not literal English word order.
- Build or update a glossary before translating a large locale. Include product names, Oracle/IntelliJ terminology, DBN domain terms, object types, action verbs, and terms intentionally kept in English.
- Preserve technical literals and placeholders exactly: `{0}`, MessageFormat choice patterns, escaped `\n`, HTML tags, mnemonic markers (`&Name`, `Na&me`), SQL/API/class names, file names, and protocol literals.
- Do not duplicate non-localizable code snippets into localized bundles. If a key family like `.code.` appears, inspect the caller and move the snippet to `@NonNls` code instead of translating it.
- Leave uncertain strings untranslated or mark them for review instead of inventing fluent-looking text. Never hide uncertainty by producing hybrid target-language output.
- For languages no maintainer can review, do not claim release-ready quality. Report the file as structurally safe and contextually translated, with any unresolved glossary or reviewer gaps called out.

### Batch-First Translation

Translate large bundles in small, isolated batches instead of sweeping the whole file.

- Partition resource keys by `family.area.element`, for example `cfg.connection.label`, `msg.connection.error`, `app.vector.title`, `ntf.debugger.info`, and `prc.java.text`. This is the default batch unit for bundle translation.
- If a `family.area.element` group is still large or semantically mixed, split it by stable key-name prefix or a contiguous block of related keys. Do not mix unrelated UI surfaces just because they are nearby in the file.
- Before translating a batch, read the unqualified source keys and values for that batch only. Identify whether each value is a noun label, command/action, title/header, sentence, inserted fragment, enum/object type, log line, progress line, validation message, URL, or technical literal.
- Translate the whole phrase from the source value and key context. Do not translate word by word, do not regex-replace terminology, and do not use another localized file as the source.
- Finish and verify one batch before starting the next. If a batch fails placeholder, capitalization, stale-English, or semantic checks, fix it before translating more keys.
- Keep progress notes by batch. When reporting results, identify the completed groups and any groups intentionally deferred for context review.

### Translation Glossary

Maintain a working glossary for every locale translation pass, and update it before each batch.

- Include DBN product terms, Oracle/IntelliJ terms, database object names, action verbs, AI/vector terms, log/progress verbs, validation verbs, and terms intentionally kept in English.
- Record the source term, target term, whether the English term is intentionally retained, the reason/context, and example keys. Keep the glossary compact enough to use actively while translating.
- Use `references/translation-glossary-template.md` when a persistent or shareable glossary is useful.
- Use the glossary to keep terminology and sentence composition stable across batches, for example the same choices for `connection`, `schema`, `statement`, `Wallet`, `embedding`, `chunking`, `source code`, `commit`, `rollback`, `grant`, and `debug session`.
- When a term has different meanings by context, record those meanings separately instead of forcing one translation everywhere. Examples: `source` as code source, data source, event source, or source schema; `type` as a database object type, Java type, or UI category; `view` as a database view or UI view.
- Reconcile the glossary after each batch by scanning the target locale for inconsistent synonyms and hybrid phrases.
- If a locale will be maintained across sessions, preserve the accepted glossary in the localization skill references or another project-approved note. Otherwise, report the important glossary decisions in the final response.

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

### Batch Verification Gate

Run these checks for every translated batch before moving to the next batch.

- Key parity: the locale bundle must have the same key set as the unqualified source bundle unless the task explicitly adds or removes keys.
- Placeholder parity: every `{0}`, `{1}`, MessageFormat choice/plural pattern, escaped newline, HTML boundary, mnemonic marker (`&Name`, `Na&me`), and quoted technical literal must be preserved or intentionally changed with caller inspection. Do not count textual ` & ` as a mnemonic.
- Source capitalization check: compare each translated value against the unqualified source value. For standalone roles such as `action`, `button`, `title`, `column`, `error`, `warning`, `info`, `message`, `question`, `aria`, user-facing `text`, progress, notification, and log lines, review any value where the source starts with an uppercase letter but the translation starts lowercase. Treat inserted fragments, `token`, `const`, `unit`, `placeholder`, object type names, URLs, and code-like values as exceptions only after classification.
- Role capitalization check: independently scan translated `title`, `error`, `warning`, `info`, `question`, `message`, progress, notification, and log values for lowercase sentence starts. Fix full-sentence and standalone values; leave lowercase fragments only when they are intentionally composed into another sentence.
- Stale-English check: compare exact source/target matches and scan for common English verbs/nouns. Classify every hit as an intentional product name/acronym/technical literal or a translation defect. Do not allow hybrid target-language values such as translated words mixed with English verbs.
- Textual ampersand check: scan the unqualified base bundle for prose ` & `. Replace it with `and` unless it is part of an official product name or literal. Do not make localized values preserve a textual ampersand as a mnemonic marker.
- Glossary consistency check: scan the target batch and previously translated batches for terms from the working glossary. Fix inconsistent synonyms unless context requires a separate glossary entry.
- Semantic ambiguity check: inspect source values containing ambiguous terms for the batch, especially `schema`, `view`, `grant`, `profile`, `credential`, `wallet`, `driver`, `statement`, `console`, `object`, `type`, `session`, `model`, `source`, `commit`, and `rollback`.
- UI role check: verify that labels/columns/constants are noun phrases, actions/buttons are commands, titles are headers, progress text describes ongoing work, validation messages are direct recovery instructions, and log lines read as complete console output.
- Diff check: run `git diff --check` for the touched locale files after each substantial batch.

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

### Semantic Translation Review

Run these passes when reviewing a translated bundle for meaning, not just structure. Work in small batches, inspect callers for every suspicious key, and prefer leaving a note over making an unsupported fluent-sounding guess.

- Ambiguous English terms: scan for source values containing terms with multiple DBN meanings, such as `schema`, `view`, `grant`, `profile`, `credential`, `wallet`, `driver`, `statement`, `console`, `object`, `type`, `session`, and `model`. Confirm the translated value reflects the caller's domain meaning.
- Terms intentionally kept in English: verify consistency for `Workspace`, `Dataset`, `View`, `Tool`, `Embedding`, `Chunking`, `Vector`, `Wallet`, `Select AI`, `MCP`, `LLM`, and `OCI`. Check whether each term should remain English in DB/AI UI or use a German domain term.
- Action versus label wording: inspect keys where the same English word can be noun or verb, such as `Refresh`, `Open`, `Load`, `Create`, `Grant`, `Verify`, `Apply`, and `Compile`. Actions/buttons should read as commands; labels/titles should read as nouns or headers.
- Database-domain collocations: check that translated terms read like database software, for example `Schema`, `View`, `Tabelle`, `Ausführungsberechtigung`, `Verbindung`, `Treiberbibliothek`, `SQL-Anweisung`, and `Datenbankobjekt`.
- Runtime composition: for values with `{0}` or inserted fragments, inspect the final composed sentence in the caller. German grammar can break when a placeholder controls gender, number, article, or case.
- Glossary consistency: build a small glossary from accepted choices and scan for inconsistent alternatives before changing values.

Useful starting scans:

```bash
rg -n -i '\b(schema|view|grant|profile|credential|wallet|driver|statement|console|object|type|session|model)\b' src/main/resources/messages/DBNResources.properties
rg -n -i '\b(workspace|dataset|view|tool|embedding|chunking|vector|wallet|select ai|mcp|llm|oci)\b' src/main/resources/messages/DBNResources.properties
rg -n -i '\b(refresh|open|load|create|grant|verify|apply|compile)\b' src/main/resources/messages/DBNResources.properties
rg -n '\{[0-9]' src/main/resources/messages/DBNResources_de.properties
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

For a broad audit, use a namespace-aware XML scan rather than grep only. Verify that all `labelFor` values resolve to component ids, that localized `JLabel` values with `labelFor` contain a mnemonic, and that same-row copied targets are reviewed manually. After editing, rerun the scan and expect zero empty `labelFor` values, zero missing target ids, zero labels-with-targets lacking mnemonics, and zero obvious copied targets.

Treat possible missing `labelFor` results as candidates, not automatic fixes. Manually exclude status/readout labels, units, preview labels, placeholders, headers, and labels in nested panels where grid row/column positions only look adjacent by coincidence.

## Workflow

1. Search with `rg` for the target class/API and nearby resource keys.
2. Inspect usages to decide whether each string is user-visible, internal, log-only, prompt/code, or a resource key.
3. Pick the functional area and family (`app`, `cfg`, `msg`, `ntf`, `prc`) from where the text appears.
4. Patch Java/.form/properties together, using static `txt(...)` imports in Java.
5. Re-scan the target files for remaining hardcoded user-visible strings.
6. Run `git diff --check`. Run the narrow compile/test command when useful; report unrelated existing blockers clearly.
