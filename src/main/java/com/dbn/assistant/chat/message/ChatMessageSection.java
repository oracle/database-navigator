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

package com.dbn.assistant.chat.message;

import com.intellij.lang.Language;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.dbn.assistant.chat.message.ChatMessageLanguages.resolveLanguage;

/**
 * Section of chat message, qualified with a language
 */
@Getter
public class ChatMessageSection {

    private String content;
    private final String languageId;

    public ChatMessageSection(String content, @Nullable String languageId) {
        this.content = content.trim();
        this.languageId = languageId;
    }

    @Nullable
    public Language getLanguage() {
        return resolveLanguage(languageId);
    }

    public void append(String content) {
        this.content = this.content + "\n" + content;
    }

    public List<ChatMessageSection> asList() {
        return List.of(this);
    }
}
