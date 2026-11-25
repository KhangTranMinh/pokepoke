# Build setup chi tiết

> Đọc cùng [plan.md](../plan.md) mục 2–3. File này là **nguồn chuẩn** cho version thư viện, plugin, convention plugin.

## 1. Version (đã kiểm tra trên Maven ngày 2026-11-23, bản stable mới nhất)

```toml
[versions]
# SDK / JVM
compileSdk = "37"
compileSdkMinor = "1"   # compileSdk 37.1: Compose UI 1.13.0 (BOM 2026.11.01) yêu cầu; plugin dùng release(compileSdk) { minorApiLevel = compileSdkMinor }
minSdk = "30"
targetSdk = "37"
jvm = "21"

# Build
agp = "9.5.2"
kotlin = "2.4.21"
ksp = "2.3.14"
hilt = "2.60.1"
room = "2.8.5"
kover = "0.9.11"
roborazzi = "1.79.0"

# AndroidX
coreKtx = "1.19.1"
coreSplashscreen = "1.2.0"
activityCompose = "1.13.0"
appcompat = "1.8.0"
lifecycle = "2.11.1"
composeBom = "2026.11.01"
materialIconsCore = "1.7.8"
material3Adaptive = "1.3.0"
navigation3 = "1.2.2"
hiltLifecycleViewmodelCompose = "1.4.0"
datastore = "1.3.0"
startup = "1.2.0"

# Kotlin / network / image / security
coroutines = "1.11.0"
javaxInject = "1"        # @Qualifier / @Inject cho module Kotlin/JVM thuần (core:common-kotlin)
serialization = "1.13.0"
retrofit = "3.0.0"
okhttp = "5.5.1"
coil = "3.6.3"
tink = "1.24.0"

# Test
junit = "4.13.2"
androidxTestJunit = "1.3.0"
espresso = "3.7.0"
turbine = "1.2.1"
mockk = "1.14.12"
robolectric = "4.18.1"
```

### 1.1. Thư viện (artifact)

| Alias gợi ý | Artifact |
|---|---|
| androidx-core-ktx | `androidx.core:core-ktx` |
| androidx-core-splashscreen | `androidx.core:core-splashscreen` |
| androidx-activity-compose | `androidx.activity:activity-compose` |
| androidx-appcompat | `androidx.appcompat:appcompat` |
| androidx-lifecycle-runtime-compose | `androidx.lifecycle:lifecycle-runtime-compose` |
| androidx-lifecycle-viewmodel-compose | `androidx.lifecycle:lifecycle-viewmodel-compose` |
| androidx-lifecycle-viewmodel-navigation3 | `androidx.lifecycle:lifecycle-viewmodel-navigation3` |
| androidx-compose-bom | `androidx.compose:compose-bom` |
| androidx-compose-ui / ui-graphics / ui-tooling / ui-tooling-preview / ui-test-junit4 / ui-test-manifest | `androidx.compose.ui:*` (version từ BOM) |
| androidx-compose-foundation | `androidx.compose.foundation:foundation` (BOM) |
| androidx-compose-material3 | `androidx.compose.material3:material3` (BOM) |
| androidx-compose-material3-navigation-suite | `androidx.compose.material3:material3-adaptive-navigation-suite` (BOM) |
| androidx-compose-material-icons-core | `androidx.compose.material:material-icons-core` (`materialIconsCore`) |
| androidx-compose-material3-adaptive | `androidx.compose.material3.adaptive:adaptive` |
| androidx-compose-material3-adaptive-layout | `androidx.compose.material3.adaptive:adaptive-layout` |
| androidx-compose-material3-adaptive-navigation3 | `androidx.compose.material3.adaptive:adaptive-navigation3` |
| androidx-navigation3-runtime / ui | `androidx.navigation3:navigation3-runtime`, `navigation3-ui` |
| androidx-hilt-lifecycle-viewmodel-compose | `androidx.hilt:hilt-lifecycle-viewmodel-compose` |
| androidx-datastore-preferences | `androidx.datastore:datastore-preferences` |
| androidx-startup | `androidx.startup:startup-runtime` |
| androidx-room-runtime / ktx / compiler / testing | `androidx.room:*` |
| hilt-android / hilt-compiler / hilt-android-testing | `com.google.dagger:*` |
| javax-inject | `javax.inject:javax.inject` |
| kotlinx-coroutines-core / android / test | `org.jetbrains.kotlinx:kotlinx-coroutines-*` |
| kotlinx-serialization-json | `org.jetbrains.kotlinx:kotlinx-serialization-json` |
| retrofit / retrofit-converter-kotlinx-serialization | `com.squareup.retrofit2:retrofit`, `converter-kotlinx-serialization` |
| okhttp / okhttp-mockwebserver | `com.squareup.okhttp3:okhttp`, `mockwebserver3` |
| coil-compose / coil-network-okhttp | `io.coil-kt.coil3:*` |
| tink-android | `com.google.crypto.tink:tink-android` |
| junit, androidx-test-junit, espresso-core, turbine, mockk, robolectric | như tên |
| roborazzi / roborazzi-compose / roborazzi-junit-rule | `io.github.takahirom.roborazzi:*` |

