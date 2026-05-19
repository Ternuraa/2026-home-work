plugins {
    java
    application
    checkstyle
    pmd
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    checkstyle("com.puppycrawl.tools:checkstyle:13.3.0")
    implementation("com.github.spotbugs:spotbugs-annotations:4.9.8")
    implementation("org.apache.kafka:kafka-clients:3.8.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("ch.qos.logback:logback-classic:1.5.32")

    testImplementation(platform("org.junit:junit-bom:6.0.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.testcontainers:junit-jupiter:1.20.4")
    testImplementation("org.testcontainers:kafka:1.20.4")
    implementation("org.apache.commons:commons-lang3:3.17.0")
}

sourceSets {
    create("integrationTest") {
        java.srcDirs("src/integrationTest/java")
        resources.srcDirs("src/integrationTest/resources")

        compileClasspath += sourceSets.main.get().output + sourceSets.test.get().output
        runtimeClasspath += sourceSets.main.get().output + sourceSets.test.get().output
    }
}

// ИСПРАВЛЕНО: Безопасное наследование конфигураций без использования строковых имен
configurations {
    val integrationTestImplementation = findByName("integrationTestImplementation")
    val integrationTestRuntimeOnly = findByName("integrationTestRuntimeOnly")

    integrationTestImplementation?.extendsFrom(
        configurations.implementation.get(),
        configurations.testImplementation.get()
    )
    integrationTestRuntimeOnly?.extendsFrom(
        configurations.runtimeOnly.get(),
        configurations.testRuntimeOnly.get()
    )
}

tasks.test {
    maxHeapSize = "128m"
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

val integrationTest = tasks.register<Test>("integrationTest") {
    description = "Runs integration tests."
    group = "verification"

    useJUnitPlatform()

    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath

    shouldRunAfter("test")
    maxHeapSize = "512m"

    // Наш жесткий внутренний фильтр, чтобы запустить именно KafkaTest
    filter {
        includeTestsMatching("company.vk.edu.distrib.compute.KafkaTest")
        isFailOnNoMatchingTests = false
    }

    // Отключаем @Disabled
    systemProperty("junit.jupiter.conditions.deactivate", "org.junit.jupiter.api.Disabled")

    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}

application {
    mainClass = "company.vk.edu.distrib.compute.Server"
    applicationDefaultJvmArgs = listOf("-Xmx128m")
}

checkstyle {
    configFile = project.layout.projectDirectory.file("checkstyle.xml").asFile
    maxWarnings = 0
}

pmd {
    isConsoleOutput = true
    toolVersion = "7.16.0"
    rulesMinimumPriority = 5
    ruleSetFiles(project.layout.projectDirectory.file("pmd.xml"))
}

tasks.register("codeStyleChecks") {
    group = "verification"
    dependsOn(
        "checkstyleMain",
        "checkstyleTest",
        "checkstyleIntegrationTest",
        "pmdMain",
    )
}

tasks.check {
    dependsOn(tasks.test, integrationTest, "codeStyleChecks")
}

tasks.named("pmdIntegrationTest") {
    enabled = false
}

tasks.named("pmdTest") {
    enabled = false
}