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

package com.dbn.assistant.credential.local.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class LocalCredentialsTableCellRenderer extends DefaultTableCellRenderer {
  public static final int SECRET_COLUMN = 2;
  public static final int VISIBLE_CHARS = 4;

  @Override
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
    if (column == SECRET_COLUMN && value != null) {
      String text = value.toString();
      if (text.length() > VISIBLE_CHARS) {
        text = text.substring(0, VISIBLE_CHARS) + "************";
      }
      value = "************";
    }
    return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
  }
}
