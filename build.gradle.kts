plugins {
  java
  kotlin("jvm") version "1.3.72"
  id("org.jetbrains.intellij") version "0.4.18"
}

group = "dev.turingcomplete"
version = "0.0.1"

repositories {
  mavenCentral()
}

intellij {
  version = "2020.1"
  updateSinceUntilBuild = false
  setPlugins("java", "gradle")
}

dependencies {
  implementation(kotlin("stdlib-jdk8"))

  val junitVersion = "5.6.2"
  testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
  testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}

tasks.test {
  useJUnitPlatform()
}

configure<JavaPluginConvention> {
  sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks {
  compileKotlin {
    kotlinOptions.jvmTarget = "1.8"
  }
  compileTestKotlin {
    kotlinOptions.jvmTarget = "1.8"
  }
}