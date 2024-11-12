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

package com.dbn.assistant.chat.message;

import com.dbn.common.constant.Constant;

/**
 * This is for the possible authors that can send a message in the chat
 *
 * @author Ayoub Aarrasse (Oracle)
 */
public enum AuthorType implements Constant<AuthorType> {
  USER,   // user prompt
  AGENT,  // assistant backend responses
  SYSTEM; // error responses or information blocks
}