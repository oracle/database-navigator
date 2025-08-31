package com.dbn.mcp;

import org.apache.maven.shared.invoker.*;
import java.util.function.Consumer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.EnumSet;
import java.util.Set;
import java.nio.file.attribute.PosixFilePermission;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MicroBuild {
    // give this another try : use intelij bundled maven instead of install one
  private static File guessIdeaBundledMaven() {
      try {
          String ideaHome = com.intellij.openapi.application.PathManager.getHomePath();
          if (ideaHome.isEmpty()) return null;
          String base = ideaHome;
          File dir = new File(base + "/plugins/maven/lib/maven3");
          File bin = new File(dir, "bin");
          File mvn = new File(bin, isWindows() ? "mvn.cmd" : "mvn");
          if (mvn.exists() && mvn.canRead()) return dir;
      } catch (Throwable ignore) { }
      return null;
  }

  private static boolean isMac() {
      String os = System.getProperty("os.name", "").toLowerCase();
      return os.contains("mac");
  }


  /** Overload that streams Maven output to a log consumer (e.g., to a ProgressIndicator). */
  public static Path buildWithMaven(Path outputDir,
                                    String mcpSdkCoord,
                                    String jdbcCoord,
                                    String javaSource,
                                    Path propertiesFile,
                                    Consumer<String> log)
          throws IOException, MavenInvocationException {

      String[] sdk  = mcpSdkCoord.split(":");   // groupId:artifactId:version
      String[] jdbc = jdbcCoord.split(":");

      // Decide a persistent project folder name next to the output JAR
      String mainClassSimple = detectMainClassName(javaSource);
      String packageName = detectPackageName(javaSource);
      String baseName = (mainClassSimple == null || mainClassSimple.isBlank()) ? "server" : mainClassSimple;
      String projectFolderName = "mcp-mvn-" + baseName;

      // Ensure unique folder if one already exists
      Path proj = outputDir.resolve(projectFolderName);
      int suffix = 1;
      while (Files.exists(proj)) {
          proj = outputDir.resolve(projectFolderName + "-" + suffix++);
      }

      // Create project structure
      Path src  = proj.resolve("src/main/java");
      Path res  = proj.resolve("src/main/resources");
      Files.createDirectories(src);
      Files.createDirectories(res);

      String mainClassFq = (packageName == null || packageName.isBlank()) ? baseName : packageName + "." + baseName;

      Path pkgDir = (packageName == null || packageName.isBlank()) ? src : src.resolve(packageName.replace('.', '/'));

      Files.createDirectories(pkgDir);
      Files.writeString(pkgDir.resolve(mainClassSimple + ".java"), javaSource);

      Files.copy(propertiesFile, res.resolve("mcp-config.properties"),
              StandardCopyOption.REPLACE_EXISTING);

      String pom = "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n"
              + "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
              + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 "
              + "                             http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
              + "  <modelVersion>4.0.0</modelVersion>\n"
              + "  <groupId>temp</groupId><artifactId>mcp-server</artifactId><version>1.0</version>\n"
              + "  <properties>\n"
              + "    <maven.compiler.source>17</maven.compiler.source>\n"
              + "    <maven.compiler.target>17</maven.compiler.target>\n"
              + "  </properties>\n"
              + "  <dependencies>\n"
              + "    <dependency><groupId>"+sdk[0]+"</groupId><artifactId>"+sdk[1]+"</artifactId><version>"+sdk[2]+"</version></dependency>\n"
              + "    <dependency><groupId>"+jdbc[0]+"</groupId><artifactId>"+jdbc[1]+"</artifactId><version>"+jdbc[2]+"</version></dependency>\n"
              + "  </dependencies>\n"
              + "  <build>\n"
              + "    <plugins>\n"
              + "      <plugin>\n"
              + "        <groupId>org.apache.maven.plugins</groupId>\n"
              + "        <artifactId>maven-shade-plugin</artifactId><version>3.5.0</version>\n"
              + "        <executions><execution><phase>package</phase><goals><goal>shade</goal></goals></execution></executions>\n"
              + "        <configuration>\n"
              + "          <transformers>\n"
              + "            <transformer implementation=\"org.apache.maven.plugins.shade.resource.ManifestResourceTransformer\">\n"
              + "              <mainClass>" + mainClassFq + "</mainClass>\n"
              + "            </transformer>\n"
              + "          </transformers>\n"
              + "        </configuration>\n"
              + "      </plugin>\n"
              + "    </plugins>\n"
              + "  </build>\n"
              + "</project>\n";
      Files.writeString(proj.resolve("pom.xml"), pom);

      InvocationRequest req = new DefaultInvocationRequest();
      req.setPomFile(proj.resolve("pom.xml").toFile());
      req.addArgs(List.of("clean", "package"));
      req.setBatchMode(true);

      org.apache.maven.shared.invoker.Invoker inv = new DefaultInvoker();
      File mavenHome = guessMavenHome();
      if (mavenHome != null) {
          inv.setMavenHome(mavenHome);
      } else {
          File mvnExec = guessMvnExecutable();
          if (mvnExec != null) {
              inv.setMavenExecutable(mvnExec);
          } else {
              Path mvnw = installMavenWrapper(proj);
              inv.setMavenExecutable(mvnw.toFile());
          }
      }

      if (log != null) {
          req.setOutputHandler(log::accept);
          req.setErrorHandler(log::accept);
      }

      InvocationResult resBuild = inv.execute(req);
      if (resBuild.getExitCode() != 0) throw new IllegalStateException("Maven build failed");

      Path jar = Files.list(proj.resolve("target"))
              .filter(p -> p.toString().endsWith(".jar"))
              .findFirst()
              .orElseThrow(() -> new IOException("Maven jar not found"));
      Files.createDirectories(outputDir);
      Path dest = outputDir.resolve(jar.getFileName());
      Files.copy(jar, dest, StandardCopyOption.REPLACE_EXISTING);
      return dest;
  }

  // --- Helper methods for Maven discovery and wrapper ---
  private static File guessMavenHome() {
      String[] candidates = new String[] {
              System.getProperty("maven.home"),
              System.getenv("MAVEN_HOME"),
              System.getenv("M2_HOME")
      };
      for (String c : candidates) {
          if (c == null || c.isBlank()) continue;
          File dir = new File(c);
          if (dir.isDirectory()) {
              File bin = new File(dir, "bin");
              File mvn = new File(bin, isWindows() ? "mvn.cmd" : "mvn");
              if (mvn.exists()) return dir;
          }
      }
      return null;
  }

  private static File guessMvnExecutable() {
      String prop = System.getProperty("mvn.executable");
      if (prop != null) {
          File f = new File(prop);
          if (f.canExecute()) return f;
      }
      String path = System.getenv("PATH");
      if (path != null) {
          for (String dir : path.split(java.io.File.pathSeparator)) {
              File f = new File(dir, isWindows() ? "mvn.cmd" : "mvn");
              if (f.canExecute()) return f;
          }
      }
      File[] candidates = new File[] {
              new File("/opt/homebrew/bin/mvn"),
              new File("/usr/local/bin/mvn")
      };
      for (File f : candidates) {
          if (f.canExecute()) return f;
      }
      return null;
  }

  private static boolean isWindows() {
      String os = System.getProperty("os.name", "");
      return os.toLowerCase().contains("win");
  }

  private static Path installMavenWrapper(Path projectDir) throws IOException {
      Path dotMvn = projectDir.resolve(".mvn");
      Path wrapperDir = dotMvn.resolve("wrapper");
      Files.createDirectories(wrapperDir);

      String properties = "distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip\n" +
              "wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar\n";
      Files.writeString(wrapperDir.resolve("maven-wrapper.properties"), properties);

      String mvnw = "" +
              "#!/bin/sh\n" +
              "set -e\n" +
              "BASE_DIR=\"$PWD\"\n" +
              "WRAPPER_JAR=\"$BASE_DIR/.mvn/wrapper/maven-wrapper.jar\"\n" +
              "PROPS=\"$BASE_DIR/.mvn/wrapper/maven-wrapper.properties\"\n" +
              "WRAPPER_URL=`sed -n 's/^wrapperUrl=//p' \"$PROPS\"`\n" +
              "if [ ! -f \"$WRAPPER_JAR\" ]; then\n" +
              "  echo \"Downloading Maven Wrapper from $WRAPPER_URL\"\n" +
              "  mkdir -p \"$BASE_DIR/.mvn/wrapper\"\n" +
              "  if command -v curl >/dev/null 2>&1; then\n" +
              "    curl -fsSL -o \"$WRAPPER_JAR\" \"$WRAPPER_URL\"\n" +
              "  elif command -v wget >/dev/null 2>&1; then\n" +
              "    wget -q -O \"$WRAPPER_JAR\" \"$WRAPPER_URL\"\n" +
              "  else\n" +
              "    echo \"Error: need curl or wget to download maven-wrapper.jar\"\n" +
              "    exit 1\n" +
              "  fi\n" +
              "fi\n" +
              "exec \"java\" -Dmaven.multiModuleProjectDirectory=\"$BASE_DIR\" -cp \"$WRAPPER_JAR\" org.apache.maven.wrapper.MavenWrapperMain \"$@\"\n";
      Path mvnwPath = projectDir.resolve("mvnw");
      Files.writeString(mvnwPath, mvnw);
      try {
          Set<PosixFilePermission> perms = EnumSet.of(
                  PosixFilePermission.OWNER_READ,
                  PosixFilePermission.OWNER_WRITE,
                  PosixFilePermission.OWNER_EXECUTE,
                  PosixFilePermission.GROUP_READ,
                  PosixFilePermission.OTHERS_READ
          );
          Files.setPosixFilePermissions(mvnwPath, perms);
      } catch (UnsupportedOperationException ignore) {
      }

      String mvnwCmd = "" +
              "@ECHO OFF\n" +
              "SETLOCAL ENABLEDELAYEDEXPANSION\n" +
              "SET BASE_DIR=%CD%\n" +
              "SET WRAPPER_JAR=%BASE_DIR%\\.mvn\\wrapper\\maven-wrapper.jar\n" +
              "SET PROPS=%BASE_DIR%\\.mvn\\wrapper\\maven-wrapper.properties\n" +
              "FOR /F \"usebackq tokens=1,2 delims==\" %%A IN (\"%PROPS%\") DO (\n" +
              "  IF \"%%A\"==\"wrapperUrl\" SET WRAPPER_URL=%%B\n" +
              ")\n" +
              "IF NOT EXIST \"%WRAPPER_JAR%\" (\n" +
              "  ECHO Downloading Maven Wrapper from %WRAPPER_URL%\n" +
              "  powershell -Command \"[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; (New-Object Net.WebClient).DownloadFile('%WRAPPER_URL%', '%WRAPPER_JAR%')\"\n" +
              ")\n" +
              "\"java\" -Dmaven.multiModuleProjectDirectory=\"%BASE_DIR%\" -cp \"%WRAPPER_JAR%\" org.apache.maven.wrapper.MavenWrapperMain %*\n";
      Files.writeString(projectDir.resolve("mvnw.cmd"), mvnwCmd);

      return isWindows() ? projectDir.resolve("mvnw.cmd") : mvnwPath;
  }

  private static String detectPackageName(String src) {
      Matcher m = Pattern.compile("\\bpackage\\s+([a-zA-Z_][a-zA-Z0-9_]*(?:\\.[a-zA-Z_][a-zA-Z0-9_]*)*)\\s*;").matcher(src);
      return m.find() ? m.group(1) : null;
  }

  private static String detectMainClassName(String src) {
      // Prefer a public class if present, otherwise take the first class
      Matcher pub = Pattern.compile("\\bpublic\\s+class\\s+([A-Za-z_][A-Za-z0-9_]*)").matcher(src);
      if (pub.find()) return pub.group(1);
      Matcher any = Pattern.compile("\\bclass\\s+([A-Za-z_][A-Za-z0-9_]*)").matcher(src);
      if (any.find()) return any.group(1);
      // Fallback to the name we historically used
      return "GeneratedMcpServer";
  }
}