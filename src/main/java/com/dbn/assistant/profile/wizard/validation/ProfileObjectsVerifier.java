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

package com.dbn.assistant.profile.wizard.validation;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static com.dbn.nls.NlsResources.txt;

/**
 * Input verifier for an AI profile object list
 */
public class ProfileObjectsVerifier extends InputVerifier implements
    ActionListener {


  private static final Border ERROR_BORDER = BorderFactory.createLineBorder(Color.RED, 1);
  private static final Border DEFAULT_BORDER = UIManager.getBorder("TextField.border");


  @Override
  public void actionPerformed(ActionEvent e) {
    JTable input = (JTable) e.getSource();
    shouldYieldFocus(input);
  }

  @Override
  public boolean shouldYieldFocus(JComponent input) {
    //return verify(input);
    return true;
  }

  @Override
  public boolean verify(JComponent input) {
    JTable profileTable = (JTable) input;
    boolean isValid = profileTable.getRowCount() > 0;
    if (!isValid) {
      profileTable.setBorder(ERROR_BORDER);
      profileTable.setToolTipText(txt("profile.mgmt.object_list_step.validation"));
    } else {
      profileTable.setBorder(DEFAULT_BORDER);
      profileTable.setToolTipText(null);
    }
    return isValid;
  }
}

