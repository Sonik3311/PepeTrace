import org.gradle.internal.os.OperatingSystem
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Properties

plugins {
    id("java")
    application
}

group = "org.pepetrace"
version = "1.0-SNAPSHOT"

application {
    mainClass = "$group.Main"
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(26)
    }
}

// Версии библиотек
val lwjglVersion by extra("3.4.1")
val imguiVersion by extra("1.90.0")

// Определение natives в зависимости от ОС
val lwjglNatives: String by extra {
    when (OperatingSystem.current()) {
        OperatingSystem.LINUX -> {
            val base = "natives-linux"
            val osArch = System.getProperty("os.arch")
            when {
                osArch.startsWith("arm") || osArch.startsWith("aarch64") -> {
                    base + if (osArch.contains("64") || osArch.startsWith("armv8")) "-arm64" else "-arm32"
                }
                osArch.startsWith("ppc") -> "$base-ppc64le"
                osArch.startsWith("riscv") -> "$base-riscv64"
                else -> base
            }
        }
        OperatingSystem.WINDOWS -> "natives-windows"
        else -> error("Unsupported operating system")
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))

    implementation("org.joml:joml:1.10.5")

	implementation("org.lwjgl:lwjgl")
	implementation("org.lwjgl:lwjgl-assimp")
	implementation("org.lwjgl:lwjgl-glfw")
	implementation("org.lwjgl:lwjgl-opengl")
	implementation("org.lwjgl:lwjgl-stb")
	implementation("org.lwjgl:lwjgl::$lwjglNatives")
	implementation("org.lwjgl:lwjgl-assimp::$lwjglNatives")
	implementation("org.lwjgl:lwjgl-glfw::$lwjglNatives")
	implementation("org.lwjgl:lwjgl-opengl::$lwjglNatives")
	implementation("org.lwjgl:lwjgl-stb::$lwjglNatives")

	implementation ("io.github.spair:imgui-java-binding:$imguiVersion")
    implementation ("io.github.spair:imgui-java-lwjgl3:$imguiVersion")
    implementation ("io.github.spair:imgui-java-$lwjglNatives:$imguiVersion")
}


abstract class GenerateBuildPassportTask : DefaultTask() {
    @get:Input
    abstract val projectName: Property<String>

    @get:Input
    abstract val gitCommitHash: Property<String>

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val projectDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val properties = Properties()

        // Получаем информацию о пользователе и системе
        val userName = System.getenv("USER") ?: System.getenv("USERNAME") ?: "unknown"
        properties.setProperty("build.user", userName)

        properties.setProperty("build.os", System.getProperty("os.name"))
        properties.setProperty("build.java.version", System.getProperty("java.version"))

        // Форматируем время сборки
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val buildTime = LocalDateTime.now()
        properties.setProperty("build.time", buildTime.format(formatter))

        // Получаем и инкрементируем номер сборки
        val buildNumber = getNextBuildNumber()
        properties.setProperty("build.number", buildNumber.toString())

        // Добавляем хеш коммита
        properties.setProperty("build.git.commit", gitCommitHash.get())

        properties.setProperty("build.message", "Project Build ${projectName.get()}")

        // Создаем директорию для выходного файла
        val outputDir = projectDir.get().asFile.toPath().resolve("src/main/resources").toFile()
        outputDir.mkdirs()

        // Сохраняем файл с паспортом сборки
        val file = File(outputDir, "build-passport.properties")
        file.outputStream().use { properties.store(it, "Build Passport") }

        // Сохраняем текущий номер сборки для следующего раза
        saveBuildNumber(buildNumber)

        println("Build passport generated: ${file.absolutePath}")
        println("Содержит: user=${userName}, os=${System.getProperty("os.name")}, " +
                "time=${buildTime.format(formatter)}, buildNumber=${buildNumber}, " +
                "gitCommit=${gitCommitHash.get()}")
    }

    private fun getNextBuildNumber(): Int {
        val buildNumberFile = File(projectDir.get().asFile, "build-number.properties")
        val props = Properties()

        if (buildNumberFile.exists()) {
            buildNumberFile.inputStream().use { props.load(it) }
        }

        val currentNumber = props.getProperty("build.number", "0").toInt()
        return currentNumber + 1
    }

    private fun saveBuildNumber(buildNumber: Int) {
        val buildNumberFile = File(projectDir.get().asFile, "build-number.properties")
        val props = Properties()
        props.setProperty("build.number", buildNumber.toString())
        buildNumberFile.outputStream().use { props.store(it, "Build Number Tracker") }
    }
}

tasks.register<GenerateBuildPassportTask>("generateBuildPassport") {
    projectName.set(project.name)
    projectDir.set(project.projectDir)

    // Получаем хеш последнего коммита
    gitCommitHash.set(provider {
        try {
            val process = ProcessBuilder("git", "rev-parse", "--short", "HEAD")
                .directory(project.rootDir)
                .redirectErrorStream(true)
                .start()

            process.inputStream.bufferedReader().readText().trim()
                .takeIf { it.isNotEmpty() }
                ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    })
}

tasks.named("compileJava") {
    dependsOn(tasks.named("generateBuildPassport"))
}

tasks.named("processResources") {
    dependsOn(tasks.named("generateBuildPassport"))
}

tasks.named("processTestResources") {
    mustRunAfter(tasks.named("generateBuildPassport"))
}