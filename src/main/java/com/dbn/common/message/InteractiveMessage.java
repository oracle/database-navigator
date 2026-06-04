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

package com.dbn.common.message;

import com.dbn.common.option.RememberOption;
import com.dbn.common.util.Classes;
import com.dbn.common.util.Commons;
import com.dbn.common.util.Messages;
import com.intellij.openapi.util.NlsContexts.Button;
import com.intellij.openapi.util.NlsContexts.DialogMessage;
import com.intellij.openapi.util.NlsContexts.DialogTitle;
import lombok.Getter;

@Getter
public class InteractiveMessage extends TitledMessage{
    private String[] options = Messages.OPTIONS_OK;
    private int defaultOptionIndex = 0;
    private MessageCallback callback;
    private RememberOption rememberOption;
    private Exception exception;

    public InteractiveMessage(MessageType type, @DialogTitle String title, @DialogMessage String text) {
        super(type, title, text);
    }

    @Override
    public String getText() {
        String text = super.getText();
        if (exception == null) return text;

        String exceptionMessage = Commons.coalesce(
                () -> exception.getLocalizedMessage(),
                () -> exception.getMessage(),
                () -> Classes.className(exception));
        return text + "\n" + exceptionMessage.trim();
    }

    public static InteractiveMessage info(@DialogTitle String title, @DialogMessage String text) {
        return new InteractiveMessage(MessageType.INFO, title, text);
    }

    public static InteractiveMessage error(@DialogTitle String title, @DialogMessage String text) {
        return new InteractiveMessage(MessageType.ERROR, title, text);
    }

    public InteractiveMessage withOptions(@Button String[] options, int defaultOptionIndex) {
        this.options = options;
        this.defaultOptionIndex = defaultOptionIndex;
        return this;
    }

    public InteractiveMessage withCallback(MessageCallback callback) {
        this.callback = callback;
        return this;
    }

    public InteractiveMessage withRememberOption(RememberOption rememberOption) {
        this.rememberOption = rememberOption;
        return this;
    }

    public InteractiveMessage withException(Exception exception) {
        this.exception = exception;
        return this;
    }
}
