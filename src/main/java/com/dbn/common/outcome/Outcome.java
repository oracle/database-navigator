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

import com.dbn.common.util.Tagged;
import com.dbn.common.util.Titled;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolderBase;
import lombok.Getter;

import static com.dbn.common.util.Unsafe.cast;

/**
 * Common purpose process outcome information-holder
 * Used in the {@link OutcomeHandler} framework to capture success or failure
 * information about a given process, and allow multiple handlers to act on the outcome
 *
 * @author Dan Cioca (Oracle)
 */
@Getter
public class Outcome extends UserDataHolderBase implements Titled, Tagged {
    private final OutcomeType type;
    private String title;
    private String message;
    private Exception exception;
    private Object data;

    private Outcome(OutcomeType type) {
        this.type = type;
    }

    public static Outcome create(OutcomeType type) {
        return new Outcome(type);
    }

    public static Outcome success() {
        return new Outcome(OutcomeType.SUCCESS);
    }

    public static Outcome warning() {
        return new Outcome(OutcomeType.WARNING);
    }

    public static Outcome failure() {
        return new Outcome(OutcomeType.FAILURE);
    }

    public Outcome withTitle(String title) {
        this.title = title;
        return this;
    }

    public Outcome withMessage(String message) {
        this.message = message;
        return this;
    }

    public Outcome withException(Exception exception) {
        this.exception = exception;
        return this;
    }

    public Outcome withData(Object data) {
        this.data = data;
        return this;
    }

    public <T> Outcome withUserData(Key<T> key, T value) {
        putUserData(key, value);
        return this;
    }

    public <T> T getData() {
        return cast(data);
    }

    @Override
    public Object getSubject() {
        return data;
    }
}
