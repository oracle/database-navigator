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

package com.dbn.assistant.chat.message.ui;

import com.dbn.assistant.chat.message.AuthorType;
import com.dbn.assistant.chat.message.ChatMessageContext;
import com.dbn.assistant.chat.message.ChatMessageSection;
import com.dbn.assistant.chat.message.PersistentChatMessage;
import com.dbn.common.color.Colors;
import com.dbn.common.icon.Icons;
import com.dbn.common.ui.util.Cursors;
import com.dbn.common.ui.util.Fonts;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

import static com.dbn.common.util.Commons.nvl;

/**
 * A custom auto-wrapped text pane component with an integrated copy button.
 * It visually differentiates messages from the user and AI and provides functionality to copy text to the clipboard.
 *
 * @author Ayoub Aarrasse (Oracle)
 * @author Dan Cioca (Oracle)
 * @deprecated broken down in {@link UserChatMessageForm}, {@link AgentChatMessageForm}, {@link SystemChatMessageForm}
 */
public class ChatMessagePanel extends JPanel {

  private final ConnectionRef connection;
  private final PersistentChatMessage message;
  private JPanel messagesPanel;
  private JPanel progressPanel;
  private JPanel titlePanel;
  /**
   * Constructor that sets up the text pane and copy button.
   */
  public ChatMessagePanel(ConnectionHandler connection, PersistentChatMessage message) {
    this.connection = ConnectionRef.of(connection);
    this.message = message;
    setLayout(new BorderLayout());
    setBackground(resolveBackground());
    initializeTitle();
    initializeButton();
    initializeMessagesPanel();
    setOpaque(false);
  }

  private ConnectionHandler getConnection() {
    return connection.ensure();
  }

  /**
   * Initializes the JTextPane with appropriate styling and editor kit.
   */
  private void createTextPane(ChatMessageSection section) {
    JTextPane textPane = new JTextPane();
    //textPane.setEditorKit(new WarpEditorKit());
    textPane.setOpaque(false);
    textPane.setEditable(false);
    textPane.setEditable(false);

    if (message.getAuthor() == AuthorType.USER) {
      textPane.setBorder(JBUI.Borders.empty(6, 10));
    } else {
      textPane.setBorder(JBUI.Borders.empty(10));
    }
    textPane.setText(section.getContent());
    messagesPanel.add(textPane);
  }

  private void createCodePane(ChatMessageSection section) {
    ChatMessageCodeViewer codePanel = ChatMessageCodeViewer.create(getConnection(), section);
    if (codePanel == null) {
      // fallback to regular text pane if code panel creation was unsuccessful
      createTextPane(section);
      return;
    }
    JPanel actionsPanel = new JPanel(new BorderLayout());
    actionsPanel.setBackground(codePanel.getViewer().getBackgroundColor());

    messagesPanel.add(actionsPanel);
    messagesPanel.add(codePanel);
  }

  /**
   * Creates a progress bar at the bottom of the message if the message is flagged as in-progress
   */
  private void createProgressPanel() {
    if (!message.isProgress()) return;

    JProgressBar progressBar = new JProgressBar();
    progressBar.setIndeterminate(true);

    progressPanel = new JPanel(new BorderLayout());
    progressPanel.setBorder(JBUI.Borders.empty(0, 10, 10, 10));
    progressPanel.add(progressBar, BorderLayout.CENTER);
    progressPanel.setOpaque(false);

    messagesPanel.add(progressPanel);
  }

  public void clearProgressPanel() {
    if (progressPanel == null) return;
    messagesPanel.remove(progressPanel);
    message.setProgress(false);
  }

  private void initializeTitle() {
    if (message.getAuthor() == AuthorType.USER) return;

    ChatMessageContext context = message.getContext();
    String title =
            context.getProfile() + " / " +
                    context.getModel() + "  -  " +
                    context.getAction().getName();
    JLabel label = new JLabel(title);
    label.setFont(Fonts.smaller(Fonts.deriveFont(Fonts.getLabelFont(), Font.BOLD), 2));
    label.setForeground(Colors.delegate(Colors::getLabelForeground));
    label.setOpaque(false);

    titlePanel = new JPanel(new BorderLayout());
    titlePanel.setOpaque(false);
    titlePanel.setBorder(JBUI.Borders.empty(4, 10, 0, 0));
    titlePanel.add(label, BorderLayout.WEST);
    add(titlePanel, BorderLayout.NORTH);
  }

