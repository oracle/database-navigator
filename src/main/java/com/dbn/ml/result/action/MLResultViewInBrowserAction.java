/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.ml.result.action;

import com.dbn.common.icon.Icons;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.thread.Progress;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionAction;
import com.dbn.connection.ConnectionHandler;
import com.dbn.ml.backend.dbms.DBMSModelHandle;
import com.dbn.ml.model.MLResult;
import com.dbn.ml.result.MLExecutionResult;
import com.dbn.object.DBAIModel;
import com.dbn.object.DBSchema;
import com.dbn.object.common.list.DBObjectList;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.nls.NlsResources.txt;

/**
 * Action to view the ML model in the database browser.
 * Only available for DBMS models that are persisted in the database.
 *
 * @author ayoub allali
 */
public class MLResultViewInBrowserAction extends AbstractMLExecutionResultAction {

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull MLExecutionResult executionResult) {
        MLResult result = executionResult.getMlResult();
        DBMSModelHandle dbmsHandle = result.getModelHandle();

        String modelName = dbmsHandle.getModelName();
        ConnectionHandler connection = dbmsHandle.getConnection();

        // Get the user schema where the model is stored
        DBSchema schema = connection.getUserSchema();
        if (schema == null) {
            Messages.showWarningDialog(project, txt("msg.machineLearning.title.SchemaNotFound"), txt("msg.machineLearning.error.SchemaNotFound"));
            return;
        }

        DBObjectList<DBAIModel> modelList = schema.getChildObjectList(DBObjectType.AI_MODEL);
        if (modelList == null) return;

        DBAIModel aiModel = modelList.getObject(modelName);
        if (aiModel != null) {
            aiModel.navigate(true);
            return;
        }

        if (modelList.isLoading() || modelList.isLoadingInBackground()) return;

        loadAndNavigateToModel(project, schema, modelList, modelName);
    }

    private void loadAndNavigateToModel(
            @NotNull Project project,
            @NotNull DBSchema schema,
            @NotNull DBObjectList<DBAIModel> modelList,
            @NotNull String modelName) {
        String modelListName = modelList.getTitleCasedName();
        String title = modelList.isLoaded() ?
                txt("msg.objects.title.ReloadingObjects", modelListName) :
                txt("msg.objects.title.LoadingObjects", modelListName);

        ConnectionAction.invoke(title, true, modelList,
                action -> Progress.prompt(project, modelList, true,
                        txt("prc.objects.title.LoadingObjects"),
                        txt("prc.objects.text.LoadingObjects", modelList.getContentDescription()),
                        progress -> {
                            if (modelList.isLoading() || modelList.isLoadingInBackground()) return;

                            if (modelList.isLoaded()) {
                                modelList.reload();
                            } else {
                                modelList.load();
                            }

                            DBAIModel aiModel = modelList.getObject(modelName);
                            if (aiModel != null) {
                                aiModel.navigate(true);
                            } else {
                                Dispatch.run(() -> Messages.showWarningDialog(project,
                                        txt("msg.machineLearning.title.ModelNotFound"),
                                        txt("msg.machineLearning.error.ModelNotFound", modelName)));
                            }
                        }));
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Presentation presentation, @NotNull Project project, @Nullable MLExecutionResult target) {
        presentation.setText(txt("app.machineLearning.action.ViewInDatabase"));
        presentation.setIcon(Icons.DBO_AI_MODEL);

        presentation.setVisible(target != null);
        presentation.setEnabled(target != null);
    }
}
