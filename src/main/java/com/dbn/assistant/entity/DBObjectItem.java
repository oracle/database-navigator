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

package com.dbn.assistant.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * This class is to define object list items ( tables & views ) for each profile instance we have, and specify whether they are selected
 */
@Getter
@AllArgsConstructor
@ToString
/**
 * POJO class that represent an object in the database, basically table or view
 */
public class DBObjectItem {
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DBObjectItem that = (DBObjectItem) o;
    //object name and owner ae case in-sensitive in Oracle DB
    return owner.equalsIgnoreCase(that.owner) && name.equalsIgnoreCase(that.name) &&
        type == that.type;
  }

  @Override
  public int hashCode() {
    return Objects.hash(owner, name, type);
  }

  @NotNull
  public String owner;
  public String name;
  public DatabaseObjectType type;

}
