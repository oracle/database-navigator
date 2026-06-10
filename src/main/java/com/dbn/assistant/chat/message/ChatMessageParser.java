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
import com.dbn.common.util.Strings;
import com.dbn.common.util.Unsafe;
import com.intellij.openapi.util.TextRange;
import kotlin.jvm.functions.Function3;
import lombok.experimental.UtilityClass;
import org.intellij.markdown.IElementType;
import org.intellij.markdown.MarkdownElementTypes;
import org.intellij.markdown.ast.ASTNode;
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor;
import org.intellij.markdown.html.HtmlGenerator;
import org.intellij.markdown.parser.MarkdownParser;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.safety.Safelist;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeVisitor;

import java.util.ArrayList;
import java.util.List;

import static org.intellij.markdown.MarkdownTokenTypes.CODE_FENCE_END;
import static org.intellij.markdown.MarkdownTokenTypes.CODE_FENCE_START;
import static org.intellij.markdown.MarkdownTokenTypes.FENCE_LANG;

@UtilityClass
public class ChatMessageParser {
    private static final Safelist CHAT_MESSAGE_HTML_SAFELIST = createChatMessageHtmlSafelist();

    public static List<ChatMessageTextSection> parse(String content, int offset, int[] sliceOffsets) {
        List<ChatMessageTextSection> sections = new ArrayList<>();

        String parseContent = content.substring(offset);
        ASTNode rootNode;
        try {
            rootNode = parseMarkdownContent(parseContent);
        } catch (Exception e) {
            createTextSection(sections, offset, sliceOffsets, parseContent);
            return sections;
        }

        StringBuilder builder = new StringBuilder();
        for (ASTNode node : rootNode.getChildren()) {
            int nodeStartOffset = node.getStartOffset() + offset;
            int nodeEndOffset = node.getEndOffset() + offset;
            String nodeText = content.substring(nodeStartOffset, nodeEndOffset);

            IElementType nodeType = node.getType();
            if (nodeType == MarkdownElementTypes.CODE_FENCE) {
                // consume the accumulated content as a text section
                createTextSection(sections, offset, sliceOffsets, builder.toString());
                builder.setLength(0);

                // create a code section
                createCodeSection(sections, offset, content, node);

            } else {
                builder.append(nodeText);
            }
        }

        // create last section if builder is not empty
        String sectionContent = builder.toString();
        createTextSection(sections, offset, sliceOffsets, sectionContent);

        return sections;
    }

    private static ASTNode parseMarkdownContent(String content) {
        MarkdownParser markdownParser = new MarkdownParser(new GFMFlavourDescriptor());
        return markdownParser.buildMarkdownTreeFromString(content);
    }

    private static void createTextSection(List<ChatMessageTextSection> sections, int offset, int[] sliceOffsets, String content) {
        if (content.isEmpty()) return;

        for (int i = 0; i < sliceOffsets.length; i++) {
            sliceOffsets[i] = sliceOffsets[i] - offset;
        }

        int sliceShift = offset;
        List<String> slicedContent = Strings.slice(content, sliceOffsets);
        for (String sliceContent : slicedContent) {
            int sliceLength = sliceContent.length();
            TextRange contentRange = createContentRange(sections, sliceShift, sliceLength);
            createSection(sections, sliceContent, contentRange, null);
            sliceShift += sliceLength;
        }
    }

    private static TextRange createContentRange(List<ChatMessageTextSection> sections, int offset, int length) {
        ChatMessageTextSection previousSection = Lists.lastElement(sections);
        int startOffset = previousSection == null ? offset : previousSection.getContentEndOffset();
        int endOffset = startOffset + length;

        return new TextRange(startOffset, endOffset);
    }

