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

package com.dbn.database.common.util;

import com.dbn.connection.Resources;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Queue;

public class MultipartResultSet extends WrappedResultSet {
    private final Queue<ResultSet> queue = new LinkedList<>();

    public MultipartResultSet(ResultSet ... resultSets) {
        super(null);
        add(resultSets);
    }

    public static MultipartResultSet create(ResultSet ... resultSets) {
        return new MultipartResultSet(resultSets);
    }

    public MultipartResultSet add(@Nullable ResultSet ... resultSets) {
        if (resultSets != null) {
            for (ResultSet resultSet : resultSets) {
                if (resultSet != null) {
                    queue.add(resultSet);
                }
            }
        }

        return this;
    }



    @Override
    public boolean next() throws SQLException {
        while (inner == null && !queue.isEmpty()) {
            inner = queue.poll();
        }

        if (inner == null) {
            return false;

        } else if (inner.next()) {
            return true;

        } else {
            Resources.close(inner);
            inner = null;
            return next();
        }
    }
}
