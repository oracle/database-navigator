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

package com.dbn.common.util;

import com.dbn.common.icon.Icons;
import com.dbn.common.message.Message;
import com.dbn.common.message.MessageBundle;
import com.dbn.common.message.MessageCallback;
import com.dbn.common.message.TitledMessage;
import com.dbn.common.option.DoNotAskOption;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.messages.DBNMessageDialog;
import com.dbn.diagnostics.Diagnostics;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts.Button;
import com.intellij.openapi.util.NlsContexts.DialogMessage;
import com.intellij.openapi.util.NlsContexts.DialogTitle;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

import static com.dbn.common.dispose.Failsafe.nd;
import static com.dbn.common.ui.progress.ProgressDialogHandler.closeProgressDialogs;
import static com.dbn.common.util.Commons.nvl;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;

// TODO NLS
@UtilityClass
public class Messages {

    public static final String[] OPTIONS_OK = options("OK");
    public static final String[] OPTIONS_YES_NO = options(txt("msg.shared.button.Yes"), txt("msg.shared.button.No"));
    public static final String[] OPTIONS_YES_NO_CANCEL = options(txt("msg.shared.button.Yes"), txt("msg.shared.button.No"), txt("msg.shared.button.Cancel"));
    public static final String[] OPTIONS_YES_CANCEL = options(txt("msg.shared.button.Yes"), txt("msg.shared.button.Cancel"));
    public static final String[] OPTIONS_RETRY_CANCEL = options(txt("msg.shared.button.Retry"), txt("msg.shared.button.Cancel"));
    public static final String[] OPTIONS_CONTINUE_CANCEL = options(txt("msg.shared.button.Continue"), txt("msg.shared.button.Cancel"));

    public static void showErrorDialog(@Nullable Project project,  @DialogTitle String title, MessageBundle messages) {
        StringBuilder buffer = new StringBuilder();
        for (Message message : messages.getErrorMessages()) {
            buffer.append(message.getText());
            buffer.append("\n");
        }
        showErrorDialog(project, title, buffer.toString());
    }

    public static void showMessageDialog(@Nullable Project project, Message message) {
        String title = null;
        if (message instanceof TitledMessage) {
            TitledMessage titledMessage = (TitledMessage) message;
            title = titledMessage.getTitle();
        }

        switch (message.getType()) {
            case INFO: showInfoDialog(project, nvl(title, txt("msg.shared.title.Info")), message.getText());
            case ERROR: showErrorDialog(project, nvl(title, txt("msg.shared.title.Error")), message.getText()); break;
            case WARNING: showWarningDialog(project, nvl(title, txt("msg.shared.title.Warning")), message.getText()); break;
            default:
        }
    }

    public static void showErrorDialog(@Nullable Project project, @DialogMessage String message, Exception exception) {
        showErrorDialog(project, null, message, exception);
    }

    public static void showErrorDialog(@Nullable Project project, @DialogTitle String title, @DialogMessage String message) {
        showErrorDialog(project, title, message, null);
    }

    public static void showErrorDialog(@Nullable Project project, @DialogMessage String message) {
        showErrorDialog(project, null, message, null);
    }

    public static void showErrorDialog(@Nullable Project project, @Nullable @DialogTitle String title, @DialogMessage String message, @Nullable Exception exception) {
        if (project != null && project.isDisposed()) {
            return; // project is disposed
        }

        if (exception != null) {
            if (exception instanceof ProcessCanceledException) {
                conditionallyLog(exception);
                return; // process was interrupted
            }

            //String className = NamingUtil.getClassName(exception.getClass());
            //message = message + "\nCause: [" + className + "] " + exception.getMessage();
            String exceptionMessage = exception.getLocalizedMessage();
            if (exceptionMessage == null) {
                exceptionMessage = Classes.className(exception);
            }
            message = message + "\n" + exceptionMessage.trim();
        }
        if (title == null) title = txt("msg.shared.title.Error");
        showDialog(project, message, title, OPTIONS_OK, 0, Icons.DIALOG_ERROR, null, null);
    }

