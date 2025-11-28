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
import org.gradle.kotlin.dsl.register

// import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.20"
    id("org.jetbrains.intellij") version "1.16.0"
    id("com.github.jk1.dependency-license-report") version "2.9"
}

val bundledJdbcOracle: Configuration by configurations.creating
val bundledJdbcMysql: Configuration by configurations.creating
val bundledJdbcPostgres: Configuration by configurations.creating
val bundledJdbcSqlite: Configuration by configurations.creating
val extFolder = "idea-sandbox/plugins/${project.name}/lib/ext"

group = "com.dbn"
version = "3.7.0.0"

repositories {
  mavenCentral()
  flatDir {
    dirs("libs")
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
    implementation("commons-io:commons-io:2.18.0")
    implementation("org.apache.commons:commons-compress:1.27.1")
    implementation("org.apache.commons:commons-collections4:4.4")
    implementation("org.apache.commons:commons-lang3:3.18.0")
    implementation("org.apache.logging.log4j:log4j-api:2.24.3")
    implementation("org.apache.xmlbeans:xmlbeans:5.3.0")

    // ssh tunnel libraries
    implementation("org.apache.sshd:sshd-common:2.14.0")
    implementation("org.apache.sshd:sshd-core:2.14.0")

    // driver download libraries
    implementation("org.apache.maven.resolver:maven-resolver-connector-basic:1.9.22")
    implementation("org.apache.maven:maven-resolver-provider:3.9.9")

    // db assistant
    implementation("dev.langchain4j:langchain4j:1.8.0")
    implementation("dev.langchain4j:langchain4j-core:1.8.0")
    implementation("dev.langchain4j:langchain4j-http-client:1.8.0")
    implementation("dev.langchain4j:langchain4j-mcp:1.8.0-beta15")

    implementation("dev.langchain4j:langchain4j-community-oci-genai:1.8.0-beta15")
    implementation("dev.langchain4j:langchain4j-open-ai:1.8.0")
    implementation("dev.langchain4j:langchain4j-google-ai-gemini:1.8.0")
    implementation("dev.langchain4j:langchain4j-anthropic:1.8.0")
    implementation("dev.langchain4j:langchain4j-cohere:1.8.0-beta15")
    implementation("dev.langchain4j:langchain4j-ollama:1.8.0")
    implementation("dev.langchain4j:langchain4j-bedrock:1.8.0")
    implementation("dev.langchain4j:langchain4j-mistral-ai:1.8.0")

    implementation("com.oracle.oci.sdk:oci-java-sdk-common:3.74.0")
    implementation("com.oracle.oci.sdk:oci-java-sdk-common-httpclient:3.74.0")
    implementation("com.oracle.oci.sdk:oci-java-sdk-common-httpclient-jersey:3.74.0")

    implementation("com.fasterxml.jackson.core:jackson-core:2.20.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.20.0")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.20")
    implementation("com.fasterxml.jackson.module:jackson-modules-base:2.20.0")
    implementation("com.fasterxml.jackson.module:jackson-modules-java8:2.20.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.20.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.2.20")

    implementation(project(":modules:dbn-api"))
    implementation(project(":modules:dbn-spi"))

    compileOnly("com.oracle:oci-intellij-plugin-api:" + project.properties["oci.ext.api.version"] + "@jar")

    // Oracle
    bundledJdbcOracle("javax.resource:connector-api:1.5@jar")
    bundledJdbcOracle("org.glassfish.hk2:hk2-api:2.6.1@jar")
    bundledJdbcOracle("org.glassfish.hk2:hk2-locator:2.6.1@jar")
    bundledJdbcOracle("org.glassfish.hk2:hk2-utils:2.6.1@jar")
    bundledJdbcOracle("com.fasterxml.jackson.core:jackson-annotations:2.17.1@jar")
    bundledJdbcOracle("com.fasterxml.jackson.core:jackson-core:2.17.1")
    bundledJdbcOracle("com.fasterxml.jackson.core:jackson-databind:2.17.1")
    bundledJdbcOracle("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.1@jar")
    bundledJdbcOracle("org.glassfish.hk2.external:jakarta.inject:2.6.1@jar")
    bundledJdbcOracle("jakarta.ws.rs:jakarta.ws.rs-api:2.1.6@jar")
    bundledJdbcOracle("jakarta.xml.bind:jakarta.xml.bind-api:2.3.3@jar")
    bundledJdbcOracle("javax.json:javax.json-api:1.1.4@jar")
    bundledJdbcOracle("org.glassfish.jersey.core:jersey-client:2.35@jar")
    bundledJdbcOracle("org.glassfish.jersey.core:jersey-common:2.35@jar")
    bundledJdbcOracle("org.glassfish.jersey.inject:jersey-hk2:2.35@jar")
    bundledJdbcOracle("org.glassfish.jersey.media:jersey-media-json-jackson:2.35@jar")
    bundledJdbcOracle("com.oracle.oci.sdk:oci-java-sdk-circuitbreaker:3.37.0@jar")
    bundledJdbcOracle("com.oracle.oci.sdk:oci-java-sdk-common:3.37.0@jar")
    bundledJdbcOracle("com.oracle.oci.sdk:oci-java-sdk-common-httpclient:3.37.0@jar")
    bundledJdbcOracle("com.oracle.oci.sdk:oci-java-sdk-common-httpclient-jersey:3.37.0@jar")
    bundledJdbcOracle("com.oracle.oci.sdk:oci-java-sdk-identitydataplane:3.37.0@jar")
    bundledJdbcOracle("com.oracle.database.jdbc:ojdbc8:23.4.0.24.05@jar")
    bundledJdbcOracle("com.oracle.database.security:oraclepki:23.4.0.24.05@jar")
    bundledJdbcOracle("com.oracle.database.nls:orai18n:23.4.0.24.05@jar")
    bundledJdbcOracle("com.oracle.database.xml:xdb:23.4.0.24.05@jar")
    bundledJdbcOracle("com.oracle.database.xml:xmlparserv2:23.4.0.24.05@jar")
    bundledJdbcOracle("com.oracle.database.jdbc:ojdbc-provider-common:1.0.1@jar")
    bundledJdbcOracle("com.oracle.database.jdbc:ojdbc-provider-oci:1.0.0@jar")
    bundledJdbcOracle("io.github.resilience4j:resilience4j-circuitbreaker:1.7.1@jar")
    bundledJdbcOracle("io.github.resilience4j:resilience4j-core:1.7.1@jar")
    bundledJdbcOracle("javax.servlet:servlet-api:2.5@jar")
    bundledJdbcOracle("io.vavr:vavr:0.10.2@jar")

    // MySQL
    bundledJdbcMysql("com.mysql:mysql-connector-j:9.4.0@jar")

    // PostgreSQL
    bundledJdbcPostgres("org.postgresql:postgresql:42.7.8@jar")
    bundledJdbcPostgres("org.osgi:org.osgi.dto:1.1.1@jar")
    bundledJdbcPostgres("org.osgi:org.osgi.framework:1.10.0@jar")
    bundledJdbcPostgres("org.osgi:org.osgi.resource:1.0.1@jar")
    bundledJdbcPostgres("org.osgi:org.osgi.service.jdbc:1.1.0@jar")

    // SQLite
    bundledJdbcSqlite("org.xerial:sqlite-jdbc:3.50.3.0@jar")
}

licenseReport {
    renderers = arrayOf<ReportRenderer>(TextReportRenderer("THIRD_PARTY_LICENSES.txt"))
    filters = arrayOf<DependencyFilter>(LicenseBundleNormalizer())
}

sourceSets {
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
                "**/*.properties"
            )
        }
    }
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2024.3.3")
    type.set("IC") // Target IDE Platform
    updateSinceUntilBuild.set(false)

    plugins.set(listOf("java", "json", "copyright"))
}

