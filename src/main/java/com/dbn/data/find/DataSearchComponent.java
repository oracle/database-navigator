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

package com.dbn.data.find;

import com.dbn.common.color.Colors;
import com.dbn.common.compatibility.CompatibilityUtil;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.util.Fonts;
import com.dbn.common.ui.util.Keyboard;
import com.dbn.common.ui.util.Mouse;
import com.dbn.common.ui.util.Popups;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.common.util.Actions;
import com.dbn.common.util.Strings;
import com.dbn.data.find.action.CloseOnESCAction;
import com.dbn.data.find.action.MatchCaseToggleAction;
import com.dbn.data.find.action.NextOccurrenceAction;
import com.dbn.data.find.action.PrevOccurrenceAction;
import com.dbn.data.find.action.RegexToggleAction;
import com.dbn.data.find.action.WholeWordsToggleAction;
import com.dbn.data.grid.ui.table.basic.BasicTable;
import com.dbn.data.model.DataModel;
import com.dbn.data.model.DataModelListener;
import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.find.FindInProjectSettings;
import com.intellij.find.FindManager;
import com.intellij.find.FindModel;
import com.intellij.find.SearchTextArea;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.event.SelectionEvent;
import com.intellij.openapi.editor.event.SelectionListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.LightColors;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public class DataSearchComponent extends DBNFormBase implements SelectionListener, DataSearchResultListener, DataModelListener {
    private static final int MATCHES_LIMIT = 10000;

    private JPanel mainPanel;
    private JPanel actionsPanel;
    private JLabel matchesLabel;
    private JLabel closeLabel;
    private JPanel searchFieldPanel;

    private DataFindModel findModel;
    private boolean myListeningSelection = false;
    private ActionToolbar actionsToolbar;
    private final DataSearchResultController searchResultController;
    private final SearchTextArea searchTextField = new SearchTextArea(new JTextArea(), true);

    public JTextComponent getSearchField() {
        return searchTextField.getTextArea();
    }

    public DataSearchComponent(@NotNull SearchableDataComponent searchableComponent) {
        super(searchableComponent);
        searchFieldPanel.add(searchTextField, BorderLayout.CENTER);
        searchTextField.setExtraActions(createExtraActions());
        searchTextField.setMultilineEnabled(false);
        //searchTextField.setBorder(Borders.EMPTY_BORDER);
        //searchTextField.getTextArea().setBorder(Borders.EMPTY_BORDER);
        //searchTextField.setShowNewLineButton(false);

        BasicTable<?> table = searchableComponent.getTable();
        DataModel<?, ?> dataModel = table.getModel();
        dataModel.addDataModelListener(this);


        initializeFindModel();
        matchesLabel.setFont(Fonts.smaller(UIUtil.getLabelFont(), 2));
        matchesLabel.setText("0 results");
        matchesLabel.setForeground(Colors.getLabelInfoForeground());

        findModel = new DataFindModel();
        DataSearchResult searchResult = dataModel.getSearchResult();
        searchResult.setMatchesLimit(MATCHES_LIMIT);
        searchResult.addListener(this);
        searchResultController = new DataSearchResultController(searchableComponent);
        searchResultController.updateResult(findModel);

        configureLeadPanel();

        findModel.addObserver(findModel -> {
            String stringToFind = findModel.getStringToFind();
            if (!wholeWordsApplicable(stringToFind)) {
                findModel.setWholeWordsOnly(false);
            }
            updateUIWithFindModel();
            updateResults(true);
            FindManager findManager = getFindManager();
            syncFindModels(findManager.getFindInFileModel(), DataSearchComponent.this.findModel);
        });

        closeLabel.setText(" ");
        closeLabel.setIcon(AllIcons.Actions.Close);
        closeLabel.addMouseListener(Mouse.listener().onClick(e -> close()));

        updateUIWithFindModel();
        //new CloseOnESCAction(this, table);
        new PrevOccurrenceAction(this, table, false);
        new NextOccurrenceAction(this, table, false);
        searchableComponent.getTable().addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!e.isConsumed()) {
                    if (e.getKeyChar() == Keyboard.Key.ESCAPE) {
                        searchableComponent.hideSearchHeader();
                    }
                }
            }
        });
        //UserInterface.setBackgroundRecursive(mainPanel, searchTextField.getBackground());
        //UserInterface.removeBorders(mainPanel);
    }

    private AnAction[] createExtraActions() {
        return new AnAction[]{
                new MatchCaseToggleAction(this),
                new WholeWordsToggleAction(this),
                new RegexToggleAction(this)};
    }

    @NotNull
    public SearchableDataComponent getSearchableComponent() {
        return this.ensureParentComponent();
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return getSearchField();
    }

    @Override
    public void modelChanged() {
        searchResultController.updateResult(findModel);
    }

    @Override
    public void searchResultUpdated(DataSearchResult searchResult) {
        JTextComponent searchField = getSearchField();
        int count = searchResult.size();
        if (searchField.getText().isEmpty()) {
            updateUIWithEmptyResults();
        } else {
            if (count <= searchResult.getMatchesLimit()) {
                if (count > 0) {
                    setRegularBackground();
                    updateMatchesLabel(count > 1 ? count + " results" : "1 result", false);
                } else {
                    setNotFoundBackground();
                    updateMatchesLabel("0 results", true);
                }
            } else {
                setRegularBackground();
                updateMatchesLabel("More than " + searchResult.getMatchesLimit() + " results", false);
            }
        }
    }

    public void initializeFindModel() {
        if (findModel == null) {
            findModel = new DataFindModel();
            FindManager findManager = getFindManager();
            findModel.copyFrom(findManager.getFindInFileModel());
            findModel.setPromptOnReplace(false);
        }
/*
        String stringToFind = searchableComponent.getSelectedText();
        findModel.setStringToFind(StringUtil.isEmpty(stringToFind) ? "" : stringToFind);
*/
    }

    public void resetFindModel() {
        if (findModel != null) {
            findModel.setStringToFind("");
        }
    }

    private void configureLeadPanel() {
        initTextField();
        JTextComponent searchField = getSearchField();
        onTextChange(searchField, e -> searchFieldDocumentChanged());

        DefaultActionGroup actionsGroup = new DefaultActionGroup("Search Bar", false);
        actionsGroup.add(new PrevOccurrenceAction(this, searchField, true));
        actionsGroup.add(new NextOccurrenceAction(this, searchField, true));
        actionsToolbar = Actions.createActionToolbar(actionsPanel, "SearchBar", true, actionsGroup);
        actionsPanel.add(actionsToolbar.getComponent(), BorderLayout.CENTER);


        JLabel closeLabel = new JLabel(" ", AllIcons.Actions.Close, SwingConstants.RIGHT);
        closeLabel.addMouseListener(Mouse.listener().onClick(e -> close()));

        closeLabel.setToolTipText("Close search bar (Escape)");
        CompatibilityUtil.setSmallerFont(searchField);

        searchField.registerKeyboardAction(e -> {
            if (Strings.isEmptyOrSpaces(searchField.getText())) {
                close();
            } else {
                // TODO
                //requestFocus(myEditor.getContentComponent());
                addTextToRecent(searchField);
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, UserInterface.ctrlDownMask()),
                JComponent.WHEN_FOCUSED);

        final String initialText = findModel.getStringToFind();

        ApplicationManager.getApplication().invokeLater(() -> setInitialText(initialText));
    }

    private void searchFieldDocumentChanged() {
        JTextComponent searchField = getSearchField();
        String text = searchField.getText();
        findModel.setStringToFind(text);
        if (!Strings.isEmpty(text)) {
            updateResults(true);
        } else {
            nothingToSearchFor();
        }
    }

    public FindModel getFindModel() {
        return findModel;
    }

    private static void syncFindModels(FindModel to, FindModel from) {
        to.setCaseSensitive(from.isCaseSensitive());
        to.setWholeWordsOnly(from.isWholeWordsOnly());
        to.setRegularExpressions(from.isRegularExpressions());
        to.setSearchContext(from.getSearchContext());
    }

    private void updateUIWithFindModel() {
        JTextComponent searchField = getSearchField();
        String stringToFind = findModel.getStringToFind();

        if (!Objects.equals(stringToFind, searchField.getText())) {
            searchField.setText(stringToFind);
        }

        setTrackingSelection(!findModel.isGlobal());
    }

    private static boolean wholeWordsApplicable(String stringToFind) {
        return !stringToFind.startsWith(" ") &&
                !stringToFind.startsWith("\t") &&
                !stringToFind.endsWith(" ") &&
                !stringToFind.endsWith("\t");
    }

    private void setTrackingSelection(boolean b) {
        if (b) {
            if (!myListeningSelection) {
                // TODO
                //myEditor.getSelectionModel().addSelectionListener(this);
            }
        } else {
            if (myListeningSelection) {
                // TODO
                //myEditor.getSelectionModel().removeSelectionListener(this);
            }
        }
        myListeningSelection = b;
    }

    public void showHistory(boolean byClickingToolbarButton, JTextField textField) {
        FeatureUsageTracker.getInstance().triggerFeatureUsed("find.recent.search");
        FindInProjectSettings settings = getFindSettings();
        String[] recent = textField == getSearchField() ? settings.getRecentFindStrings() : settings.getRecentReplaceStrings();
        List<String> recentOptions = Arrays.asList(ArrayUtil.reverseArray(recent));
        Popups.showCompletionPopup(byClickingToolbarButton ? actionsPanel : null, recentOptions, "Recent Searches", textField, null);
    }

    private void initTextField() {
        JTextComponent searchField = getSearchField();
        //searchField.setColumns(25);
        if (CompatibilityUtil.isUnderGTKLookAndFeel()) {
            searchField.setOpaque(false);
        }
        searchField.putClientProperty("AuxEditorComponent", Boolean.TRUE);
        searchField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(final FocusEvent e) {
                UserInterface.repaint(searchField);
            }

            @Override
            public void focusLost(final FocusEvent e) {
                UserInterface.repaint(searchField);
            }
        });
        new CloseOnESCAction(this, searchField);
    }


    public void setInitialText(final String initialText) {
        String text = initialText != null ? initialText : "";
        setTextInField(text);
        JTextComponent searchField = getSearchField();
        searchField.selectAll();
    }

    private void requestFocus(Component component) {
        Project project = getSearchableComponent().getTable().getProject();
        IdeFocusManager.getInstance(project).requestFocus(component, true);
    }

    public void searchBackward() {
        JTextComponent searchField = getSearchField();
        moveCursor(DataSearchDirection.UP);
        addTextToRecent(searchField);
    }

    public void searchForward() {
        JTextComponent searchField = getSearchField();
        moveCursor(DataSearchDirection.DOWN);
        addTextToRecent(searchField);
    }

    private void addTextToRecent(JTextComponent textField) {
        String text = textField.getText();
        if (text.isEmpty()) return;

        JTextComponent searchField = getSearchField();
        FindInProjectSettings findSettings = getFindSettings();
        if (textField == searchField) {
            findSettings.addStringToFind(text);
        } else {
            findSettings.addStringToReplace(text);
        }
    }

    private FindInProjectSettings getFindSettings() {
        return FindInProjectSettings.getInstance(ensureProject());
    }

    @Override
    public void selectionChanged(@NotNull SelectionEvent e) {
        updateResults(true);
    }

    private void moveCursor(DataSearchDirection direction) {
        searchResultController.moveCursor(direction);
    }

    public void requestFocus() {
        JTextComponent searchField = getSearchField();
        searchField.setSelectionStart(0);
        searchField.setSelectionEnd(searchField.getText().length());
        requestFocus(searchField);
    }

    public void close() {
        getSearchResult().clear();
        getSearchableComponent().hideSearchHeader();
    }

    public void removeNotify() {
/*
        // TODO
        myLivePreview.cleanUp();
        myLivePreview.dispose();
*/
        JTextComponent searchField = getSearchField();
        setTrackingSelection(false);
        addTextToRecent(searchField);
    }

    private void updateResults(final boolean allowedToChangedEditorSelection) {
        JTextComponent searchField = getSearchField();
        String text = searchField.getText();
        if (text.isEmpty()) {
            nothingToSearchFor();
            SearchableDataComponent searchableComponent = getSearchableComponent();
            searchableComponent.cancelEditActions();
            BasicTable table = getSearchableComponent().getTable();
            table.clearSelection();
            UserInterface.repaint(table);
        } else {

            if (findModel.isRegularExpressions()) {
                try {
                    Pattern.compile(text);
                } catch (Exception e) {
                    conditionallyLog(e);
                    setNotFoundBackground();
                    updateMatchesLabel("Incorrect regular expression", true);
                    getSearchResult().clear();
                    return;
                }
            }

            FindManager findManager = getFindManager();
            if (allowedToChangedEditorSelection) {
                findManager.setFindWasPerformed();
                FindModel copy = new FindModel();
                copy.copyFrom(findModel);
                copy.setReplaceState(false);
                findManager.setFindNextModel(copy);
            }

            searchResultController.updateResult(findModel);
        }
    }

    private void updateMatchesLabel(String text, boolean error) {
        matchesLabel.setText(text);
        matchesLabel.setForeground(error ?
                Colors.getLabelErrorForeground() :
                Colors.getLabelInfoForeground());
    }

    private FindManager getFindManager() {
        Project project = getSearchableComponent().getTable().getProject();
        return FindManager.getInstance(project);
    }

    private void nothingToSearchFor() {
        updateUIWithEmptyResults();
        getSearchResult().clear();
    }

    private void updateUIWithEmptyResults() {
        setRegularBackground();
        updateMatchesLabel("0 results", false);

    }

    private void setNotFoundBackground() {
        getSearchField().setBackground(LightColors.RED);
    }

    private void setRegularBackground() {
        getSearchField().setBackground(Colors.getTextFieldBackground());
    }

    public String getTextInField() {
        return getSearchField().getText();
    }

    public void setTextInField(final String text) {
        getSearchField().setText(text);
        findModel.setStringToFind(text);
    }

    public boolean hasMatches() {
        DataSearchResult searchResult = getSearchResult();
        return searchResult != null && !searchResult.isEmpty();
    }

    private DataSearchResult getSearchResult() {
        return getSearchableComponent().getTable().getModel().getSearchResult();
    }

}
