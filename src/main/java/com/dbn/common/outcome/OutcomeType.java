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

package com.dbn.common.outcome;

/**
 * Definition of possible types of outcomes for a given process or routine
 * Used to qualify {@link Outcome} entities with the execution status of a process
 *
 * @author Dan Cioca (Oracle)
 */
public enum OutcomeType {
    SUCCESS,
    WARNING,
    FAILURE;
}
