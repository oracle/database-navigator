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

package com.dbn.common.ui.util;

import com.intellij.openapi.util.NlsContexts.Label;
import com.intellij.util.ui.UIUtil;
import lombok.experimental.UtilityClass;

import javax.swing.JLabel;

import static java.awt.event.KeyEvent.VK_UNDEFINED;

@UtilityClass
public class Labels {
    public static void setText(JLabel label, @Label String text) {
        int mnemonicIndex = getMnemonicIndex(text);
        String labelText = text == null ? "" : text;

        label.setText(UIUtil.replaceMnemonicAmpersand(labelText));
        if (mnemonicIndex == -1) {
            label.setDisplayedMnemonic(VK_UNDEFINED);
            label.setDisplayedMnemonicIndex(-1);
        } else {
            label.setDisplayedMnemonic(label.getText().charAt(mnemonicIndex));
            label.setDisplayedMnemonicIndex(mnemonicIndex);
        }
    }

    private static int getMnemonicIndex(String text) {
        if (text == null) return -1;

        int offset = 0;
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            if (character != '&') {
                offset++;
                continue;
            }

            if (i == text.length() - 1) return -1;

            char mnemonic = text.charAt(i + 1);
            if (mnemonic == '&') {
                i++;
                offset++;
                continue;
            }

            return offset;
        }
        return -1;
    }
}
