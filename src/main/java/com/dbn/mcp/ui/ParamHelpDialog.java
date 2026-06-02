package com.dbn.mcp.ui;

import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.function.Consumer;

/**
 * Help dialog explaining SQL parameter syntax for MCP tool definitions.
 * Shows example SQL and allows users to copy or insert it into their query.
 */
public class ParamHelpDialog extends DialogWrapper {

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
        setTitle("Named parameters: quick guide");
        setOKButtonText("Close");
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
        return new JLabel(
                "<html>" +
                        "<b>Use <code>:param</code> placeholders in your SQL.</b>" +
                        "<ul style='margin-top:4; margin-bottom:4;'>" +
                        "<li>Write <code>:param</code> (e.g., <code>WHERE dept_id = :dept</code>).</li>" +
                        "<li>We detect names and list them in the Parameters table.</li>" +
                        "<li>At build-time they become JDBC <code>?</code> in left-to-right order.</li>" +
                        "<li>Repeated names bind the <i>same</i> value to each occurrence.</li>" +
                        "</ul>" +
                        "</html>"
        );
    }

    private JScrollPane createExamplePanel() {
        JTextArea example = new JTextArea(exampleSql);
        example.setEditable(false);
        example.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        example.setLineWrap(false);

        JScrollPane scrollPane = new JBScrollPane(example);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Example SQL"));
        return scrollPane;
    }

    private JPanel createButtonPanel() {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton copyButton = new JButton("Copy example");
        copyButton.addActionListener(e ->
                CopyPasteManager.getInstance().setContents(new StringSelection(exampleSql))
        );
        buttons.add(copyButton);

        if (onInsert != null) {
            JButton insertButton = new JButton("Insert into Query");
            insertButton.addActionListener(e -> {
                onInsert.accept(exampleSql);
                close(OK_EXIT_CODE);
            });
            buttons.add(insertButton);
        }

        return buttons;
    }
}
