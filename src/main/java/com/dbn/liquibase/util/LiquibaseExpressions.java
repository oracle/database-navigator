/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 */

package com.dbn.liquibase.util;

import liquibase.ContextExpression;
import liquibase.LabelExpression;

import static com.dbn.common.util.Strings.isEmpty;

/** Validation utilities for Liquibase context and label expressions. */
public final class LiquibaseExpressions {
    private LiquibaseExpressions() {
    }

    public static boolean isValidContexts(String expression) {
        return isValid(expression, true);
    }

    public static boolean isValidLabels(String expression) {
        return isValid(expression, false);
    }

    private static boolean isValid(String expression, boolean contexts) {
        if (isEmpty(expression) || isMalformed(expression)) return isEmpty(expression);

        try {
            if (contexts) new ContextExpression(expression);
            else new LabelExpression(expression);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static boolean isMalformed(String expression) {
        int parentheses = 0;
        boolean emptyTerm = true;
        for (int i = 0; i < expression.length(); i++) {
            char character = expression.charAt(i);
            if (character == '(') {
                parentheses++;
                emptyTerm = true;
            } else if (character == ')') {
                if (parentheses-- == 0 || emptyTerm) return true;
            } else if (character == ',') {
                if (emptyTerm) return true;
                emptyTerm = true;
            } else if (!Character.isWhitespace(character)) {
                emptyTerm = false;
            }
        }
        return parentheses != 0 || emptyTerm;
    }
}
