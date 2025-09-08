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

import com.github.jk1.license.filter.DependencyFilter
import com.github.jk1.license.filter.LicenseBundleNormalizer
import com.github.jk1.license.render.ReportRenderer
import com.github.jk1.license.render.TextReportRenderer

// import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm") version "1.9.20"
  id("org.jetbrains.intellij") version "1.16.0"
  id("com.github.jk1.dependency-license-report") version "2.9"
}

group = "com.dbn"
version = "3.6.1.0"

repositories {
  mavenCentral {
    content {
      excludeModule("com.oracle", "oci-intellij-plugin-api")
    }
  }
  flatDir {
    dirs("libs")
    content {
      includeModule("com.oracle", "oci-intellij-plugin-api")
    }
  }
}

dependencies {
  testImplementation("junit:junit:4.13.2")

  annotationProcessor("org.projectlombok:lombok:1.18.34")
  testAnnotationProcessor("org.projectlombok:lombok:1.18.34")

  implementation("org.projectlombok:lombok:1.18.34")

  // poi libraries (xls export)
  implementation("org.apache.poi:poi:5.4.1")
  implementation("org.apache.poi:poi-ooxml:5.4.1")
  implementation("org.apache.poi:poi-ooxml-lite:5.4.1")

  // poi library dependencies
  implementation("commons-io:commons-io:2.17.0")
  implementation("org.apache.commons:commons-compress:1.27.1")
  implementation("org.apache.commons:commons-collections4:4.4")
  implementation("org.apache.commons:commons-lang3:3.18.0")
  implementation("org.apache.logging.log4j:log4j-api:2.24.1")
  implementation("org.apache.xmlbeans:xmlbeans:5.2.1")

  // ssh tunnel libraries
  implementation("org.apache.sshd:sshd-common:2.13.2")
  implementation("org.apache.sshd:sshd-core:2.13.2")

  // driver download libraries
  implementation("org.apache.maven.resolver:maven-resolver-connector-basic:1.9.22")
  implementation("org.apache.maven:maven-resolver-provider:3.9.9")

  // db assistant
  implementation("dev.langchain4j:langchain4j:1.4.0")
  implementation("dev.langchain4j:langchain4j-core:1.4.0")
  implementation("dev.langchain4j:langchain4j-http-client:1.4.0")
  implementation("dev.langchain4j:langchain4j-mcp:1.4.0-beta10")

  implementation("dev.langchain4j:langchain4j-open-ai:1.4.0")
  implementation("dev.langchain4j:langchain4j-google-ai-gemini:1.4.0")
  implementation("dev.langchain4j:langchain4j-anthropic:1.4.0")
  implementation("dev.langchain4j:langchain4j-cohere:1.4.0-beta10")
  implementation("dev.langchain4j:langchain4j-hugging-face:1.4.0-beta10")
  implementation("dev.langchain4j:langchain4j-ollama:1.4.0")
  implementation("dev.langchain4j:langchain4j-bedrock:1.4.0")
  implementation("dev.langchain4j:langchain4j-mistral-ai:1.4.0")

  implementation("com.fasterxml.jackson.core:jackson-core:2.20.0")
  implementation("com.fasterxml.jackson.core:jackson-databind:2.20.0")
  implementation("com.fasterxml.jackson.core:jackson-annotations:2.20.0")
  implementation("com.fasterxml.jackson.module:jackson-modules-base:2.20.0")
  implementation("com.fasterxml.jackson.module:jackson-modules-java8:2.20.0")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.20.0")

  implementation(project(":modules:dbn-api"))
  implementation(project(":modules:dbn-spi"))

  compileOnly("com.oracle:oci-intellij-plugin-api:"+project.properties["oci.ext.api.version"])
}

licenseReport {
    renderers = arrayOf<ReportRenderer>(TextReportRenderer("THIRD_PARTY_LICENSES.txt"))
    filters = arrayOf<DependencyFilter>(LicenseBundleNormalizer())
}

sourceSets{
  main {
    resources {
      srcDir("src/main/java")
      include("**/*.xml")
    }
    resources {
      include(
              "**/*.ft",
              "**/*.png",
              "**/*.jpg",
              "**/*.txt",
              "**/*.xml",
              "**/*.svg",
              "**/*.css",
              "**/*.html",
              "**/*.template",
              "**/*.properties")
    }
  }
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
  version.set("2025.1.1")
  type.set("IC") // Target IDE Platform
  updateSinceUntilBuild.set(false)

  plugins.set(listOf("java", "json", "copyright"))
}

tasks.register<Zip>("packageDistribution") {
  archiveFileName.set("DBN.zip")
  destinationDirectory.set(layout.buildDirectory.dir("dist"))

  from("lib/ext/") {
    include("**/*.jar")
    into("dbn/lib/ext")
  }
  from(layout.buildDirectory.dir("libs")) {
    include("${project.name}-${project.version}.jar")
    into("dbn/lib")
  }
}

tasks {
  // Set the JVM compatibility versions
  withType<JavaCompile> {
    sourceCompatibility = "11"
    targetCompatibility = "11"
  }

/* no kotlin code yet
withType<KotlinCompile> {
  kotlinOptions.jvmTarget = "11"
}
*/

  withType<JavaCompile>{
    copy {
      from("lib/ext")
      include("**/*.jar")
      into(layout.buildDirectory.dir("idea-sandbox/plugins/${project.name}/lib/ext"))
    }
  }
  test {
    // we are also excluding two ChecksumTest cases if we are on Linux
    if (project.hasProperty("excludeTests")) {
      var excludeTests: String = project.properties["excludeTests"] as String
      excludeTests.replace("\\s", "").split(",", ";").forEach { excluded ->
        System.out.println("Excluding testcase: "+excluded)
        exclude(excluded)
      }
    }
  }

  signPlugin {
    certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
    privateKey.set(System.getenv("PRIVATE_KEY"))
    password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
  }

  publishPlugin {
    token.set(System.getenv("PUBLISH_TOKEN"))
  }
  runIde {
        systemProperties["idea.auto.reload.plugins"] = true
        var waitForDebugger = "n"
        if (project.hasProperty("waitForDebugger")) {
            waitForDebugger = "y"
            System.out.println("runIde is waiting for a debugger to attach")
        }
        var jvmArgsMutable = mutableListOf(
            "-Xms512m",
            "-Xmx2048m",
            "-agentlib:jdwp=transport=dt_socket,server=y,suspend=$waitForDebugger,address=1044",
        )

	    if (project.hasProperty("enableAssertions")) {
            jvmArgsMutable.add("-ea")
            System.out.println("Java Assertions enabled")
        }
        // make it immutable
        jvmArgs = jvmArgsMutable.toList()
   }

   buildSearchableOptions {
    enabled = false
  }
}
