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

package com.dbn.generator;

import com.dbn.code.common.style.DBLCodeStyleManager;
import com.dbn.code.common.style.options.CodeStyleCaseOption;
import com.dbn.code.common.style.options.CodeStyleCaseSettings;
import com.dbn.common.message.MessageBundle;
import com.dbn.language.sql.SQLLanguage;
import com.dbn.object.DBColumn;
import com.dbn.object.DBDataset;
import com.dbn.object.common.DBObject;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.project.Project;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static com.dbn.common.dispose.Failsafe.nd;

public class SelectStatementGenerator extends StatementGenerator {
    private final AliasBundle aliases = new AliasBundle();
    private final List<DBObjectRef<DBObject>> objects;
    private final boolean enforceAliasUsage;

    public SelectStatementGenerator(DBDataset dataset) {
        objects = new ArrayList<>();
        objects.add(DBObjectRef.of(dataset));
        enforceAliasUsage = false;
    }

    public SelectStatementGenerator(List<DBObject> objects, boolean enforceAliasUsage) {
        this.objects = DBObjectRef.from(objects);
        this.enforceAliasUsage = enforceAliasUsage;
    }

    @Override
    public StatementGeneratorResult generateStatement(Project project) {
        StatementGeneratorResult result = new StatementGeneratorResult();
        MessageBundle messages = result.getMessages();

        Set<DBDataset> datasets = new TreeSet<>(DATASET_COMPARATOR);
        Set<DBColumn> columns = new TreeSet<>(COLUMN_COMPARATOR);

        for (DBObject object : DBObjectRef.get(objects)) {
            if (object instanceof DBColumn) {
                DBColumn column = (DBColumn) object;
                columns.add(column);
                datasets.add(column.getDataset());
            } else if (object instanceof DBDataset) {
                DBDataset dataset = (DBDataset) object;
                datasets.add(dataset);
                columns.addAll(dataset.getColumns());
            } else {
                messages.addErrorMessage(
                        "Only objects of type DATASET and COLUMN are supported for select statement generation.\n" +
                        "Please review your selection and try again.");
            }
        }

        DatasetJoinBundle joinBundle = null;
        if (datasets.size() > 1) {
            joinBundle = new DatasetJoinBundle(datasets, true);
            for (DBDataset dataset : datasets) {
                if (!joinBundle.contains(dataset)) {
                    messages.addWarningMessage("Could not join table " +
                            dataset.getName() + ". No references found to the other tables.");
                }
            }
        }

        String statement = generateSelectStatement(project, datasets, columns, joinBundle, enforceAliasUsage);
        result.setStatement(statement);
        return result;
    }

    private String generateSelectStatement(Project project, Set<DBDataset> datasets, Set<DBColumn> columns, DatasetJoinBundle joinBundle, boolean enforceAliasUsage) {
        CodeStyleCaseSettings styleCaseSettings = DBLCodeStyleManager.getInstance(project).getCodeStyleCaseSettings(SQLLanguage.INSTANCE);
        CodeStyleCaseOption kco = styleCaseSettings.getKeywordCaseOption();
        CodeStyleCaseOption oco = styleCaseSettings.getObjectCaseOption();

        boolean useAliases = datasets.size() > 1 || enforceAliasUsage;

        StringBuilder statement = new StringBuilder();
        statement.append(kco.format("select\n"));
        Iterator<DBColumn> columnIterator = columns.iterator();
        while (columnIterator.hasNext()) {
            DBColumn column = nd(columnIterator.next());
            DBDataset dataset = nd(column.getDataset());
            statement.append("    ");
            if (useAliases) {
                statement.append(aliases.getAlias(dataset));
                statement.append(".");
            }
            statement.append(oco.format(column.getQuotedName(false)));
            if (columnIterator.hasNext()) {
                statement.append(",");
            }
            statement.append("\n");
        }

        statement.append(kco.format("from\n"));
        Iterator<DBDataset> datasetIterator = datasets.iterator();
        while (datasetIterator.hasNext()) {
            DBDataset dataset = datasetIterator.next();

            statement.append("    ");
            statement.append(oco.format(dataset.getQuotedName(false)));
            if (useAliases) {
                statement.append(" ");
                statement.append(aliases.getAlias(dataset));
            }
            if (datasetIterator.hasNext()) {
                statement.append(",\n");
            }
        }

        if (joinBundle != null && !joinBundle.isEmpty()) {
            statement.append(kco.format("\nwhere\n"));

            Iterator<DatasetJoin> joinIterator = joinBundle.getJoins().iterator();
            while (joinIterator.hasNext()) {
                DatasetJoin join = joinIterator.next();


                if (!join.isEmpty()) {
                    Map<DBColumn,DBColumn> mappings = join.getMappings();
                    Iterator<DBColumn> joinColumnIterator = mappings.keySet().iterator();
                    while (joinColumnIterator.hasNext()) {
                        DBColumn column1 = joinColumnIterator.next();
                        DBColumn column2 = mappings.get(column1);
                        statement.append("    ");
                        if (useAliases) {
                            statement.append(aliases.getAlias(column1.getDataset()));
                            statement.append(".");
                        }
                        statement.append(oco.format(column1.getQuotedName(false)));
                        statement.append(" = ");

                        if (useAliases) {
                            statement.append(aliases.getAlias(column2.getDataset()));
                            statement.append(".");
                        }
                        statement.append(oco.format(column2.getQuotedName(false)));
                        if (joinIterator.hasNext() || joinColumnIterator.hasNext()) {
                            statement.append(kco.format(" and\n"));
                        }
                    }
                }
            }
        }
        statement.append(";");
        return statement.toString();
    }


    private static final Comparator<DBDataset> DATASET_COMPARATOR = Comparator.comparing(DBObjectRef::of);
    private static final Comparator<DBColumn> COLUMN_COMPARATOR = Comparator.comparing(DBObjectRef::of);

}