### 1.2. Plugin

| Alias | Id | Version |
|---|---|---|
| android-application | `com.android.application` | agp |
| android-library | `com.android.library` | agp |
| kotlin-jvm | `org.jetbrains.kotlin.jvm` | kotlin |
| kotlin-compose | `org.jetbrains.kotlin.plugin.compose` | kotlin |
| kotlin-serialization | `org.jetbrains.kotlin.plugin.serialization` | kotlin |
| ksp | `com.google.devtools.ksp` | ksp |
| hilt | `com.google.dagger.hilt.android` | hilt |
| room | `androidx.room` | room |
| kover | `org.jetbrains.kotlinx.kover` | kover |
| roborazzi | `io.github.takahirom.roborazzi` | roborazzi |
| pokepoke-* | convention plugin (mục 3) | `unspecified` |

### 1.3. Xử lý khi combo version không build được (không hỏi lại, làm theo thứ tự)

AGP 9 có Kotlin tích hợp sẵn (built-in Kotlin) với KGP đi kèm. Compose compiler plugin và serialization plugin **phải cùng version với KGP thực sự được dùng**.

1. Thử như bảng trên: khai báo `kotlin-compose`, `kotlin-serialization` (`apply false`) ở root `build.gradle.kts` để KGP 2.4.21 nằm trên classpath.
2. Nếu Gradle báo xung đột KGP / built-in Kotlin: thêm `buildscript { dependencies { classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:<kotlin>") } }` ở root.
3. Nếu vẫn lỗi: hạ `kotlin` về version KGP mà AGP 9.5.2 đi kèm (xem `./gradlew buildEnvironment`), hạ các thư viện báo lỗi "compiled with newer Kotlin" về bản stable gần nhất tương thích.
4. Nếu API của một thư viện khác với mô tả trong docs (vd. Navigation 3, Material3 Adaptive): **theo API thật của version đã pin** (đọc sources jar / KDoc), giữ nguyên ý nghĩa thiết kế.
5. Ghi mọi thay đổi version / lệch API vào [`docs/decisions.md`](../decisions.md).

## 2. `settings.gradle.kts`

```kotlin
pluginManagement {
    includeBuild("build-logic")
    repositories { google(); mavenCentral(); gradlePluginPortal() }   // giữ content filter của google() như hiện tại
}
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
// dependencyResolutionManagement giữ nguyên
rootProject.name = "PokePoke"     // đổi từ "Poke Poke" (dấu cách gây khó chịu cho một số tool)
include(":app", ":debugtool", ":core:common-kotlin", ":core:common-android", ...)  // đủ danh sách ở plan.md mục 4
```

## 3. Convention plugins (`build-logic/convention`)

- `build-logic/settings.gradle.kts` dùng chung version catalog: `versionCatalogs { create("libs") { from(files("../gradle/libs.versions.toml")) } }`.
- `build-logic/convention/build.gradle.kts`: `kotlin-dsl`, `compileOnly` AGP, KGP, compose compiler gradle plugin, KSP, Room, Hilt, Kover, Roborazzi gradle plugins.
- Đọc version trong plugin: `extensions.getByType<VersionCatalogsExtension>().named("libs")`.
- Mỗi plugin là một class Kotlin; đăng ký trong `gradlePlugin { plugins { register(...) } }`; thêm alias vào `[plugins]` của catalog với `version = "unspecified"` hoặc dùng `id("pokepoke.xxx")`.

