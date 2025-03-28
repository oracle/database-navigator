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

package com.dbn.execution.java.action;

import com.dbn.browser.DatabaseBrowserManager;
import com.dbn.browser.ui.DatabaseBrowserTree;
import com.dbn.common.action.BasicAction;
import com.dbn.common.color.Colors;
import com.dbn.common.icon.Icons;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.thread.Progress;
import com.dbn.common.ui.dialog.SelectionListDialog;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionAction;
import com.dbn.connection.ConnectionHandler;
import com.dbn.execution.java.wrapper.WrapperStatementExecutor;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.common.DBObject;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.util.Naming.capitalizeWords;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;

public class JavaObjectCreateWrapperAction extends BasicAction {
	private final DBObjectRef<?> sourceObject;
	private RelativePoint popupLocation;

	public JavaObjectCreateWrapperAction(DBJavaClass sourceObject) {
		super("Create Execution Wrappers...");
		this.sourceObject = DBObjectRef.of(sourceObject);
	}

	@Override
	public void actionPerformed(@NotNull AnActionEvent e) {
		DBObject sourceObject = getSourceObject();
		Project project = sourceObject.getProject();
		String listName = "executable elements";
		String title = txt("msg.objects.title.LoadingObjects", capitalizeWords(listName));
		ConnectionAction.invoke(title, true, sourceObject,
				action -> Progress.prompt(project, sourceObject, true,
						txt("prc.objects.title.LoadingObjects"),
						txt("prc.objects.text.LoadingObjects", listName),
						progress -> showObjectList(e.getDataContext(), action)));
	}

	@NotNull
	public DBObject getSourceObject() {
		return DBObjectRef.ensure(sourceObject);
	}

	private void showObjectList(DataContext dataContext, ConnectionAction action) {
        if (action.isCancelled()) return;

        DBJavaClass javaClass = (DBJavaClass) getSourceObject();

        List<DBJavaMethod> objects = new ArrayList<>(javaClass.getStaticMethods());
        if (action.isCancelled()) return;

        Dispatch.run(dataContext, true, () -> {
            if (objects.isEmpty()) {
                JLabel label = new JLabel("No public static method", Icons.EXEC_MESSAGES_INFO, SwingConstants.LEFT);
                label.setBorder(JBUI.Borders.empty(8));
                JPanel panel = new JPanel(new BorderLayout());
                panel.add(label);
                panel.setBackground(Colors.LIGHT_BLUE);
                ComponentPopupBuilder popupBuilder = JBPopupFactory.getInstance().createComponentPopupBuilder(panel, null);
                JBPopup popup = popupBuilder.createPopup();
                showPopup(popup);
            } else {
				Project project = javaClass.getProject();
				SelectionListDialog<DBJavaMethod> dialog = new SelectionListDialog<>(project,"Select method to create wrapper", objects, null, javaClass);
                dialog.show();

                List<DBJavaMethod> methods = dialog.getSelection();
				if (methods == null || methods.isEmpty()) return;

				Progress.prompt(project, javaClass, true,
						"Creating execution wrappers",
						"Creating execution wrappers for java methods selected ",
						progress -> {
							ConnectionHandler connection = javaClass.getConnection();
							if (connection.isValid()) {
								try {
									WrapperStatementExecutor statementExecutor = new WrapperStatementExecutor();
									statementExecutor.createExecutionWrappers(javaClass, methods, true);
								} catch (Exception ex) {
									Messages.showErrorDialog(project,
											"Error creating execution wrappers for java methods \nCause: " + ex.getMessage());
									conditionallyLog(ex);
								}
							} else {
								String message =
										"Can not create execution wrappers for java methods.\n" +
												"No connectivity to '" + connection.getName() + "'. " +
												"Please check your connection settings and try again.";
								Messages.showErrorDialog(project, message);
							}
						});
            }
        });
    }

	private void showPopup(JBPopup popup) {
		if (popupLocation == null) {
			DBObject sourceObject = getSourceObject();
			DatabaseBrowserManager browserManager = DatabaseBrowserManager.getInstance(sourceObject.getProject());
			DatabaseBrowserTree activeBrowserTree = browserManager.getActiveBrowserTree();
			if (activeBrowserTree != null) {
				popupLocation = TreeUtil.getPointForSelection(activeBrowserTree);
				Point point = popupLocation.getPoint();
				point.setLocation(point.getX() + 20, point.getY() + 4);
			}
		}
		if (popupLocation != null) {
			popup.show(popupLocation);
		}
	}
}
