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

package com.dbn.assistant.service.generic.ui;

import com.dbn.assistant.AssistantType;
import com.dbn.assistant.DatabaseAssistantManager;
import com.dbn.assistant.adapter.ui.AssistantDetailFormBase;
import com.dbn.assistant.adapter.ui.AssistantIntroductionForm;
import com.dbn.assistant.chat.window.ui.ChatBoxForm;
import com.dbn.assistant.state.AssistantState;
import com.dbn.common.feature.FeatureAvailability;
import com.dbn.common.text.TextContent;
import com.dbn.common.text.TextResources;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Database Assistant introduction form
 * This form is presented to the user on top of the chat-box.
 * It contains basic information about the functionality and acts as an acknowledgement step.
 *
 * @author Dan Cioca (Oracle)
 */
public class GenericAssistantIntroductionForm extends AssistantDetailFormBase implements AssistantIntroductionForm {

    private JPanel mainPanel;
    private JPanel hintPanel;
    private JButton helpButton;
    private JButton continueButton;

    @SneakyThrows
    public GenericAssistantIntroductionForm(@NotNull ChatBoxForm parent) {
        super(parent);
        initIntroContent();
        initButtons();
        initAssistantState();

    }

    private void initAssistantState() {
        // generic assistant should always be available (mark as such if reaching this point)
        ConnectionId connectionId = getConnectionId();
        DatabaseAssistantManager assistantManager = DatabaseAssistantManager.getInstance(ensureProject());
        AssistantState assistantState = assistantManager.getAssistantState(connectionId, AssistantType.PUBLIC);
        assistantState.setAvailability(FeatureAvailability.AVAILABLE);
    }

    private void initIntroContent() {
        TextContent introContent = loadIntroContent();
        DBNHintForm hintForm = new DBNHintForm(this, introContent, null, true);
        hintForm.setHighlighted(true);
        hintPanel.add(hintForm.getComponent());
    }

    @SneakyThrows
    private TextContent loadIntroContent() {
        String content = TextResources.get(this, "intro_content.html.ft");
        return TextContent.html(content);
    }


    protected void initButtons() {
        continueButton.addActionListener(e -> getChatBox().acknowledgeIntro());
        helpButton.addActionListener(e -> showHelpDialog());
        helpButton.setVisible(false); // TODO
    }

    private void showHelpDialog() {
        //Dialogs.show(() -> new GenericAssistantHelpDialog(getConnection()));
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
