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

package com.dbn.code.common.style.options;

import com.dbn.common.options.PersistentConfiguration;
import com.dbn.common.util.Naming;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import java.util.Objects;

import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.util.Strings.isMixedCase;
import static com.dbn.common.util.Strings.toLowerCase;
import static com.dbn.common.util.Strings.toUpperCase;

@Getter
@Setter
@EqualsAndHashCode
public class CodeStyleCaseOption implements PersistentConfiguration {
    private String name;
    private boolean ignoreMixedCase;
    private CodeStyleCase styleCase;

    public CodeStyleCaseOption(String id, CodeStyleCase styleCase, boolean ignoreMixedCase) {
        this.name = id;
        this.styleCase = styleCase;
        this.ignoreMixedCase = ignoreMixedCase;
    }

    public @NonNls String format(String string) {
        if (string != null) {
            switch (styleCase) {
                case UPPER: return ignore(string) ? string : toUpperCase(string);
                case LOWER: return ignore(string) ? string : toLowerCase(string);
                case CAPITALIZED: return ignore(string) ? string : Naming.capitalize(string);
                case PRESERVE: return string;
            }
        }
        return null;
    }

    boolean ignore(String string) {
        return string.startsWith("`") || string.startsWith("'") || string.startsWith("\"") || (ignoreMixedCase && isMixedCase(string));
    }

    /*********************************************************
     *                 PersistentConfiguration               *
     *********************************************************/
    @Override
    public void readConfiguration(Element element) {
        name = stringAttribute(element, "name");
        String style = stringAttribute(element, "value");
        styleCase =
                Objects.equals(style, "upper") ? CodeStyleCase.UPPER :
                Objects.equals(style, "lower") ? CodeStyleCase.LOWER :
                Objects.equals(style, "capitalized") ? CodeStyleCase.CAPITALIZED :
                Objects.equals(style, "preserve") ? CodeStyleCase.PRESERVE : CodeStyleCase.PRESERVE;
    }

    @Override
    public void writeConfiguration(Element element) {
        String value =
                styleCase == CodeStyleCase.UPPER ? "upper" :
                styleCase == CodeStyleCase.LOWER ? "lower" :
                styleCase == CodeStyleCase.CAPITALIZED ? "capitalized" :
                styleCase == CodeStyleCase.PRESERVE ? "preserve" :  "preserve";

        element.setAttribute("name", name);
        element.setAttribute("value", value);
    }

    @Override
    public String toString() {
        return name + "=" + styleCase.name();
    }
}
