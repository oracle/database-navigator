package com.dbn.mcp.ui;

import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NonNls;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.datatransfer.StringSelection;
import java.util.function.Consumer;

import static com.dbn.nls.NlsResources.txt;

/**
 * Help dialog explaining SQL parameter syntax for MCP tool definitions.
 * Shows example SQL and allows users to copy or insert it into their query.
 */
public class ParamHelpDialog extends DialogWrapper {

    @NonNls
    private static final String DEFAULT_EXAMPLE_SQL =
            "SELECT e.name, e.salary\n" +
            "FROM employees e\n" +
            "WHERE e.dept_id = :dept\n" +
            "  AND e.salary >= :min_salary;";

    private final String exampleSql;
    private final Consumer<String> onInsert;

    /**
     * Creates a parameter help dialog with default example SQL.
     *
     * @param onInsert callback to insert SQL into the query editor (may be null)
     */
    public ParamHelpDialog(Consumer<String> onInsert) {
        this(DEFAULT_EXAMPLE_SQL, onInsert);
    }

    /**
     * Creates a parameter help dialog with custom example SQL.
     *
     * @param exampleSql the example SQL to display
     * @param onInsert   callback to insert SQL into the query editor (may be null)
     */
    public ParamHelpDialog(String exampleSql, Consumer<String> onInsert) {
        super(true);
        this.exampleSql = exampleSql;
        this.onInsert = onInsert;
        setTitle(txt("msg.mcp.title.NamedParametersQuickGuide"));
        setOKButtonText(txt("msg.shared.button.Close"));
        init();
    }

    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));

        panel.add(createInfoLabel(), BorderLayout.NORTH);
        panel.add(createExamplePanel(), BorderLayout.CENTER);
        panel.add(createButtonPanel(), BorderLayout.SOUTH);

        return panel;
    }

    private JLabel createInfoLabel() {
        return new JLabel(txt("msg.mcp.text.NamedParametersQuickGuide"));
    }

    private JScrollPane createExamplePanel() {
        JTextArea example = new JTextArea(exampleSql);
        example.setEditable(false);
        example.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        example.setLineWrap(false);

        JScrollPane scrollPane = new JBScrollPane(example);
        scrollPane.setBorder(BorderFactory.createTitledBorder(txt("msg.mcp.title.ExampleSql")));
        return scrollPane;
    }

    private JPanel createButtonPanel() {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton copyButton = new JButton(txt("msg.mcp.button.CopyExample"));
        copyButton.addActionListener(e ->
                CopyPasteManager.getInstance().setContents(new StringSelection(exampleSql))
        );
        buttons.add(copyButton);

        if (onInsert != null) {
            JButton insertButton = new JButton(txt("msg.mcp.button.InsertIntoQuery"));
            insertButton.addActionListener(e -> {
                onInsert.accept(exampleSql);
                close(OK_EXIT_CODE);
            });
            buttons.add(insertButton);
        }

        return buttons;
    }
}
