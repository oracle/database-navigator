# Parser Element XML Reference

## Mental Model

DBN parser XML files are declarative grammar trees. `element-defs` is the dialect bundle. Each top-level `element-def` creates a named reusable grammar node. The runtime maps XML tags through `ElementTypeDefinition` into `ElementTypeBase` subclasses, builds lookup caches from possible tokens, then parses through the corresponding `*ElementTypeParser`.

The XML is also semantic. Identifier nodes do more than match text: they create PSI elements that support object resolution, aliases, variables, references, rename behavior, structure view, and executable statement metadata.

## Specification References

- SQL-92 / ISO/IEC 9075:1992 draft text: `https://www.contrib.andrew.cmu.edu/~shadow/sql/sql1992.txt`
- MySQL 9.7 SQL statements reference: `https://dev.mysql.com/doc/refman/9.7/en/sql-statements.html`

The SQL-92 public link above is an online draft copy, not the official ISO publication. Use it as a convenient grammar reference, and prefer exact specification term names when creating parser element ids.

When working from vendor documentation such as MySQL, start from the linked statement overview, open the exact statement page being changed, and keep grammar element names close to the documented production names. Parser XML files may include one top-level overview reference comment for their dialect, but detailed specification URLs belong here.

## DBN Dev Tooling

Language parser tooling lives in `modules/dbn-dev/src/main/java/com/dbn/dev/language`.

- `LanguageSpecificationBuilder` is the interactive entry point.
- `LanguageSpecificationBuilderInput` maps database/language choices to dialect paths and file prefixes.
- `LanguageSpecificationLexerBuilder` updates parser lexer sections, parser token XMLs, and highlighter lexer sections from source token lists.
- `LanguageSpecificationLexerClassBuilder` is invoked through `LanguageSpecificationBuilder` operation `LEXER_CLASS` to run JFlex and generate lexer classes from `.flex` files.
- `LanguageSpecificationParserBuilder` instantiates parser bundles with `ElementTypeBundle.Builder.rebuilding = true`; it is the parser-definition rebuilding hook.
- Shared builder configuration is loaded from `modules/dbn-dev/language-builder.properties`, with JVM `-D` properties taking precedence. Commit only `language-builder.properties.example`; keep the real local file ignored.

Source token lists live under `modules/dbn-dev/src/main/resources/language/<database>/`, for example `oracle_sql_keywords.txt` or `postgres_sql_datatypes.txt`. Generated parser token files live beside the dialect parser XMLs as `*_parser_tokens.xml`.

When grammar work needs a new token, check this module and the relevant token registry/source list first. New token keys must be registered in the appropriate registry, such as `db_sql_keywords` and sibling keyword/function/datatype/parameter registries, before parser element XML references them. Do not invent a `type-id` that is absent from the dialect token XMLs unless the corresponding token-generation work is also part of the change.

`*_parser_tokens.xml` and `*_parser.flex` are mixed files: some surrounding content is hand-written, while marker-delimited token/lexer blocks are filled by `dbn-dev` tooling. Do not edit tooling-managed marker blocks directly. Change the dbn-dev token registry/source list or lexer input, then let the developer run the tooling to refresh those blocks. Generated flex lexer Java files are pure generated outputs and should not be edited directly.

Keep token registry files sorted and de-duplicated. Empty registry files are valid and should produce no marker-block entries. If a tooling run emits empty generated tokens such as `PRM_` or `EX_`, or empty lexer rules such as `"" {return tt.ptt(0);}`, fix the dbn-dev registry loading/generation logic and ask the developer to rerun the tooling rather than editing the generated marker blocks by hand.

Treat `dbn-dev` language tooling execution as a developer task for now. Do not run builds or tooling from `modules/dbn-dev` unless the developer explicitly asks for it; inspect and edit source/registry files only.

ISO92 is modeled as `DatabaseType.ISO92` for language tooling even though it is not a real connection database. Keep it excluded from public connection support arrays such as `SUPPORTED` and `NATIVELY_SUPPORTED`, like other non-offered enum values. Its registry path is `modules/dbn-dev/src/main/resources/language/iso92`, and the generated SQL files use the `iso92_sql_*` prefix.

