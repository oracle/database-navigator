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

package com.dbn.object.dependency.ui;

import com.dbn.common.dispose.AlreadyDisposedException;
import com.dbn.common.dispose.Disposed;
import com.dbn.common.dispose.StatefulDisposable;
import com.dbn.common.dispose.StatefulDisposableBase;
import com.dbn.common.dispose.UnlistedDisposable;
import com.dbn.common.thread.Background;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.dependency.ObjectDependencyType;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.dispose.Disposer.replace;
import static java.util.Collections.emptyList;

public class ObjectDependencyTreeNode extends StatefulDisposableBase implements StatefulDisposable, UnlistedDisposable {
    private final DBObjectRef<DBObject> object;

    private List<ObjectDependencyTreeNode> dependencies;
    private ObjectDependencyTreeModel model;
    private ObjectDependencyTreeNode parent;
    private boolean shouldLoad = true;
    private boolean loading = false;
    private static int loaderCount = 0;

    private ObjectDependencyTreeNode(ObjectDependencyTreeNode parent, DBObject object) {
        this.parent = parent;
        this.object = DBObjectRef.of(object);
    }

    ObjectDependencyTreeNode(ObjectDependencyTreeModel model, DBObject object) {
        this.model = model;
        this.object = DBObjectRef.of(object);
    }

    @Nullable
    DBObject getObject() {
        return DBObjectRef.get(object);
    }

    Project getProject() {
        DBObject object = getObject();
        return object == null ? null : object.getProject();
    }

    public ObjectDependencyTreeModel getModel() {
        if (model == null && parent == null) {
            throw new AlreadyDisposedException(this);
        }
        return model == null ? getParent().getModel() : model;
    }

    public ObjectDependencyTreeNode getParent() {
        return parent;
    }

    public synchronized List<ObjectDependencyTreeNode> getChildren(boolean load) {
        ObjectDependencyTreeModel model = getModel();
        if (object == null || model == null) return emptyList();

        if (dependencies == null && load) {
            DBObject object = getObject();
            if (isDisposed() || object == null || isRecursive(object)) {
                dependencies = emptyList();
                shouldLoad = false;
            } else {
                dependencies = new ArrayList<>();
                if (getTreePath().length < 2) {
                    ObjectDependencyTreeNode loadInProgressNode = new ObjectDependencyTreeNode(this, null);
                    dependencies.add(loadInProgressNode);
                    model.getTree().registerLoadInProgressNode(loadInProgressNode);
                }
            }
        }

        if (load && shouldLoad) {
            loading = true;

            if (loaderCount < 10) {
                shouldLoad = false;
                loaderCount++;
                Background.run(() -> {
                    try {
                        DBObject object = getObject();
                        if (object instanceof DBSchemaObject) {
                            List<ObjectDependencyTreeNode> newDependencies = new ArrayList<>();
                            DBSchemaObject schemaObject = (DBSchemaObject) object;
                            List<DBObject> dependentObjects = loadDependencies(schemaObject);

                            if (dependentObjects != null) {
                                for (DBObject dependentObject : dependentObjects) {
                                        /*if (dependentObject instanceof DBSchemaObject) {
                                            loadDependencies((DBSchemaObject) dependentObject);
                                        }*/
                                    ObjectDependencyTreeNode node = new ObjectDependencyTreeNode(ObjectDependencyTreeNode.this, dependentObject);
                                    newDependencies.add(node);
                                }
                            }

                            dependencies = replace(dependencies, newDependencies);
                            getModel().notifyNodeLoaded(ObjectDependencyTreeNode.this);
                        }
                    } finally {
                        loading = false;
                        loaderCount--;
                    }
                });
            }
        }
        return dependencies;
    }

    public boolean isLoading() {
        return loading;
    }

    @Nullable
    private List<DBObject> loadDependencies(DBSchemaObject schemaObject) {
        ObjectDependencyType dependencyType = getModel().getDependencyType();
        return
            dependencyType == ObjectDependencyType.INCOMING ? schemaObject.getReferencedObjects() :
            dependencyType == ObjectDependencyType.OUTGOING ? schemaObject.getReferencingObjects() : null;
    }

    private boolean isRecursive(DBObject object) {
        if (object == null) return false;

        ObjectDependencyTreeNode parent = getParent();
        while (parent != null) {
            if (object.equals(parent.getObject())) {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }

    public ObjectDependencyTreeNode[] getTreePath() {
        List<ObjectDependencyTreeNode> path = new ArrayList<>();
        path.add(this);
        ObjectDependencyTreeNode parent = getParent();
        while (parent != null) {
            path.add(0, parent);
            parent = parent.getParent();
        }
        return path.toArray(new ObjectDependencyTreeNode[0]);
    }

    @Override
    public void disposeInner() {
        dependencies = replace(dependencies, Disposed.list());
        nullify();
    }
}
