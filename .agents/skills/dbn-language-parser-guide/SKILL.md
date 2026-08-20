---
name: dbn-language-parser-guide
description: Oracle Database Navigator (DBN) language parser guide. Use when changing, reviewing, explaining, validating, or generating SQL/PSQL parser element XML files, language-parser-elements.dtd, grammar element definitions, parser branches, wrappers, semantic object/alias/variable identifiers, and dialect parser definitions under com/dbn/language.
---

# DBN Language Parser Definitions

Use this skill for DBN SQL/PSQL parser grammar XML work, especially files named `*_parser_elements.xml` and the shared DTD at `src/main/java/com/dbn/language/common/definition/language-parser-elements.dtd`.

When a specification provides a syntax diagram or linked diagram-text definition, treat the diagram as the authoritative grammar source. Prefer it over reconstructing syntax from prose descriptions; use prose for semantics, restrictions, and examples.

When reading Oracle diagram text, use whitespace around parentheses as a useful structural cue: spaced forms such as `( MOUNT ... )` generally denote diagram grouping and should not become lexical SQL parentheses, while unspaced forms such as `(column, ...)` generally denote literal parentheses in the SQL syntax. Confirm this against the diagram and verified examples; if the diagram is unreadable or ambiguous, do not infer the grammar—ask for the diagram text or an image.

## Quick Workflow

1. Inspect the target dialect XML and at least one equivalent construct in sibling dialects before editing.
2. Read [references/parser-elements.md](references/parser-elements.md) when you need element semantics, allowed structures, validation rules, or examples.
3. For dialect syntax work, use the official links in [references/parser-elements.md](references/parser-elements.md) `Specification References`.
4. For token ids or lexer/token-list changes, inspect the `modules/dbn-dev` language tooling before inventing names.
5. Keep XML changes declarative and local to the grammar construct requested. Do not refactor unrelated statement definitions while adding one clause or alternative.
6. Preserve existing generated numeric child ids, but never add ids to new parser elements manually. Leave new elements without `id`; the parser tooling/reindexer assigns ids.
7. Keep dbn-dev token registries sorted, de-duplicated, and free of blank token entries.
8. Keep parser XMLs pointed at the shared DTD:

```xml
<!DOCTYPE element-defs SYSTEM "../../../common/definition/language-parser-elements.dtd">
```

9. If the XML uses a new tag, attribute, wrapper template, or child relationship, update `language-parser-elements.dtd` in the same change.
10. Validate edited parser XMLs with `xmllint --noout --valid <files>` when available.

## Important Files

- SQL dialect grammars: `src/main/java/com/dbn/language/sql/dialect/*/*_sql_parser_elements.xml`
- PSQL dialect grammars: `src/main/java/com/dbn/language/psql/dialect/*/*_psql_parser_elements.xml` and Oracle `oracle_plsql_parser_elements.xml`
- Shared DTD: `src/main/java/com/dbn/language/common/definition/language-parser-elements.dtd`
- Parser extension DTD: `src/main/java/com/dbn/language/common/definition/language-parser-elements-ext.dtd`
- Language tooling module: `modules/dbn-dev/src/main/java/com/dbn/dev/language`
- Parser extension generator: `modules/dbn-dev/src/main/java/com/dbn/dev/language/LanguageSpecificationParserExtensionBuilder.java`
- Source token registries/lists for generated token ids: `modules/dbn-dev/src/main/resources/language` and the relevant `db_*_keywords`/function/datatype/parameter registries
- Parser token XMLs: `src/main/java/com/dbn/language/**/dialect/*/*_parser_tokens.xml` are generated artifacts. Do not add generated tokens directly there; update the maintained `modules/dbn-dev` registry/source list and let tooling regenerate the token XML.
- Parser/highlighter flex sources: `src/main/java/com/dbn/language/**/dialect/*/*_parser.flex` and `*_highlighter.flex` are partially generated: hand-written lexer rules outside marker blocks may be edited, but generated `MARKER_*` blocks should be refreshed through tooling.
- Parser extension XMLs: `src/main/java/com/dbn/language/**/dialect/*/*_parser_elements_ext.xml` (generated ambiguity-resolution metadata)
- Generated flex lexer Java files: generated outputs; do not edit directly
- Runtime element definitions: `src/main/java/com/dbn/language/common/element/util/ElementTypeDefinition.java`
- Core runtime model: `src/main/java/com/dbn/language/common/element/impl/*ElementType.java`
- Parser implementations: `src/main/java/com/dbn/language/common/element/parser/impl/*ElementTypeParser.java`

## Editing Guidance

