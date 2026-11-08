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
