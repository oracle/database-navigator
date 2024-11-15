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

import lombok.experimental.UtilityClass;

/**
 * Utility class allowing creation of different types of {@link Outcome}
 *
 * @author Dan Cioca (Oracle)
 */
@UtilityClass
public class Outcomes {
    public static Outcome success(String title, String message) {
        return new Outcome(OutcomeType.SUCCESS, title, message);
    }

    public static Outcome warning(String title, String message) {
        return new Outcome(OutcomeType.WARNING, title, message);
    }

    public static Outcome warning(String title, String message, Exception exception) {
        return new Outcome(OutcomeType.WARNING, title, message, exception);
    }

    public static Outcome failure(String title, String message, Exception exception) {
        return new Outcome(OutcomeType.FAILURE, title, message, exception);
    }
}
