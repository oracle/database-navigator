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

package com.dbn.language.common.psi.scrambler;

import com.dbn.common.thread.Read;
import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.language.common.TokenType;
import com.dbn.language.common.TokenTypeCategory;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static com.dbn.common.util.Strings.toLowerCase;

@NonNls
public class DBLLanguageFileScrambler {
    AtomicInteger fineNameIndex = new AtomicInteger();
    private final Map<String, String> locationMap = new HashMap<>();
    private final Map<DBObjectType, Map<String, String>> objectTypeIndex = new HashMap<>();

    private static final Random RANDOM = new Random();
    private static final String CHARSET = "1234567890abcdefghijklmnopqrstuvyxz";

    public String scramble(DBLanguagePsiFile psiFile) {
        return Read.call(psiFile, f -> {
            StringBuilder builder = new StringBuilder();
            PsiElement child = f.getFirstChild();
            while (child != null) {
                scramble(child, builder);
                child = child.getNextSibling();
            }
            return builder.toString();
        });
    }

    private void scramble(PsiElement child, StringBuilder builder) {
        if (child instanceof PsiWhiteSpace) {
            builder.append(child.getText().replaceAll("\t", "    "));
        } else if (child instanceof PsiComment) {
            String text = child.getText();
            builder.append(text.replaceAll("[a-zA-Z0-9]", "#"));
        } else if (child instanceof LeafPsiElement) {
            if (child instanceof TokenPsiElement) {
                TokenPsiElement token = (TokenPsiElement) child;
                TokenType tokenType = token.getTokenType();
                String text = child.getText();
                if (tokenType.getCategory() == TokenTypeCategory.LITERAL) {
                    if (text.startsWith("'")) {
                        builder.append(text.replaceAll("[a-zA-Z0-9]", "#"));
                    } else {
                        builder.append(text, 0, 3);
                        builder.append(text.substring(3, text.length() - 2).replaceAll("[a-zA-Z0-9]", "#"));
                        builder.append(text, text.length() - 2, text.length());
                    }

                } else {
                    builder.append(text);
                }

            } else if (child instanceof IdentifierPsiElement) {
                IdentifierPsiElement identifier = (IdentifierPsiElement) child;

                String objectName = identifier.getText();
                int objectNameLength = objectName.length();
                DBObjectType objectType = identifier.getObjectType();
                objectName = getObjectName(objectType, objectName);

                boolean normalize = true;
                PsiElement nextLeaf = PsiUtil.getNextLeaf(child);
                if (nextLeaf instanceof TokenPsiElement) {
                    TokenPsiElement tokenPsiElement = (TokenPsiElement) nextLeaf;
                    normalize = !tokenPsiElement.isCharacterToken();
                }
                if (normalize) {
                    objectName = StringUtils.rightPad(objectName, objectNameLength, " ");
                }
                builder.append(objectName);
            }
        } else if (child instanceof com.intellij.psi.impl.source.tree.LeafPsiElement) {
            IElementType elementType = ((com.intellij.psi.impl.source.tree.LeafPsiElement) child).getElementType();
            String text = child.getText();
            if (elementType instanceof TokenType) {
                TokenType tokenType = (TokenType) elementType;
                TokenTypeCategory category = tokenType.getCategory();
                if (category == TokenTypeCategory.LITERAL) {
                    builder.append(text.replaceAll("[a-zA-Z0-9]", "#"));
                } else if (category == TokenTypeCategory.IDENTIFIER || category == TokenTypeCategory.UNKNOWN) {
                    builder.append(getObjectName(DBObjectType.ANY, text));
                } else {
                    builder.append(text);
                }
            } else {
                builder.append(text);
            }

        } else {
            child = child.getFirstChild();
            while (child != null) {
                scramble(child, builder);
                child = child.getNextSibling();
            }
        }
    }

    public String scrambleName(VirtualFile file) {
        StringBuilder newPath = new StringBuilder();
        File originalFile = new File(file.getPath());
        File directory = originalFile.getParentFile();
        String path = directory.getPath();
        String[] pathTokens = path.split("[\\\\/]");
        for (String pathToken : pathTokens) {
            String newPathToken = locationMap.computeIfAbsent(pathToken, t -> scrambledName("package", locationMap.size() + 1));
            newPath.append(newPathToken);
            newPath.append(File.separator);
        }
        newPath.append(scrambledName("script", fineNameIndex.incrementAndGet()));
        newPath.append(".");
        newPath.append(file.getExtension());
        return newPath.toString();
    }

    private String scrambledName(String suffix, int index) {
        return suffix + "_" + StringUtils.leftPad(Integer.toString(index), 2, "0");
    }

    @NotNull
    private String randomChar() {
        return "" + CHARSET.charAt(RANDOM.nextInt(CHARSET.length()));
    }

    private String getObjectName(DBObjectType objectType, String objectName) {
        Map<String, String> indexMap = objectTypeIndex.computeIfAbsent(objectType, t -> new HashMap<>());
        return indexMap.computeIfAbsent(toLowerCase(objectName), n -> scrambledName(objectType.getName().replaceAll(" ", "_"), indexMap.size() + 1));
    }
}
