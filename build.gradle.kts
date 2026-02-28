import org.gradle.internal.os.OperatingSystem

// Версии библиотек
val lwjglVersion by extra("3.4.1")
val imguiVersion by extra("1.90.0")

// Ставим переменную lwjglNatives.
// Просто смотрим, на какой системе собираем (линукс и виндовс),
// и ставим нужное значение в переменную (natives-linux, natives-windows и суффиксы armv8, arm64, arm32, ppc64le, riscv)
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

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")


    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))

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
