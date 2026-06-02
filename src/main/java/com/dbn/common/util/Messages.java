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

import com.dbn.common.Reflection;
import com.dbn.common.compatibility.Compatibility;
import com.dbn.common.compatibility.Workaround;
import com.dbn.common.icon.Icons;
import com.dbn.common.message.InteractiveMessage;
import com.dbn.common.message.Message;
import com.dbn.common.message.MessageBundle;
import com.dbn.common.message.MessageCallback;
import com.dbn.common.message.MessageType;
import com.dbn.common.message.TitledMessage;
import com.dbn.common.message.TitledMessageBundle;
import com.dbn.common.message.ui.MessageBundleDialog;
import com.dbn.common.message.ui.MessageBundleDialogConfig;
import com.dbn.common.option.RememberOption;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.messages.DBNMessageDialog;
import com.dbn.diagnostics.Diagnostics;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DoNotAskOption;
import com.intellij.openapi.util.NlsContexts.Button;
import com.intellij.openapi.util.NlsContexts.DialogMessage;
import com.intellij.openapi.util.NlsContexts.DialogTitle;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.lang.reflect.Method;
import java.util.Objects;

import static com.dbn.common.dispose.Failsafe.nd;
import static com.dbn.common.thread.Dispatch.getCurrentModalityState;
import static com.dbn.common.ui.progress.ProgressDialogHandler.closeProgressDialogs;
import static com.dbn.common.util.Commons.array;
import static com.dbn.common.util.Commons.nvl;
import static com.dbn.common.util.Unsafe.cast;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;

// TODO NLS
@Slf4j
@UtilityClass
public class Messages {

    public static final String[] OPTIONS_OK = options(txt("msg.shared.button.OK"));
    public static final String[] OPTIONS_YES_NO = options(txt("msg.shared.button.Yes"), txt("msg.shared.button.No"));
    public static final String[] OPTIONS_YES_NO_CANCEL = options(txt("msg.shared.button.Yes"), txt("msg.shared.button.No"), txt("msg.shared.button.Cancel"));
    public static final String[] OPTIONS_YES_CANCEL = options(txt("msg.shared.button.Yes"), txt("msg.shared.button.Cancel"));
    public static final String[] OPTIONS_RETRY_CANCEL = options(txt("msg.shared.button.Retry"), txt("msg.shared.button.Cancel"));
    public static final String[] OPTIONS_CONTINUE_CANCEL = options(txt("msg.shared.button.Continue"), txt("msg.shared.button.Cancel"));

    public static void showMessagesDialog(@Nullable Project project, TitledMessageBundle messages) {
        MessageBundleDialogConfig config = MessageBundleDialogConfig.create(project, messages.getTitle());
        Dialogs.show(() -> new MessageBundleDialog(config, messages));
    }

    @Deprecated // use showMessageBundleDialog(Project project, TitledMessageBundle messages)
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
        if (message instanceof InteractiveMessage interactiveMessage) {

            showDialog(project,
                    interactiveMessage.getText(),
                    interactiveMessage.getTitle(),
                    interactiveMessage.getOptions(),
                    interactiveMessage.getDefaultOptionIndex(),
                    getDialogIcon(interactiveMessage.getType()),
                    interactiveMessage.getCallback(),
                    interactiveMessage.getRememberOption());
            return;

        }

        if (message instanceof TitledMessage titledMessage) {
            title = titledMessage.getTitle();
        }

