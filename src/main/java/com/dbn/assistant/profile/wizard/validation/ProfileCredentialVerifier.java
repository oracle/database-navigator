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

package com.dbn.assistant.profile.wizard.validation;

import javax.swing.BorderFactory;
import javax.swing.InputVerifier;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.border.Border;
import java.awt.Color;

import static com.dbn.nls.NlsResources.txt;

/**
 * Input verifier for an AI profile credential
 */
public class ProfileCredentialVerifier extends InputVerifier {
  private static final Border ERROR_BORDER = BorderFactory.createLineBorder(Color.RED, 1);
  private static final Border DEFAULT_BORDER = UIManager.getBorder("TextField.border");


  @Override
  public boolean verify(JComponent input) {
    JComboBox comboBox = (JComboBox) input;
    boolean isValid = comboBox.getSelectedItem() != null;
    if (!isValid) {
      comboBox.setBorder(ERROR_BORDER);
      comboBox.setToolTipText(txt("cfg.assistant.error.CredentialNameEmpty"));
    } else {
      comboBox.setBorder(DEFAULT_BORDER);
      comboBox.setToolTipText(null);
    }
    return isValid;
  }

  @Override
  public boolean shouldYieldFocus(JComponent input) {
    return true;
  }


}
