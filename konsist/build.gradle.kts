plugins {
    alias(pokepoke.plugins.pokepoke.jvm.library)
}

dependencies {
    testImplementation(libs.konsist)
}

// Konsist đọc source của cả repo qua file system, nên Gradle không biết test phụ thuộc vào những file đó.
// Khai báo chúng là input để sửa source ở module khác thì `:konsist:test` chạy lại (không "up-to-date").
val repoRoot = rootProject.layout.projectDirectory
val mainSources = rootProject.fileTree(repoRoot) {
    include("**/src/main/kotlin/**/*.kt")
    exclude("**/build/**", "**/.gradle/**")
}

tasks.test {
    inputs.files(mainSources)
        .withPropertyName("repoMainSources")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}
