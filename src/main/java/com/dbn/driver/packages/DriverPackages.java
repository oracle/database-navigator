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

import com.dbn.common.util.XmlContents;
import lombok.SneakyThrows;
import org.jdom.Element;

import java.util.List;

import static com.dbn.common.options.setting.Settings.*;
import static com.dbn.common.util.Lists.convert;
import static java.util.Collections.unmodifiableList;

/**
 * DriverPackages holds metadata for supported database drivers.
 * The information is parsed from a driver-packages.xml file that includes the following structure:
 * <pre>
 * {@code
 * <driver-packages>
 *     <driver-package id="oracle-23.3-standard" name="Oracle 23.3" database-type="ORACLE">
 *         <library group-id="javax.resource" artifact-id="connector-api" version="1.5"/>
 *         <library group-id="oracle.jdbc" artifact-id="ojdbc8" version="23.3.0.23.09"/>
 *     </driver-package>
 *     <driver-package id="mysql-8.0-standard" name="MySQL 8.0.33" database-type="MYSQL">
 *         <library group-id="mysql" artifact-id="mysql-connector-j" version="8.0.33"/>
 *     </driver-package>
 *     ...
 * </driver-packages>
 * }
 * </pre>
 * It supports the association of database types and corresponding libraries.
 * Libraries are specified with groupId, artifactId, and version attributes.
 *
 * @author Ayoub Aarrasse
 */
public class DriverPackages {
  private static final DriverPackages INSTANCE = new DriverPackages();
  private final List<DriverPackage> driverPackages;

  @SneakyThrows
  private DriverPackages() {
    Element element = XmlContents.fileToElement(getClass(), "driver-packages.xml");
    List<Element> packageElements = element.getChildren("driver-package");

    driverPackages = unmodifiableList(convert(packageElements, DriverPackages::createDriverPackage));
  }

  private static DriverPackage createDriverPackage(Element element) {
    String id = stringAttribute(element, "id");
    String name = stringAttribute(element, "name");
    String databaseType = stringAttribute(element, "database-type");

    List<Library> libraries = convert(element.getChildren("library"), DriverPackages::createLibrary);

    return new DriverPackage(id, name, databaseType, libraries);
  }

  private static Library createLibrary(Element element) {
    String groupId = stringAttribute(element, "group-id");
    String artifactId = stringAttribute(element, "artifact-id");
    String version = stringAttribute(element, "version");

    return new Library(groupId, artifactId, version);
  }

  public static List<DriverPackage> driverPackages() {
    return INSTANCE.driverPackages;
  }
}