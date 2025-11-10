plugins {
    application
    java
    jacoco
    checkstyle
    id("com.diffplug.spotless") version "6.25.0"
}



group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass.set("org.example.Main")
}

spotless {
    java {
        target("src/**/*.java")

        // автоформат по Google Java Style
        googleJavaFormat("1.17.0")

        trimTrailingWhitespace()
        endWithNewline()
    }
}

tasks.test {
    useJUnitPlatform()

    testLogging {
        events(
            org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED,
            org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED,
            org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
        )
        showStandardStreams = false
    }

    finalizedBy(tasks.jacocoTestReport)
}


checkstyle {
    toolVersion = "10.17.0"
    configDirectory.set(file("config/checkstyle"))
}

tasks.withType<Checkstyle> {
    isIgnoreFailures = false
}


tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}


tasks.check {
    dependsOn("spotlessCheck")
}


tasks.jacocoTestReport {
    dependsOn(tasks.test)

    reports {
        xml.required.set(true)
        csv.required.set(false)
        html.required.set(true)        // build/reports/jacoco/test/html/index.html
    }
}
