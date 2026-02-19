package com.dbn.mcp.build;

import com.dbn.common.template.TemplateUtilities;
import com.intellij.openapi.project.Project;
import com.intellij.credentialStore.Credentials;
import com.intellij.util.net.ProxyConfiguration;
import com.intellij.util.net.ProxyCredentialStore;
import com.intellij.util.net.ProxySettings;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public final class McpMavenBuild {
    private static final String POM_TEMPLATE = "DBN - MCP Server POM.xml";
    private static final Pattern PKG = Pattern.compile("\\bpackage\\s+([a-zA-Z_][a-zA-Z0-9_]*(?:\\.[a-zA-Z_][a-zA-Z0-9_]*)*)\\s*;");
    private static final Pattern PUB_CLASS = Pattern.compile("\\bpublic\\s+class\\s+([A-Za-z_][A-Za-z0-9_]*)");
    private static final Pattern ANY_CLASS = Pattern.compile("\\bclass\\s+([A-Za-z_][A-Za-z0-9_]*)");

    private McpMavenBuild() {}

    public static Path buildWithMaven(Project project, Path outDir, String serverName, String sdkCoord, String jdbcCoord, String java, Path props, boolean allowWrapper, Consumer<String> outputHandler)
            throws IOException, MavenInvocationException {
        Coord sdk = Coord.parse(sdkCoord);
        Coord jdbc = Coord.parse(jdbcCoord);
        Info info = analyze(java);

        Path projDir = uniqueDir(outDir, info.className);
        setupProject(projDir, java, props, info);
        writePom(project, projDir, serverName, sdk, jdbc, info.fqn);
        runMaven(projDir, allowWrapper, outputHandler);

        return copyJar(projDir, outDir);
    }

    private static Info analyze(String src) {
        String pkg = find(PKG, src);
        String cls = find(PUB_CLASS, src);
        if (cls == null) cls = find(ANY_CLASS, src);
        if (cls == null) cls = "GeneratedMcpServer";
        return new Info(pkg, cls, pkg != null ? pkg + "." + cls : cls);
    }

    private static String find(Pattern p, String src) {
        Matcher m = p.matcher(src);
        return m.find() ? m.group(1) : null;
    }

    private static Path uniqueDir(Path base, String name) throws IOException {
        Path dir = base.resolve("mcp-mvn-" + name);
        int i = 1;
        while (Files.exists(dir)) dir = base.resolve("mcp-mvn-" + name + "-" + i++);
        return dir;
    }

    private static void setupProject(Path dir, String java, Path props, Info info) throws IOException {
        Path src = dir.resolve("src/main/java");
        Path res = dir.resolve("src/main/resources");
        Files.createDirectories(src);
        Files.createDirectories(res);

        Path pkg = info.pkg != null ? src.resolve(info.pkg.replace('.', '/')) : src;
        Files.createDirectories(pkg);
        Files.writeString(pkg.resolve(info.className + ".java"), java);
        Files.copy(props, res.resolve("mcp-config.yaml"), StandardCopyOption.REPLACE_EXISTING);
    }

    private static void writePom(Project project, Path dir, String serverName, Coord sdk, Coord jdbc, String main) throws IOException {
        Properties p = new Properties();
        p.setProperty("SERVER_NAME", serverName);
        p.setProperty("MCP_SDK_GROUP_ID", sdk.g);
        p.setProperty("MCP_SDK_ARTIFACT_ID", sdk.a);
        p.setProperty("MCP_SDK_VERSION", sdk.v);
        p.setProperty("JDBC_GROUP_ID", jdbc.g);
        p.setProperty("JDBC_ARTIFACT_ID", jdbc.a);
        p.setProperty("JDBC_VERSION", jdbc.v);
        p.setProperty("MAIN_CLASS_FQ", main);
        Files.writeString(dir.resolve("pom.xml"), TemplateUtilities.generateCode(project, POM_TEMPLATE, p));
    }

    private static void runMaven(Path dir, boolean allowWrapper, Consumer<String> outputHandler) throws MavenInvocationException, IOException {
        InvocationRequest req = new DefaultInvocationRequest();
        req.setPomFile(dir.resolve("pom.xml").toFile());
        req.addArgs(List.of("clean", "package"));
        req.setBatchMode(true);
        if (outputHandler != null) { req.setOutputHandler(outputHandler::accept); req.setErrorHandler(outputHandler::accept); }

        Properties mavenProps = getIdeProxyProperties();
        if (!mavenProps.isEmpty()) {
            req.setProperties(mavenProps);
        }

        Invoker inv = new DefaultInvoker();
        configureMaven(inv, dir, allowWrapper);

        log.info("POM: {}", dir.resolve("pom.xml"));
        log.info("Maven home: {}", inv.getMavenHome());
        log.info("Maven executable: {}", inv.getMavenExecutable());
        log.info("Working dir: {}", dir);

        try {
            InvocationResult res = inv.execute(req);
            log.info("Maven exit code: {}", res.getExitCode());
            if (res.getExecutionException() != null) {
                log.error("Maven execution exception", res.getExecutionException());
            }
            if (res.getExitCode() != 0) throw new IllegalStateException("Maven failed: " + res.getExitCode());
        } catch (MavenInvocationException e) {
            log.error("MavenInvocationException", e);
            throw e;
        }
    }

    public static boolean isMavenAvailable() {
        return findMavenHome() != null || findMavenExe() != null;
    }

    private static void configureMaven(Invoker inv, Path dir, boolean allowWrapper) throws IOException {
        File home = findMavenHome();
        if (home != null) {
            ensureExecutable(new File(home, "bin/" + (win() ? "mvn.cmd" : "mvn")));
            inv.setMavenHome(home);
            return;
        }
        File exe = findMavenExe();
        if (exe != null) { inv.setMavenExecutable(exe); return; }
        if (!allowWrapper) throw new IOException("Maven not found");
        inv.setMavenExecutable(installWrapper(dir).toFile());
    }

    private static void ensureExecutable(File file) {
        if (file.exists() && !file.canExecute()) {
            log.info("Setting execute permission on: {}", file);
            boolean ok = file.setExecutable(true);
            if (!ok) log.warn("Failed to set execute permission on: {}", file);
        }
    }

    private static Path copyJar(Path proj, Path out) throws IOException {
        Path jar;
        try (var stream = Files.list(proj.resolve("target"))) {
            jar = stream.filter(p -> p.toString().endsWith(".jar") && !p.toString().contains("original"))
                    .findFirst().orElseThrow(() -> new IOException("JAR not found"));
        }
        Files.createDirectories(out);
        Path dest = out.resolve(jar.getFileName());
        Files.copy(jar, dest, StandardCopyOption.REPLACE_EXISTING);
        return dest;
    }

    private static File findMavenHome() {
        // MAVEN_HOME env var (set by users who installed Maven manually)
        String mavenHome = System.getenv("MAVEN_HOME");
        if (mavenHome != null && !mavenHome.isBlank()) {
            File d = new File(mavenHome);
            if (validHome(d)) return d;
        }
        // IntelliJ bundles Maven inside its Maven plugin
        try {
            File d = new File(com.intellij.openapi.application.PathManager.getHomePath() + "/plugins/maven/lib/maven3");
            if (validHome(d)) return d;
        } catch (Throwable ignored) {}
        return null;
    }

    private static boolean validHome(File d) {
        return d.isDirectory() && new File(d, "bin/" + (win() ? "mvn.cmd" : "mvn")).exists();
    }



    private static File findMavenExe() {
        String path = System.getenv("PATH");
        if (path != null) {
            for (String dir : path.split(File.pathSeparator)) {
                File f = new File(dir, win() ? "mvn.cmd" : "mvn");
                if (f.canExecute()) return f;
            }
        }
        return null;
    }

    private static Path installWrapper(Path dir) throws IOException {
        Path wrap = dir.resolve(".mvn/wrapper");
        Files.createDirectories(wrap);
        Files.writeString(wrap.resolve("maven-wrapper.properties"),
                "distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip\n" +
                "wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar\n");

        Path unix = dir.resolve("mvnw");
        Files.writeString(unix,
                "#!/bin/sh\nset -e\nBASE=\"$PWD\"\nJAR=\"$BASE/.mvn/wrapper/maven-wrapper.jar\"\n" +
                "PROPS=\"$BASE/.mvn/wrapper/maven-wrapper.properties\"\nURL=`sed -n 's/^wrapperUrl=//p' \"$PROPS\"`\n" +
                "[ -f \"$JAR\" ] || { curl --max-time 60 -fsSL -o \"$JAR\" \"$URL\" 2>/dev/null || wget --timeout=60 -q -O \"$JAR\" \"$URL\"; }\n" +
                "exec java -Dmaven.multiModuleProjectDirectory=\"$BASE\" -cp \"$JAR\" org.apache.maven.wrapper.MavenWrapperMain \"$@\"\n");
        try { Files.setPosixFilePermissions(unix, EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE)); }
        catch (UnsupportedOperationException ignored) {}

        Files.writeString(dir.resolve("mvnw.cmd"),
                "@ECHO OFF\nSETLOCAL\nSET BASE=%CD%\nSET JAR=%BASE%\\.mvn\\wrapper\\maven-wrapper.jar\n" +
                "SET PROPS=%BASE%\\.mvn\\wrapper\\maven-wrapper.properties\n" +
                "FOR /F \"tokens=1,2 delims==\" %%A IN (%PROPS%) DO IF \"%%A\"==\"wrapperUrl\" SET URL=%%B\n" +
                "IF NOT EXIST \"%JAR%\" powershell -Command \"(New-Object Net.WebClient).DownloadFile('%URL%','%JAR%')\"\n" +
                "java -Dmaven.multiModuleProjectDirectory=\"%BASE%\" -cp \"%JAR%\" org.apache.maven.wrapper.MavenWrapperMain %*\n");

        return win() ? dir.resolve("mvnw.cmd") : unix;
    }

    private static boolean win() { return System.getProperty("os.name", "").toLowerCase().contains("win"); }

    private static Properties getIdeProxyProperties() {
        Properties props = new Properties();
        try {
            ProxyConfiguration config = ProxySettings.getInstance().getProxyConfiguration();
            if (!(config instanceof ProxyConfiguration.StaticProxyConfiguration)) return props;

            ProxyConfiguration.StaticProxyConfiguration proxy = (ProxyConfiguration.StaticProxyConfiguration) config;
            String host = proxy.getHost();
            if (host == null || host.isEmpty()) return props;

            int port = proxy.getPort();
            if (proxy.getProtocol() == ProxyConfiguration.ProxyProtocol.SOCKS) {
                props.setProperty("socksProxyHost", host);
                props.setProperty("socksProxyPort", String.valueOf(port));
            } else {
                props.setProperty("http.proxyHost", host);
                props.setProperty("http.proxyPort", String.valueOf(port));
                props.setProperty("https.proxyHost", host);
                props.setProperty("https.proxyPort", String.valueOf(port));
            }

            Credentials credentials = ProxyCredentialStore.getInstance().getCredentials(host, port);
            if (credentials != null) {
                String login = credentials.getUserName();
                if (login != null) {
                    props.setProperty("http.proxyUser", login);
                    props.setProperty("https.proxyUser", login);
                }
                String pwd = credentials.getPasswordAsString();
                if (pwd != null) {
                    props.setProperty("http.proxyPassword", pwd);
                    props.setProperty("https.proxyPassword", pwd);
                }
            }
        } catch (Exception e) {
            log.warn("Could not read IDE proxy settings", e);
        }
        return props;
    }

    private static class Coord {
        final String g, a, v;
        Coord(String g, String a, String v) { this.g = g; this.a = a; this.v = v; }
        static Coord parse(String s) { String[] p = s.split(":"); return new Coord(p[0], p[1], p[2]); }
    }

    private static class Info {
        final String pkg, className, fqn;
        Info(String pkg, String className, String fqn) { this.pkg = pkg; this.className = className; this.fqn = fqn; }
    }
}
