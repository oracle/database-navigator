/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.mcp.registry;

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.latent.Latent;
import com.dbn.common.thread.Background;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.window.ToolWindows;
import com.dbn.connection.ConnectionId;
import com.dbn.mcp.build.McpBuilderResult;
import com.dbn.mcp.build.McpDistPaths;
import com.dbn.mcp.deploy.McpDeploymentStep;
import com.dbn.mcp.model.McpServerDefinition;
import com.dbn.mcp.ui.McpServersForm;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static com.dbn.common.component.Components.projectService;
import static com.dbn.common.options.setting.Settings.childrenOf;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.newStateElement;
import static com.dbn.common.util.Modality.nonModal;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@Slf4j
@State(
        name = McpServerRegistry.COMPONENT_NAME,
        storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class McpServerRegistry extends ProjectComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Project.McpServerRegistry";
    public static final String TOOL_WINDOW_ID = "DB MCP Servers";

    private final Map<String, McpServerRecord> records = new ConcurrentHashMap<>();
    @Getter
    private final McpBuildLogs buildLogs;
    private final Latent<McpServersForm> dashboardForm = Latent.basic(() ->
            Dispatch.call(true, () -> new McpServersForm(getProject())));

    public McpServerRegistry(@NotNull Project project) {
        super(project, COMPONENT_NAME);
        this.buildLogs = new McpBuildLogs(project);
    }

    /**
     * Registers a server as building before its output exists, so the dashboard can show the
     * build as it runs. Re-running a build keeps the previous artifact details until it succeeds.
     */
    public @NotNull McpServerRecord registerBuildStarted(
            @NotNull ConnectionId connectionId,
            @NotNull McpServerDefinition definition,
            @NotNull Path outputDirectory) {
        String key = normalizePath(outputDirectory.toAbsolutePath().toString());
        buildLogs.clear(key);

        McpServerRecord record = records.get(key);
        boolean existing = record != null;
        if (!existing) {
            record = new McpServerRecord();
            record.setOutputDirectory(key);
            record.setConnectionId(connectionId);
            record.setArtifactType(McpArtifactType.JAR);
        }
        record.setDefinition(definition.clone());
        record.setStatus(McpServerStatus.BUILDING);

        McpServerRecord building = record;
        records.put(key, building);
        ProjectEvents.notify(getProject(), McpServerRegistryListener.TOPIC, listener -> {
            if (existing) {
                listener.recordUpdated(building);
            } else {
                listener.recordAdded(building);
            }
        });
        return building;
    }

    public static McpServerRegistry getInstance(@NotNull Project project) {
        return projectService(project, McpServerRegistry.class);
    }

    public @NotNull List<McpServerRecord> getRecords() {
        List<McpServerRecord> result = new ArrayList<>(records.values());
        result.sort(Comparator
                .comparing(McpServerRecord::getServerName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(McpServerRecord::getOutputDirectory));
        return result;
    }

    public @Nullable McpServerRecord getRecord(@NotNull String outputDirectory) {
        return records.get(normalizePath(outputDirectory));
    }

    public @NotNull McpServerRecord registerBuild(
            @NotNull ConnectionId connectionId,
            @NotNull McpServerDefinition definition,
            @NotNull McpBuilderResult result,
            long buildDuration) {
        Path outputDirectory = result.getOutputDirectory().toAbsolutePath().normalize();

        McpServerRecord record = new McpServerRecord();
        record.setOutputDirectory(outputDirectory.toString());
        record.setDefinition(definition.clone());
        record.setConnectionId(connectionId);
        record.setBuildTimestamp(System.currentTimeMillis());
        record.setBuildDuration(buildDuration);
        record.setStatus(McpServerStatus.BUILT);
        initArtifact(record, result, outputDirectory);

        writeManifest(record);
        McpServerRecord previous = records.put(record.getOutputDirectory(), record);
        ProjectEvents.notify(getProject(), McpServerRegistryListener.TOPIC, listener -> {
            if (previous == null) {
                listener.recordAdded(record);
            } else {
                listener.recordUpdated(record);
            }
        });
        return record;
    }

    /** Persists a successful Graal deployment and refreshes every dashboard view of the server. */
    public void applyDeployment(
            @NotNull String outputDirectory,
            @NotNull String applicationName,
            @NotNull String imageOcid,
            @NotNull String endpoint) {
        McpServerRecord record = getRecord(outputDirectory);
        if (record == null) return;

        // merged, not replaced: the earlier steps recorded their own progress here
        McpDeploymentInfo deployment = ensureDeployment(record);
        deployment.setApplicationName(applicationName);
        deployment.setImageOcid(imageOcid);
        deployment.setEndpoint(endpoint);
        deployment.setDeployTimestamp(System.currentTimeMillis());

        record.setStatus(McpServerStatus.DEPLOYED);
        writeManifest(record);
        ProjectEvents.notify(getProject(), McpServerRegistryListener.TOPIC,
                listener -> listener.recordUpdated(record));
    }

    /**
     * Records that a deployment step succeeded, along with the registry coordinates it used, so a
     * later step - or a retry after a failure - resumes from what has already been achieved.
     */
    public void applyDeploymentStep(
            @NotNull String outputDirectory,
            @NotNull McpDeploymentStep step,
            @NotNull String regionKey,
            @NotNull String namespace,
            @NotNull String repository,
            @NotNull String tag) {
        McpServerRecord record = getRecord(outputDirectory);
        if (record == null) return;

        McpDeploymentInfo deployment = ensureDeployment(record);
        deployment.setRegionKey(regionKey);
        deployment.setNamespace(namespace);
        deployment.setRepository(repository);
        deployment.setTag(tag);

        long now = System.currentTimeMillis();
        switch (step) {
            case BUILD_IMAGE -> {
                deployment.setImageBuiltAt(now);
                // a rebuilt image is no longer the pushed one
                deployment.setImagePushedAt(0);
            }
            case PUSH_IMAGE -> deployment.setImagePushedAt(now);
            case CREATE_APPLICATION -> { /* recorded by applyDeployment once it is active */ }
        }

        writeManifest(record);
        ProjectEvents.notify(getProject(), McpServerRegistryListener.TOPIC,
                listener -> listener.recordUpdated(record));
    }

    private static McpDeploymentInfo ensureDeployment(McpServerRecord record) {
        McpDeploymentInfo deployment = record.getDeployment();
        if (deployment == null) {
            deployment = new McpDeploymentInfo();
            record.setDeployment(deployment);
        }
        return deployment;
    }

    /**
     * Records a build that produced no artifact, so its output remains reachable from the
     * dashboard - a failed build is exactly when a user needs to read it. An existing record is
     * only marked failed, keeping the artifact details of the last successful build.
     */
    public void registerFailedBuild(
            @NotNull ConnectionId connectionId,
            @NotNull McpServerDefinition definition,
            @NotNull Path outputDirectory,
            long buildDuration) {
        String key = normalizePath(outputDirectory.toAbsolutePath().toString());

        McpServerRecord record = records.get(key);
        boolean existing = record != null;
        if (!existing) {
            record = new McpServerRecord();
            record.setOutputDirectory(key);
            record.setDefinition(definition.clone());
            record.setConnectionId(connectionId);
            record.setArtifactType(McpArtifactType.JAR);
        }
        record.setStatus(McpServerStatus.FAILED);
        record.setBuildTimestamp(System.currentTimeMillis());
        record.setBuildDuration(buildDuration);

        McpServerRecord failed = record;
        writeManifest(failed);
        records.put(key, failed);
        ProjectEvents.notify(getProject(), McpServerRegistryListener.TOPIC, listener -> {
            if (existing) {
                listener.recordUpdated(failed);
            } else {
                listener.recordAdded(failed);
            }
        });
    }

    /**
     * The manifest is a redundancy convenience (folder portability and rediscovery) - failing to
     * write it must never fail an otherwise successful build, so it is logged and skipped.
     */
    private static void writeManifest(McpServerRecord record) {
        try {
            McpServerManifest.write(record);
        } catch (IOException e) {
            conditionallyLog(e);
            log.warn("Could not write MCP server manifest for {}: {}",
                    record.getOutputDirectory(), e.getMessage());
        }
    }

    public void removeRecord(@NotNull McpServerRecord record, boolean deleteFiles) {
        if (deleteFiles) {
            Background.run(() -> {
                deleteFiles(record.getOutputPath());
                removeEntry(record);
            });
        } else {
            removeEntry(record);
        }
    }

    private void removeEntry(McpServerRecord record) {
        McpServerRecord removed = records.remove(record.getOutputDirectory());
        if (removed != null) {
            ProjectEvents.notify(getProject(), McpServerRegistryListener.TOPIC,
                    listener -> listener.recordRemoved(removed));
        }
    }

    public void reconcile() {
        List<McpServerRecord> discovered = McpServerDiscovery.scan(
                getProject(), McpDistPaths.distRoot(getProject()), getRecords());
        records.clear();
        for (McpServerRecord record : discovered) {
            records.put(record.getOutputDirectory(), record);
        }
        ProjectEvents.notify(getProject(), McpServerRegistryListener.TOPIC,
                McpServerRegistryListener::registryReloaded);
    }

    public void showDashboard(@Nullable String selectKey) {
        Dispatch.run(nonModal(), () -> {
            ToolWindow toolWindow = ToolWindows.getToolWindow(getProject(), TOOL_WINDOW_ID);
            if (toolWindow == null) return;

            McpServersForm form = dashboardForm.get();
            ContentManager contentManager = toolWindow.getContentManager();
            if (contentManager.getContentCount() == 0) {
                ContentFactory contentFactory = contentManager.getFactory();
                Content content = contentFactory.createContent(form.getComponent(), null, false);
                Disposer.register(content, form);
                contentManager.addContent(content);
                toolWindow.setAvailable(true, null);
            }

            if (selectKey != null) form.selectRecord(selectKey);
            toolWindow.show(null);
        });
    }

    private static void initArtifact(McpServerRecord record, McpBuilderResult result, Path outputDirectory) {
        if (record.getImplementation().isContainer()) {
            record.setArtifactType(McpArtifactType.IMAGE);
            record.setImageName(result.getImageName());
            return;
        }

        record.setArtifactType(record.getImplementation().isNative() ?
                McpArtifactType.EXECUTABLE : McpArtifactType.JAR);
        Path artifact = result.getServerJar();
        if (artifact != null) {
            String relativePath = outputDirectory.relativize(artifact.toAbsolutePath().normalize()).toString();
            record.setArtifactPath(relativePath.replace('\\', '/'));
        }
    }

    private static void deleteFiles(Path outputDirectory) throws IOException {
        if (!Files.exists(outputDirectory)) return;

        try (Stream<Path> paths = Files.walk(outputDirectory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    /*********************************************
     *            PersistentStateComponent       *
     *********************************************/
    @Override
    public @Nullable Element getComponentState() {
        Element state = newStateElement();
        Element servers = newElement(state, "mcp-servers");
        for (McpServerRecord record : getRecords()) {
            record.writeState(newElement(servers, "mcp-server"));
        }
        return state;
    }

    @Override
    public void loadComponentState(@NotNull Element state) {
        records.clear();
        Element servers = state.getChild("mcp-servers");
        for (Element serverElement : childrenOf(servers, "mcp-server")) {
            McpServerRecord record = new McpServerRecord();
            record.readState(serverElement);
            String outputDirectory = record.getOutputDirectory();
            if (outputDirectory != null) records.put(outputDirectory, record);
        }
    }

    private static String normalizePath(String path) {
        return Path.of(path).toAbsolutePath().normalize().toString();
    }
}
