package com.panaxeo.shadowplugin

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.kotlin.dsl.dependencies
import java.io.File

abstract class ShadowAarPluginExtension {
    @get:Input
    abstract val transformR: Property<String?>
    @get:Input
    abstract val repackages: MapProperty<String, String>
}

class ShadowAarPlugin : Plugin<Project> {

    companion object {
        const val SHADOW_AAR_EXTENSION_NAME = "shadowAar"
        const val SHADOW_ARR_CONFIGURATION_NAME = "implementationEmbed"
        const val JARS_FROM_AARS_DIR = "jars-from-aars"
        const val REPACKAGE_JAR_TASK_NAME = "repackageImplementationJARs"
        const val UNZIP_CLASSES_TASK_NAME = "unzipClassesForRelocation"
        const val ANDROID_ARTIFACT_EXTENSION = "aar"
        const val JAVA_ARTIFACT_EXTENSION = "jar"
        const val JAR_IN_AAR_FILE_NAME = "classes.jar"
        const val SUBCLASS_USAGE_REGEX = "\\$.+"
    }

    private fun isAndroidArtifact(artifact: ResolvedArtifact): Boolean =
        artifact.extension == ANDROID_ARTIFACT_EXTENSION

    private fun isJavaArtifact(artifact: ResolvedArtifact): Boolean =
        artifact.extension == JAVA_ARTIFACT_EXTENSION

    private fun isFromGroups(artifact: ResolvedArtifact, groups: List<String?>): Boolean =
        groups.any { artifact.moduleVersion.id.group == it }

    private fun getAarsWithTopLevelDependencyGroups(
        artifacts: Set<ResolvedArtifact>,
        topLevelDeps: DependencySet
    ): List<ResolvedArtifact> {
        val topLevelDepGroups = topLevelDeps.map { it.group }
        return artifacts.filter {
            isAndroidArtifact(it) && isFromGroups(it, topLevelDepGroups)
        }
    }

    private fun getJarsWithTopLevelDependencyGroups(
        artifacts: Set<ResolvedArtifact>,
        topLevelDeps: DependencySet
    ): List<ResolvedArtifact> {
        val topLevelDepGroups = topLevelDeps.map { it.group }
        return artifacts.filter {
            isJavaArtifact(it) && isFromGroups(it, topLevelDepGroups)
        }
    }

    private fun extractClassesJarFromAar(project: Project, targetDir: File, source: ResolvedArtifact): File {
        val artifactQualifiedName = getFullyQualifiedDependencyCoordinates(source)
        val jarName = getJarFileName(artifactQualifiedName)
        project.copy {
            from(project.zipTree(source.file)) { rename(JAR_IN_AAR_FILE_NAME, jarName) }
            include(JAR_IN_AAR_FILE_NAME)
            into(targetDir)
        }
        return targetDir.resolve(jarName)
    }

    private fun collectClassesJarFromAar(targetDir: File, source: ResolvedArtifact): File {
        val artifactQualifiedName = getFullyQualifiedDependencyCoordinates(source)
        val jarName = getJarFileName(artifactQualifiedName)
        return targetDir.resolve(jarName)
    }

    private fun getJarFileName(artifactQualifiedName: String) =
        "${artifactQualifiedName.replace(":", "-")}.$JAVA_ARTIFACT_EXTENSION"

    private fun getFullyQualifiedDependencyCoordinates(artifact: ResolvedArtifact): String {
        val version = artifact.moduleVersion.id.version
        val group = artifact.moduleVersion.id.group
        val name = artifact.moduleVersion.id.name
        return "$group:$name:$version"
    }

    override fun apply(project: Project) {
        val extension = project.extensions.create(
            SHADOW_AAR_EXTENSION_NAME,
            ShadowAarPluginExtension::class.java
        )
        val aarConfig = project.configurations.create(SHADOW_ARR_CONFIGURATION_NAME)
        val implementationConfig = project.configurations.getByName("implementation")
        project.afterEvaluate {
            project.tasks.findByName("clean")?.doFirst {
                project.delete(JARS_FROM_AARS_DIR)
            }
            val repackageDefs = extension.repackages.get()
            val transformR = extension.transformR.orNull
            val allArtifacts = aarConfig.resolvedConfiguration.resolvedArtifacts
            val aarTopLevelDeps = aarConfig.dependencies
            val aarDepsForRelocation = getAarsWithTopLevelDependencyGroups(allArtifacts, aarTopLevelDeps)
            val jarDepsForRelocation = getJarsWithTopLevelDependencyGroups(allArtifacts, aarTopLevelDeps)
            val extractClassesJarTask = project.tasks.register(UNZIP_CLASSES_TASK_NAME) {
                mustRunAfter("clean")
                setOnlyIf { true }
                outputs.upToDateWhen { false }
                var unzipDir = ensureDir(project, JARS_FROM_AARS_DIR)
                val futureJars = aarDepsForRelocation.map {
                    collectClassesJarFromAar(unzipDir, it)
                }
                outputs.files(futureJars)
                doLast {
                    aarDepsForRelocation.forEach {
                        unzipDir = ensureDir(project, JARS_FROM_AARS_DIR)
                        extractClassesJarFromAar(project, unzipDir, it)
                    }
                }
            }
            val repackageTask = project.tasks.register(REPACKAGE_JAR_TASK_NAME, ShadowJar::class.java) {
                dependsOn(extractClassesJarTask)
                setOnlyIf { true }
                outputs.upToDateWhen { false }
                val allJarsForRelocation = extractClassesJarTask.get().outputs.files + jarDepsForRelocation.map { it.file }
                from(allJarsForRelocation)
                repackageDefs.forEach {
                    val (repackageFrom, repackageTo) = it
                    transformR?.let { targetRClass ->
                        relocate(RClassRelocator(repackageFrom, targetRClass))
                    }
                    relocate(repackageFrom, repackageTo)
                }
            }
            project.dependencies {
                val notRelocatedArtifacts = allArtifacts - aarDepsForRelocation.toSet() - jarDepsForRelocation.toSet()
                notRelocatedArtifacts.forEach {
                    add(implementationConfig.name, getFullyQualifiedDependencyCoordinates(it))
                }
                val relocatedArtifacts = repackageTask.get().outputs.files
                add(implementationConfig.name, relocatedArtifacts)
            }
        }
    }

    private fun ensureDir(project: Project, dirPath: String): File {
        val dir = project.file(dirPath)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
}
