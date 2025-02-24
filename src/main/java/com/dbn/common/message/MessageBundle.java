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

package com.dbn.common.message;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class MessageBundle implements MessageCollector {
    private List<Message> infoMessages;
    private List<Message> warningMessages;
    private List<Message> errorMessages;

    @Override
    public void addMessage(Message message) {
        switch (message.getType()) {
            case INFO: infoMessages = addMessage(message, infoMessages); break;
            case WARNING: warningMessages = addMessage(message, warningMessages); break;
            case ERROR: errorMessages = addMessage(message, errorMessages); break;
        }
    }

    @Override
    public void addInfoMessage(String message) {
        addMessage(new Message(MessageType.INFO, message));
    }

    @Override
    public void addWarningMessage(String message) {
        addMessage(new Message(MessageType.WARNING, message));
    }

    @Override
    public void addErrorMessage(String message) {
        addMessage(new Message(MessageType.ERROR, message));
    }

    private static List<Message> addMessage(Message message, List<Message> list) {
        if (list == null) list = new ArrayList<>();
        if (!list.contains(message)) list.add(message);
        return list;
    }

    @Override
    public boolean hasErrors() {
        return errorMessages != null && !errorMessages.isEmpty();
    }

    @Override
    public boolean hasWarnings() {
        return warningMessages != null && !warningMessages.isEmpty();
    }

}
