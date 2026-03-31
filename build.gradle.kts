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

// https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.2.20"
    id("org.jetbrains.intellij.platform") version "2.11.0"
    id("com.github.jk1.dependency-license-report") version "2.9"
}

val bundledJdbcOracle: Configuration by configurations.creating
val bundledJdbcMysql: Configuration by configurations.creating
val bundledJdbcPostgres: Configuration by configurations.creating
val bundledJdbcSqlite: Configuration by configurations.creating

group = "com.dbn"
version = "4.0.0.0"

repositories {
    // locally built 3rd party dependencies
    maven(url = uri("../dbn-libraries/_repository"))
    maven(url = uri("lib"))
    mavenCentral()
    flatDir { dirs("libs") }

    intellijPlatform { defaultRepositories() }
}

dependencies {
    testImplementation("junit:junit:4.13.2")

    annotationProcessor("org.projectlombok:lombok:1.18.36")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.36")

    intellijPlatform {
        intellijIdea("2024.3.3")

        // https://plugins.jetbrains.com/docs/intellij/plugin-dependencies.html#bundled-and-other-plugins
        bundledPlugins(
            "com.intellij.java",
            "com.intellij.modules.json",
            "com.intellij.copyright"
        )
    }

    // ********** DEPENDENCY TREE MODEL **********
/*
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.21")
    implementation("com.fasterxml.jackson.core:jackson-core:2.21.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.21.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.21.1")
    implementation("com.fasterxml.jackson.module:jackson-modules-base:2.21.1")
    implementation("com.fasterxml.jackson.module:jackson-modules-java8:2.21.1")
    implementation("com.oracle.oci.sdk:oci-java-sdk-circuitbreaker:3.76.1")
    implementation("com.oracle.oci.sdk:oci-java-sdk-common-httpclient-jersey:3.76.1")
    implementation("com.oracle.oci.sdk:oci-java-sdk-common-httpclient:3.76.1")
    implementation("com.oracle.oci.sdk:oci-java-sdk-common:3.76.1")
    implementation("com.oracle.oci.sdk:oci-java-sdk-generativeaiinference:3.76.1")
    implementation("commons-io:commons-io:2.18.0")
    implementation("dev.langchain4j:langchain4j-anthropic:1.12.2")
    implementation("dev.langchain4j:langchain4j-community-oci-genai:1.12.2-beta17")
    implementation("dev.langchain4j:langchain4j-core:1.12.2")
    implementation("dev.langchain4j:langchain4j-google-ai-gemini:1.12.2")
    implementation("dev.langchain4j:langchain4j-http-client:1.12.2")
    implementation("dev.langchain4j:langchain4j-mcp:1.12.2-beta17")
    implementation("dev.langchain4j:langchain4j-mistral-ai:1.12.2")
    implementation("dev.langchain4j:langchain4j-ollama:1.12.2")
    implementation("dev.langchain4j:langchain4j-open-ai:1.12.2")
    implementation("dev.langchain4j:langchain4j:1.12.2")
    implementation("org.apache.commons:commons-collections4:4.4")
    implementation("org.apache.commons:commons-compress:1.27.1")
    implementation("org.apache.commons:commons-lang3:3.18.0")
    implementation("org.apache.logging.log4j:log4j-api:2.24.3")
    implementation("org.apache.maven.resolver:maven-resolver-connector-basic:1.9.22")
    implementation("org.apache.maven:maven-resolver-provider:3.9.9")
    implementation("org.apache.poi:poi-ooxml-lite:5.4.1")
    implementation("org.apache.poi:poi-ooxml:5.4.1")
    implementation("org.apache.poi:poi:5.4.1")
    implementation("org.apache.sshd:sshd-common:2.16.0")
    implementation("org.apache.sshd:sshd-core:2.16.0")
    implementation("org.apache.xmlbeans:xmlbeans:5.3.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.2.20")
    implementation("org.projectlombok:lombok:1.18.36")
*/


    // ********** DEPENDENCY FLAT MODEL **********
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.21@jar")
    implementation("com.fasterxml.jackson.core:jackson-core:2.21.1@jar")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.21.1@jar")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.21.1@jar")
    implementation("com.fasterxml.jackson.module:jackson-module-jaxb-annotations:2.21.1@jar")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.21.1@jar")
    implementation("com.oracle.oci.sdk:oci-java-sdk-circuitbreaker:3.76.1@jar")
    implementation("com.oracle.oci.sdk:oci-java-sdk-common-httpclient-jersey:3.76.1@jar")
    implementation("com.oracle.oci.sdk:oci-java-sdk-common-httpclient:3.76.1@jar")
    implementation("com.oracle.oci.sdk:oci-java-sdk-common:3.76.1@jar")
    implementation("com.oracle.oci.sdk:oci-java-sdk-generativeaiinference:3.76.1@jar")
    implementation("com.oracle.oci.sdk:oci-java-sdk-identity:3.76.1@jar")
    implementation("commons-io:commons-io:2.18.0@jar")
    implementation("dev.langchain4j:langchain4j-anthropic:1.12.2@jar")
    implementation("dev.langchain4j:langchain4j-community-oci-genai:1.12.2-beta22@jar")
    implementation("dev.langchain4j:langchain4j-core:1.12.2@jar")
    implementation("dev.langchain4j:langchain4j-google-ai-gemini:1.12.2@jar")
    implementation("dev.langchain4j:langchain4j-http-client:1.12.2@jar")
    implementation("dev.langchain4j:langchain4j-mcp:1.12.2-beta22@jar")
    implementation("dev.langchain4j:langchain4j-mistral-ai:1.12.2@jar")
    implementation("dev.langchain4j:langchain4j-ollama:1.12.2@jar")
    implementation("dev.langchain4j:langchain4j-open-ai:1.12.2@jar")
    implementation("dev.langchain4j:langchain4j:1.12.2@jar")
    implementation("io.github.resilience4j:resilience4j-circuitbreaker:1.7.1@jar")
    implementation("io.github.resilience4j:resilience4j-core:1.7.1@jar")
    implementation("io.vavr:vavr:0.10.2@jar")
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:2.1.6@jar")
    implementation("javax.inject:javax.inject:1@jar")
    implementation("org.apache.commons:commons-collections4:4.4@jar")
    implementation("org.apache.commons:commons-compress:1.27.1@jar")
    implementation("org.apache.commons:commons-lang3:3.18.0@jar")
    implementation("org.apache.logging.log4j:log4j-api:2.24.3@jar")
    implementation("org.apache.maven.resolver:maven-resolver-api:1.9.22@jar")
    implementation("org.apache.maven.resolver:maven-resolver-connector-basic:1.9.22@jar")
    implementation("org.apache.maven.resolver:maven-resolver-impl:1.9.22@jar")
    implementation("org.apache.maven.resolver:maven-resolver-named-locks:1.9.22@jar")
    implementation("org.apache.maven.resolver:maven-resolver-spi:1.9.22@jar")
    implementation("org.apache.maven.resolver:maven-resolver-util:1.9.22@jar")
    implementation("org.apache.maven.shared:maven-invoker:3.3.0@jar")
    implementation("org.apache.maven.shared:maven-shared-utils:3.4.2@jar")
    implementation("org.apache.maven:maven-artifact:3.9.9@jar")
    implementation("org.apache.maven:maven-builder-support:3.9.9@jar")
    implementation("org.apache.maven:maven-model-builder:3.9.9@jar")
    implementation("org.apache.maven:maven-model:3.9.9@jar")
    implementation("org.apache.maven:maven-repository-metadata:3.9.9@jar")
    implementation("org.apache.maven:maven-resolver-provider:3.9.9@jar")
    implementation("org.apache.poi:poi-ooxml-lite:5.4.1@jar")
    implementation("org.apache.poi:poi-ooxml:5.4.1@jar")
    implementation("org.apache.poi:poi:5.4.1@jar")
    implementation("org.apache.sshd:sshd-common:2.16.0@jar")
    implementation("org.apache.sshd:sshd-core:2.16.0@jar")
    implementation("org.apache.xmlbeans:xmlbeans:5.3.0@jar")
    implementation("org.codehaus.plexus:plexus-interpolation:1.27@jar")
    implementation("org.codehaus.plexus:plexus-utils:3.5.1@jar")
    implementation("org.eclipse.sisu:org.eclipse.sisu.inject:0.9.0.M3@jar")
    implementation("org.glassfish.hk2.external:jakarta.inject:2.6.1@jar")
    implementation("org.glassfish.hk2:hk2-api:2.6.1@jar")
    implementation("org.glassfish.hk2:hk2-locator:2.6.1@jar")
    implementation("org.glassfish.hk2:hk2-utils:2.6.1@jar")
    implementation("org.glassfish.jersey.core:jersey-client:2.35@jar")
    implementation("org.glassfish.jersey.core:jersey-common:2.35@jar")
    implementation("org.glassfish.jersey.inject:jersey-hk2:2.35@jar")
    implementation("org.glassfish.jersey.media:jersey-media-json-jackson:2.35@jar")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.2.20@jar")
    implementation("org.projectlombok:lombok:1.18.36@jar")


    implementation(project(":modules:dbn-api"))
    implementation(project(":modules:dbn-spi"))

    compileOnly("com.oracle:oci-intellij-plugin-api:" + project.properties["oci.ext.api.version"] + "@jar")

    // Oracle
    bundledJdbcOracle("javax.resource:connector-api:1.5@jar")
    bundledJdbcOracle("org.glassfish.hk2:hk2-api:2.6.1@jar")
    bundledJdbcOracle("org.glassfish.hk2:hk2-locator:2.6.1@jar")
    bundledJdbcOracle("org.glassfish.hk2:hk2-utils:2.6.1@jar")
    bundledJdbcOracle("com.fasterxml.jackson.core:jackson-annotations:2.17.1@jar")
    bundledJdbcOracle("com.fasterxml.jackson.core:jackson-core:2.17.1@jar")
    bundledJdbcOracle("com.fasterxml.jackson.core:jackson-databind:2.17.1@jar")
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
    bundledJdbcMysql("com.mysql:mysql-connector-j:9.5.0@jar")

    // PostgreSQL
    bundledJdbcPostgres("org.postgresql:postgresql:42.7.10@jar")

    // SQLite
    bundledJdbcSqlite("org.xerial:sqlite-jdbc:3.51.1.0@jar")
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
intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            untilBuild = provider { null }
        }
    }

    buildSearchableOptions = false
    signing {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishing {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}

tasks.register("copyBundledJdbcLibs") {
    group = "build"
    description = "Copies all bundled JDBC libraries to the ext folder in the build directory."

    doLast {
        // Copy for Oracle JDBC
        copy {
            from(bundledJdbcOracle)
            include("*.jar")
            into(layout.buildDirectory.dir("libs/ext/bundled-jdbc-oracle").get().asFile)
        }

        // Copy for MySQL JDBC
        copy {
            from(bundledJdbcMysql)
            include("*.jar")
            into(layout.buildDirectory.dir("libs/ext/bundled-jdbc-mysql").get().asFile)
        }

        // Copy for PostgreSQL JDBC
        copy {
            from(bundledJdbcPostgres)
            include("*.jar")
            into(layout.buildDirectory.dir("libs/ext/bundled-jdbc-postgres").get().asFile)
        }

        // Copy for SQLite JDBC
        copy {
            from(bundledJdbcSqlite)
            include("*.jar")
            into(layout.buildDirectory.dir("libs/ext/bundled-jdbc-sqlite").get().asFile)
        }
    }
}

tasks.build {
    dependsOn("copyBundledJdbcLibs")
}

tasks.prepareSandbox {
    dependsOn("copyBundledJdbcLibs")

    from(layout.buildDirectory.dir("libs/ext")) {
        into(layout.buildDirectory.dir("/${project.name}/lib/ext").get().asFile)
    }
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"

        // deprecated api usage is a common thing in DBN given the
        // wide range of IntelliJ versions we support with few compatibility builds
        options.compilerArgs.add("-Xlint:-deprecation")
    }

    test {
        if (project.hasProperty("excludeTests")) {
            val excludeTests: String = project.properties["excludeTests"] as String
            excludeTests.replace("\\s", "").split(",", ";").forEach { excluded ->
                    println("Excluding testcase: $excluded")
                exclude(excluded)
            }
        }
    }

    runIde {
        systemProperties["idea.auto.reload.plugins"] = true
        var waitForDebugger = "n"
        if (project.hasProperty("waitForDebugger")) {
            waitForDebugger = "y"
            println("runIde is waiting for a debugger to attach")
        }
        val jvmArgsMutable = mutableListOf(
            "-Xms512m",
            "-Xmx2048m",
            "-agentlib:jdwp=transport=dt_socket,server=y,suspend=$waitForDebugger,address=1044",
        )

        if (project.hasProperty("enableAssertions")) {
            jvmArgsMutable.add("-ea")
            println("Java Assertions enabled")
        }
        jvmArgs = jvmArgsMutable.toList()
    }
}
