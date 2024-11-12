package com.dbn.language.common;

import com.dbn.common.index.IndexRegistry;
import com.dbn.common.util.Compactables;
import com.dbn.common.util.Lists;
import com.dbn.common.util.Measured;
import com.dbn.common.util.Strings;
import com.intellij.lang.Language;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jdom.Document;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.util.Strings.toLowerCase;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@Slf4j
public abstract class TokenTypeBundleBase {
    private final AtomicInteger tokenIndexer = new AtomicInteger();
    private final IndexRegistry<TokenType> tokenRegistry = new IndexRegistry<>();

    private final Language language;
    private SimpleTokenType[] keywords;
    private SimpleTokenType[] functions;
    private SimpleTokenType[] parameters;
    private SimpleTokenType[] dataTypes;
    private SimpleTokenType[] exceptions;
    private SimpleTokenType[] objects;
    private SimpleTokenType[] characters;
    private SimpleTokenType[] operators;
    private Map<String, SimpleTokenType> keywordsMap;
    private Map<String, SimpleTokenType> functionsMap;
    private Map<String, SimpleTokenType> parametersMap;
    private Map<String, SimpleTokenType> dataTypesMap;
    private Map<String, SimpleTokenType> exceptionsMap;
    private Map<String, SimpleTokenType> objectsMap;
    private Map<String, SimpleTokenType> charactersMap;
    private Map<String, SimpleTokenType> operatorsMap;

    private final Map<String, SimpleTokenType> tokenTypes = new HashMap<>();
    private final Map<String, TokenSet> tokenSets = new HashMap<>();

    public Map<String, SimpleTokenType> getTokenTypes() {
        return tokenTypes;
    }

    public TokenTypeBundleBase(Language language, Document document) {
        this.language = language;
        loadDefinition(language, document);
    }

    public Language getLanguage() {
        return language;
    }

    protected void initIndex(int index) {
        tokenIndexer.set(index);
    }

    protected int size() {
        return tokenRegistry.size();
    }

    protected short nextIndex() {
        int index = tokenIndexer.incrementAndGet();
        return (short) index;
    }

    protected void registerToken(TokenType tokenType) {
        tokenRegistry.add(tokenType);
    }

    public TokenType getTokenType(int index) {
        return tokenRegistry.get(index);
    }

    private void loadDefinition(Language language, Document document) {
        try {
            Element element = document.getRootElement();
            Element tokensElement = element.getChild("tokens");
            Element tokenSetsElement = element.getChild("token-sets");

            Measured.run("building token-type bundle for " + language.getID(), () -> {
                Map<String, Set<String>> tokenSetIds = parseTokenSets(tokenSetsElement);
                createTokens(tokensElement, language, tokenSetIds);
                createTokenSets(tokenSetIds);
            });

        } catch (Exception e) {
            conditionallyLog(e);
            log.error("[DBN] Failed to build token-type bundle for {}", language.getID(), e);
        }
    }

    private void createTokens(Element tokenDefs, Language language, Map<String, Set<String>> tokenSetIds) {
        List<SimpleTokenType> keywordList = new ArrayList<>();
        List<SimpleTokenType> functionList = new ArrayList<>();
        List<SimpleTokenType> parameterList = new ArrayList<>();
        List<SimpleTokenType> dataTypeList = new ArrayList<>();
        List<SimpleTokenType> exceptionList = new ArrayList<>();
        List<SimpleTokenType> objectsList = new ArrayList<>();
        List<SimpleTokenType> characterList = new ArrayList<>();
        List<SimpleTokenType> operatorList = new ArrayList<>();
        for (Element o : tokenDefs.getChildren()) {
            String tokenTypeId = stringAttribute(o, "id");
            boolean registered = isRegisteredToken(tokenSetIds, tokenTypeId);
            SimpleTokenType tokenType = new SimpleTokenType(o, language, this, registered);
            log.debug("Creating token type '{}'", tokenType.getId());
            tokenTypes.put(tokenType.getId(), tokenType);
            switch(tokenType.getCategory()) {
                case KEYWORD: keywordList.add(tokenType); break;
                case FUNCTION: functionList.add(tokenType); break;
                case PARAMETER: parameterList.add(tokenType); break;
                case DATATYPE: dataTypeList.add(tokenType); break;
                case EXCEPTION: exceptionList.add(tokenType); break;
                case OBJECT: objectsList.add(tokenType); break;
                case CHARACTER: characterList.add(tokenType); break;
                case OPERATOR: operatorList.add(tokenType); break;
            }
        }
        keywordsMap =   createTokenMap(keywordList);
        functionsMap =  createTokenMap(functionList);
        parametersMap = createTokenMap(parameterList);
        dataTypesMap =  createTokenMap(dataTypeList);
        exceptionsMap = createTokenMap(exceptionList);
        objectsMap =    createTokenMap(objectsList);
        charactersMap = createTokenMap(characterList);
        operatorsMap =  createTokenMap(operatorList);

        keywords =   createTokenArray(keywordList);
        functions =  createTokenArray(functionList);
        parameters = createTokenArray(parameterList);
        dataTypes =  createTokenArray(dataTypeList);
        exceptions = createTokenArray(exceptionList);
        objects =    createTokenArray(objectsList);
        characters = createTokenArray(characterList);
        operators =  createTokenArray(operatorList);
    }

