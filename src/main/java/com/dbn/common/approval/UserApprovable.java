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

package com.dbn.common.approval;

/**
 * Marker for domain objects that require user approval before use.
 * <p>
 * Approval title, message, and key data are supplied by a matching
 * {@link UserApprovalAdapter} extension, keeping approval UI concerns out of
 * the domain object itself.
 */
public interface UserApprovable {
    boolean isAcknowledged();
}
