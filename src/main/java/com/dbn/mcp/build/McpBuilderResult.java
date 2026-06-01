package com.dbn.mcp.build;

import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;

@Getter
@Setter
public class McpBuilderResult {
    private Path baseDirectory;       // Project base directory used as the MCP build root.
    private Path sourceDirectory;     // Exported Maven source project under the output directory.
    private Path outputDirectory;     // Final MCP server distribution directory.
    private Path walletDirectory;     // Oracle wallet directory created for runtime credentials.
    private Path configFile;          // Generated mcp-config.yaml file in the output directory.
    private Path serverJar;           // Built server JAR in the output directory.

    private String claudeSnippetJson; // Claude Desktop MCP server configuration snippet.
    private String clineSnippetJson;  // Cline MCP server configuration snippet, when applicable.
    private String mainClassContent;  // Generated Java main class source used for the Maven build.
}