- Prefer explicit child elements for complex grammar. Use `tokens="A,B,C"` only for simple fixed token sequences or alternatives already following that style.
- `one-of` children are alternatives; child-level `optional="true"` is not supported there and the runtime warns about it. Put optionality on the `one-of` itself or wrap the optional alternative in a parent `sequence`.
- `iteration` and `wrapper` must each contain exactly one child. Put repeated or wrapped multi-part constructs inside a single child `sequence`.
- For deeply similar optional clause sequences that create parser ambiguity, preserve the established workaround of representing the sequence as an `iteration` over a `one-of` of the optional elements. Do not replace these intentional ambiguity workarounds with a plain optional sequence during unrelated grammar edits.
- Use semantic identifier nodes (`object-def`, `object-ref`, `alias-def`, `alias-ref`, `variable-def`, `variable-ref`) when the parsed token should participate in resolve, rename, structure, or object context behavior.
- Prefer exactly one semantic `SUBJECT` object per statement: normally the object being created, altered, dropped, or otherwise acted upon.
- Add `original-name` only when the semantic name differs from the element or referenced definition name. Do not add redundant `original-name` attributes merely to repeat the ref-id name.
- Preserve undefined statement or helper placeholders when the surrounding grammar expects them. Do not delete placeholders or replace them with guessed definitions unless that syntax is explicitly being defined.
- Use `element ref-id="..."` to reuse a named `element-def`; use inline `sequence`, `one-of`, `iteration`, or `wrapper` for local grammar structure.
- Name `element-def` ids as lowercase underscored versions of the specification term, for example `query_specification`, `select_statement`, `group_by_clause`.
- Write `description` as the human-readable version of the element id/specification term. If the description includes SQL keywords from that grammar block, uppercase those keywords, for example `SELECT statement`, `GROUP BY clause`.
- Mark statement structures that qualify as clauses with `attributes="CLAUSE"`, for example `from_clause`, `where_clause`, `group_by_clause`, and `having_clause`. The attribute is intended for formatting behavior even where it is not yet actively used.
- `custom="true"` marks intentional helper definitions, usually repetitive structures defined once and reused from several grammar positions. They do not have to map directly to a SQL specification term, but should still be named and described clearly.
- When several root statements share the same leading keyword, add an artificial `custom="true"` grouping element such as `create_statement`, `drop_statement`, `alter_statement`, `transaction_control_statement`, or `access_control_statement` with a `one-of` of the concrete statement roots, then reference that group from the block/root statement list.
- Add section comments for main statement roots in large grammar blocks, for example `<!-- ========= create table ========= -->`, while keeping helper definitions under the nearest relevant section.
- When alternatives are all identifier-name shapes, prefer one `qualified-identifier` with multiple `variant` children over a `one-of` of separate `*_name` elements or individual identifier refs. This applies to object-type alternatives such as `TABLE` vs `DOMAIN` and identifier-category alternatives such as `object-ref DATASET` vs `alias-ref DATASET`.
- Model schema-qualified objects with a `qualified-identifier` variant containing an optional `SCHEMA` reference followed by the semantic object reference/definition. Audit every occurrence of the object, including `DROP` subjects, `ON` targets, partition/inheritance parents, callback functions, and index references; do not assume only the primary statement subject needs qualification.
- `qualified-identifier` falls back to `CHR_DOT` as its separator. Omit a redundant `separator="CHR_DOT"` attribute unless a construct genuinely requires a different separator.
- Keep names unqualified when the dialect scopes them through a surrounding object, such as column names in table definitions or DML target clauses, constraint names, policy names, rule names, and trigger names. Query column references should use the existing dataset-alias and schema/table/column variants; leave broader expression qualification to the expression grammar.
- Treat `variant original-name="..."` as definition-only metadata for documenting specification-name deviations; do not add Java/runtime behavior for it unless explicitly requested.
- Use the generated parser-extension metadata for ambiguity resolution; keep the grammar tree declarative and unchanged by ambiguity handling.
- Keep overlapping alternatives as raw grammar children; generated extension look-ahead resolves them without rewriting the grammar tree.
- Treat `LanguageSpecificationParserExtensionBuilder` as the current ambiguity model. It scans raw grammar trees, builds token look-ahead tries, and emits `one-of-extension` nodes into `*_parser_elements_ext.xml`. Runtime parsing then narrows candidates by deepest matching trie node before trying normal child parsers, keeping the grammar tree intact.
- If a `one-of` ambiguity is unresolved, first inspect the generated extension XML and the candidate token prefixes before changing grammar structure. Only edit parser element XML or extension tooling source; generated `*_parser_elements_ext.xml` files should normally be refreshed by tooling.
- When fixing a parser-definition gap, inspect the surrounding grammar construct and its sibling forms for related omissions, including alternate operators, optional clauses, delimiters, nesting, and lexer token-category collisions. Make the adjacent completeness fixes in the same pass when they are clearly part of the same syntax family.
- Use token id prefixes from `LanguageSpecificationLexerBuilder.TokenDefinition`, not guesses: `KW_` keywords, `FN_` functions, `DT_` datatypes, `PRM_` parameters, `EX_` exceptions. Before introducing any token, search every relevant token registry/category. Never duplicate a word across keyword, function, datatype, or parameter registries: the lexer can match a word only once. If an existing token needs a different presentation at a grammar site, use `flavor` there instead. Register genuinely new token keys in the maintained registry/source list before using them in parser element XML, and keep registries alphabetically sorted. Empty registries should generate no tokens; if tooling emits `PRM_`, `EX_`, or empty lexer rules from blank files, fix blank-line handling in dbn-dev and let the developer run the tooling. Treat `token text="..." type-id="IDENTIFIER"` as legacy/deprecated for known SQL words: register a real keyword or use the existing token id. Common shared tokens such as `CHR_*`, `OPR_*`, `IDENTIFIER`, `STRING`, `NUMBER`, and comments come from the shared/parser token XMLs.
- If the same word can behave as different token categories, define it in the most common/generated family and use `flavor` at parser-use sites to qualify the role, for example `<token type-id="DT_INTERVAL" flavor="keyword" />`. Do not duplicate the same word across registries: the lexer can match a word only once, so duplicate generated rules clash instead of producing context-sensitive token categories.
- Treat `attributes`, `branch`, `branch-check`, `version`, `formatting-*`, and `optional-wrapping` as runtime behavior, not comments.