## Token Id Patterns

`LanguageSpecificationLexerBuilder.TokenDefinition.toParserTokenDefinition()` defines the generated token id prefixes:

- `KW_`: keyword tokens from `*_keywords.txt`
- `FN_`: function tokens from `*_functions.txt`
- `DT_`: datatype tokens from `*_datatypes.txt`
- `PRM_`: parameter tokens from `*_parameters.txt`
- `EX_`: exception tokens from `*_exceptions.txt`

The builder converts spaces in source token names to underscores for token ids. For example a source keyword `ORDER BY` becomes `KW_ORDER_BY` and lexer text matching uses whitespace-aware fragments.

Other token families are usually maintained in shared or dialect parser token XMLs rather than generated by those category lists:

- `CHR_*`: punctuation/character tokens, such as `CHR_DOT`, `CHR_COMMA`, `CHR_LEFT_PARENTHESIS`
- `OPR_*`: operator tokens, such as `OPR_NOT_EQUAL`
- Generic lexical tokens: `IDENTIFIER`, `VARIABLE`, `STRING`, `NUMBER`, `INTEGER`, `LINE_COMMENT`, `BLOCK_COMMENT`, `WHITE_SPACE`

Always verify a token id in the relevant `*_parser_tokens.xml` or `db_language_common_tokens.xml` before using it in `type-id`, `separator`, `begin-token`, `end-token`, or compact `tokens="..."` attributes. If a generated-family token is missing, add it to the dbn-dev registry/input, not directly to the tooling-managed marker block in parser token XML.

When a word can be interpreted in more than one token role, register it in the most common or lexer-practical generated family and qualify local grammar usage with `flavor`. For example, if `INTERVAL` is defined as `DT_INTERVAL` because it is most often a datatype token, a grammar position that treats it as a keyword can use `<token type-id="DT_INTERVAL" flavor="keyword" />`. Do not duplicate the same word in multiple generated registries only to change the local role: the lexer can match each word only once, so duplicate generated rules clash instead of creating context-sensitive token categories.

## Generated Element Ids

Existing parser element ids are generated numeric identifiers. Preserve existing ids when editing existing XML, but do not assign ids to newly added parser elements manually. Leave new elements without `id`; the parser tooling/reindexer is responsible for assigning and normalizing ids.

Example:

```xml
<sequence optional="true">
    <token type-id="KW_WHATEVER"/>
    <element ref-id="some_clause"/>
</sequence>
```

Do not create local next-number ids such as `id="05647"` for new elements by hand.

## Top-Level Shape

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE element-defs SYSTEM "../../../common/definition/language-parser-elements.dtd">
<element-defs language="ORACLE-SQL" version="10.1">
    <element-def id="sql_block" description="SQL block" attributes="ROOT, STRUCTURE">
        ...
    </element-def>
