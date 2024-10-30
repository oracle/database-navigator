/*
 * Copyright (c) 2024, Oracle and/or its affiliates.
 *
 * This software is dual-licensed to you under the Universal Permissive License
 * (UPL) 1.0 as shown at https://oss.oracle.com/licenses/upl or Apache License
 * 2.0 as shown at http://www.apache.org/licenses/LICENSE-2.0. You may choose
 * either license.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.dbn.assistant.editor;

import com.dbn.assistant.chat.message.ChatMessageConverter;
import com.dbn.assistant.chat.message.GenericChatMessageConverter;
import com.dbn.language.sql.SQLLanguage;

/**
 * SQL implementation of the {@link ChatMessageConverter} adjusting the AI message content to match the SQL language editor
 *
 * @author Dan Cioca (Oracle)
 */
public class SQLChatMessageConverter extends GenericChatMessageConverter {
    public static final SQLChatMessageConverter INSTANCE = new SQLChatMessageConverter();

    private SQLChatMessageConverter(){
        super(SQLLanguage.INSTANCE);
    };

    @Override
    protected String getBockCommentStart() {
        return "\n/*\n";
    }

    @Override
    protected String getBockCommentEnd() {
        return "\n*/\n";
    }

    protected String adjustComment(String comment) {
        StringBuilder builder = new StringBuilder();
        String[] rows = comment.split("\n");
        for (String row : rows) {
            if (row.isBlank()) continue;
            row = row.trim();
            boolean bullets = row.startsWith("- ");
            boolean numbers = row.matches("^\\d+\\..*$");

            while (row.length() > 120) {
                int index = row.indexOf(' ', 120);
                if (index == -1) {
                    appendRow(builder, row, bullets, numbers);
                    row = "";
                } else {
                    appendRow(builder, row.substring(0, index), bullets, numbers);
                    row = row.substring(index).trim();
                }
            }
            appendRow(builder, row, bullets, numbers);
        }
        return "  " + builder.toString().trim();
    }

    private void appendRow(StringBuilder builder, String row, boolean bullets, boolean numbers) {
        if (row.isBlank()) return;
        row = row.trim();

        // bullet points indentation
        builder.append("  ");
        if (bullets) builder.append(row.startsWith("-") ? "  " : "     ");
        if (numbers) builder.append(row.matches("^\\d+\\..*$") ? "  " : "      ");
        builder.append(row);
        builder.append("\n");
    }
}