        switch (message.getType()) {
            case INFO: showInfoDialog(project, nvl(title, txt("msg.shared.title.Info")), message.getText());
            case ERROR: showErrorDialog(project, nvl(title, txt("msg.shared.title.Error")), message.getText()); break;
            case WARNING: showWarningDialog(project, nvl(title, txt("msg.shared.title.Warning")), message.getText()); break;
            default:
        }
    }


    private static Icon getDialogIcon(MessageType messageType) {
        return switch (messageType) {
            case INFO -> Icons.DIALOG_INFORMATION;
            case ERROR -> Icons.DIALOG_ERROR;
            case WARNING -> Icons.DIALOG_WARNING;
            case QUESTION -> Icons.DIALOG_QUESTION;
            case SUCCESS -> Icons.DIALOG_SUCCESS;
            default -> null;
        };
    }

    public static void showErrorDialog(@Nullable Project project, @DialogMessage String message, Throwable exception) {
        showErrorDialog(project, null, message, exception);
    }

    public static void showErrorDialog(@Nullable Project project, @DialogTitle String title, @DialogMessage String message) {
        showErrorDialog(project, title, message, null);
    }

    public static void showErrorDialog(@Nullable Project project, @DialogMessage String message) {
        showErrorDialog(project, null, message, null);
    }

    public static void showErrorDialog(@Nullable Project project, @Nullable @DialogTitle String title, @DialogMessage String message, @Nullable Throwable exception) {
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

    public static void showQuestionDialog(@Nullable Project project,  @DialogTitle String title, @DialogMessage String message, @Button String[] options, int defaultOptionIndex, MessageCallback callback, @Nullable RememberOption rememberOption) {
        showDialog(project, message, title, options, defaultOptionIndex, Icons.DIALOG_QUESTION, callback, rememberOption);
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

    public static void showSuccessDialog(@Nullable Project project,  @DialogTitle String title, @DialogMessage String message, @Button String[] options, int defaultOptionIndex, MessageCallback callback) {
        showDialog(project, message, title, options, defaultOptionIndex, Icons.DIALOG_SUCCESS, callback, null);
    }

    public static void showSuccessDialog(@Nullable Project project,  @DialogTitle String title, @DialogMessage String message) {
        showSuccessDialog(project, title, message, OPTIONS_OK, 0, null);
    }

    public static int showConfirmationDialog(@Nullable Project project, String title, String message, String[] options, int defaultOptionIndex) {
        return Dispatch.call(() -> showDialog(project, message, Titles.signed(title), options, defaultOptionIndex, Icons.DIALOG_QUESTION, null));
    }

    public static int showAcknowledgementDialog(@Nullable Project project, String title, String message, String[] options, int defaultOptionIndex,  MessageCallback callback) {
        return Dispatch.call(() -> {
            int option = showDialog(project, message, Titles.signed(title), options, defaultOptionIndex, Icons.DIALOG_WARNING, null);
            if (callback != null) {
                callback.accept(option);
            }

            return option;
        });
    }

    private static void showDialog(
            @Nullable Project project,
            @DialogMessage String message,
            @DialogTitle String title,
            @Button String[] options,
            int defaultOptionIndex,
            @Nullable Icon icon,
            @Nullable MessageCallback callback,
            @Nullable RememberOption rememberOption) {

        Dispatch.execute(getCurrentModalityState(), () -> {
            if (project != null) nd(project);
            int option = showDialog(project, message, title, options, defaultOptionIndex, icon, rememberOption);
            //int option = com.intellij.openapi.ui.Messages.showDialog(project, message, Titles.signed(title), options, defaultOptionIndex, icon, doNotAskOption);
            if (callback != null) {
                callback.accept(option);
            }
        });
    }

    public static int showDialog(@Nullable Project project, String message, String title, String[] options, int defaultOptionIndex, @Nullable Icon icon, @Nullable RememberOption rememberOption) {
        closeProgressDialogs();
        return Diagnostics.isNativeAlertsEnabled() ?
                showNativeDialog(project, message, title, options, defaultOptionIndex, icon, rememberOption) :
                showCustomDialog(project, message, title, options, defaultOptionIndex, icon, rememberOption);
    }

    private static int showCustomDialog(@Nullable Project project, String message, String title, String[] options, int defaultOptionIndex, @Nullable Icon icon, @Nullable RememberOption rememberOption) {
        DBNMessageDialog messageDialog = new DBNMessageDialog(project, icon, title, message, options, defaultOptionIndex, rememberOption);
        messageDialog.show();
        return messageDialog.getExitCode();
    }

    @Workaround
    @Compatibility
    private static int showNativeDialog(@Nullable Project project, String message, String title, String[] options, int defaultOptionIndex, @Nullable Icon icon, @Nullable RememberOption rememberOption) {
        try {
            Class<DoNotAskOption> optionClass = RememberOption.spec();
            DoNotAskOption option = RememberOption.wrap(rememberOption);
            Method method = Reflection.findMethod(
                    com.intellij.openapi.ui.Messages.class,
                    "showDialog", String.class, String.class, String[].class, int.class, Icon.class, optionClass);
            Objects.requireNonNull(method);
            return cast(method.invoke(null, message, title, options, defaultOptionIndex, icon, option));
            //return com.intellij.openapi.ui.Messages.showDialog(message, title, options, defaultOptionIndex, icon, doNotAskOption);
        } catch (Throwable e) {
            log.error("Failed to open native dialog.", e);
            return showCustomDialog(project, message, title, options, defaultOptionIndex, icon, rememberOption);
        }
    }

    public static @Button String[] options(String ... options) {
        return array(options);
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
