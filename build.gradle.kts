import cn.lalaki.pub.BaseCentralPortalPlusExtension

plugins {
    `kotlin-dsl`
    `maven-publish`
    signing
    id("cn.lalaki.central") version "1.2.5"
    id("org.jetbrains.dokka") version "1.9.20"
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
version = "0.1.1"

gradlePlugin {
    // Define the plugin
    val shadowAarPlugin by plugins.creating {
        id = "com.panaxeo.shadow-aar"
        implementationClass = "com.panaxeo.shadowplugin.ShadowAarPlugin"
        isAutomatedPublishing = true
    }
}

// Publish part

ext["signing.keyId"] = System.getenv("SHADOW_AAR_SIGNING_KEY_ID")
ext["signing.password"] = System.getenv("SHADOW_AAR_SIGNING_PASSWORD")
ext["signing.secretKeyRingFile"] = System.getenv("SHADOW_AAR_SIGNING_SECRET_KEY_RING_FILE")
ext["sonatypeUsername"] = System.getenv("SHADOW_AAR_REPO_USERNAME")
ext["sonatypePassword"] = System.getenv("SHADOW_AAR_REPO_PASSWORD")

// Maven Bug, maven-publish ignores custom repository and default is used anyway
val localMavenRepo = project.repositories.mavenLocal().url

val dokkaOutputDir = "${layout.buildDirectory.get().asFile.absolutePath}/dokka"
tasks.dokkaHtml {
    outputDirectory.set(file(dokkaOutputDir))
}
val deleteDokkaOutputDir by tasks.register<Delete>("deleteDokkaOutputDirectory") {
    delete(dokkaOutputDir)
}
val javadocJar = tasks.register<Jar>("javadocJar") {
    dependsOn(deleteDokkaOutputDir, tasks.dokkaHtml)
    archiveClassifier.set("javadoc")
    from(dokkaOutputDir)
}

val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.getByName("main").allSource)
}

publishing {
    publications {
        create("shadowAar", MavenPublication::class.java) {
            artifactId = project.name
            from(components["kotlin"])
            artifact(javadocJar)
            artifact(sourcesJar)
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
                        email = "infra@panaxeo.com"
                    }
                }
                scm {
                    url = "https://github.com/panaxeo/shadow-aar/tree/main"
                    connection = "scm:https://github.com/panaxeo/shadow-aar.git"
                }
            }
        }
    }
    repositories {
        maven {
            url = localMavenRepo
        }
    }
}

signing {
    sign(publishing.publications["shadowAar"])
}

tasks.withType(GenerateMavenPom::class.java).configureEach {
    enabled = pom?.name?.orNull != null
}
tasks.withType(GenerateModuleMetadata::class.java).configureEach {
    enabled = (publication.orNull as? MavenPublication)?.artifactId != null
}
tasks.withType(PublishToMavenRepository::class.java).configureEach {
    enabled = publication?.artifactId != null
}
tasks.withType(PublishToMavenLocal::class.java).configureEach {
    enabled = publication?.artifactId != null
}

centralPortalPlus {
    url = localMavenRepo
    username = project.ext["sonatypeUsername"] as? String
    password = project.ext["sonatypePassword"] as? String
    publishingType = BaseCentralPortalPlusExtension.PublishingType.AUTOMATIC
}

// Plugin test part

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
