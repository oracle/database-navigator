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

package com.dbn.common.validation;

/**
 * Interface representing a generic validator for an object of type T.
 * Implementations of this interface are used to perform validation checks
 * on the provided target object to ensure it meets specific criteria or rules.
 *
 * @param <T> the type of object this validator is intended to validate
 *
 * @author Dan Cioca (Oracle)
 */
public interface Validator<T> {

    /**
     * Validates the given target object to ensure it meets the required criteria or rules.
     * If the target object fails validation, a {@link ValidationException} is thrown.
     *
     * @param target the object to be validated, of type T
     * @throws ValidationException if the validation of the target object fails
     */
    void validate(T target) throws ValidationException;
}
