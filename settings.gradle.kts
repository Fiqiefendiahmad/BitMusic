pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // NewPipeExtractor is published via JitPack.
        maven("https://jitpack.io")
    }
}

rootProject.name = "BitMusic"

// Desktop modules (original targets)
include(":shared")
include(":sharedUi")
include(":desktopUi")
include(":desktopApp")

// Android small-screen redesign (fork addition). Requires Android SDK + local.properties sdk.dir.
if (file("androidApp").isDirectory) {
    include(":androidApp")
}
