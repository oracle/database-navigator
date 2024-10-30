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

package com.dbn.assistant.init.ui;

import com.dbn.assistant.chat.window.ui.ChatBoxForm;
import com.dbn.assistant.help.ui.AssistantHelpDialog;
import com.dbn.assistant.state.AssistantState;
import com.dbn.common.feature.FeatureAvailability;
import com.dbn.common.text.TextContent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.util.Commons;
import com.dbn.common.util.Dialogs;
import com.dbn.connection.ConnectionHandler;
import com.intellij.ide.BrowserUtil;
import com.intellij.ui.HyperlinkLabel;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;

/**
 * Database Assistant introduction form
 * This form is presented to the user on top of the chat-box after the availability of the AI-Assistant is evaluated.
 * It contains basic information about the functionality and acts as an acknowledgement step.
 *
 * @author Dan Cioca (Oracle)
 */
public class AssistantIntroductionForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel hintPanel;
    private HyperlinkLabel selectAiHyperlink;
    private JButton continueButton;
    private JButton helpButton;
    private JPanel initPanel;
    private JPanel introPanel;

    @SneakyThrows
    public AssistantIntroductionForm(@NotNull ChatBoxForm parent) {
        super(parent);
        createInitForm();
        createIntroForm();
        evaluateAvailability();
    }

    private FeatureAvailability getAvailability() {
        AssistantState assistantState = getChatBox().getAssistantState();
        return assistantState.getAvailability();
    }

    public void evaluateAvailability() {
        FeatureAvailability availability = getAvailability();
        boolean available = availability == FeatureAvailability.AVAILABLE;
        initPanel.setVisible(!available);
        introPanel.setVisible(available);
    }

    private void createInitForm() {
        AssistantInitializationForm initializationForm = new AssistantInitializationForm(this);
        initPanel.add(initializationForm.getComponent(), BorderLayout.CENTER);
    }

    private void createIntroForm() {
        initHyperlink();
        initIntroContent();
        initButtons();
    }

    private void initHyperlink() {
        selectAiHyperlink.setHyperlinkText("Oracle Select AI");
        selectAiHyperlink.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                BrowserUtil.browse("https://www.oracle.com/autonomous-database/select-ai/");
            }
        });
    }

    private void initIntroContent() {
        TextContent introContent = loadIntroContent();
        DBNHintForm hintForm = new DBNHintForm(this, introContent, null, true);
        hintForm.setHighlighted(true);
        hintPanel.add(hintForm.getComponent());
    }

    @SneakyThrows
    private TextContent loadIntroContent() {
        String content = Commons.readInputStream(getClass().getResourceAsStream("intro_content.html"));
        return TextContent.html(content);
    }


    protected void initButtons() {
        continueButton.addActionListener(e -> getChatBox().acknowledgeIntro());
        helpButton.addActionListener(e -> showHelpDialog());
    }

    private void showHelpDialog() {
        Dialogs.show(() -> new AssistantHelpDialog(getConnection()));
    }

    private @NotNull ConnectionHandler getConnection() {
        return getChatBox().getConnection();
    }

    ChatBoxForm getChatBox() {
        return getParentComponent();
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }
}
