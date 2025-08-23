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

import com.dbn.common.compatibility.Compatibility;
import com.dbn.common.text.TextContent;
import com.dbn.common.text.TextResources;
import com.dbn.common.util.Lists;
import com.dbn.common.util.Unsafe;
import com.intellij.openapi.util.TextRange;
import lombok.experimental.UtilityClass;
import org.intellij.markdown.IElementType;
import org.intellij.markdown.MarkdownElementTypes;
import org.intellij.markdown.ast.ASTNode;
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor;
import org.intellij.markdown.html.HtmlGenerator;
import org.intellij.markdown.parser.MarkdownParser;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeVisitor;

import java.util.ArrayList;
import java.util.List;

import static org.intellij.markdown.MarkdownTokenTypes.CODE_FENCE_END;
import static org.intellij.markdown.MarkdownTokenTypes.CODE_FENCE_START;
import static org.intellij.markdown.MarkdownTokenTypes.FENCE_LANG;

@UtilityClass
public class ChatMessageParser {

    public static List<ChatMessageSection> parse(String content, int shift) {
        List<ChatMessageSection> sections = new ArrayList<>();

        String parseContent = content.substring(shift);
        ASTNode rootNode;
        try {
            rootNode = parseMadkdownContent(parseContent);
        } catch (Exception e) {
            createTextSection(sections, shift, parseContent);
            return sections;
        }

        StringBuilder builder = new StringBuilder();
        for (ASTNode node : rootNode.getChildren()) {
            int nodeStartOffset = node.getStartOffset() + shift;
            int nodeEndOffset = node.getEndOffset() + shift;
            String nodeText = content.substring(nodeStartOffset, nodeEndOffset);

            IElementType nodeType = node.getType();
            if (nodeType == MarkdownElementTypes.CODE_FENCE) {
                // consume the accumulated content as a text section
                createTextSection(sections, shift, builder.toString());
                builder.setLength(0);

                // create a code section
                createCodeSection(sections, shift, content, node);

            } else {
                builder.append(nodeText);
            }
        }

        // create last section if builder is not empty
        String sectionContent = builder.toString();
        createTextSection(sections, shift, sectionContent);

        return sections;
    }

    private static ASTNode parseMadkdownContent(String content) {
        MarkdownParser markdownParser = new MarkdownParser(new GFMFlavourDescriptor());
        return markdownParser.buildMarkdownTreeFromString(content);
    }

    private static void createTextSection(List<ChatMessageSection> sections, int shift, String content) {
        if (content.isEmpty()) return;

        TextRange contentRange = createContentRange(sections, shift, content.length());
        createSection(sections, content, contentRange, null);
    }

    private static TextRange createContentRange(List<ChatMessageSection> sections, int shift, int length) {
        ChatMessageSection previousSection = Lists.lastElement(sections);
        int startOffset = previousSection == null ? shift : previousSection.getContentEndOffset();
        int endOffset = startOffset + length;

        return new TextRange(startOffset, endOffset);
    }

    private static void createCodeSection(List<ChatMessageSection> sections, int shift, String content, ASTNode rootNode) {
        String language = null;
        StringBuilder builder = new StringBuilder();
        for (ASTNode codeNode : rootNode.getChildren()) {
            IElementType codeNodeType = codeNode.getType();
            if (codeNodeType == CODE_FENCE_START) continue;
            if (codeNodeType == CODE_FENCE_END) continue;

            int startOffset = codeNode.getStartOffset() + shift;
            int endOffset = codeNode.getEndOffset() + shift;
            if (codeNodeType == FENCE_LANG) {
                language = content.substring(startOffset, endOffset);
            } else {
                builder.append(content, startOffset, endOffset);
            }
        }

        int length = rootNode.getEndOffset() - rootNode.getStartOffset();
        if (language != null || length > 3) {
            TextRange contentRange = createContentRange(sections, shift, length);
            createSection(sections, builder.toString(), contentRange, language);
        }
    }

    private void createSection(List<ChatMessageSection> sections, String content, TextRange contentRange, String language) {
        ChatMessageSection currentSection = new ChatMessageSection(content, contentRange, language);
        currentSection.setContentRange(contentRange);
        sections.add(currentSection);
    }

    public static TextContent convertMarkdownToHtml(String content) {
        GFMFlavourDescriptor flavourDescriptor = new GFMFlavourDescriptor();
        //content = content.replaceAll("<", "&lt;");
        //content = content.replaceAll(">", "&gt;");
        ASTNode rootNode = parseMadkdownContent(content);

        HtmlGenerator htmlGenerator = new HtmlGenerator(content, rootNode, flavourDescriptor, false);
        HtmlGenerator.TagRenderer tagRenderer = new HtmlGenerator.DefaultTagRenderer((n, s, cs) -> cs, false);
        String body = htmlGenerator.generateHtml(tagRenderer);

        String wrapperContent = TextResources.get(ChatMessageParser.class, "chat_message_wrapper.html.ft");
        TextContent htmlContent = TextContent.html(wrapperContent);
        htmlContent.initFonts();
        htmlContent.initField("BODY_CONTENT", body);
        htmlContent.adjustContent(t -> Unsafe.logged(t, () ->cleanupHtml(t)));

        return htmlContent;
    }

    private static @NotNull String cleanupHtml(String html) {
        Document document = Jsoup.parse(html);

        // remove <p> tags from within <li>
        Elements listItems = document.select("li");
        for (Element listItem : listItems) {
            listItem.select("p").unwrap();
        }

        // remove comments
        document.traverse(new NodeVisitor() {
            @Override
            public void head(@NotNull Node node, int i) {
                if (node instanceof Comment) {
                    node.remove();
                }
            }

            @Override
            @Compatibility // earlier versions of jsoup don't "default" this interface method
            public void tail(@NotNull Node node, int depth) {}
        });

        html = document.html();
        return html;
    }
}
