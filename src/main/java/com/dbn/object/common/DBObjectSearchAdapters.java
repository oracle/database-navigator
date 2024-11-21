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

package com.dbn.object.common;

import com.dbn.common.search.SearchAdapter;
import com.dbn.object.DBType;

import java.util.function.Predicate;

public class DBObjectSearchAdapters {
    private DBObjectSearchAdapters() {}

    public static <O extends DBObject> SearchAdapter<O> binary(String name) {
        return object -> {
            String objName = object.getName();
            return objName.compareToIgnoreCase(name);
        };
    }

    public static <O extends DBObject> SearchAdapter<O> binary(String name, short overload) {
        return object -> {
            String objName = object.getName();
            short objectOverload = object.getOverload();

            int result = objName.compareToIgnoreCase(name);
            return result == 0 ? objectOverload - overload : result;
        };
    }

    public static <O extends DBObject> SearchAdapter<O> binary(String name, short overload, boolean collection) {
        return object -> {
            if (object instanceof DBType && ((DBType) object).isCollection() == collection) {
                int result = object.getName().compareToIgnoreCase(name);
                return result == 0 ? object.getOverload() - overload : result;
            } else {
                return collection ? -1 : 1;
            }
        };
    }

    public static <O extends DBObject> SearchAdapter<O> linear(String name, Predicate<O> match) {
        return object -> {
              if (match.test(object)) {
                  return object.getName().equalsIgnoreCase(name) ? 0 : 1;
              }
              return -1;
        };
    }
}
