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

package com.dbn.assistant.state;

import com.dbn.connection.ConnectionId;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.Topic;

import java.util.EventListener;

public interface AssistantStateListener extends EventListener {
    Topic<AssistantStateListener> TOPIC = Topic.create("AssistantStateListener", AssistantStateListener.class);
    void stateChanged(Project project, ConnectionId connectionId);
}