    private static SimpleTokenType[] createTokenArray(List<SimpleTokenType> tokenList) {
        SimpleTokenType[] tokenArray = new SimpleTokenType[tokenList.size()];
        for (SimpleTokenType token : tokenList) {
            tokenArray[token.getLookupIndex()] = token;
        }
        return tokenArray;
    }

    @NotNull
    private static Map<String, SimpleTokenType> createTokenMap(List<SimpleTokenType> tokenList) {
        Map<String, SimpleTokenType> map = new HashMap<>(tokenList.size());
        for (SimpleTokenType token : tokenList) {
            map.put(token.getValue(), token);
        }
        return Compactables.compact(map);
    }

    public SimpleTokenType getKeywordTokenType(int index) {
        return keywords[index];
    }

    public SimpleTokenType getFunctionTokenType(int index) {
        return functions[index];
    }

    public SimpleTokenType getParameterTokenType(int index) {
        return parameters[index];
    }

    public SimpleTokenType getDataTypeTokenType(int index) {
        return dataTypes[index];
    }

    public SimpleTokenType getExceptionTokenType(int index) {
        return exceptions[index];
    }

    public SimpleTokenType getObjectTokenType(int index) {
        return objects[index];
    }

    public SimpleTokenType getCharacterTokenType(int index) {
        return characters[index];
    }

    public SimpleTokenType getOperatorTokenType(int index) {
        return operators[index];
    }

    /** Shorter equivalent of {@link #getKeywordTokenType(int)} to reduce the size of the generated java lexer */
    public SimpleTokenType ktt(int index) {
        return getKeywordTokenType(index);
    }

    /** Shorter equivalent of {@link #getFunctionTokenType(int)} to reduce the size of the generated java lexer */
    public SimpleTokenType ftt(int index) {
        return functions[index];
    }

    /** Shorter equivalent of {@link #getParameterTokenType(int)} to reduce the size of the generated java lexer */
    public SimpleTokenType ptt(int index) {
        return parameters[index];
    }

    /** Shorter equivalent of {@link #getDataTypeTokenType(int)} to reduce the size of the generated java lexer */
    public SimpleTokenType dtt(int index) {
        return dataTypes[index];
    }

    /** Shorter equivalent of {@link #getOperatorTokenType(int)} to reduce the size of the generated java lexer */
    public SimpleTokenType ott(int index) {
        return operators[index];
    }

    private Map<String, Set<String>> parseTokenSets(Element tokenSetDefs) {
        Map<String, Set<String>> tokenSetDef = new HashMap<>();
        for (Element o : tokenSetDefs.getChildren()) {
            String tokenSetId = stringAttribute(o, "id");
            Set<String> tokenIds = new HashSet<>();

            for (String tokenId : o.getText().split(",")) {
                if (Strings.isNotEmpty(tokenId)) {
                    tokenIds.add(tokenId.trim());
                }
            }
            tokenSetDef.put(tokenSetId, tokenIds);
        }
        
        return tokenSetDef;
    }
    
    private void createTokenSets(Map<String, Set<String>> tokenSetIds) {
        for (val entry : tokenSetIds.entrySet()) {
            String tokenSetId = entry.getKey();
            Set<String> tokenIds = entry.getValue();

            List<SimpleTokenType> tokenSetList = new ArrayList<>();
            for (String tokenId : tokenIds) {
                SimpleTokenType tokenType = tokenTypes.get(tokenId);
                if (tokenType == null) {
                    System.out.println("DEBUG - [" + language.getID() + "] undefined token type: " + tokenId);
                } else {
                    tokenSetList.add(tokenType);
                }
            }
            IElementType[] tokenSetArray = tokenSetList.toArray(new IElementType[0]);
            TokenSet tokenSet = TokenSet.create(tokenSetArray);
            tokenSets.put(tokenSetId, tokenSet);
        }
    }

    private boolean isRegisteredToken(Map<String, Set<String>> tokenSetIds, String tokenId) {
        return Lists.anyMatch(tokenSetIds.values(), tokenIds -> tokenIds.contains(tokenId));
    }

    public SimpleTokenType getTokenType(String id) {
        return tokenTypes.get(id);
    }

    public TokenSet getTokenSet(String id) {
        return tokenSets.get(id);
    }

    public boolean isReservedWord(String text) {
        if (Strings.isEmpty(text)) return false;

        return
            isKeyword(text) ||
            isFunction(text) ||
            isParameter(text) ||
            isDataType(text) || 
            isException(text) ||
            isObject(text);
    }

    public boolean isKeyword(String text) {
        return isTokenType(text, keywordsMap);
    }

    public boolean isFunction(String text) {
        return isTokenType(text, functionsMap);
    }

    public boolean isParameter(String text) {
        return isTokenType(text, parametersMap);
    }

    public boolean isDataType(String text) {
        return isTokenType(text, dataTypesMap);
    }

    public boolean isException(String text) {
        return isTokenType(text, exceptionsMap);
    }

    public boolean isObject(String text) {
        return isTokenType(text, objectsMap);
    }

    private static boolean isTokenType(String text, Map<String, SimpleTokenType> tokenTypesMap) {
        return tokenTypesMap.containsKey(text) || tokenTypesMap.containsKey(toLowerCase(text));
    }
}

