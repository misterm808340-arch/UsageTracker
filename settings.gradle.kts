pluginManagement {
    repositories {
        // Huawei mirror first: serves google + central artifacts and handles
        // HEAD checks correctly (aliyun/google returns 502 on HEAD, mavenCentral
        // is currently rate-limiting HEAD requests).
        maven("https://repo.huaweicloud.com/repository/maven")
        maven("https://maven.aliyun.com/repository/central")
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven("https://repo.huaweicloud.com/repository/maven")
        maven("https://maven.aliyun.com/repository/central")
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        google()
        mavenCentral()
    }
}

rootProject.name = "UsageTracker"
include(":app")
