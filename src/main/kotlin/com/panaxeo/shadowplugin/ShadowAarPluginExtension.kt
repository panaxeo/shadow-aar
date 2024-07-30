package com.panaxeo.shadowplugin

import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input

abstract class ShadowAarPluginExtension {
    @get:Input
    abstract val transformR: Property<String?>
    @get:Input
    abstract val repackages: MapProperty<String, String>
}
