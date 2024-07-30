# Shadow AAR

Gradle plugin to create "fat aar" with support of relocation/shadowing of dependencies.

## Motivation

Every developer has to face a problem named "dependency-hell". This problem occurs when you are updating some library/dependency in your project, but you found 2 or more libraries that are incompatible with each other due to their shared dependency.<br>
One (and optimal) solution is to find compatible version of these libraries. But sometimes you are not able to, or you are waiting too long for library team to update their code.<br>
For Java world using JAR dependencies, there is known solution to use [ShadowJar](https://github.com/GradleUp/shadow) to embed shadowed/relocated versions of libraries to avoid collision with library versions or class implementations. But that could not be used for Android AAR packages.<br>
We found multiple plugins to tried to do shadowing upon AARs but are not complete or fix only partial problems.<br>
Therefor we try to bring general solution to fix this problem (we hope) for all AAR requirements.

## How to use

This guide provides steps how to use plugin in gradle project with root and submodule project. This structure is typically used for Android projects. If you are using this plugin in 'flat' or 'multi-project' structure, steps may differ.

### Step 1: Add dependency on plugin

Add `classpath` dependency to your `build.gradle` in root project:

```groovy
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath 'com.panaxeo:shadow-aar:0.1.0'
    }
}
```

### Step 2: Apply plugin

Apply plugin in each `build.gradle` for project you need to shadow dependencies:

```groovy
apply plugin: 'com.panaxeo.shadow-aar'
```

### Step 3: List dependencies to shadow

Use `implementationEmbed` instead of `implementation` to the dependencies you need to shadow. For demo purposes, we chose [coil library](https://coil-kt.github.io/coil/) to demonstrate plugin usage:

```groovy
implementationEmbed 'io.coil-kt:coil:1.4.0'
```

> **Note:** Plugin will embed dependency into final package but without transitive dependencies. These dependencies are added to dependency tree respectively but will not be embedded in final product.<br>
> For example - embedding of `io.coil-kt:coil` will embed `coil` but it depends on `io.coil-kt:coil-base` that will not be embedded but added as direct dependency to your project.<br>
> Please list all transient dependencies that you want to embed too.

### Step 4: Configure shadowing

Dependencies are now "just" embedded into your project. Please add shadowing configuration if you also want to relocate/shadow a packages:

```groovy
shadowAar {
    repackages.put("coil", "repackage.coil")
}
```

> **Note:** Shadowing configuration is optional step. There is no "default" shadowing configuration. If you skip this step then dependencies will be just embedded, so you lost purpose of using this plugin.

### Step 5: Use shadowed implementations

Plugin is applied before any compilation gradle tasks, and as it swaps original packages with shadowed, you have to update your source code to use shadow implementation.

```kotlin
// before
import coil.ImageLoader
// after
import repackage.coil.ImageLoader
// but no other changes in usage
val loader = ImageLoader.Builder(context).build()
```

> **Note:** Please accept this decision. Yes, 'shadowing idea' could be applied after compilation gradle tasks, but you may face runtime issues if plugin will not map something correctly. For current plugin behaviour, you'll be working with "final result" and you are able to re-implement your code to fix any compilation problem. 

### Step 6: Additional changes for Android AARs

Some dependencies as AAR may contain Android resources that has to be merged with your application.

Library `io.coil-kt:coil-base` contains Android resource named `coil_request_manager`. Coil implementation is accessing this resource as `coil.base.R.id.coil_request_manager` but as this is not merged into your project (yet) it has to be done manually:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <item name="coil_request_manager" type="id"/>
</resources>
```

But Coil is still referencing to `coil.base.R.id.coil_request_manager` (or `repackage.coil.base.R.id.coil_request_manager` after shadowing) and there is no such a resource. Therefor you have to configure your R class path to shadow configuration:

```groovy
shadowAar {
    transformR = "com.example.app.R"
    repackages.put("coil", "repackage.coil")
}
```

> **Note**: Configuration of `transformR` is applied to all shadowed dependencies. Plugin implements "auto-detection" of using R-classes in libraries and shadows them effectively.<br>
> Please be aware of other Android resources as drawables, layouts, etc. All these resources has to be manually copied into your Android project.

## Next steps for plugin

As motivation of this project tries to describe, we would like to bring fully Android-AAR support into plugin.<br>
So our next goal is to bring a support of:

1. Automatically merge Android resources (drawable, layout, ...) into you project
   1. Check `Step 6`
2. Re-compile kotlin_module files (probably possible with Kotlin2.0) after shadowing
   1. Check `Step 5` - that's why it is better to face compilation problems in your source code rather that investigate runtime issues.

## Thanks
This plugin would not be possible to create without large effort of [ShadowJar](https://github.com/GradleUp/shadow) so thank you and your contributors!