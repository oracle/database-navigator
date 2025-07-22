/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.assistant.chat.message;

import lombok.experimental.UtilityClass;
import org.intellij.markdown.IElementType;
import org.intellij.markdown.MarkdownElementTypes;
import org.intellij.markdown.ast.ASTNode;
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor;
import org.intellij.markdown.parser.MarkdownParser;

import java.util.ArrayList;
import java.util.List;

import static org.intellij.markdown.MarkdownTokenTypes.CODE_FENCE_END;
import static org.intellij.markdown.MarkdownTokenTypes.CODE_FENCE_START;
import static org.intellij.markdown.MarkdownTokenTypes.FENCE_LANG;

@UtilityClass
public class ChatMessageParser {

    public static List<ChatMessageSection> parse(String content) {
        List<ChatMessageSection> sections = new ArrayList<>();
        MarkdownParser markdownParser = new MarkdownParser(new GFMFlavourDescriptor());
        ASTNode rootNode = markdownParser.buildMarkdownTreeFromString(content);

        StringBuilder builder = new StringBuilder();
        for (ASTNode node : rootNode.getChildren()) {
            int startOffset = node.getStartOffset();
            int endOffset = node.getEndOffset();
            String nodeText = content.substring(startOffset, endOffset);

            IElementType nodeType = node.getType();
            if (nodeType == MarkdownElementTypes.CODE_FENCE) {
                createTextSection(sections, builder);
                createCodeSection(sections, content, node);

            } else {
                builder.append(nodeText);
            }
        }

        // create last section if builder is not empty
        createTextSection(sections, builder);

        return sections;
    }

    private static void createTextSection(List<ChatMessageSection> sections, StringBuilder builder) {
        if (builder.length() > 0) {
            ChatMessageSection section = new ChatMessageSection(builder.toString(), null);
            sections.add(section);
            builder.setLength(0);
        }
    }

    private static void createCodeSection(List<ChatMessageSection> sections, String content, ASTNode rootNode) {
        String language = null;
        StringBuilder builder = new StringBuilder();
        for (ASTNode codeNode : rootNode.getChildren()) {
            IElementType codeNodeType = codeNode.getType();
            if (codeNodeType == CODE_FENCE_START) continue;
            if (codeNodeType == CODE_FENCE_END) continue;

            int startOffset = codeNode.getStartOffset();
            int endOffset = codeNode.getEndOffset();
            if (codeNodeType == FENCE_LANG) {
                language = content.substring(startOffset, endOffset);
            } else {
                builder.append(content, startOffset, endOffset);
            }
        }

        ChatMessageSection section = new ChatMessageSection(builder.toString(), language);
        sections.add(section);
    }
}
