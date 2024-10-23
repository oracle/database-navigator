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

package com.dbn.database.common.metadata.impl;

import com.dbn.database.common.metadata.DBObjectMetadataBase;
import com.dbn.database.common.metadata.def.DBJavaObjectMetadata;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DBJavaObjectMetadataImpl extends DBObjectMetadataBase implements DBJavaObjectMetadata {

	public DBJavaObjectMetadataImpl(ResultSet resultSet) {
		super(resultSet);
	}

	@Override
	public String getObjectName() throws SQLException {
		return getString("OBJECT_NAME");
	}

	@Override
	public String getObjectKind() throws SQLException {
		return getString("OBJECT_KIND");
	}

	@Override
	public String getObjectAccessibility() throws SQLException {
		return getString("OBJECT_ACCESSIBILITY");
	}

	@Override
	public boolean isFinal() throws SQLException {
		return isYesFlag("IS_FINAL");
	}

	@Override
	public boolean isAbstract() throws SQLException {
		return isYesFlag("IS_ABSTRACT");
	}

	@Override
	public boolean isStatic() throws SQLException {
		return isYesFlag("IS_STATIC");
	}

	@Override
	public boolean isInner() throws SQLException {
		return isYesFlag("IS_INNER");
	}
}
