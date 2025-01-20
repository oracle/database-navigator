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

package com.dbn.execution.java.wrapper;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class SqlComplexType {
    private String name;
    private boolean isArray = false;
    private String containedTypeName;
    private List<Field> fields = new ArrayList<>();

    // Inner class to represent a field
    @Getter
    @Setter
    public static class Field {
        private final String name;
        private final String type;
        private final short fieldIndex;

        public Field(String name, String type, short fieldIndex) {
            this.name = name;
            this.type = type;
			this.fieldIndex = fieldIndex;
		}
    }

    public void addField(String name, String type, short fieldIdx) {
        this.fields.add(new Field(name, type, fieldIdx));
    }
}