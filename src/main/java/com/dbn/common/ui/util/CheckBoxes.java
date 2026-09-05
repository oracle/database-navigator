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

package com.dbn.common.ui.util;

import com.dbn.common.message.TitledMessage;
import com.dbn.common.routine.Consumer;
import com.dbn.common.util.Messages;
import com.intellij.openapi.project.Project;
import lombok.experimental.UtilityClass;

import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import java.awt.event.ItemEvent;

import static com.dbn.common.ui.util.ClientProperty.CONFIRMING_CHANGE;

@UtilityClass
public class CheckBoxes {
    public static void onSelectionChange(AbstractButton button, Consumer<ItemEvent> consumer) {
        button.addItemListener(e -> consumer.accept(e));
    }


    public static void installCheckConfirmation(JCheckBox checkBox, Project project, TitledMessage message) {
        onSelectionChange(checkBox, e -> confirmChange(checkBox, project, message));
    }

    private static void confirmChange(JCheckBox checkBox, Project project, TitledMessage message) {
        if (!checkBox.isSelected()) return;
        if (CONFIRMING_CHANGE.is(checkBox)) return;

        try {
            CONFIRMING_CHANGE.set(checkBox, true);

            int option = Messages.showAcknowledgementDialog(
                    project,
                    message.getTitle(),
                    message.getText(),
                    Messages.OPTIONS_YES_NO,
                    1,
                    null);
            checkBox.setSelected(option == 0);
        } finally {
            CONFIRMING_CHANGE.set(checkBox, false);
        }
    }
}
