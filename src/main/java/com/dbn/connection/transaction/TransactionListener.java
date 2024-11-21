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

package com.dbn.connection.transaction;

import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.jdbc.DBNConnection;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;

import java.util.EventListener;

public interface TransactionListener extends EventListener{
    Topic<TransactionListener> TOPIC = Topic.create("Transaction event fired", TransactionListener.class);

    /**
     * This is typically called before the connection has been operationally committed
     */
    default void beforeAction(@NotNull ConnectionHandler connection, DBNConnection conn, TransactionAction action){};

    /**
     * This is typically called after the connection has been operationally committed
     * @param succeeded indicates if the commit operation was successful or not
     */
    default void afterAction(@NotNull ConnectionHandler connection, DBNConnection conn, TransactionAction action, boolean succeeded){};

}
