/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.assistant.tool.approval;

import com.dbn.assistant.tool.AssistantToolCategory;
import com.dbn.assistant.tool.AssistantToolType;
import org.jdom.Element;
import org.junit.Assert;
import org.junit.Test;

import static com.dbn.assistant.tool.AssistantToolCategory.USER_INTERACTION;
import static com.dbn.assistant.tool.AssistantToolType.USER_PROMPTS;
import static com.dbn.assistant.tool.approval.AssistantToolApprovalStatus.APPROVED;
import static com.dbn.assistant.tool.approval.AssistantToolApprovalStatus.PROMPTED;

public class AssistantToolApprovalsTest {

    @Test
    public void normalizesPersistedInteractiveApprovals() {
        AssistantToolApprovals approvals = new AssistantToolApprovals();
        approvals.readState(approvalsElement(USER_INTERACTION, USER_PROMPTS, APPROVED));

        Assert.assertEquals(PROMPTED, approvals.getStatus(USER_INTERACTION));
        Assert.assertEquals(PROMPTED, approvals.getStatus(USER_PROMPTS));
    }

    @Test
    public void doesNotAllowInteractiveCategoryApprovalsToBeSet() {
        AssistantToolApprovals approvals = new AssistantToolApprovals();
        approvals.setStatus(USER_INTERACTION, APPROVED);

        Assert.assertEquals(PROMPTED, approvals.getStatus(USER_INTERACTION));
    }

    private static Element approvalsElement(
            AssistantToolCategory category,
            AssistantToolType type,
            AssistantToolApprovalStatus status) {
        Element approvals = new Element("approvals");
        Element categories = new Element("categories");
        categories.addContent(new Element("category")
                .setAttribute("id", category.name())
                .setAttribute("status", status.name()));
        approvals.addContent(categories);

        Element types = new Element("types");
        types.addContent(new Element("type")
                .setAttribute("id", type.name())
                .setAttribute("status", status.name()));
        approvals.addContent(types);
        return approvals;
    }
}
