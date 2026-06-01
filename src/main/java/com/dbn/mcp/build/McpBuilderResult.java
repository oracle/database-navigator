package com.dbn.mcp.build;

import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;

@Getter
@Setter
public class McpBuilderResult {
    private String projectPath;
    private Path baseDirectory;
    private Path serverOutputDir;
    private Path configFile;
    private Path serverJar;

    private String configPath;
    private String walletPath;
    private String claudeSnippetJson;
    private String clineSnippetJson;
    private String mainClassContent;
}
