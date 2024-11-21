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

package com.dbn.object.filter.quick;

import com.dbn.common.state.PersistentStateElement;
import com.dbn.object.common.DBObject;
import com.dbn.object.filter.ConditionOperator;
import com.dbn.object.filter.NameFilterCondition;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;

import static com.dbn.common.options.setting.Settings.booleanAttribute;
import static com.dbn.common.options.setting.Settings.setBooleanAttribute;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class ObjectQuickFilterCondition extends NameFilterCondition implements PersistentStateElement {
    private transient ObjectQuickFilter<?> filter;
    private boolean active = true;

    public ObjectQuickFilterCondition(ObjectQuickFilter<?> filter, ConditionOperator operator, String pattern, boolean active) {
        super(operator, pattern);
        this.filter = filter;
        this.active = active;
    }

    public ObjectQuickFilterCondition(ObjectQuickFilter<?> filter) {
        this.filter = filter;
    }

    public boolean accepts(DBObject object) {
        return accepts(object.getName());
    }

    public int index() {
        return filter.getConditions().indexOf(this);
    }

    @Override
    public void readState(Element element) {
        super.readState(element);
        active = booleanAttribute(element, "active", true);
    }

    @Override
    public void writeState(Element element) {
        super.writeState(element);
        setBooleanAttribute(element, "active", active);
    }
}
