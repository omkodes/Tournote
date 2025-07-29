// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // Ensure you have the Android Gradle Plugin
        classpath("com.android.tools.build:gradle:8.1.0")

        // IMPORTANT: Ensure the Kotlin Gradle Plugin is here with a specific version
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.0")

        // Google Services plugin for Firebase
        classpath("com.google.gms:google-services:4.3.15")
    }
}
