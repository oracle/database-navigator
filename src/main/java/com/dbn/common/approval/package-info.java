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

/**
 * User approval infrastructure for operations that require an explicit
 * user decision before they may proceed.
 * <p>
 * Domain objects mark themselves as {@link com.dbn.common.approval.UserApprovable}
 * or {@link com.dbn.common.approval.ProjectUserApprovable} and declare the approval actions
 * they support. Action metadata is provided through a registered {@link com.dbn.common.approval.UserApprovalAdapter}.
 * The
 * {@link com.dbn.common.approval.UserApprovalManager} owns the persisted and
 * temporary approval keys.
 */
package com.dbn.common.approval;
