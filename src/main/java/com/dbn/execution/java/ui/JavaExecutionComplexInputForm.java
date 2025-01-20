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

package com.dbn.execution.java.ui;

import com.dbn.common.ui.form.DBNCollapsibleForm;
import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.ui.form.DBNFormBase;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.util.List;

public class JavaExecutionComplexInputForm extends DBNFormBase implements DBNCollapsibleForm {
	private final JPanel mainPanel;
	private final String title;

	public JavaExecutionComplexInputForm(DBNForm parent, String clazz, List<JavaExecutionInputFieldForm> inputs) {
		super(parent);
		mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

		this.title = clazz;
		JPanel cp = new JPanel();

		for(JavaExecutionInputFieldForm input : inputs){
			mainPanel.add(input.getComponent());
		}

		mainPanel.add(cp);

	}

	@Override
	public String getCollapsedTitle() {
		return title;
	}

	@Override
	public String getCollapsedTitleDetail() {
		return "";
	}

	@Override
	public String getExpandedTitle() {
		return title;
	}

	@Override
	protected JComponent getMainComponent() {
		return mainPanel;
	}
}
