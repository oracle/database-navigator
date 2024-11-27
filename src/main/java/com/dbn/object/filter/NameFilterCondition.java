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

package com.dbn.object.filter;

import com.dbn.common.state.PersistentStateElement;
import com.dbn.common.util.Commons;
import com.dbn.common.util.Strings;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.StringTokenizer;

import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.setEnumAttribute;

@Getter
@Setter
@EqualsAndHashCode
public abstract class NameFilterCondition implements PersistentStateElement {
    private ConditionOperator operator = ConditionOperator.EQUAL;
    private String pattern;

    protected NameFilterCondition() {
    }

    public NameFilterCondition(ConditionOperator operator, String pattern) {
        this.operator = operator;
        this.pattern = pattern;
    }

    @NotNull
    public ConditionOperator getOperator() {
        return Commons.nvl(operator, ConditionOperator.EQUAL);
    }

    public boolean accepts(String name) {
        switch (operator) {
            case EQUAL: return isEqual(name, pattern);
            case NOT_EQUAL: return !isEqual(name, pattern);
            case LIKE: return isLike(name, pattern);
            case NOT_LIKE: return !isLike(name, pattern);
        }
        return false;
    }

    private static boolean isEqual(String name, String pattern) {
        return Strings.equalsIgnoreCase(name, pattern);
    }

    private static boolean isLike(String name, String pattern) {
        StringTokenizer tokenizer = new StringTokenizer(pattern, "*%");
        int startIndex = 0;
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            int index = Strings.indexOfIgnoreCase(name, token, startIndex);
            if (index == -1 || (index > 0 && startIndex == 0 && !startsWithWildcard(pattern))) return false;
            startIndex = index + token.length();
        }

        return true;
    }

    private static boolean startsWithWildcard(String pattern) {
        return pattern.indexOf('*') == 0 || pattern.indexOf('%') == 0;
    }

    @Override
    public void readState(Element element) {
        operator = enumAttribute(element, "operator", ConditionOperator.class);
        pattern = element.getAttributeValue("pattern");
    }

    @Override
    public void writeState(Element element) {
        setEnumAttribute(element, "operator", operator);
        element.setAttribute("pattern", pattern);
    }

    @Override
    public String toString() {
        return "NAME " + operator + "'" + pattern + "'";
    }
}