  private void initializeMessagesPanel() {
    messagesPanel = new JPanel();
    messagesPanel.setOpaque(false);
    messagesPanel.setLayout(new BoxLayout(messagesPanel, BoxLayout.Y_AXIS));
    add(messagesPanel, BorderLayout.CENTER);

    createMessagePanels();
    createProgressPanel();
  }

  private void createMessagePanels() {
    for (ChatMessageSection section : message.getSections()) {
      if (section.getLanguage() == null)
        createTextPane(section); else
        createCodePane(section);
    }
  }

  /**
   * Creates and configures the copy button and its panel.
   */
  private void initializeButton() {
    JButton button = new JButton(Icons.ACTION_COPY);
    button.setBorder(BorderFactory.createEmptyBorder());
    button.setContentAreaFilled(false);
    button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    button.setPreferredSize(new Dimension(30, 30));
    button.addActionListener(e -> copyTextToClipboard());
    button.setCursor(Cursors.handCursor());

    JPanel buttonPanel = new JPanel(new BorderLayout());
    buttonPanel.setOpaque(false);
    buttonPanel.add(button, BorderLayout.NORTH);
    buttonPanel.setPreferredSize(new Dimension(30, 30));

    JPanel container = nvl(titlePanel, this);
    container.add(buttonPanel, BorderLayout.EAST);
  }

  /**
   * Copies the current text in the JTextPane to the system clipboard.
   */
  private void copyTextToClipboard() {
    StringSelection selection = new StringSelection(message.getContent());
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    clipboard.setContents(selection, null);
  }

  private static final Color USER_MSG_COLOR = new JBColor(new Color(218, 234, 255), new Color(68, 95, 128));
  private static final Color AI_MSG_COLOR = new JBColor(new Color(207, 239, 198), new Color(64, 80, 60));
  private static final Color ERROR_MSG_COLOR = new JBColor(new Color(255, 213, 204), new Color(69, 48, 43));

  private Color resolveBackground() {
    switch (message.getAuthor()) {
      case USER: return USER_MSG_COLOR;
      case AGENT: return Colors.delegate(() -> Colors.lafDarker(Colors.getPanelBackground(), 2));
      //case AI: return AI_MSG_COLOR;
      case SYSTEM: return ERROR_MSG_COLOR;
    } return null;
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setColor(getBackground());
    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
    g2.dispose();
  }
/*
  private static class WarpEditorKit extends StyledEditorKit {
    private final ViewFactory defaultFactory = new WarpColumnFactory();

    @Override
    public ViewFactory getViewFactory() {
      return defaultFactory;
    }
  }

  private static class WarpColumnFactory implements ViewFactory {
    public View create(Element elem) {
      String kind = elem.getName();
      if (kind != null) {
        switch (kind) {
          case AbstractDocument.ContentElementName:
            return new WarpLabelView(elem);
          case AbstractDocument.ParagraphElementName:
            return new ParagraphView(elem);
          case AbstractDocument.SectionElementName:
            return new BoxView(elem, View.Y_AXIS);
          case StyleConstants.ComponentElementName:
            return new ComponentView(elem);
          case StyleConstants.IconElementName:
            return new IconView(elem);
          default:
            return new LabelView(elem);
        }
      }
      return new LabelView(elem);
    }
  }

  private static class WarpLabelView extends LabelView {
    public WarpLabelView(Element elem) {
      super(elem);
    }

    @Override
    public float getMinimumSpan(int axis) {
      if (axis == View.X_AXIS) {
        return 0;
      } else if (axis == View.Y_AXIS) {
        return super.getMinimumSpan(axis);
      } else {
        throw new IllegalArgumentException("Invalid axis: " + axis);
      }
    }
  }
*/
}
