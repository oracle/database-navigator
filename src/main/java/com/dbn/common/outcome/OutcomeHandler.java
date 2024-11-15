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

import com.dbn.common.util.Prioritized;

/**
 * Common purpose outcome handling component acting on a given {@link Outcome}
 * THe handler is naturally prioritized allowing to control the sequence of handlers when invoked for same outcome
 *
 * @author Dan Cioca (Oracle)
 */
public interface OutcomeHandler extends Prioritized {

    /**
     * Handler method that is expected to entail the handling logic
     * @param outcome the {@link Outcome} to be handled
     */
    void handle(Outcome outcome);

}
