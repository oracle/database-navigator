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

package com.dbn.object.common.sorting;

import com.dbn.object.DBMethod;
import com.dbn.object.DBProgram;
import com.dbn.object.type.DBObjectType;

public abstract class DBMethodPositionComparator<T extends DBMethod> extends DBObjectComparator<T> {
    public DBMethodPositionComparator(DBObjectType objectType) {
        super(objectType, SortingType.POSITION);
    }

    @Override
    public int compare(DBMethod method1, DBMethod method2) {
        DBProgram program1 = method1.getProgram();
        DBProgram program2 = method2.getProgram();
        if (program1 != null && program2 != null) {
            int result = compareRef(program1, program2);
            if (result == 0) {
                return comparePosition(method1, method2);
            }

            return result;
        } else {
            int result = comparePosition(method1, method2);
            if (result == 0) {
                result = compareName(method1, method2);
                if (result == 0) {
                    return compareOverload(method1, method2);
                }
            }

            return result;
        }
    }
}
