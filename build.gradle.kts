// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.1.4" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
    id("com.google.gms.google-services") version "4.4.0" apply false
}

val mapkitApiKey by extra(getMapkitKey())

fun getMapkitKey(): String {
    val properties = java.util.Properties()
    project.file("local.properties").inputStream().apply {
        properties.load(this)
    }
    return properties.getProperty("MAPKIT_API_KEY", "")
}