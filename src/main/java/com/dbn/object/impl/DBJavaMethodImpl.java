/*
 * Copyright 2024 Oracle and/or its affiliates
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

package com.dbn.object.impl;

import com.dbn.browser.model.BrowserTreeNode;
import com.dbn.common.icon.Icons;
import com.dbn.connection.ConnectionHandler;
import com.dbn.database.common.metadata.def.DBJavaMethodMetadata;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.DBJavaParameter;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBObjectImpl;
import com.dbn.object.common.list.DBObjectList;
import com.dbn.object.common.list.DBObjectListContainer;
import com.dbn.object.common.list.DBObjectNavigationList;
import com.dbn.object.common.list.ObjectListProvider;
import com.dbn.object.lookup.DBJavaClassRef;
import com.dbn.object.type.DBJavaAccessibility;
import com.dbn.object.type.DBObjectType;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static com.dbn.common.icon.Icons.withStaticMarker;
import static com.dbn.object.common.property.DBObjectProperty.ABSTRACT;
import static com.dbn.object.common.property.DBObjectProperty.FINAL;
import static com.dbn.object.common.property.DBObjectProperty.STATIC;

@Getter
public class DBJavaMethodImpl extends DBObjectImpl<DBJavaMethodMetadata> implements DBJavaMethod {
	private short index;
	private String signature;
	private String className;
	private String returnType;
	private DBJavaClassRef returnClass;
	private DBJavaAccessibility accessibility;

	public DBJavaMethodImpl(@NotNull DBJavaClass javaClass, DBJavaMethodMetadata metadata) throws SQLException {
		super(javaClass, metadata);
	}

	@Override
	public @NotNull DBObjectType getObjectType() {
		return DBObjectType.JAVA_METHOD;
	}

	@Override
	protected void initLists(ConnectionHandler connection) {
		super.initLists(connection);

		DBSchema schema = getSchema();
		DBObjectListContainer childObjects = ensureChildObjects();
		childObjects.createSubcontentObjectList(DBObjectType.JAVA_PARAMETER, this, schema);
	}

	@Override
	protected String initObject(ConnectionHandler connection, DBObject parentObject, DBJavaMethodMetadata metadata) throws SQLException {
		index = metadata.getMethodIndex();
		signature = metadata.getMethodSignature();
		className = metadata.getClassName();
		returnType = metadata.getReturnType();
		accessibility = DBJavaAccessibility.get(metadata.getAccessibility());

		String returnClassName = metadata.getReturnClassName();
		if (returnClassName != null) {
			DBSchema schema = parentObject.getSchema();
			returnClass = new DBJavaClassRef(schema, returnClassName, "SYS");
		}

		set(STATIC, metadata.isStatic());
		set(FINAL, metadata.isFinal());
		set(ABSTRACT, metadata.isAbstract());
		return metadata.getMethodName();
	}

	@Override
	public short getPosition() {
		return index;
	}

	@Override
	public String getPresentableText() {
		return signature;
	}

	@Nullable
	private DBObjectList<DBJavaParameter> initParameterList() {
		DBObjectList<DBJavaParameter> parameterList = getChildObjectList(DBObjectType.JAVA_PARAMETER);
		if (parameterList == null) return null;
		if (parameterList.isLoaded()) return parameterList;

		if (!parameterList.isLoading()) parameterList.loadInBackground();
		return null;
	}

	@Override
	@Nullable
	public Icon getIcon() {
		return
			isAbstract() ? Icons.DBO_JAVA_METHOD_ABSTRACT :
			isStatic() ? withStaticMarker(Icons.DBO_JAVA_METHOD) :
				Icons.DBO_JAVA_METHOD;
		// TODO accessibility overlays
	}

	@Override
	public DBJavaClass getReturnClass() {
		return returnClass == null ? null : returnClass.get();
	}

	@Override
	public List<DBJavaParameter> getParameters() {
		return getChildObjects(DBObjectType.JAVA_PARAMETER);
	}

	@Override
	public DBJavaClass getOwnerClass() {
		return getParentObject();
	}

	@Override
	public boolean isStatic() {
		return is(STATIC);
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
	protected @Nullable List<DBObjectNavigationList> createNavigationLists() {
		List<DBObjectNavigationList> navigationLists = new LinkedList<>();
		DBObjectList<DBJavaParameter> parameterList = initParameterList();
		if (parameterList != null) {
			if (parameterList.isLoaded()) {
                navigationLists.add(DBObjectNavigationList.create("Parameters", getParameters()));
            } else {
				ObjectListProvider<DBJavaParameter> provider = () -> getParameters();
				navigationLists.add(DBObjectNavigationList.create("Parameters", provider)); // lazy
			}
		}

		if (returnClass != null) {
			if (returnClass.isLoaded()) {
                navigationLists.add(DBObjectNavigationList.create("Return Type", getReturnClass()));
            } else {
				ObjectListProvider<DBJavaClass> provider = () -> {
					DBJavaClass returnClass = getReturnClass();
					return returnClass == null ? Collections.emptyList() : List.of(returnClass);
				};
				navigationLists.add(DBObjectNavigationList.create("Return Type", provider));
			}
		}

		return navigationLists;
	}

	/*********************************************************
	 *                     TreeElement                       *
	 *********************************************************/
	@Override
	@NotNull
	public List<BrowserTreeNode> buildPossibleTreeChildren() {
		return super.buildPossibleTreeChildren();
/*
		return DatabaseBrowserUtils.createList(
				getChildObjectList(DBObjectType.JAVA_PARAMETER));
*/
	}

	@Override
	public boolean hasVisibleTreeChildren() {
		return false;
/*
		ObjectTypeFilterSettings settings = getObjectTypeFilterSettings();
		return settings.isVisible(DBObjectType.JAVA_PARAMETER);
*/
	}
}
