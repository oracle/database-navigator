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

import java.util.List;

/**
 * The MessageCollector interface provides a contract for collecting and categorizing messages.
 * It supports adding messages of various types (info, warning, error) and provides functionality
 * to retrieve categorized messages or check if specific types of messages exist.
 *
 * @author Dan Cioca (Oracle)
 */
public interface MessageCollector {

    void addMessage(Message message);

    void addInfoMessage(String message);

    void addWarningMessage(String message);

    void addErrorMessage(String message);

    boolean hasErrors();

    boolean hasWarnings();

    List<Message> getInfoMessages();

    List<Message> getWarningMessages();

    List<Message> getErrorMessages();
}
