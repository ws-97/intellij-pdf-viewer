plugins {
  kotlin("multiplatform")
  kotlin("plugin.serialization")
}

val kotlinVersion: String by project
val kotlinxSerializationJsonVersion: String by project

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
}

kotlin {
  jvm {
    compilerOptions {
      jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
  }
  js(IR) {
    browser {
      binaries.executable()
    }
  }
  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(kotlin("stdlib-common"))
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationJsonVersion")
      }
    }
  }
}