## Expression Grammar Patterns

Expression grammar is usually the most fragile part of each SQL dialect because optional parentheses, broad operator chains, predicates, functions, subqueries, and lists all overlap. Keep the dialect close to the vendor production names, but use small `custom="true"` helpers where the parser needs a delimiter-safe shape.

- Prefer the vendor's expression tiers where they exist, such as `expr`, `boolean_primary`, `predicate`, `bit_expr`, and `simple_expression`. Add custom helper elements only for repeated parser conveniences or delimiter-sensitive sub-productions.
- For binary operator chains where the head and repeated item are the same grammar unit, prefer a single separator-driven `iteration` over that unit. Avoid a separate head plus tail iteration unless the operator token is inside the repeated child sequence; otherwise the second expression can be parsed as an adjacent expression without a real operator.
- Broad operator lists are useful for parser resilience, but they can steal grammar delimiters. Use scoped helper expressions that omit only the problematic delimiter, for example a `between_expr` that excludes `AND` for the lower bound, rather than weakening the global `expr`.
- Model special expression atoms as simple-expression alternatives when that matches the specification shape: parameter markers such as `?`, user/system/bind variables emitted by the lexer, row constructors, `{identifier expr}`, ODBC escapes, interval expressions, current temporal expressions, and quantified subquery operands such as `{ALL | ANY} (subquery)`.
- Put concrete callable/function alternatives before generic qualified identifiers. If a function token can also satisfy an identifier sequence, the generic branch can consume the name and leave the opening parenthesis orphaned.
- Use `optional-wrapping="PARENTHESES"` on expression tiers and list elements that can legally be parenthesized. Prefer optional wrappers over duplicating parenthesized alternatives, unless parentheses introduce a distinct production such as a subquery, row/list constructor, or function argument list.
- Add wrapping cautiously and locally. Changing the expression tree shape just to handle parentheses tends to break unrelated arithmetic, predicate, and function cases.
- Keep delimiter-sensitive constructs separate from generic expression chains: `BETWEEN ... AND ...`, `IN (expr_list | subquery)`, `ANY/ALL (subquery)`, interval units after `INTERVAL`, and expression lists inside `INSERT`, `LOAD DATA`, function calls, and row constructors.
- When a statement embeds query expressions, check branch ordering around standalone query forms. For example, regular `INSERT ... VALUES (...)` or `REPLACE ... VALUES (...)` alternatives should win before broader standalone `VALUES` query/table-value-constructor branches.
- After expression changes, run a focused dialect corpus before broad validation. Include arithmetic precedence chains, nested parentheses, shift/bit operators, intervals, `BETWEEN`, `IN` lists, `ANY/ALL` subqueries, `CASE`, built-in and qualified functions, window functions, `SELECT ... INTO`, `INSERT ... VALUES`, and `LOAD DATA ... SET` expressions.

## Parser Extension Metadata

The extension builder generates look-ahead metadata for ambiguous `one-of` branches. Keep overlapping alternatives as raw grammar children and inspect the generated extension XML when diagnosing candidate-selection problems. Do not encode ambiguity resolution through grammar rewrites or definition-specific checks.

## Tooling Runs

- Treat generated parser token, flex, and extension artifacts as tooling outputs. Update their maintained source definitions and leave regeneration to the developer unless explicitly requested.

## Structural Constraints

- `iteration` must have exactly one child element. The runtime throws for zero or multiple children.
- `wrapper` must have exactly one child element. The runtime throws for zero or multiple children.
- `qualified-identifier` contains one or more `variant` children; each `variant` contains token/identifier leaf definitions.
- `element-def`, `sequence`, and `one-of` can contain multiple grammar children.
- A single-child `one-of` is acceptable when it is intentionally reserving an alternatives slot, such as a statement list that will gain more statement kinds later.
- When a repeated or wrapped grammar unit has several parts, introduce one child `sequence` and put the parts inside it.

## Validation

Use direct validation after adding or changing a DOCTYPE-aware parser XML:

```bash
xmllint --noout --valid src/main/java/com/dbn/language/sql/dialect/oracle/oracle_sql_parser_elements.xml
```

For broad schema changes, validate all parser element XMLs:

```bash
xmllint --noout --valid $(rg --files -g '*_parser_elements.xml' src/main/java/com/dbn/language)
```