</element-defs>
```

`element-defs` attributes:

- `language`: dialect id such as `ORACLE-SQL`, `MYSQL-SQL`, `POSTGRES-PSQL`.
- `version`: dialect grammar version string.

`element-def` is a named reusable grammar definition. Its `id` is referenced by `<element ref-id="..."/>`. Common attributes:

- `description`: human description used in diagnostics and tooling.
- `attributes`: comma-separated `ElementTypeAttribute` names such as `ROOT`, `STRUCTURE`, `STATEMENT`, `EXECUTABLE`, `CLAUSE`, `SUBJECT`.
- `language`: usually `SQL` or `PSQL` for root/cross-language definitions.
- `icon`: icon key for structure/presentation.
- `branch`: declares a parser branch flag when this element matches.
- `optional-wrapping`: optional paired delimiters around the element, usually `PARENTHESES`.
- `virtual-object`: DB object type represented by this element.
- `custom="true"`: marks an intentional helper definition, usually a repetitive grammar structure defined once and reused from multiple locations rather than a direct SQL specification term.
- `formatting-*`: formatter behavior for PSI generated from this grammar.

## Element Naming and Descriptions

Top-level `element-def id` values should be lowercase underscored versions of the SQL specification term or local grammar concept:

- `query specification` -> `query_specification`
- `select statement` -> `select_statement`
- `group by clause` -> `group_by_clause`
- `value expression primary` -> `value_expression_primary`

Descriptions should be human-readable versions of the id/specification term. When the description contains actual SQL keywords from that grammar block, write those keywords uppercase:

- `select_statement`: `SELECT statement`
- `group_by_clause`: `GROUP BY clause`
- `order_by_clause`: `ORDER BY clause`
- `query_specification`: `Query specification`
- `value_expression`: `Value expression`

Do not invent camelCase, dash-case, or abbreviated ids for specification terms. Prefer the standard term normalized to lowercase underscore.

Helper elements marked `custom="true"` may use local grammar names instead of exact specification terms, especially when they capture a repeated parser structure. Still keep names lowercase underscored and descriptions clear.

Statement structures that qualify as clauses should carry `attributes="CLAUSE"`, for example `from_clause`, `where_clause`, `group_by_clause`, and `having_clause`. This is intended for formatting behavior even if not all formatting logic currently consumes it.

## Composition Elements

### `token`

Matches a lexer token by `type-id`.

```xml
<token id="00003" type-id="KW_ALTER"/>
```

Useful attributes:

- `optional="true"` when the token is optional inside a sequence-like parent.
- `text` for token text constraints when supported by surrounding code.
- `flavor` for keyword-like parameter tokens.
- `branch`, `branch-check`, `version`, `exit`, `original-name`.

Before adding or changing a `type-id`, check the token id patterns above and the generated token XMLs.

### `element`

References a named top-level `element-def`.

```xml
<element ref-id="where_clause" optional="true"/>
```

Use this for reuse and named grammar concepts. `ref-id` points to an `element-def id`. DBN ids are treated as `CDATA` in the DTD because generated child ids often start with digits. When adding new inline parser elements, omit generated numeric `id` attributes and let the reindexing tool assign them.

### `sequence`

Matches children in order. A `sequence` succeeds when its required children match; optional children may be skipped. Parser recovery uses later child tokens as landmarks.

```xml
<sequence id="02387" optional="true">
    <token id="02388" type-id="KW_USING"/>
    <element ref-id="wrapped_column_list"/>
</sequence>
```

Notes:

- Child `optional="true"` is meaningful here.
- `exit="true"` is only valid for direct children of a sequence and affects partial-match behavior. Treat it as deprecated: existing uses are a workaround for ambiguous parser branches, and new definitions should not add it. Ambiguous branches should eventually be handled with trie-based look-ahead.
- `tokens="KW_A,KW_B"` creates a compact fixed token sequence.
- Use a nested `sequence` as the single child of `iteration` for repeated multi-part constructs.

### `one-of`

Matches the first alternative that parses.

```xml
<one-of id="00001">
    <element ref-id="select_statement"/>
    <element ref-id="insert_statement"/>
</one-of>
```

Notes:

- Child-level `optional="true"` is not supported and logs a runtime warning.
- Put optionality on the `one-of` itself or on a parent sequence child.
- `sortable="true"` allows runtime reordering by token categories.
- Overlapping alternatives remain as raw children; generated parser-extension look-ahead narrows candidates without rewriting the grammar tree.
- `tokens="KW_A,KW_B"` creates compact token alternatives.

### `iteration`

Repeats exactly one child element, optionally separated by one or more separator tokens.

```xml
<iteration id="00004" separator="CHR_COMMA">
    <element ref-id="expression"/>
</iteration>
```

Notes:

- Must contain exactly one child. The runtime throws if there are zero or multiple children.
- If the repeated unit has multiple parts, wrap them in a child `sequence`.
- `separator` accepts comma-separated token ids.
- `elements-count` can constrain allowed counts, including ranges such as `1-3`.
- `min-iterations` sets a minimum repeat count.

### `wrapper`

Matches a paired begin/end token around one child.

```xml
<wrapper id="00015" template="PARENTHESES">
    <iteration id="00016" separator="CHR_COMMA">
        <element ref-id="expression"/>
    </iteration>
