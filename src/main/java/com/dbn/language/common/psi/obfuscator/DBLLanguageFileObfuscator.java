/*
 * Copyright 2024 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.language.common.psi.obfuscator;

import com.dbn.common.thread.Read;
import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.common.DBLanguageDialectIdentifier;
import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.language.common.TokenType;
import com.dbn.language.common.TokenTypeCategory;
import com.dbn.language.common.psi.ExecVariablePsiElement;
import com.dbn.language.common.psi.IdentifierPsiElement;
import com.dbn.language.common.psi.LeafPsiElement;
import com.dbn.language.common.psi.PsiUtil;
import com.dbn.language.common.psi.TokenPsiElement;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.tree.IElementType;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static com.dbn.common.util.Strings.toLowerCase;

@NonNls
public class DBLLanguageFileObfuscator {
    AtomicInteger fineNameIndex = new AtomicInteger();
    private final Map<String, String> locationMap = new HashMap<>();
    private final Map<DBObjectType, Map<String, String>> objectTypeIndex = new HashMap<>();

    private static final Random RANDOM = new Random();
    private static final String CHARSET = "1234567890abcdefghijklmnopqrstuvyxz";

    public String obfuscate(DBLanguagePsiFile psiFile) {
        List<ObfuscationPart> parts = Read.call(psiFile, f -> {
            List<ObfuscationPart> result = new ArrayList<>();
            PsiElement child = f.getFirstChild();
            while (child != null) {
                collect(psiFile, child, result);
                child = child.getNextSibling();
            }
            return result;
        });

        StringBuilder builder = new StringBuilder();
        for (ObfuscationPart part : parts) {
            append(part, builder);
        }
        return builder.toString();
    }

    private void collect(DBLanguagePsiFile psiFile, PsiElement child, List<ObfuscationPart> parts) {
        if (child instanceof PsiWhiteSpace) {
            parts.add(new ObfuscationPart(PartType.WHITESPACE, child.getText(), null, false));
        } else if (child instanceof PsiComment) {
            parts.add(new ObfuscationPart(PartType.COMMENT, child.getText(), null, false));
        } else if (child instanceof LeafPsiElement) {
            if (child instanceof TokenPsiElement token) {
                TokenType tokenType = token.getTokenType();
                String text = child.getText();
                if (tokenType.getCategory() == TokenTypeCategory.LITERAL) {
                    parts.add(new ObfuscationPart(PartType.LITERAL, text, null, false));
                } else {
                    parts.add(new ObfuscationPart(PartType.TEXT, text, null, false));
                }

            } else if (child instanceof IdentifierPsiElement identifier) {
                String objectName = identifier.getText();
                if (!isReservedWord(psiFile, objectName)) {
                    boolean normalize = true;
                    PsiElement nextLeaf = PsiUtil.getNextLeaf(child);
                    if (nextLeaf instanceof TokenPsiElement tokenPsiElement) {
                        normalize = !tokenPsiElement.isCharacterToken();
                    }
                    parts.add(new ObfuscationPart(PartType.IDENTIFIER, objectName, identifier.getObjectType(), normalize));
                } else {
                    parts.add(new ObfuscationPart(PartType.TEXT, objectName, null, false));
                }
            } else if (child instanceof ExecVariablePsiElement) {
                parts.add(new ObfuscationPart(PartType.EXEC_VARIABLE, child.getText(), null, false));
            }
        } else if (child instanceof com.intellij.psi.impl.source.tree.LeafPsiElement leaf) {
            IElementType elementType = leaf.getElementType();
            String text = child.getText();
            if (elementType instanceof TokenType tokenType) {
                TokenTypeCategory category = tokenType.getCategory();
                if (category == TokenTypeCategory.LITERAL) {
                    parts.add(new ObfuscationPart(PartType.LITERAL, text, null, false));
                } else if (category == TokenTypeCategory.IDENTIFIER || category == TokenTypeCategory.UNKNOWN) {
                    PartType partType = isReservedWord(psiFile, text) ? PartType.TEXT : PartType.IDENTIFIER;
                    parts.add(new ObfuscationPart(partType, text, DBObjectType.ANY, false));
                } else {
                    parts.add(new ObfuscationPart(PartType.TEXT, text, null, false));
                }
            } else {
                parts.add(new ObfuscationPart(PartType.TEXT, text, null, false));
            }

        } else {
            child = child.getFirstChild();
            while (child != null) {
                collect(psiFile, child, parts);
                child = child.getNextSibling();
            }
        }
    }

    private void append(ObfuscationPart part, StringBuilder builder) {
        switch (part.type()) {
            case WHITESPACE -> builder.append(part.text().replace("\t", "    "));
            case COMMENT -> builder.append(obfuscateComment(part.text()));
            case LITERAL -> builder.append(obfuscateLiteral(part.text()));
            case TEXT -> builder.append(part.text());
            case EXEC_VARIABLE -> builder.append(obfuscateSubstitutionVariable(part.text()));
            case IDENTIFIER -> {
                String objectName = getObjectName(part.objectType(), part.text());
                if (part.normalize()) {
                    objectName = StringUtils.rightPad(objectName, part.text().length(), " ");
                }
                builder.append(objectName);
            }
        }
    }

    private enum PartType {
        WHITESPACE, COMMENT, LITERAL, TEXT, EXEC_VARIABLE, IDENTIFIER
    }

    private record ObfuscationPart(PartType type, String text, DBObjectType objectType, boolean normalize) {}

    private static String obfuscateLiteral(String text) {
        if (text.startsWith("'")) {
            return obfuscateText(text);
        }

        StringBuilder builder = new StringBuilder(text.length());
        builder.append(text, 0, 3);
        builder.append(obfuscateText(text.substring(3, text.length() - 2)));
        builder.append(text, text.length() - 2, text.length());
        return builder.toString();
    }

    private static boolean isReservedWord(DBLanguagePsiFile psiFile, String text) {
        if (StringUtils.isBlank(text) || isQuotedIdentifier(text)) return false;

        DBLanguageDialect dialect = psiFile.getLanguageDialect();
        if (dialect == null) return false;
        if (dialect.isReservedWord(text)) return true;

        DBLanguageDialectIdentifier companionIdentifier = companionDialect(dialect.getIdentifier());
        if (companionIdentifier == null) return false;

        DBLanguageDialect companion = DBLanguageDialect.get(companionIdentifier);
        return companion != null && companion.isReservedWord(text);
    }

    private static DBLanguageDialectIdentifier companionDialect(DBLanguageDialectIdentifier identifier) {
        return switch (identifier) {
            case ORACLE_SQL -> DBLanguageDialectIdentifier.ORACLE_PLSQL;
            case ORACLE_PLSQL -> DBLanguageDialectIdentifier.ORACLE_SQL;
            case MYSQL_SQL -> DBLanguageDialectIdentifier.MYSQL_PSQL;
            case MYSQL_PSQL -> DBLanguageDialectIdentifier.MYSQL_SQL;
            case POSTGRES_SQL -> DBLanguageDialectIdentifier.POSTGRES_PSQL;
            case POSTGRES_PSQL -> DBLanguageDialectIdentifier.POSTGRES_SQL;
            case SQLITE_SQL -> DBLanguageDialectIdentifier.SQLITE_PSQL;
            case SQLITE_PSQL -> DBLanguageDialectIdentifier.SQLITE_SQL;
            case ISO92_SQL -> null;
        };
    }

    private static boolean isQuotedIdentifier(String text) {
        if (text.length() < 2) return false;
        char first = text.charAt(0);
        char last = text.charAt(text.length() - 1);
        return (first == '"' && last == '"') ||
                (first == '`' && last == '`') ||
                (first == '[' && last == ']');
    }

    static String obfuscateText(String text) {
        return text.replaceAll("[\\p{L}\\p{N}]", "#");
    }

    private String obfuscateSubstitutionVariable(String text) {
        int prefixLength = text.startsWith("&&") ? 2 :
                text.startsWith("&") || text.startsWith(":") ? 1 : 0;
        String prefix = text.substring(0, prefixLength);
        String variableName = text.substring(prefixLength);
        return prefix + getObjectName(DBObjectType.VARIABLE, variableName);
    }

    static String obfuscateComment(String text) {
        int prefixEnd = 0;
        while (prefixEnd < text.length() && Character.isWhitespace(text.charAt(prefixEnd))) {
            prefixEnd++;
        }
        if (text.regionMatches(true, prefixEnd, "rem", 0, 3) &&
                (prefixEnd + 3 == text.length() || Character.isWhitespace(text.charAt(prefixEnd + 3)))) {
            prefixEnd += 3;
            return text.substring(0, prefixEnd) + obfuscateText(text.substring(prefixEnd));
        }
        return obfuscateText(text);
    }

    public String obfuscateName(VirtualFile file) {
        StringBuilder newPath = new StringBuilder();
        File originalFile = new File(file.getPath());
        File directory = originalFile.getParentFile();
        String path = directory.getPath();
        String[] pathTokens = path.split("[\\\\/]");
        for (String pathToken : pathTokens) {
            String newPathToken = locationMap.computeIfAbsent(pathToken, t -> obfuscatedName("package", locationMap.size() + 1));
            newPath.append(newPathToken);
            newPath.append(File.separator);
        }
        newPath.append(obfuscatedName("script", fineNameIndex.incrementAndGet()));
        newPath.append(".");
        newPath.append(file.getExtension());
        return newPath.toString();
    }

    private String obfuscatedName(String suffix, int index) {
        return suffix + "_" + StringUtils.leftPad(Integer.toString(index), 2, "0");
    }

    @NotNull
    private String randomChar() {
        return "" + CHARSET.charAt(RANDOM.nextInt(CHARSET.length()));
    }

    private String getObjectName(DBObjectType objectType, String objectName) {
        Map<String, String> indexMap = objectTypeIndex.computeIfAbsent(objectType, t -> new HashMap<>());
        return indexMap.computeIfAbsent(toLowerCase(objectName), n -> obfuscatedName(objectType.getName().replace(" ", "_"), indexMap.size() + 1));
    }
}