| Plugin id | Class | Áp dụng / cấu hình |
|---|---|---|
| `pokepoke.android.application` | `AndroidApplicationConventionPlugin` | `com.android.application`; SDK từ catalog; `jvmToolchain(jvm)`; version từ `version.properties` (mục 4); buildTypes `debug` (`applicationIdSuffix = ".debug"`, `versionNameSuffix = "-debug"`) và `release` (giữ `optimization { enable = true }` hiện có; ký bằng debug keystore tạm thời); `buildFeatures.buildConfig = true`; `androidResources.generateLocaleConfig = true`; task `verifyReleaseHasNoDebugTool` (mục 5) |
| `pokepoke.android.library` | `AndroidLibraryConventionPlugin` | `com.android.library`; SDK; `jvmToolchain(jvm)`; `testOptions.unitTests.isIncludeAndroidResources = true`; test deps chung (junit, mockk, turbine, coroutines-test, robolectric, androidx-test-junit, `testImplementation(project(":core:testing"))` trừ `:core:testing` và `:translation`); áp dụng `pokepoke.kover`; task `verifyModuleDependencies` (mục 5) |
| `pokepoke.android.compose` | `AndroidComposeConventionPlugin` | `org.jetbrains.kotlin.plugin.compose`; `buildFeatures.compose = true`; BOM cho implementation/testImplementation/androidTestImplementation; ui, ui-graphics, foundation, material3, material-icons-core, ui-tooling-preview; `debugImplementation` ui-tooling, ui-test-manifest; `testImplementation` ui-test-junit4; Roborazzi plugin + roborazzi, roborazzi-compose, roborazzi-junit-rule; `roborazzi { outputDir = file("src/test/screenshots") }` |
| `pokepoke.android.feature.api` | `AndroidFeatureApiConventionPlugin` | `pokepoke.android.library`; `android.testFixtures.enable = true`; `api(core:common-kotlin)`, `api(core:navigation)`; `kotlinx-coroutines-core` |
| `pokepoke.android.feature.impl` | `AndroidFeatureImplConventionPlugin` | `pokepoke.android.library` + `pokepoke.android.compose` + `pokepoke.hilt`; `implementation` core:designsystem, core:ui, core:common-kotlin, core:navigation, translation, lifecycle-runtime-compose, lifecycle-viewmodel-compose, hilt-lifecycle-viewmodel-compose, navigation3-runtime, material3-adaptive, material3-adaptive-layout, material3-adaptive-navigation3; `testImplementation(testFixtures(core:navigation))` |
| `pokepoke.hilt` | `HiltConventionPlugin` | `com.google.devtools.ksp` + `com.google.dagger.hilt.android`; `implementation` hilt-android; `ksp` hilt-compiler; `testImplementation` hilt-android-testing; `kspTest` hilt-compiler |
| `pokepoke.room` | `RoomConventionPlugin` | `androidx.room` + ksp; `room { schemaDirectory("$projectDir/schemas") }`; room-runtime, room-ktx, `ksp` room-compiler; `testImplementation` room-testing |
| `pokepoke.kotlin.serialization` | `KotlinSerializationConventionPlugin` | `org.jetbrains.kotlin.plugin.serialization`; `implementation` kotlinx-serialization-json |
| `pokepoke.jvm.library` | `JvmLibraryConventionPlugin` | `org.jetbrains.kotlin.jvm` + `java-test-fixtures`; `jvmToolchain(jvm)`; `pokepoke.kover`; task `verifyModuleDependencies`; junit, coroutines-test, turbine, mockk (không có `:core:testing` vì đó là Android library) |
| `pokepoke.kover` | `KoverConventionPlugin` | Kover; exclude generated (mục 6); verify rule cho module thuộc nhóm bắt buộc |

Ghi chú:
- AGP 9 built-in Kotlin: **không** apply `org.jetbrains.kotlin.android`.
- Nếu `testFixtures` Kotlin cần cờ: thêm `android.experimental.enableTestFixturesKotlinSupport=true` vào `gradle.properties`.
- Module `:api` là **Kotlin/JVM thuần** (`pokepoke.jvm.library`) khi không cần Android: `core:storage:api`, `core:securestorage:api`, `data:pokemon:api`, `core:common-kotlin`. Các module này không được chứa `@Inject` class / Hilt module (KSP Hilt không chạy ở đây).
- Giữ Android library cho `core:navigation` (`@StringRes`, `ImageVector`, Compose; không tách api/impl), `core:deeplink` (`PendingIntent`, phụ thuộc `core:navigation`; không tách api/impl) và `feature:*:api`.

