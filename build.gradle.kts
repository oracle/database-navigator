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
version = "3.5.0.0"

repositories {
  mavenCentral()
}
dependencies {
  testImplementation("junit:junit:4.13.2")

  annotationProcessor("org.projectlombok:lombok:1.18.34")
  testAnnotationProcessor("org.projectlombok:lombok:1.18.34")

  implementation("org.projectlombok:lombok:1.18.34")

  // poi libraries (xls export)
  implementation("org.apache.poi:poi:5.3.0")
  implementation("org.apache.poi:poi-ooxml:5.3.0")
  implementation("org.apache.poi:poi-ooxml-lite:5.3.0")

  // poi library dependencies
  implementation("commons-io:commons-io:2.17.0")
  implementation("org.apache.commons:commons-compress:1.27.1")
  implementation("org.apache.commons:commons-collections4:4.4")
  implementation("org.apache.commons:commons-lang3:3.17.0")
  implementation("org.apache.logging.log4j:log4j-api:2.24.1")
  implementation("org.apache.sshd:sshd-common:2.13.2")
  implementation("org.apache.sshd:sshd-core:2.13.2")
  implementation("org.apache.xmlbeans:xmlbeans:5.2.1")

  implementation(project(":modules:dbn-api"))
  implementation(project(":modules:dbn-spi"))
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
  version.set("242.23339.11")
  type.set("IC") // Target IDE Platform

  plugins.set(listOf("java", "copyright"))

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
        jvmArgs = listOf(
            "-Xms512m",
            "-Xmx2048m",
            "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=1044",
        )
   }
}