    private static void createCodeSection(List<ChatMessageTextSection> sections, int offset, String content, ASTNode rootNode) {
        String language = null;
        StringBuilder builder = new StringBuilder();
        for (ASTNode codeNode : rootNode.getChildren()) {
            IElementType codeNodeType = codeNode.getType();
            if (codeNodeType == CODE_FENCE_START) continue;
            if (codeNodeType == CODE_FENCE_END) continue;

            int startOffset = codeNode.getStartOffset() + offset;
            int endOffset = codeNode.getEndOffset() + offset;
            if (codeNodeType == FENCE_LANG) {
                language = content.substring(startOffset, endOffset);
            } else {
                builder.append(content, startOffset, endOffset);
            }
        }

        int length = rootNode.getEndOffset() - rootNode.getStartOffset();
        if (language != null || length > 3) {
            TextRange contentRange = createContentRange(sections, offset, length);
            createSection(sections, builder.toString(), contentRange, language);
        }
    }

    private void createSection(List<ChatMessageTextSection> sections, String content, TextRange contentRange, String language) {
        ChatMessageTextSection currentSection = new ChatMessageTextSection(content, contentRange, language);
        currentSection.setContentRange(contentRange);
        sections.add(currentSection);
    }

    public static TextContent convertMarkdownToHtml(String content) {
        GFMFlavourDescriptor flavourDescriptor = new GFMFlavourDescriptor();
        ASTNode rootNode = parseMarkdownContent(content);

        HtmlGenerator htmlGenerator = new HtmlGenerator(content, rootNode, flavourDescriptor, false);
        HtmlGenerator.TagRenderer tagRenderer = new CustomTagTenderer((n, s, cs) -> cs, false);
        String body = htmlGenerator.generateHtml(tagRenderer);
        body = sanitizeHtml(body);

        String wrapperContent = TextResources.get(ChatMessageParser.class, "chat_message_wrapper.html.ft");
        TextContent htmlContent = TextContent.html(wrapperContent);
        htmlContent.initFonts();
        htmlContent.initField("BODY_CONTENT", body);
        htmlContent.adjustContent(t -> Unsafe.logged(t, () -> cleanupHtml(t)));

        return htmlContent;
    }

    private static String sanitizeHtml(String html) {
        Document.OutputSettings outputSettings = new Document.OutputSettings().prettyPrint(false);
        return Jsoup.clean(html, "", CHAT_MESSAGE_HTML_SAFELIST, outputSettings);
    }

    private static Safelist createChatMessageHtmlSafelist() {
        return new Safelist()
                .addTags(
                        "a", "b", "blockquote", "br", "code", "dd", "del", "dl", "dt", "em",
                        "h1", "h2", "h3", "h4", "h5", "h6", "hr", "i", "li", "ol", "p", "pre",
                        "s", "small", "span", "strike", "strong", "sub", "sup", "table", "tbody",
                        "td", "tfoot", "th", "thead", "tr", "u", "ul")
                .addAttributes("a", "href")
                .addAttributes("table", "cellspacing", "cellpadding")
                .addAttributes("td", "colspan", "rowspan")
                .addAttributes("th", "colspan", "rowspan", "scope")
                .addProtocols("a", "href", "http", "https")
                .addEnforcedAttribute("a", "rel", "nofollow");
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

    private static final class CustomTagTenderer extends HtmlGenerator.DefaultTagRenderer {
        public CustomTagTenderer(@NotNull Function3<? super ASTNode, ? super CharSequence, ? super Iterable<? extends CharSequence>, ? extends Iterable<? extends CharSequence>> customizer, boolean includeSrcPositions) {
            super(customizer, includeSrcPositions);
        }

        @NonNls
        @Override
        public @NotNull CharSequence openTag(@NotNull ASTNode node, @NotNull @NonNls CharSequence tagName, @NotNull CharSequence[] attributes, boolean autoClose) {
            if (tagName.equals("table")) {
                return "<table cellspacing='0' cellpadding='4'>";
            }
            return super.openTag(node, tagName, attributes, autoClose);
        }
    }
}
