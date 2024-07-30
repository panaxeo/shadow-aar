# Shadow AAR

Gradle plugin to create "fat aar" with the support of relocation/shadowing of dependencies.

## Motivation

Every developer has to face a problem named "dependency-hell". This problem occurs when you are updating a library/dependency in your project but find 2 or more libraries that are incompatible with each other due to their shared dependency.<br>
One (and also the optimal) solution is to find a compatible version of these libraries. However, sometimes you are not able to, or you have to wait too long for the library team to update their code.<br>
In the Java world using JAR dependencies, there is a known solution: to use [ShadowJar](https://github.com/GradleUp/shadow) to embed shadowed/relocated versions of libraries to avoid collision with library versions or class implementations. But that cannot be used for Android AAR packages.<br>
We tried multiple plugins to do shadowing upon AARs. However, they are incomplete or the problem only partially.<br>
Therefore, we devised a general solution to fix this problem (we hope) for all AAR requirements.

## How to use

This guide provides steps to using the plugin in gradle projects with root and submodule projects. This structure is typically used for Android projects. If you are using this plugin in 'flat' or 'multi-project' structure, the steps may differ.

### Step 1: Add dependency on plugin

Add `classpath` dependency to your `build.gradle` in the root project:

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

### Step 2: Apply the plugin

Apply the plugin in each `build.gradle` for projects where you need to shadow dependencies:

```groovy
apply plugin: 'com.panaxeo.shadow-aar'
```

### Step 3: List dependencies to shadow

Use `implementationEmbed` instead of `implementation` to the dependencies you need to shadow. For demo purposes, we chose [coil library](https://coil-kt.github.io/coil/) to demonstrate plugin usage:

```groovy
implementationEmbed 'io.coil-kt:coil:1.4.0'
```

> **Note:** The plugin will embed the dependency into the final package but without transitive dependencies. These dependencies are added to the dependency tree respectively but will not be embedded in the final product.<br>
> For example - embedding `io.coil-kt:coil` will embed `coil`. However, since it depends on `io.coil-kt:coil-base`, that will not be embedded but added as a direct dependency to your project.<br>
> Please list all transient dependencies that you want to embed too.

### Step 4: Configure shadowing

Dependencies are now "just" embedded into your project. Please add a shadowing configuration if you also want to relocate/shadow a packages:

```groovy
shadowAar {
    repackages.put("coil", "repackage.coil")
}
```

> **Note:** Shadowing configuration is an optional step. There is no "default" shadowing configuration. Skipping this step will make the dependencies just embedded, so the purpose of using this plugin will be lost.

### Step 5: Use shadowed implementations

The plugin is applied before any compilation gradle tasks. As it swaps original packages with shadowed ones, you have to update your source code to use the shadow implementation.

```kotlin
// before
import coil.ImageLoader
// after
import repackage.coil.ImageLoader
// but no other changes in use
val loader = ImageLoader.Builder(context).build()
```

> **Note:** Yes, 'shadowing idea' could be applied after compilation gradle tasks, but you may face runtime issues if the plugin does not map something correctly. For current plugin behavior, you are working with "final result" and are able to re-implement your code to fix any compilation problems.

### Step 6: Additional changes for Android AARs

Some dependencies as AAR may contain Android resources that have to be merged with your application.

Library `io.coil-kt:coil-base` contains an Android resource named `coil_request_manager`. Coil implementation is accessing this resource as `coil.base.R.id.coil_request_manager` but as this is not merged into your project (yet) it has to be done manually:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <item name="coil_request_manager" type="id"/>
</resources>
```

But Coil is still referencing to `coil.base.R.id.coil_request_manager` (or `repackage.coil.base.R.id.coil_request_manager` after shadowing) and there is no such resource. Therefore, you have to configure your R class path to the shadow configuration:

```groovy
shadowAar {
    transformR = "com.example.app.R"
    repackages.put("coil", "repackage.coil")
}
```

> **Note**: Configuration of `transformR` is applied to all shadowed dependencies. The plugin implements "auto-detection" of using R-classes in libraries and shadows them efficiently.<br>
> Please be aware of other Android resources as drawables, layouts, etc. All these resources have to be manually copied into your Android project.

## Next steps for the plugin

As the motivation of this project tries to describe, we would like to bring full Android-AAR support into the plugin.<br>
Our next goal is the support of:

1. Automatically mergeing Android resources (drawable, layout, ...) into you project
   1. Check `Step 6`
2. Re-compiling kotlin_module files (probably possible with Kotlin2.0) after shadowing
   1. Check `Step 5` - that's why it is better to face compilation problems in your source code rather that investigating runtime issues.

## Thanks
This plugin would not be possible without the large effort of [ShadowJar](https://github.com/GradleUp/shadow) – thank you and all your contributors!