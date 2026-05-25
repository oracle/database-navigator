package com.dbn.mcp.build;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.nio.file.Path;

@Getter
@AllArgsConstructor
public class McpBuildConfig {
    private final Path dir;
    private final Path file;
    private final String serverName;
}
