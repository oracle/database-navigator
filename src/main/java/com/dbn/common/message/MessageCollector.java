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

import com.intellij.openapi.util.NlsContexts.DialogMessage;
import com.intellij.openapi.util.NlsContexts.DialogTitle;
import com.intellij.openapi.util.UserDataHolderBase;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.dbn.common.message.MessageType.ERROR;
import static com.dbn.common.message.MessageType.INFO;
import static com.dbn.common.message.MessageType.WARNING;
import static com.dbn.common.util.Lists.filtered;

@Getter
public class MessageCollector extends UserDataHolderBase implements MessageBundle {
    private final List<Message> messages;

    public MessageCollector() {
        this(false);
    }

    public MessageCollector(boolean async) {
        messages = async ?
                new CopyOnWriteArrayList<>() :
                new ArrayList<>();
    }

    @Override
    public void addMessage(Message message) {
        messages.add(message);
    }

    public void addMessage(MessageType type, @DialogMessage String message) {
        addMessage(new Message(type, message));
    }

    public void addMessage(MessageType type, @DialogTitle String title, @DialogMessage String message) {
        addMessage(new TitledMessage(type, title, message));
    }

    @Override
    public void addInfoMessage(@DialogMessage String message) {
        addMessage(INFO, message);
    }

    @Override
    public void addWarningMessage(@DialogMessage String message) {
        addMessage(WARNING, message);
    }

    @Override
    public void addErrorMessage(@DialogMessage String message) {
        addMessage(ERROR, message);
    }

    @Override
    public void addInfoMessage(@DialogTitle String title, @DialogMessage String message) {
        addMessage(INFO, title, message);
    }

    @Override
    public void addWarningMessage(@DialogTitle String title, @DialogMessage String message) {
        addMessage(WARNING, title, message);
    }

    @Override
    public void addErrorMessage(@DialogTitle String title, @DialogMessage String message) {
        addMessage(ERROR, title, message);
    }

    @Override
    public boolean hasErrors() {
        return hasMessagesOfType(ERROR);
    }

    @Override
    public boolean hasWarnings() {
        return hasMessagesOfType(WARNING);
    }

    @Override
    public boolean hasInfos() {
        return hasMessagesOfType(INFO);
    }

    @Override
    public int countErrors() {
        return countMessagesOfType(ERROR);
    }

    @Override
    public int countWarnings() {
        return countMessagesOfType(WARNING);
    }

    @Override
    public int countInfos() {
        return countMessagesOfType(INFO);
    }

    public boolean hasMessagesOfType(MessageType type) {
        return messages.stream().anyMatch(m -> m.getType() == type);
    }

    public int countMessagesOfType(MessageType type) {
        return (int) messages.stream().filter(m -> m.getType() == type).count();
    }

    @Override
    public List<Message> getMessages(MessageType type) {
        return filtered(messages, m -> m.getType() == type);
    }


    @Override
    public List<Message> getInfoMessages() {
        return getMessages(INFO);
    }

    @Override
    public List<Message> getWarningMessages() {
        return getMessages(WARNING);
    }

    @Override
    public List<Message> getErrorMessages() {
        return getMessages(ERROR);
    }
}
