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

package com.dbn.language.common.element.impl;

import com.dbn.common.Linked;
import com.dbn.language.common.element.parser.Branch;
import com.dbn.language.common.element.parser.BranchCheck;

import java.util.Set;

public class ElementTypeRef extends Linked<ElementTypeRef> {
    public final ElementTypeBase elementType;
    public final double version;
    public boolean optional;
    public final Branch branch;
    public final Set<BranchCheck> branchChecks;

    public ElementTypeRef(ElementTypeBase elementType) {
        this(elementType, false, 0, null, null);
    }

    public ElementTypeRef(ElementTypeBase elementType, boolean optional, double version, Branch branch, Set<BranchCheck> branchChecks) {
        super(null);
        this.elementType = elementType;
        this.optional = optional;
        this.version = version;
        this.branch = branch;
        this.branchChecks = branchChecks;
    }

    public boolean check(Set<Branch> branches, double currentVersion) {
        if (version > currentVersion) {
            return false;
        }

        if (branches != null && !branches.isEmpty() && branchChecks != null) {
            boolean finalCheckValue = true;
            for (Branch branch : branches) {
                for (BranchCheck branchCheck : branchChecks) {
                    boolean checkValue = branchCheck.check(branch, currentVersion);
                    if (branchCheck.getType() == BranchCheck.Type.FORBIDDEN) {
                        if (!checkValue) return false;
                    } else if (branchCheck.getType() == BranchCheck.Type.ALLOWED) {
                        finalCheckValue = finalCheckValue || checkValue;
                    }
                }
            }
            return finalCheckValue;
        }


/*
        if (branches != null) {
            Set<Branch> checkedBranches = getParentElementType().getCheckedBranches();
            if (checkedBranches != null) {
                if (supportedBranches != null) {
                    for (Branch branch : branches) {
                        if (checkedBranches.contains(branch)) {
                            for (Branch supportedBranch : supportedBranches) {
                                if (supportedBranch.equals(branch) && currentVersion >= supportedBranch.getVersion()) {
                                    return true;
                                }
                            }
                            return false;
                        }
                    }
                } else {
                    return false;
                }
            }
        }
*/

        return true;
    }

    boolean isOptionalToHere() {
        if (getIndex() == 0) return false;

        ElementTypeRef previous = this.previous;
        while (previous != null) {
            if (!previous.optional) {
                return false;
            }
            previous = previous.previous;
        }
        return true;
    }

    public boolean isOptionalFromHere() {
        ElementTypeRef next = this.next;
        while (next != null) {
            if (!next.optional) {
                return false;
            }
            next = next.next;
        }
        return true;
    }

    @Override
    public String toString() {
        return elementType.getName();
    }
}
