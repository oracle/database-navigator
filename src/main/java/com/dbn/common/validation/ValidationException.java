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

import org.jetbrains.annotations.Nls;

/**
 * Exception thrown to indicate a validation error in a given object or process.
 *
 * This exception is typically used in scenarios where specific validation criteria
 * are not met, and it provides a descriptive message to enable better understanding
 * of the encountered validation error.
 *
 * The {@link ValidationException} is commonly utilized in conjunction with validation frameworks
 * or mechanisms such as the {@link Validator} interface.
 *
 * @author Dan Cioca (Oracle)
 */
public class ValidationException extends Exception {
    public ValidationException(@Nls String message) {
        super(message);
    }
}
