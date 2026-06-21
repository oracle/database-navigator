/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.dev.language;

import org.jdom.Attribute;
import org.jdom.CDATA;
import org.jdom.Comment;
import org.jdom.Content;
import org.jdom.DocType;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.EntityRef;
import org.jdom.JDOMFactory;
import org.jdom.ProcessingInstruction;
import org.jdom.Text;
import org.jdom.input.SAXBuilder;
import org.jdom.input.sax.SAXHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.util.ArrayList;
import java.util.List;

final class LanguageSpecificationXmlUtil {
    private LanguageSpecificationXmlUtil() {
    }

    static SAXBuilder createSaxBuilder() {
        SAXBuilder builder = new SAXBuilder();
        builder.setSAXHandlerFactory(CommentPreservingSaxHandler::new);
        return builder;
    }

    static String outputString(Document document) {
        StringBuilder builder = new StringBuilder();
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n");
        appendDocumentComments(builder, document);
        for (Content content : document.getContent()) {
            if (content instanceof Comment) continue;

            appendContent(builder, content);
            if (content instanceof DocType) {
                builder.append('\n');
            }
        }
        return builder.toString();
    }

    static String outputPrettyString(Document document) {
        StringBuilder builder = new StringBuilder();
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n");
        appendDocumentComments(builder, document);
        for (Content content : document.getContent()) {
            if (content instanceof Comment) continue;

            appendPrettyContent(builder, content, 0);
            builder.append('\n');
            if (content instanceof DocType) {
                builder.append('\n');
            }
        }
        return builder.toString();
    }

    private static void appendDocumentComments(StringBuilder builder, Document document) {
        for (Content content : document.getContent()) {
            if (content instanceof Comment comment) {
                appendComment(builder, comment);
                builder.append('\n');
            }
        }
    }

    private static class CommentPreservingSaxHandler extends SAXHandler {
        private final List<Comment> prologComments = new ArrayList<>();
        private boolean inDtd;

        private CommentPreservingSaxHandler(JDOMFactory factory) {
            super(factory);
        }

        @Override
        public void startDTD(String name, String publicId, String systemId) throws SAXException {
            inDtd = true;
            super.startDTD(name, publicId, systemId);
        }

        @Override
        public void endDTD() {
            super.endDTD();
            inDtd = false;
        }

        @Override
        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            super.startElement(namespaceURI, localName, qName, atts);
            if (!prologComments.isEmpty()) {
                Element rootElement = getCurrentElement();
                Document document = getDocument();
                int rootIndex = document.indexOf(rootElement);
                document.addContent(rootIndex, prologComments);
                prologComments.clear();
            }
        }

        @Override
        public void comment(char[] ch, int start, int length) throws SAXException {
            if (inDtd) return;

            flushCharacters();
            Comment comment = new Comment(new String(ch, start, length));
            try {
                getFactory().addContent(getCurrentElement(), comment);
            } catch (SAXException e) {
                prologComments.add(comment);
            }
        }
    }

    private static void appendPrettyContent(StringBuilder builder, Content content, int level) {
        if (content instanceof Element element) {
            appendPrettyElement(builder, element, level);
        } else {
            indent(builder, level);
            appendContent(builder, content);
        }
    }

    private static void appendPrettyElement(StringBuilder builder, Element element, int level) {
        indent(builder, level);
        builder.append('<').append(element.getQualifiedName());
        List<Attribute> attributes = element.getAttributes();
        for (Attribute attribute : attributes) {
            builder
                    .append(' ')
                    .append(attribute.getQualifiedName())
                    .append("=\"")
                    .append(escapeAttribute(attribute.getValue()))
                    .append('"');
        }

        if (element.getContentSize() == 0) {
            builder.append(" />");
            return;
        }

        if (isInlineContent(element)) {
            builder.append('>');
            for (Content content : element.getContent()) {
                appendContent(builder, content);
            }
            builder.append("</").append(element.getQualifiedName()).append('>');
            return;
        }

        builder.append(">\n");
        for (Content content : element.getContent()) {
            appendPrettyContent(builder, content, level + 1);
            builder.append('\n');
        }
        indent(builder, level);
        builder.append("</").append(element.getQualifiedName()).append('>');
    }

    private static boolean isInlineContent(Element element) {
        for (Content content : element.getContent()) {
            if (content instanceof Element || content instanceof Comment) {
                return false;
            }
        }
        return true;
    }

    private static void indent(StringBuilder builder, int level) {
        for (int i = 0; i < level; i++) {
            builder.append("    ");
        }
    }

    private static void appendContent(StringBuilder builder, Content content) {
        if (content instanceof Element element) {
            appendElement(builder, element);
        } else if (content instanceof Comment comment) {
            appendComment(builder, comment);
        } else if (content instanceof DocType docType) {
            appendDocType(builder, docType);
        } else if (content instanceof CDATA cdata) {
            appendCData(builder, cdata);
        } else if (content instanceof Text text) {
            builder.append(escapeText(text.getText()));
        } else if (content instanceof EntityRef entityRef) {
            builder.append('&').append(entityRef.getName()).append(';');
        } else if (content instanceof ProcessingInstruction instruction) {
            builder.append("<?").append(instruction.getTarget());
            String data = instruction.getData();
            if (data != null && !data.isEmpty()) {
                builder.append(' ').append(data);
            }
            builder.append("?>");
        }
    }

    private static void appendElement(StringBuilder builder, Element element) {
        builder.append('<').append(element.getQualifiedName());
        List<Attribute> attributes = element.getAttributes();
        for (Attribute attribute : attributes) {
            builder
                    .append(' ')
                    .append(attribute.getQualifiedName())
                    .append("=\"")
                    .append(escapeAttribute(attribute.getValue()))
                    .append('"');
        }

        if (element.getContentSize() == 0) {
            builder.append(" />");
            return;
        }

        builder.append('>');
        for (Content content : element.getContent()) {
            appendContent(builder, content);
        }
        builder.append("</").append(element.getQualifiedName()).append('>');
    }

    private static void appendComment(StringBuilder builder, Comment comment) {
        builder.append("<!--").append(comment.getText()).append("-->");
    }

    private static void appendDocType(StringBuilder builder, DocType docType) {
        builder.append("<!DOCTYPE ").append(docType.getElementName());
        String publicId = docType.getPublicID();
        String systemId = docType.getSystemID();
        if (publicId != null && !publicId.isEmpty()) {
            builder.append(" PUBLIC \"").append(publicId).append("\"");
            if (systemId != null && !systemId.isEmpty()) {
                builder.append(" \"").append(systemId).append("\"");
            }
        } else if (systemId != null && !systemId.isEmpty()) {
            builder.append(" SYSTEM \"").append(systemId).append("\"");
        }
        String internalSubset = docType.getInternalSubset();
        if (internalSubset != null && !internalSubset.isEmpty()) {
            builder.append(" [").append(internalSubset).append(']');
        }
        builder.append('>');
    }

    private static void appendCData(StringBuilder builder, CDATA cdata) {
        builder.append("<![CDATA[").append(cdata.getText()).append("]]>");
    }

    private static String escapeText(String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private static String escapeAttribute(String text) {
        return escapeText(text).replace("\"", "&quot;");
    }
}
