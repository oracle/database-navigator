plugins {
    id("org.jetbrains.intellij.platform.module")
}

group = "com.dbn"
version = "1.0.0"

repositories {
    mavenCentral()
    intellijPlatform { defaultRepositories() }
}

dependencies {
    implementation(project(":"))
    compileOnly("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.projectlombok:lombok:1.18.46")
    testImplementation("junit:junit:4.13.2")
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
}

tasks.test {
    useJUnitPlatform()
}
