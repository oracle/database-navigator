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

package com.dbn.driver.packages;

import lombok.Getter;

import java.util.List;

/**
 * DriverPackage represents a set of Maven libraries required for a specific database driver.
 * Each driver package includes an id, a name, a database type, and a list of libraries.
 *
 * Example:
 * <pre>
 * {@code
 * <driver-package id="oracle-23.3-standard" name="Oracle 23.3" database-type="ORACLE">
 *     <library group-id="javax.resource" artifact-id="connector-api" version="1.5"/>
 *     <library group-id="oracle.jdbc" artifact-id="ojdbc8" version="23.3.0.23.09"/>
 * </driver-package>
 * }
 * </pre>
 *
 * @author Ayoub Aarrasse
 */
@Getter
public class DriverPackage {
  private final String id;
  private final String name;
  private final String databaseType;
  private final List<Library> libraries;

  public DriverPackage(String id, String name, String databaseType, List<Library> libraries) {
    this.id = id;
    this.name = name;
    this.databaseType = databaseType;
    this.libraries = libraries;
  }

  @Override
  public String toString() {
    return String.format("DriverPackage [id=%s, name=%s, databaseType=%s, libraries=%s]", id, name, databaseType, libraries);
  }
}