    public static void showErrorDialog(@Nullable Project project,  @DialogTitle String title, @DialogMessage String message, @Button String[] options, int defaultOptionIndex, MessageCallback callback) {
        showDialog(project, message, title, options, defaultOptionIndex, Icons.DIALOG_ERROR, callback, null);
    }

    public static void showQuestionDialog(@Nullable Project project,  @DialogTitle String title, @DialogMessage String message, @Button String[] options, int defaultOptionIndex, MessageCallback callback) {
        showQuestionDialog(project, title, message, options, defaultOptionIndex, callback, null);
    }

    public static void showQuestionDialog(@Nullable Project project,  @DialogTitle String title, @DialogMessage String message, @Button String[] options, int defaultOptionIndex, MessageCallback callback, @Nullable DoNotAskOption doNotAskOption) {
        showDialog(project, message, title, options, defaultOptionIndex, Icons.DIALOG_QUESTION, callback, doNotAskOption);
    }


    public static void showWarningDialog(@Nullable Project project,  @DialogTitle String title, @DialogMessage String message) {
        showWarningDialog(project, title, message, OPTIONS_OK, 0, null);
    }

    public static void showWarningDialog(@Nullable Project project,  @DialogTitle String title, @DialogMessage String message, @Button String[] options, int defaultOptionIndex, MessageCallback callback) {
        showDialog(project, message, title, options, defaultOptionIndex, Icons.DIALOG_WARNING, callback, null);
    }

    public static void showInfoDialog(@Nullable Project project,  @DialogTitle String title, @DialogMessage String message) {
        showInfoDialog(project, title, message, OPTIONS_OK, 0, null);
    }

    public static void showInfoDialog(@Nullable Project project,  @DialogTitle String title, @DialogMessage String message, @Button String[] options, int defaultOptionIndex, MessageCallback callback) {
        showDialog(project, message, title, options, defaultOptionIndex, Icons.DIALOG_INFORMATION, callback, null);
    }

    public static int showConfirmationDialog(@Nullable Project project, String title, String message, String[] options, int defaultOptionIndex) {
        return Dispatch.call(() -> showDialog(project, message, Titles.signed(title), options, defaultOptionIndex, Icons.DIALOG_QUESTION, null));
    }

    private static void showDialog(
            @Nullable Project project,
            @DialogMessage String message,
            @DialogTitle String title,
            @Button String[] options,
            int defaultOptionIndex,
            @Nullable Icon icon,
            @Nullable MessageCallback callback,
            @Nullable DoNotAskOption doNotAskOption) {

        Dispatch.run(getModalityState(), () -> {
            if (project != null) nd(project);
            int option = showDialog(project, message, title, options, defaultOptionIndex, icon, doNotAskOption);
            //int option = com.intellij.openapi.ui.Messages.showDialog(project, message, Titles.signed(title), options, defaultOptionIndex, icon, doNotAskOption);
            if (callback != null) {
                callback.accept(option);
            }
        });
    }

    public static int showDialog(@Nullable Project project, String message, String title, String[] options, int defaultOptionIndex, @Nullable Icon icon, @Nullable DoNotAskOption doNotAskOption) {
        closeProgressDialogs();
        if (Diagnostics.isNativeAlertsEnabled()) {
            return com.intellij.openapi.ui.Messages.showDialog(message, title, options, defaultOptionIndex, icon, doNotAskOption);
        } else {
            DBNMessageDialog messageDialog = new DBNMessageDialog(project, icon, title, message, options, defaultOptionIndex, doNotAskOption);
            messageDialog.show();
            return messageDialog.getExitCode();
        }
    }

    public static @Button String[] options(String ... options) {
        return Commons.list(options);
    }

    /**
     * Use ANY modality state for message dialogs
     *
     * Messages dialogs issued from within another modal dialog could be displayed after the
     * closing of the dialog if modality state is not properly evaluated
     */
    private static @NotNull ModalityState getModalityState() {
        return ModalityState.any();
    }

}
