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

import com.dbn.common.filter.Filter;
import com.dbn.common.options.setting.Settings;
import com.dbn.common.state.PersistentStateElement;
import com.dbn.common.util.Cloneable;
import com.dbn.common.util.Lists;
import com.dbn.object.common.DBObject;
import com.dbn.object.filter.ConditionJoinType;
import com.dbn.object.filter.ConditionOperator;
import com.dbn.object.type.DBObjectType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.options.setting.Settings.newElement;

@Getter
@Setter
@EqualsAndHashCode
public class ObjectQuickFilter<T extends DBObject> implements Filter<T>, Cloneable<ObjectQuickFilter<T>>, PersistentStateElement {
    private final DBObjectType objectType;
    private final List<ObjectQuickFilterCondition> conditions = new ArrayList<>();
    private ConditionJoinType joinType = ConditionJoinType.AND;

    private ObjectQuickFilter(DBObjectType objectType, ConditionJoinType joinType) {
        this.objectType = objectType;
        this.joinType = joinType;
    }

    public ObjectQuickFilter(DBObjectType objectType) {
        this.objectType = objectType;
    }

    public ObjectQuickFilterCondition addNewCondition(ConditionOperator operator) {
        return addCondition(operator, "", true);
    }

    public ObjectQuickFilterCondition addCondition(ConditionOperator operator, String pattern, boolean active) {
        ObjectQuickFilterCondition condition = new ObjectQuickFilterCondition(this, operator, pattern, active);
        conditions.add(condition);
        return condition;
    }

    public void removeCondition(ObjectQuickFilterCondition condition) {
        conditions.remove(condition);
    }

    public boolean isEmpty() {
        return conditions.isEmpty() || Lists.noneMatch(conditions, condition -> condition.isActive());
    }

    @Override
    public boolean accepts(DBObject object) {
        if (conditions.size() > 0) {
            if (joinType == ConditionJoinType.AND) {
                for (ObjectQuickFilterCondition condition : conditions) {
                    if (condition.isActive() && !condition.accepts(object)) return false;
                }
                return true;
            } else if (joinType == ConditionJoinType.OR) {
                for (ObjectQuickFilterCondition condition : conditions) {
                    if (condition.isActive() && condition.accepts(object)) return true;
                }
                return false;
            }
        }
        return true;
    }

    @Override
    public ObjectQuickFilter<T> clone() {
        ObjectQuickFilter<T> filterClone = new ObjectQuickFilter<>(objectType, joinType);
        for (ObjectQuickFilterCondition condition : conditions) {
            filterClone.addCondition(
                    condition.getOperator(),
                    condition.getPattern(),
                    condition.isActive());
        }
        return filterClone;
    }

    @Override
    public void readState(Element element) {
        joinType = Settings.enumAttribute(element, "join-type", ConditionJoinType.AND);
        for (Element child : element.getChildren()) {
            ObjectQuickFilterCondition condition = new ObjectQuickFilterCondition(this);
            condition.readState(child);
            conditions.add(condition);
        }
    }

    @Override
    public void writeState(Element element) {
        element.setAttribute("join-type", joinType.name());
        for (ObjectQuickFilterCondition condition : conditions) {
            Element conditionElement = newElement(element, "condition");
            condition.writeState(conditionElement);
        }


    }

    public void clear() {
        conditions.clear();
    }
}
