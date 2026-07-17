/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.mcp.model;

public enum McpServerImplementation {
    STANDARD_JAVA("Standard Java"),
    MICRONAUT_NATIVE("Micronaut Native (GraalVM)"),
    MICRONAUT_CONTAINER("Micronaut Container Image");

    private final String displayName;

    McpServerImplementation(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Both Micronaut variants compile to a GraalVM native executable; the container
     * variant just performs the compilation inside a Linux builder container.
     */
    public boolean isNative() {
        return this != STANDARD_JAVA;
    }

    public boolean isContainer() {
        return this == MICRONAUT_CONTAINER;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
