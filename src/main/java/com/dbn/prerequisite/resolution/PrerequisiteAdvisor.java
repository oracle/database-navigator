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

package com.dbn.prerequisite.resolution;

import com.dbn.connection.context.DatabaseContext;

/**
 * The PrerequisiteAdvisor interface provides a mechanism for generating
 * advice related to a prerequisite within a given database context.
 * It is used to generate a user-readable recommendation on the resolution
 * of an unmet prerequisite, encapsulated in a {@link PrerequisiteAdvice} object.
 */
public interface PrerequisiteAdvisor {
    PrerequisiteAdvice advise(DatabaseContext context);
}