tasks.register("copyBundledJdbcLibs") {
    group = "build"
    description = "Copies all bundled JDBC libraries to the ext folder in the build directory."

    doLast {
        // Copy for Oracle JDBC
        copy {
            from(bundledJdbcOracle)
            include("*.jar")
            into(layout.buildDirectory.dir("$extFolder/bundled-jdbc-oracle").get().asFile)
        }

        // Copy for MySQL JDBC
        copy {
            from(bundledJdbcMysql)
            include("*.jar")
            into(layout.buildDirectory.dir("$extFolder/bundled-jdbc-mysql").get().asFile)
        }

        // Copy for PostgreSQL JDBC
        copy {
            from(bundledJdbcPostgres)
            include("*.jar")
            into(layout.buildDirectory.dir("$extFolder/bundled-jdbc-postgres").get().asFile)
        }

        // Copy for SQLite JDBC
        copy {
            from(bundledJdbcSqlite)
            include("*.jar")
            into(layout.buildDirectory.dir("$extFolder/bundled-jdbc-sqlite").get().asFile)
        }
    }
}

tasks.build {
    dependsOn("copyBundledJdbcLibs")
}

tasks.prepareSandbox {
    dependsOn("copyBundledJdbcLibs")
    from(layout.buildDirectory.dir(extFolder)) {
        into("lib/ext")
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

    test {
        // we are also excluding two ChecksumTest cases if we are on Linux
        if (project.hasProperty("excludeTests")) {
            var excludeTests: String = project.properties["excludeTests"] as String
            excludeTests.replace("\\s", "").split(",", ";").forEach { excluded ->
                System.out.println("Excluding testcase: " + excluded)
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
