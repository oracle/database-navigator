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

package com.dbn.assistant.chat.window.util;

import com.dbn.assistant.chat.message.PersistentChatMessage;
import com.dbn.assistant.chat.message.ui.ChatMessageForm;
import com.dbn.assistant.chat.window.ui.ChatBoxForm;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.ui.util.UserInterface;
import com.intellij.openapi.Disposable;
import lombok.extern.slf4j.Slf4j;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Wrapper class around a JPanel that will display <code>ChatMessage</code>
 * This class will maintain a fixed capacity using FIFO principle
 *
 * @author Emmanuel Jannetti (Oracle)
 */
@Slf4j
public class RollingMessageContainer implements Disposable {

  private final int maxCapacity;
  private final JPanel messageContainer;

  private List<ChatMessageForm> messageForms = new ArrayList<>();

  /**
   * Creates a new RollingJPanelWrapper
   *
   * @param maxCapacity max capacity
   * @param panel       the panel to display the chat message
   *
   * @author Emmanuel Jannetti (Oracle)
   */
  public RollingMessageContainer(int maxCapacity, JPanel panel) {
    this.maxCapacity = maxCapacity;
    this.messageContainer = panel;
    this.messageContainer.setLayout(new MigLayout("fillx"));
  }

  private void ensureFreeSlot(int howMany) {
    int currentSize = messageForms.size();
    int s = maxCapacity - currentSize - howMany;
    while (s++ < 0) {
      this.messageContainer.remove(0);
      ChatMessageForm form = this.messageForms.remove(0);
      Disposer.dispose(form);
    }
  }

  private void removeProgressIndicator() {
    Component[] messagePanels = messageContainer.getComponents();
    if (messagePanels.length == 0) return;

    Component panel = messagePanels[messagePanels.length - 1];
    if (panel instanceof JComponent) {
      // identify the message panels that have progress indicators and hide them
      JComponent component = (JComponent) panel;
      UserInterface.visitRecursively(component, JProgressBar.class, b -> b.setVisible(false));
    }
  }

  public void clear() {
    this.messageForms = Disposer.replace(this.messageForms, new ArrayList<>());
    this.messageContainer.removeAll();

    UserInterface.repaint(messageContainer);
  }

  public void addAll(List<PersistentChatMessage> chatMessages, ChatBoxForm parent) {
    removeProgressIndicator();
    ensureFreeSlot(chatMessages.size());

    for (PersistentChatMessage message : chatMessages) {
      ChatMessageForm form = ChatMessageForm.create(parent, message);
      messageForms.add(form);
      this.messageContainer.add(form.getComponent(), "growx, wrap, w ::93%"); // TODO try to occupy the entire width (100% breaks the wrapping for some reason)
    }
    UserInterface.repaint(messageContainer);
  }

  @Override
  public void dispose() {
    this.messageForms = Disposer.replace(this.messageForms, Collections.emptyList());
  }

}
