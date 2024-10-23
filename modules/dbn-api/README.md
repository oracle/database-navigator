# Database Navigator - API Library
API exposing DBN services to be integrated in IntelliJ plugins that want to extend DBN functionality or inherit DBN features  

### Optional Dependency Definition
It is assumed that your plugin can act as a standalone plugin, but also as an extension to Database Navigator plugin.


For adding DBN as optional dependency to your plugin, following steps must be taken
1. add `dbn-api.jar` dependency in your project
1. create dependency config xml next to your `plugin.xml` file
1. add optional dependency entry in your `plugin.xml`

#### 1. Library dependency
(work in progress)\
The api library is currently not published in any repository.\
Temporary workaround: copy the library jar in the project resources and add it as local dependency in gradle build configuration  


#### 2. Create dependency config file
Create a dependency definition xml in the same location as `plugin.xml` e.g. called `dbn-api-support.xml`

<u>XML dependency definition</u>
```xml
<idea-plugin>
    <extensions defaultExtensionNs="com.intellij">
        <!--sample project service with dependency on DBN api-->
        <projectService serviceImplementation="com.myplugin.dbn.extension.SampleProjectService"/>
    </extensions>
</idea-plugin>
```

#### 3. Add dependency in plugin config
Inside your `plugin.xml` file, add optional dependency definition for `dbn-api-support.xml` next to the other `<depends .. ` entries

<u>plugin.xml dependency entry</u>
```xml
<idea-plugin>
    ...
    <depends>com.intellij.modules.platform</depends>
    <depends optional="true" config-file="dbn-api-support.xml">com.dbn.api</depends>
    ...
</idea-plugin>
```

### Usage of Dependency API
Sample usage of [DatabaseService.java](src/main/java/com/dbn/api/database/DatabaseService.java) project service

<u>Sample java class</u>
```java
package com.myplugin.project.service;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import com.dbn.api.database.Database;
import com.dbn.api.database.DatabaseService;

public class SampleDatabaseServiceProxy {
    private Project project;
    
    public SampleDatabaseServiceProxy(@NotNull Project project) {
        this.project = project;
    }

    public static SampleDatabaseServiceProxy getInstance(@NotNull Project project) {
        return project.getService(SampleDatabaseServiceProxy.class);
    }

    public List<Database> getDatabases() {
        DatabaseService databaseService = project.getService(DatabaseService.class);
        return databaseService.getDatabases();
    }
}
```
<u>Sample XML dependency</u>
```xml
<!--dbn-api-support.xml-->
<idea-plugin>
    <extensions defaultExtensionNs="com.intellij">
        <projectService serviceImplementation="com.myplugin.project.service.SampleDatabaseServiceProxy"/>
    </extensions>
</idea-plugin>
```