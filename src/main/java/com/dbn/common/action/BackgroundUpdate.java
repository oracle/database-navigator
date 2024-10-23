/*
 * Copyright (c) 2024, Oracle and/or its affiliates.
 *
 * This software is dual-licensed to you under the Universal Permissive License
 * (UPL) 1.0 as shown at https://oss.oracle.com/licenses/upl or Apache License
 * 2.0 as shown at http://www.apache.org/licenses/LICENSE-2.0. You may choose
 * either license.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.dbn.common.action;

import com.intellij.openapi.actionSystem.AnActionEvent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation for actions that should be updated in background
 * Actions that take long to update their presentation should be annotated with {@link BackgroundUpdate}
 * (i.e. actions that have intensive computational logic inside the {@link com.intellij.openapi.actionSystem.AnAction#update(AnActionEvent)})
 * see {@link com.intellij.openapi.actionSystem.ActionUpdateThread}
 *
 * @author Dan Cioca (Oracle)
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface BackgroundUpdate {
}
