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

package com.dbn.assistant.tool;

import org.jetbrains.annotations.NonNls;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public interface AssistantToolInfo {

    @NonNls
    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @interface ToolSpec {
        AssistantToolType type();

        AssistantToolCategory category();

        String name();

        String description();

        boolean interactive() default false;
    }

    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @interface FactorySpec {
        Class<? extends AssistantTool> spec();
        Class<? extends AssistantToolBase> impl();
    }

    @NonNls
    @Target({ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @interface UtilitySpec {
        String name();
        String description();
        String summary() default "";
        boolean discontinued() default false;
    }

    @NonNls
    @Target({ElementType.PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    @interface ParamSpec {
        String value();
    }
}