</wrapper>
```

Notes:

- Must contain exactly one child.
- `template` maps through `TokenPairTemplate`, commonly `PARENTHESES`, `BEGIN_END`, `BRACKETS`, `BRACES`.
- Alternatively use explicit `begin-token` and `end-token`.
- Child `optional="true"` makes the wrapped content optional, not the delimiters.

### `qualified-identifier` and `variant`

Models multi-part identifiers such as `schema.table` or dialect-specific object names.

```xml
<qualified-identifier id="00005" separator="CHR_DOT">
    <variant>
        <object-ref id="00006" optional="true" type="SCHEMA"/>
        <object-ref id="00007" type="TABLE" attributes="SUBJECT"/>
    </variant>
</qualified-identifier>
```

Notes:

- `separator` is usually `CHR_DOT`, but some Oracle definitions omit it.
- Contains one or more `variant` children.
- A variant contains token and semantic identifier nodes.

## Structural Child-Count Rules

These constraints come from the runtime element loaders, not only the DTD:

- `iteration`: exactly one child. Use a child `sequence` when the repeated item has several tokens/elements.
- `wrapper`: exactly one child. Use a child `sequence`, `one-of`, `iteration`, `element`, `token`, or another supported single child as appropriate.
- `qualified-identifier`: one or more `variant` children.
- `variant`: token/identifier leaf children such as `token`, `object-def`, `object-ref`, `alias-def`, `alias-ref`, `variable-ref`.
- `sequence`: zero or more children, matched in order.
- `one-of`: one or more alternatives. Prefer two or more for a finished local choice, but a single-child `one-of` is acceptable when it intentionally reserves an alternatives slot, such as a statement list that will gain more statement kinds later. The runtime throws for no children.
- `element-def`: zero or more grammar children, but useful named definitions normally contain at least one child.

## Semantic Identifier Nodes

Identifier tags map to `IdentifierElementType` and create definition/reference PSI:

- `object-def`: defines a database object.
- `object-ref`: references a database object.
- `alias-def`: defines an alias.
- `alias-ref`: references an alias.
- `variable-def`: defines a variable.
- `variable-ref`: references a variable.
- `exec-variable`: execution variable placeholder.

Common attributes:

- `type`: mandatory DB object/category type for object/alias/variable nodes.
- `attributes`: semantic flags such as `SUBJECT`.
- `optional`: optional in sequence-like parents and qualified identifier variants.
- `referenceable="true"`: reference participates in reference behavior.
- `local="true"`: local reference behavior.
- `underlying-object-resolver`: resolver id for definitions that represent underlying objects.
- `original-name`: source grammar name/hint.

Use these instead of raw `token type-id="IDENTIFIER"` when name resolution, aliasing, object lookup, rename, or structure behavior matters.

## Branches, Versions, and Optionality

- `branch="name"` marks a parse branch when that element matches.
- `branch-check="+name"` is intended to require a branch; `branch-check="-name"` rejects when a branch exists. Version-qualified checks such as `+br_with@8.4` appear in Postgres grammar.
- `version` gates an element by dialect version.
- `optional="true"` works on sequence children, referenced elements, tokens, wrappers, identifiers, and composition nodes where the runtime models optional children.
- Do not rely on optional children inside `one-of` or `iteration`; runtime code warns because those positions are unsupported.
- Do not copy existing `exit="true"` markers into new grammar. They are deprecated ambiguity workarounds, not the intended long-term branch strategy.

### Branch Runtime Model

Branch state is scoped parser context. `ElementTypeBase.loadDefinition()` reads `branch` into a `Branch`. `SequenceElementType` and `OneOfElementType` read child `branch-check` strings into `ElementTypeRef.branchChecks`.

When an element with `branch="..."` successfully parses, `ElementTypeParser.stepOut()` calls `context.addBranchMarker(node, branch)`. The context associates the branch with the nearest enclosing `NamedElementType`, which usually means the current `element-def`. When that named element is exited, `context.removeBranchMarkers(...)` removes branch markers scoped to it.

Use branches to make later siblings or alternatives conditional on earlier optional grammar:

```xml
<sequence id="00043" optional="true" branch="br_or_replace">
    <token id="00044" type-id="KW_OR"/>
    <token id="00045" type-id="KW_REPLACE"/>
