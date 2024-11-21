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

package com.dbn.language.common.element.parser;

import lombok.Getter;

@Getter
public enum ParseResultType {
    FULL_MATCH (2),
    PARTIAL_MATCH(1),
    NO_MATCH(0);

    private final int score;

    ParseResultType(int score) {
        this.score = score;
    }

    public static ParseResultType worseOf(ParseResultType ... resultTypes) {
        ParseResultType worse = null;
        for (ParseResultType resultType : resultTypes) {
            if (worse == null || worse.score > resultType.score) {
                worse = resultType;
            }
        }
        return worse;
    }

}
