package com.dbn.language.common;

import com.dbn.code.common.style.formatting.FormattingDefinition;
import com.dbn.code.common.style.formatting.FormattingDefinitionFactory;
import com.dbn.common.util.Strings;
import com.dbn.language.common.element.TokenPairTemplate;
import com.dbn.object.type.DBObjectType;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.psi.tree.IElementType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static com.dbn.common.options.setting.Settings.*;

@Slf4j
@Getter
public class SimpleTokenType<T extends SimpleTokenType<T>> extends IElementType implements TokenType {
    private int idx;
    private String id;
    private String value;
    private String description;
    private boolean suppressibleReservedWord;
    private TokenTypeCategory category;
    private DBObjectType objectType;
    private int lookupIndex;
    private final int hashCode;

    private TokenTypeBundleBase bundle;
    private FormattingDefinition formatting;
    private TokenPairTemplate tokenPairTemplate;
    private static final AtomicInteger REGISTERED_COUNT = new AtomicInteger();
    private TextAttributesKey[] textAttributesKeys;

    public SimpleTokenType(@NotNull @NonNls String debugName, @Nullable Language language) {
        super(debugName, language, false);
        this.hashCode = System.identityHashCode(this);
    }

    public SimpleTokenType(Element element, Language language, TokenTypeBundleBase bundle, boolean register) {
        super(stringAttribute(element, "id"), language, register);
        this.bundle = bundle;
        this.idx = bundle.nextIndex();
        this.bundle.registerToken(this);
        this.id = stringAttribute(element, "id");
        this.value = stringAttribute(element, "value");
        this.description = stringAttribute(element, "description");

        if (register) {
            int count = REGISTERED_COUNT.incrementAndGet();
            log.info("Registering element " + id + " for language " + language.getID() + " (" + count + ")");
        }

        this.lookupIndex = integerAttribute(element, "index", lookupIndex);

        String type = stringAttribute(element, "type");
        this.category = TokenTypeCategory.getCategory(type);
        this.suppressibleReservedWord = isReservedWord() && !booleanAttribute(element, "reserved", false);
        this.hashCode = System.identityHashCode(this);

        String objectType = stringAttribute(element, "objectType");
        if (Strings.isNotEmpty(objectType)) {
            this.objectType = DBObjectType.get(objectType);
        }

        this.formatting = FormattingDefinitionFactory.loadDefinition(element);
        this.tokenPairTemplate = TokenPairTemplate.get(id);
    }

    @Override
    public int index() {
        return idx;
    }

    @Override
    public void setDefaultFormatting(FormattingDefinition defaultFormatting) {
        formatting = FormattingDefinitionFactory.mergeDefinitions(formatting, defaultFormatting);
    }

    @Override
    public String getValue() {
        return value == null ? "" : value;
    }

    @Override
    public String getTypeName() {
        return category.getName();
    }

    public int compareTo(Object o) {
        SimpleTokenType tokenType = (SimpleTokenType) o;
        return getValue().compareTo(tokenType.getValue());
    }

    @Override
    public boolean isSuppressibleReservedWord() {
        return isReservedWord() && suppressibleReservedWord;
    }

    @Override
    public boolean isIdentifier() {
        return category == TokenTypeCategory.IDENTIFIER;
    }

    @Override
    public boolean isVariable() {
        return getSharedTokenTypes().isVariable(this);
    }

    @Override
    public boolean isQuotedIdentifier() {
        return this == getSharedTokenTypes().getQuotedIdentifier();
    }

    @Override
    public boolean isKeyword() {
        return category == TokenTypeCategory.KEYWORD;
    }

    @Override
    public boolean isFunction() {
        return category == TokenTypeCategory.FUNCTION;
    }

    @Override
    public boolean isParameter() {
        return category == TokenTypeCategory.PARAMETER;
    }

    @Override
    public boolean isDataType() {
        return category == TokenTypeCategory.DATATYPE;
    }

    @Override
    public boolean isLiteral() {
        return category == TokenTypeCategory.LITERAL;
    }

    @Override
    public boolean isNumeric() {
        return category == TokenTypeCategory.NUMERIC;
    }

    @Override
    public boolean isCharacter() {
        return category == TokenTypeCategory.CHARACTER;
    }

    @Override
    public boolean isOperator() {
        return category == TokenTypeCategory.OPERATOR;
    }

    @Override
    public boolean isChameleon() {
        return category == TokenTypeCategory.CHAMELEON;
    }

    @Override
    public boolean isReservedWord() {
        return isKeyword() || isFunction() || isParameter() || isDataType();
    }

    @Override
    public boolean isParserLandmark() {
        return !isIdentifier();
        //return isKeyword() || isFunction() || isParameter() || isCharacter() || isOperator();
        //return isCharacter() || isOperator() || !isSuppressibleReservedWord();
    }

    @Override
    public FormattingDefinition getFormatting() {
        if (formatting == null) {
            formatting = new FormattingDefinition();
        }
        return formatting;
    }

    @NotNull
    private SharedTokenTypeBundle getSharedTokenTypes() {
        Language lang = getLanguage();
        if (lang instanceof DBLanguageDialect) {
            DBLanguageDialect languageDialect = (DBLanguageDialect) lang;
            return languageDialect.getSharedTokenTypes();
        } else if (lang instanceof DBLanguage) {
            DBLanguage language = (DBLanguage) lang;
            return language.getSharedTokenTypes();
        }
        throw new IllegalArgumentException("Language element of type " + lang + "is not supported");
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public boolean matches(TokenType tokenType) {
        if (this.equals(tokenType)) return true;
        if (this.isIdentifier() && tokenType.isIdentifier()) return true;
        return false;
    }

    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean isOneOf(TokenType... tokenTypes) {
        for (TokenType tokenType : tokenTypes) {
            if (this == tokenType) return true;
        }
        return false;
    }

    public TextAttributesKey[] getTokenHighlights(Supplier<TextAttributesKey[]> supplier) {
        if (textAttributesKeys == null) {
            textAttributesKeys = supplier.get();
        }
        return textAttributesKeys;
    }
}
