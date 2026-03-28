pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "verdant"

include(":app")

include(":core:model")
include(":core:database")
include(":core:datastore")
include(":core:network")
include(":core:ai")
include(":core:common")
include(":core:sms")
include(":core:designsystem")
include(":core:health")
include(":core:devicestats")
include(":core:prediction")
include(":core:emotional")
include(":core:geofence")
include(":core:context")
include(":core:voice")
include(":core:social")
include(":core:sync")
include(":core:supabase")

include(":feature:home")
include(":feature:habits")
include(":feature:analytics")
include(":feature:insights")
include(":feature:settings")
include(":feature:finance")
include(":feature:lifedashboard")

include(":widget")
include(":work")