</sequence>
<token id="00046" type-id="KW_TEMPORARY" optional="true" branch="br_temporary"/>
<one-of id="00047">
    <element ref-id="create_view" branch-check="-br_temporary"/>
    <element ref-id="create_table" branch-check="-br_or_replace"/>
</one-of>
```

In this MySQL pattern, parsing `OR REPLACE` blocks `create_table`, and parsing `TEMPORARY` blocks `create_view`.

Postgres uses the same mechanism for `WITH`:

```xml
<element optional="true" ref-id="with_clause" version="8.4"/>
<one-of id="00001">
    <element branch-check="-br_with" ref-id="create_statement"/>
    <element branch-check="+br_with@8.4" ref-id="select_statement"/>
    <element branch-check="+br_with@9.1" ref-id="insert_statement"/>
</one-of>
```

The intended meaning is: without `WITH`, parse ordinary statements; with `WITH`, allow only statement kinds that support `WITH` at the current DB version.

Implementation caveat: current `ElementTypeRef.check(...)` clearly enforces forbidden checks (`-branch`) when a matching branch is active, but allowed checks (`+branch`) appear not to be enforced as written because the accumulator starts as `true`, and branch checks are skipped entirely when no branch exists. Treat `+branch` as intended schema meaning, but verify runtime behavior before relying on it for new grammar until this implementation is corrected.

## Formatting and Presentation

Formatting attributes are consumed by `FormattingDefinitionFactory`:

- `formatting-wrap`: `NORMAL`, `ALWAYS`, `IF_LONG`
- `formatting-indent`: `NORMAL`, `CONTINUE`, `NONE`, `ABSOLUTE_NONE`
- `formatting-spacing-before`: `NO_SPACE`, `ONE_SPACE`, `ONE_LINE`, `MIN_LINE_BREAK`, `LINE_BREAK`
- `formatting-spacing-after`: `NO_SPACE`, `ONE_SPACE`, `ONE_LINE`

Statement elements automatically get statement formatting defaults when they carry the `STATEMENT` attribute.

## DTD Maintenance

The shared DTD should describe the real parser XML surface, not an idealized XML schema. Keep in mind:

- Generated child ids such as `00001` are not valid XML `ID` values; use `CDATA` for `id` and `ref-id`.
- New parser elements should be valid without manually assigned ids; the reindexing tool owns generated ids.
- `qualified-identifier separator` is optional because some Oracle definitions omit it.
- `sequence` may contain text content in existing PSQL XML, so the DTD uses mixed content for it.
- If adding a new wrapper template, confirm `TokenPairTemplate` supports it.
- If adding a new XML tag, add it to `ElementTypeDefinition` and implement/cache/parser classes as needed, not only to the DTD.

## Review Checklist

- Does the new grammar construct exist in another dialect with a reusable pattern?
- After resolving the reported syntax gap, were adjacent forms in the same construct checked for missing operators, clauses, delimiters, nesting, or token-category collisions?
- Do new `element-def` ids use lowercase underscored specification names and human-readable descriptions with SQL keywords uppercased?
- Do overlapping alternatives have sufficient generated look-ahead to distinguish their first-token paths?
- Is a repeated multi-token construct wrapped as a single child `sequence` inside `iteration`?
- Are semantic identifiers used where resolution/rename/structure behavior matters?
- Did the change avoid adding new deprecated `exit="true"` branch workarounds?
- Did new parser elements omit manual generated ids so the reindexing tool can assign them?
- Were new or changed generated-family token ids registered in the appropriate dbn-dev token registry/source list, with parser token/flex marker blocks left to the tooling?
- Does the shared DTD include any new attribute/tag/template?
- Do edited XML files keep the relative DOCTYPE?
- Did `xmllint --noout --valid` pass for edited files?
