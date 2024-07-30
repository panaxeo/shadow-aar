package com.panaxeo.shadowplugin

import com.github.jengelman.gradle.plugins.shadow.relocation.RelocatePathContext
import com.github.jengelman.gradle.plugins.shadow.relocation.SimpleRelocator
import org.codehaus.plexus.util.SelectorUtils
import org.gradle.api.GradleScriptException

class RClassRelocator(
    basePackage: String,
    targetRClass: String
) : SimpleRelocator(
    basePackage,
    targetRClass,
    mutableListOf(),
    mutableListOf(),
    false
) {

    private var rClassDefinitionRegex: Regex

    init {
        val rClassDefinition = buildRclassDefinition(basePackage)
        val regExPrefix = SelectorUtils.REGEX_HANDLER_PREFIX
        val subclassDelimiter = ShadowAarPlugin.SUBCLASS_USAGE_REGEX
        val regExSuffix = SelectorUtils.PATTERN_HANDLER_SUFFIX
        val rUsageTransformRegex = "$regExPrefix$rClassDefinition$subclassDelimiter$regExSuffix"
        val rTransformRegex = "$regExPrefix$rClassDefinition$regExSuffix"
        include(rTransformRegex)
        include(rUsageTransformRegex)
        this.rClassDefinitionRegex = Regex(rClassDefinition)
    }

    private fun buildRclassDefinition(classDefinition: String): String {
        var classBasePattern = classDefinition
        if (classDefinition.startsWith(SelectorUtils.REGEX_HANDLER_PREFIX)) {
            classBasePattern = unwrapRegexHandler(classBasePattern)
        } else {
            classBasePattern = classDefinition.replace('.', '/')
        }
        if (classBasePattern.endsWith("/*") || classBasePattern.endsWith("/")) {
            classBasePattern = classBasePattern.substring(0, classBasePattern.lastIndexOf('/'))
        }
        return "$classBasePattern/.*/R"
    }

    private fun unwrapRegexHandler(classBasePattern: String) = classBasePattern.substring(
        startIndex = SelectorUtils.REGEX_HANDLER_PREFIX.length,
        endIndex = classBasePattern.length - SelectorUtils.PATTERN_HANDLER_SUFFIX.length
    )

    override fun canRelocateClass(className: String?): Boolean {
        // should not happen, R class will be re-builded upon relocated sources
        return false
    }

    override fun relocatePath(context: RelocatePathContext?): String {
        if (context == null) {
            throw GradleScriptException("RelocatePathContext is null", NullPointerException())
        }
        val pathToRelocate = context.path
        val pathPatternToRelocate = rClassDefinitionRegex.find(pathToRelocate)?.value
        if (pathPatternToRelocate == null) {
            throw GradleScriptException("No path part found to relocate for $pathToRelocate", NullPointerException())
        }
        context.stats.relocate(pathPatternToRelocate, shadedPathPattern)
        return pathToRelocate.replaceFirst(pathPatternToRelocate, shadedPathPattern)
    }
}
