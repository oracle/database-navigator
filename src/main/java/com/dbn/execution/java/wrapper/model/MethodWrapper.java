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

package com.dbn.execution.java.wrapper.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Getter
@Setter
public class MethodWrapper {
    private String originalJavaMethodName;
    // method signatures of wrapper java methods may be same even though original java methods with same name have different signatures
    private String javaMethodName;
    private String sqlMethodName;
    private List<ParameterWrapper> parameters = new ArrayList<>();
    private ParameterWrapper returnParameter;
    private String javaMethodSignature;

    public void addParameter(ParameterWrapper parameterWrapper) {
        parameters.add(parameterWrapper);
    }

    public String getJavaSignature(boolean includeArgumentNames){

        AtomicInteger idx = new AtomicInteger(0);
        return this.getParameters()
                .stream()
                .map(e -> (
                        e.isArray() ? "java.sql.Array" : e.isComplexType() ? "java.sql.Struct" : e.getJavaTypeName())
                        + (includeArgumentNames ? " arg" + idx.getAndIncrement(): "")
                )
                .collect(Collectors.joining(", "));
    }

}
