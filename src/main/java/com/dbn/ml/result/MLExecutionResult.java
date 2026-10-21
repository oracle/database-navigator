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

package com.dbn.ml.result;

import com.dbn.common.icon.Icons;
import com.dbn.common.message.MessageType;
import com.dbn.common.message.TitledMessage;
import com.dbn.common.thread.Dispatch;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.execution.ExecutionResultBase;
import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.ml.model.MLResult;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

import static com.dbn.common.dispose.Checks.isValid;
import static com.dbn.nls.NlsResources.txt;

@Getter
public class MLExecutionResult extends ExecutionResultBase<MLExecutionResultForm> {
    private final String name;
    private volatile MLResult mlResult;
    private volatile boolean loading;
    private volatile String loadError;

    public MLExecutionResult(MLResult mlResult, String name) {
        this.mlResult = mlResult;
        this.name = name;
    }

    public void beginLoading() {
        loading = true;
    }

    public void completeLoading(MLResult result) {
        Dispatch.run(false, () -> {
            if (!isValid(this)) return;
            mlResult = result;
            loading = false;
            refreshForm();
        });
    }

    public void failLoading(String message) {
        Dispatch.run(false, () -> {
            if (!isValid(this)) return;
            loadError = message == null ? txt("app.machineLearning.text.ResultLoadFailed") : message;
            loading = false;
            refreshForm();
        });
    }

    private void refreshForm() {
        MLExecutionResultForm form = getForm();
        if (isValid(form)) form.refreshResult();
    }

    public TitledMessage getSummaryMessage() {
        if (loading) {
            return new TitledMessage(MessageType.PROCESSING,
                    txt("app.machineLearning.title.LoadingResult"),
                    txt("app.machineLearning.text.LoadingResult"));
        }
        if (loadError != null) {
            return new TitledMessage(MessageType.ERROR,
                    txt("app.machineLearning.title.ResultLoadFailed"), loadError);
        }
        if (mlResult.getEvaluationResult() == null) {
            return new TitledMessage(MessageType.WARNING,
                    txt("app.machineLearning.title.TrainingCompleted"),
                    txt("app.machineLearning.text.NoEvaluationAvailable"));
        }
        return new TitledMessage(MessageType.SUCCESS,
                txt("app.machineLearning.title.TrainingCompleted"),
                txt("app.machineLearning.text.ResultReady"));
    }

    @Override
    public @Nullable MLExecutionResultForm createForm() {
        return new MLExecutionResultForm(this);
    }

    @Override
    public @NotNull String getName() {
        return name;
    }

    @Override
    public Icon getIcon() {
        return Icons.FILE_SQL_CONSOLE;
    }

    @Override
    public @NotNull Project getProject() {
        return getConnection().getProject();
    }

    @Override
    public ConnectionId getConnectionId() {
        return getConnection().getConnectionId();
    }

    @Override
    public @NotNull ConnectionHandler getConnection() {
        return mlResult.getConnection();
    }

    @Override
    public DBLanguagePsiFile createPreviewFile() {
        return null;
    }

}
