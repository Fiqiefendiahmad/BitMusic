import java.io.File

// Auto-provision local.properties and platforms;android-37 in CI environments
val detectedSdk: File? = run {
    val candidates = listOf(
        System.getenv("ANDROID_HOME")?.takeIf { it.isNotBlank() },
        System.getenv("ANDROID_SDK_ROOT")?.takeIf { it.isNotBlank() },
        "/usr/local/lib/android/sdk",
        "${System.getProperty("user.home")}/Android/Sdk",
        "${System.getProperty("user.home")}/android-sdk",
    )
    candidates.filterNotNull().map { File(it) }.firstOrNull { it.isDirectory }
}

if (detectedSdk != null) {
    val localProps = file("local.properties")
    if (!localProps.exists()) {
        localProps.writeText("sdk.dir=${detectedSdk.absolutePath.replace("\\", "/")}\n")
    }
    val platform37 = File(detectedSdk, "platforms/android-37")
    if (!platform37.exists()) {
        val sdkmanager = listOf(
            File(detectedSdk, "cmdline-tools/latest/bin/sdkmanager"),
            File(detectedSdk, "cmdline-tools/bin/sdkmanager"),
            File(detectedSdk, "tools/bin/sdkmanager"),
        ).firstOrNull { it.exists() && it.canExecute() }
        if (sdkmanager != null) {
            runCatching {
                val proc = ProcessBuilder(sdkmanager.absolutePath, "platforms;android-37")
                    .redirectErrorStream(true)
                    .start()
                proc.outputStream.bufferedWriter().use { w ->
                    repeat(10) { w.write("y\n") }
                    w.flush()
                }
                proc.waitFor()
            }
        }
    }
}

pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral(); maven("https://jitpack.io") }
}
rootProject.name = "BitMusic"

// Desktop modules
include(":shared")
include(":sharedUi")
include(":desktopUi")
include(":desktopApp")

// Android module
include(":androidApp")
