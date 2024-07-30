plugins {
    `kotlin-dsl`
    `maven-publish`
}

repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
}

dependencies {
    implementation("com.github.johnrengelman:shadow:8.1.1")
    implementation("org.codehaus.plexus:plexus-utils:4.0.1")

    // Use the Kotlin JUnit 5 integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

group = "com.panaxeo"
version = "0.1.0"

gradlePlugin {
    val pluginName by plugins.creating {
        id = "$group.${project.name}"
        implementationClass = "$group.shadowplugin.ShadowAarPlugin"
    }
}

publishing {
    publications {
        create<MavenPublication>(name) {
            from(components["kotlin"])
            pom {
                name = "Shadow-AAR"
                description = "Utility to shadow dependencies wrapped in AAR"
                url = "https://github.com/panaxeo/shadow-aar"
                licenses {
                    license {
                        name = "MIT"
                        url = "https://opensource.org/license/mit"
                    }
                }
                developers {
                    developer {
                        id = "panaxeo"
                        name = "Panaxeo"
                        email = "contact@panaxeo.com"
                    }
                }
                scm {
                    url = "https://github.com/panaxeo/shadow-aar/tree/main"
                    connection = "scm:https://github.com/panaxeo/shadow-aar.git"
                }
            }
        }
    }
}

val functionalTestSourceSet = sourceSets.create("functionalTest") {
}

configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])
configurations["functionalTestRuntimeOnly"].extendsFrom(configurations["testRuntimeOnly"])

val functionalTest by tasks.registering(Test::class) {
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath
    useJUnitPlatform()
}

gradlePlugin.testSourceSets.add(functionalTestSourceSet)

tasks.named<Task>("check") {
    dependsOn(functionalTest)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
