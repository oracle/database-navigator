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

package com.dbn.object.impl;

import com.dbn.common.icon.CompositeIcon;
import com.dbn.connection.ConnectionHandler;
import com.dbn.database.common.metadata.def.DBJavaObjectMetadata;
import com.dbn.object.DBJavaObject;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBSchemaObjectImpl;
import com.dbn.object.type.DBJavaObjectAccessibility;
import com.dbn.object.type.DBJavaObjectKind;
import com.dbn.object.type.DBObjectType;
import com.intellij.icons.AllIcons;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.sql.SQLException;

import static com.dbn.object.common.property.DBObjectProperty.ABSTRACT;
import static com.dbn.object.common.property.DBObjectProperty.FINAL;
import static com.dbn.object.common.property.DBObjectProperty.INNER;
import static com.dbn.object.common.property.DBObjectProperty.STATIC;
import static com.dbn.object.type.DBJavaObjectAccessibility.PRIVATE;
import static com.dbn.object.type.DBJavaObjectKind.ENUM;
import static com.dbn.object.type.DBJavaObjectKind.INTERFACE;

@Getter
public class DBJavaObjectImpl extends DBSchemaObjectImpl<DBJavaObjectMetadata> implements DBJavaObject {

	private DBJavaObjectKind kind;
	private DBJavaObjectAccessibility accessibility;

	DBJavaObjectImpl(DBSchema schema, DBJavaObjectMetadata metadata) throws SQLException {
		super(schema, metadata);
	}

	@Override
	public @NotNull DBObjectType getObjectType() {
		return DBObjectType.JAVA_OBJECT;
	}

	@Override
	protected String initObject(ConnectionHandler connection, DBObject parentObject, DBJavaObjectMetadata metadata) throws SQLException {
		this.kind = DBJavaObjectKind.get(metadata.getObjectKind());
		this.accessibility = DBJavaObjectAccessibility.get(metadata.getObjectAccessibility());

		set(FINAL, metadata.isFinal());
		set(ABSTRACT, metadata.isAbstract());
		set(STATIC, metadata.isStatic());
		set(INNER, metadata.isInner());

		return metadata.getObjectName().replace("/",".");
	}


	@Override
	protected void initLists(ConnectionHandler connection) {
		super.initLists(connection);
/*
		// TODO support inner classes as child objects
		DBSchema schema = getSchema();
		DBObjectListContainer childObjects = ensureChildObjects();

		childObjects.createSubcontentObjectList(JAVA_OBJECT, this, schema);
*/
	}

	@Override
	@Nullable
	public Icon getIcon() {
		// TODO cache all these icon variants in Icons (creating composites on every access is resource intensive)
		Icon baseIcon;

		if (kind == ENUM)
			baseIcon = AllIcons.Nodes.Enum;
		else if (kind == INTERFACE)
			baseIcon = AllIcons.Nodes.Interface;
		else if (isAbstract())
			baseIcon = AllIcons.Nodes.AbstractClass;
		else
			baseIcon = AllIcons.Nodes.Class;

		if (isFinal() && kind != ENUM)
			baseIcon = new CompositeIcon(baseIcon, AllIcons.Nodes.FinalMark, -17);

		if (this.accessibility == PRIVATE)
			baseIcon = new CompositeIcon(baseIcon, AllIcons.Nodes.Private, -10);

		return baseIcon;
	}

	@Override
	public boolean isFinal() {
		return is(FINAL);
	}

	@Override
	public boolean isAbstract() {
		return is(ABSTRACT);
	}

	@Override
	public boolean isStatic() {
		return is(STATIC);
	}

	@Override
	public boolean isInner() {
		return is(INNER);
	}
}