## 4. `version.properties`

```properties
versionMajor=1
versionMinor=0
versionPatch=0
versionCode=1
```

Plugin application đọc qua `providers.fileContents(rootProject.layout.projectDirectory.file("version.properties")).asText` → parse `Properties`. Thiếu key → fail build với message rõ ràng. `versionName = "$major.$minor.$patch"`.

## 5. Check kiến trúc

**Task `verifyModuleDependencies`** (mọi module Android/JVM, hook vào `preBuild` và `check`). Input: danh sách project path mà module khai báo trong mọi configuration (`implementation`, `api`, `compileOnly`, `runtimeOnly`, `debugImplementation`, `releaseImplementation`, `testImplementation`, `testFixturesImplementation`, `testFixturesApi`, `androidTestImplementation`, `ksp`), thu thập lúc cấu hình thành `List<String>` (tương thích configuration cache). Luật (vi phạm → fail với message chỉ rõ module + dependency):

| Module hiện tại | Không được phụ thuộc |
|---|---|
| bất kỳ, trừ `:app` | `:*:impl`, `:debugtool` |
| `:core:*` | `:data:*`, `:feature:*` |
| `:data:*` | `:feature:*` |
| `:debugtool` | `:data:*`, `:feature:*` |
| `:core:testing` | `:*:impl` |
| `:translation` | mọi module khác (chỉ chứa resource, không phụ thuộc gì; mọi module được phụ thuộc vào nó) |

Ngoại lệ: `testFixtures(project(X))` được tính như phụ thuộc vào X (áp luật bình thường).

**Task `verifyReleaseHasNoDebugTool`** (chỉ `:app`, hook vào `check` và `assembleRelease`): duyệt `releaseRuntimeClasspath.incoming.resolutionResult.allComponents`, fail nếu có `ProjectComponentIdentifier` với `projectPath == ":debugtool"`.

## 6. Kover

- Áp dụng cho mọi module; root aggregate (`kover(project(":x"))` cho tất cả) → `./gradlew koverHtmlReport`.
- Exclude: `*_Factory`, `*_MembersInjector`, `*_HiltModules*`, `Hilt_*`, `*_Impl` (Room), `*_Impl$*`, `*ComposableSingletons*`, `*.BuildConfig`, `*Module` / `*Module$*` (Hilt module), `*RouteKt` (composable root — xem screens.md), `*PreviewKt`, `*.di.*`.
- **Verify rule** `minBound(70)` (line) cho: `:core:common-kotlin`, `:core:common-android`, `:core:network`, `:core:connectivity`, `:core:navigation`, `:core:deeplink`, `:core:*:impl`, `:data:*:impl`, `:feature:*:impl`, `:debugtool`. Module khác không đặt ngưỡng.

## 7. Roborazzi / Robolectric

- `src/test/resources/robolectric.properties`: `sdk=36` (nếu Robolectric 4.18.1 báo không hỗ trợ, dùng mức cao nhất được hỗ trợ) và `graphicsMode=NATIVE`.
- Ghi ảnh: `./gradlew recordRoborazziDebug`; kiểm tra: `./gradlew verifyRoborazziDebug`. Ảnh commit vào `src/test/screenshots/` của từng module.
- Thiết bị mặc định: `@Config(qualifiers = "w411dp-h891dp-xxhdpi")`; ảnh tablet/foldable mở: `w841dp-h701dp-xhdpi`.

## 8. `gradle.properties`

Thêm:

```properties
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8
ksp.incremental=false   # KSP 2.3.14 + AGP 9 lỗi "Number of loaded files in snapshots differs" (decisions.md, T08)
```

Giữ nguyên `org.gradle.configuration-cache=true` và `kotlin.code.style=official` có sẵn.

## 9. Lệnh kiểm tra chuẩn

| Mục đích | Lệnh |
|---|---|
| Build debug + release | `./gradlew assembleDebug assembleRelease` |
| Test + check kiến trúc + lint một module | `./gradlew :<module>:check` |
| Toàn bộ | `./gradlew check verifyRoborazziDebug koverVerify` |
| Coverage report | `./gradlew koverHtmlReport` |
