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

package com.dbn.common.option;


import com.dbn.common.icon.Icons;
import com.dbn.common.options.PersistentConfiguration;
import com.dbn.common.options.setting.Settings;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.util.Messages;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import javax.swing.Icon;

@Getter
@Setter
public class InteractiveConfirmationBroker implements DoNotAskOption, PersistentConfiguration{
    private final String configName;
    private final String title;
    private final String message;

    private Icon dialogIcon = Icons.DIALOG_QUESTION;
    private String doNotShowMessage = "Do not ask again";

    protected transient boolean confirm;

    public InteractiveConfirmationBroker(@NonNls String configName, String title, String message, boolean defaultKeepAsking) {
        this.configName = configName;
        this.title = title;
        this.message = message;
        this.confirm = defaultKeepAsking;
    }

    public InteractiveConfirmationBroker withDialogIcon(Icon dialogIcon) {
        this.dialogIcon = dialogIcon;
        return this;
    }

    public InteractiveConfirmationBroker withDoNotShowMessage(String doNotShowMessage) {
        this.doNotShowMessage = doNotShowMessage;
        return this;
    }

    @Override
    public boolean isToBeShown() {
        return true;
    }

    @Override
    public void setToBeShown(boolean keepAsking, int selectedIndex) {
        this.confirm = keepAsking;
    }

    @Override
    public boolean canBeHidden() {
        return true;
    }

    @Override
    public boolean shouldSaveOptionsOnCancel() {
        return false;
    }

    public boolean resolve(Project project, Object ... messageArgs) {
        if (!confirm) return true;
        return Dispatch.call(() -> prompt(project, messageArgs));
    }

    private boolean prompt(Project project, Object[] messageArgs) {
        int optionIndex = Messages.showDialog(
                project,
                txt(message, messageArgs),
                txt(title),
                Messages.OPTIONS_YES_NO, 0,
                dialogIcon, this);
        return optionIndex == 0;
    }

    /*******************************************************
     *              PersistentConfiguration                *
     *******************************************************/
    @Override
    public void readConfiguration(Element element) {
        confirm = Settings.getBoolean(element, configName, confirm);
    }

    @Override
    public void writeConfiguration(Element element) {
        Settings.setBoolean(element, configName, confirm);
    }
}
