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

package com.dbn.execution.java.history.ui;

import com.dbn.common.icon.Icons;
import com.dbn.common.ui.tree.DBNTreeNode;
import com.dbn.common.util.Commons;
import com.dbn.object.type.DBObjectType;
import lombok.Getter;

import javax.swing.Icon;
import javax.swing.tree.TreeNode;
import java.util.Collections;
import java.util.List;

@Getter
public class JavaExecutionHistoryTreeNode extends DBNTreeNode {
	public enum Type {
		ROOT,
		CONNECTION,
		SCHEMA,
		CLASS,
		METHOD,
		UNKNOWN
	}

	private final String name;
	private final Type type;

	public JavaExecutionHistoryTreeNode(JavaExecutionHistoryTreeNode parent, Type type, String name) {
		this.name = name;
		this.type = type;
		if (parent != null) {
			parent.add(this);
		}
	}

	public Icon getIcon() {
		return
				type == Type.CONNECTION ? Icons.CONNECTION_CONNECTED :
				type == Type.SCHEMA ? Icons.DBO_SCHEMA :
				type == Type.CLASS ? Icons.DBO_JAVA_CLASS :
				type == Type.METHOD ? Icons.DBO_JAVA_METHOD : null;
	}

	public static Type getNodeType(DBObjectType objectType) {
		return
				objectType == DBObjectType.SCHEMA ? Type.SCHEMA :
				objectType == DBObjectType.JAVA_CLASS ? Type.CLASS :
				objectType == DBObjectType.JAVA_METHOD ? Type.METHOD :
						Type.UNKNOWN;
	}

	public List<TreeNode> getChildren() {
		return Commons.nvl(children, () -> Collections.emptyList());
	}

	@Override
	public boolean getAllowsChildren() {
		return type != Type.METHOD;
	}

	public boolean isValid() {
		return true;
	}
}