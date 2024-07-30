package com.panaxeo.shadowplugin

import com.panaxeo.shadowplugin.ShadowAarPlugin.Companion.SHADOW_ARR_CONFIGURATION_NAME
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertNotNull

class ShadowAarPluginTest {
    @Test fun `plugin registers task`() {
        // Create a test project and apply the plugin
        val project = ProjectBuilder.builder().build()
        project.configurations.create("implementation")
        project.plugins.apply("com.panaxeo.shadow-aar")

        // Verify the result
        assertNotNull(project.configurations.findByName("implementationEmbed"))
    }
}
