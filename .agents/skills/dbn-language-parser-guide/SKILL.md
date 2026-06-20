---
name: dbn-language-parser-guide
description: Oracle Database Navigator (DBN) language parser guide. Use when changing, reviewing, explaining, validating, or generating SQL/PSQL parser element XML files, language-parser-elements.dtd, grammar element definitions, parser branches, wrappers, semantic object/alias/variable identifiers, or dialect parser definitions under com/dbn/language.
---

# DBN Language Parser Definitions

Use this skill for DBN SQL/PSQL parser grammar XML work, especially files named `*_parser_elements.xml` and the shared DTD at `src/main/java/com/dbn/language/common/definition/language-parser-elements.dtd`.

## Quick Workflow

1. Inspect the target dialect XML and at least one equivalent construct in sibling dialects before editing.
2. Read [references/parser-elements.md](references/parser-elements.md) when you need element semantics, allowed structures, validation rules, or examples.
3. For token ids or lexer/token-list changes, inspect the `modules/dbn-dev` language tooling before inventing names.
4. Keep XML changes declarative and local to the grammar construct requested. Do not refactor unrelated statement definitions while adding one clause or alternative.
5. Preserve existing generated numeric child ids, but never add ids to new parser elements manually. Leave new elements without `id`; the parser tooling/reindexer assigns ids.
6. Keep parser XMLs pointed at the shared DTD:

```xml
<!DOCTYPE element-defs SYSTEM "../../../common/definition/language-parser-elements.dtd">
```

7. If the XML uses a new tag, attribute, wrapper template, or child relationship, update `language-parser-elements.dtd` in the same change.
8. Validate edited parser XMLs with `xmllint --noout --valid <files>` when available.

## Important Files

- SQL dialect grammars: `src/main/java/com/dbn/language/sql/dialect/*/*_sql_parser_elements.xml`
- PSQL dialect grammars: `src/main/java/com/dbn/language/psql/dialect/*/*_psql_parser_elements.xml` and Oracle `oracle_plsql_parser_elements.xml`
- Shared DTD: `src/main/java/com/dbn/language/common/definition/language-parser-elements.dtd`
- Language tooling module: `modules/dbn-dev/src/main/java/com/dbn/dev/language`
- Source token registries/lists for generated token ids: `modules/dbn-dev/src/main/resources/language` and the relevant `db_*_keywords`/function/datatype/parameter registries
- Generated parser token XMLs: `src/main/java/com/dbn/language/**/dialect/*/*_parser_tokens.xml`
- Runtime element definitions: `src/main/java/com/dbn/language/common/element/util/ElementTypeDefinition.java`
- Core runtime model: `src/main/java/com/dbn/language/common/element/impl/*ElementType.java`
- Parser implementations: `src/main/java/com/dbn/language/common/element/parser/impl/*ElementTypeParser.java`

## Editing Guidance

- Prefer explicit child elements for complex grammar. Use `tokens="A,B,C"` only for simple fixed token sequences or alternatives already following that style.
- `one-of` children are alternatives; child-level `optional="true"` is not supported there and the runtime warns about it. Put optionality on the `one-of` itself or wrap the optional alternative in a parent `sequence`.
- `iteration` and `wrapper` must each contain exactly one child. Put repeated or wrapped multi-part constructs inside a single child `sequence`.
- Use semantic identifier nodes (`object-def`, `object-ref`, `alias-def`, `alias-ref`, `variable-def`, `variable-ref`) when the parsed token should participate in resolve, rename, structure, or object context behavior.
- Use `element ref-id="..."` to reuse a named `element-def`; use inline `sequence`, `one-of`, `iteration`, or `wrapper` for local grammar structure.
- Do not introduce new `exit="true"` markers. Existing markers are a deprecated workaround for ambiguous parser branches; future ambiguity handling should move to trie-based look-ahead.
- Use token id prefixes from `LanguageSpecificationLexerBuilder.TokenDefinition`, not guesses: `KW_` keywords, `FN_` functions, `DT_` datatypes, `PRM_` parameters, `EX_` exceptions. Register new token keys in the relevant token registry/source list, such as `db_sql_keywords` and its sibling registries, before using them in parser element XML. Common shared tokens such as `CHR_*`, `OPR_*`, `IDENTIFIER`, `STRING`, `NUMBER`, and comments come from the shared/parser token XMLs.
- Treat `attributes`, `branch`, `branch-check`, `version`, `formatting-*`, and `optional-wrapping` as runtime behavior, not comments.

## Structural Constraints

- `iteration` must have exactly one child element. The runtime throws for zero or multiple children.
- `wrapper` must have exactly one child element. The runtime throws for zero or multiple children.
- `qualified-identifier` contains one or more `variant` children; each `variant` contains token/identifier leaf definitions.
- `element-def`, `sequence`, and `one-of` can contain multiple grammar children.
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
