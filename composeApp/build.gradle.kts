import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation("org.xerial:sqlite-jdbc:3.45.1.0")
            implementation("org.slf4j:slf4j-simple:1.7.36")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}

compose.desktop {
    application {
        mainClass = "org.einfachirgendwa1.jugendForscht.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "org.einfachirgendwa1.jugendForscht"
            packageVersion = "1.0.2"
        }
    }
}

fun isWindows() = System.getProperty("os.name").lowercase().contains("windows")

val receiverName = "jugend-forscht-receiver${if (isWindows()) ".exe" else ""}"
val receiverFile = rootProject.file("jugend-forscht-receiver/target/release/$receiverName")!!
val resourcesDir = "src/jvmMain/resources"

val buildReceiver by tasks.registering(Exec::class) {
    workingDir = rootProject.file("jugend-forscht-receiver")
    commandLine("cargo", "build", "--release")
    outputs.file(receiverFile)
    outputs.upToDateWhen { false }
}

val generateReceiver by tasks.registering(Copy::class) {
    dependsOn(buildReceiver)

    from(receiverFile)
    into(resourcesDir)

    outputs.file(File(resourcesDir, receiverName))
}

tasks.named("jvmProcessResources") {
    dependsOn(generateReceiver)
}