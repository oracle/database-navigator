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

package com.dbn.mcp.deploy;

import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static com.dbn.nls.NlsResources.txt;

/**
 * Resolves the container runtime CLI. Docker and Podman accept identical arguments for the
 * commands used here (run / build / push), so either works without further special-casing -
 * only the executable differs.
 * <p>
 * The IDE process frequently does not inherit a login shell PATH, so a bare command lookup can
 * fail on machines where the runtime is installed and working; the common install locations are
 * probed first. Whether the daemon (or Podman machine) is actually up is left to the command
 * itself, whose own error is clearer than anything re-derived here.
 */
final class McpContainerRuntimeSupport {
    private static final @NonNls List<String> CLI_NAMES = List.of("docker", "podman");
    private static final @NonNls List<String> CLI_LOCATIONS = List.of(
            "/usr/local/bin",
            "/opt/homebrew/bin",
            "/opt/podman/bin",
            "/usr/bin");

    private McpContainerRuntimeSupport() {}

    static @NonNls String locateContainerRuntime() throws IOException {
        for (String cli : CLI_NAMES) {
            for (String location : CLI_LOCATIONS) {
                Path candidate = Paths.get(location, cli);
                if (isExecutable(candidate)) return candidate.toAbsolutePath().toString();
            }
        }
        for (String cli : CLI_NAMES) {
            if (isResolvableOnPath(cli)) return cli;
        }
        throw new IOException(txt("msg.mcp.exception.ContainerRuntimeNotFound"));
    }

    private static boolean isResolvableOnPath(@NonNls String cli) {
        String path = System.getenv("PATH");
        if (path == null) return false;

        for (String entry : path.split(File.pathSeparator)) {
            if (entry.isBlank()) continue;
            if (isExecutable(Paths.get(entry, cli))) return true;
        }
        return false;
    }

    private static boolean isExecutable(Path candidate) {
        return Files.isRegularFile(candidate) && Files.isExecutable(candidate);
    }
}
