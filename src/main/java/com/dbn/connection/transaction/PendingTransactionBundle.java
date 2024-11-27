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


import com.dbn.common.util.Lists;
import com.dbn.connection.SessionId;
import com.dbn.connection.jdbc.DBNConnection;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PendingTransactionBundle {
    private final List<PendingTransaction> entries = new ArrayList<>();

    public void notifyChange(VirtualFile file, DBNConnection connection){

        PendingTransaction pendingTransaction = getPendingTransaction(file, connection.getSessionId());
        if (pendingTransaction == null) {
            pendingTransaction = new PendingTransaction(connection, file);
            entries.add(pendingTransaction);
        }
        pendingTransaction.incrementChangesCount();
    }

    @Nullable
    public PendingTransaction getPendingTransaction(VirtualFile file, SessionId sessionId) {
        return Lists.first(entries, transaction ->
                file.equals(transaction.getFile()) &&
                transaction.getSessionId().equals(sessionId));
    }

    public List<PendingTransaction> getEntries() {
        return entries;
    }

    public int size() {
        return entries.size();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }
}
