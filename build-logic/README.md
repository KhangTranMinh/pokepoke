# build-logic: convention plugin `pokepoke.*`

Bộ convention plugin Gradle cho project Android nhiều module (AGP 9, Kotlin, Compose, Hilt, Room, Kover, Roborazzi).
Một module chỉ cần `plugins { alias(...) }`; SDK, JVM, test dependency, kiểm tra kiến trúc, flavor, signing,
config theo môi trường do plugin cấu hình. File này tự đọc hiểu được: không cần tài liệu nào khác của repo gốc.

## 1. Plugin có gì

| Plugin id | Áp dụng / cấu hình |
|---|---|
| `pokepoke.android.application` | `com.android.application`; SDK + JVM toolchain từ catalog; `versionName` / `versionCode` từ `version.properties`; build type `debug` (`.debug`, `-debug`) và `release` (R8 bật); flavor `dev` / `qa` / `prod`; signing; `BuildConfig` + manifest placeholder từ `configs/env/`; task `verifyModuleDependencies`, `verifyReleaseHasNoDebugTool` |
| `pokepoke.android.library` | `com.android.library`; SDK + JVM; test dependency chung (junit, mockk, turbine, coroutines-test, robolectric, androidx-test-junit + module trong `[library].testImplementation`); áp `pokepoke.kover`; task `verifyModuleDependencies` |
| `pokepoke.android.compose` | Compose compiler plugin + `buildFeatures.compose`; Compose BOM, ui, foundation, material3, icons, tooling; Roborazzi (ảnh vào `src/test/screenshots`) |
| `pokepoke.android.feature.api` | `pokepoke.android.library` + test fixtures; `api` các module trong `[feature.api]` + coroutines-core |
| `pokepoke.android.feature.impl` | `pokepoke.android.library` + `pokepoke.android.compose` + `pokepoke.hilt`; `implementation` các module trong `[feature.impl]` + lifecycle, Navigation 3, Material3 Adaptive; test fixtures của `[feature.impl].testFixtures` |
| `pokepoke.hilt` | KSP + Hilt: `hilt-android`, `ksp(hilt-compiler)`, `hilt-android-testing`, `kspTest(hilt-compiler)` |
| `pokepoke.room` | Room + KSP: runtime, ktx, `ksp(room-compiler)`, `room-testing`; schema ở `<module>/schemas` |
| `pokepoke.kotlin.serialization` | Plugin kotlinx serialization + `kotlinx-serialization-json` |
| `pokepoke.jvm.library` | `org.jetbrains.kotlin.jvm` + `java-test-fixtures`; JVM toolchain; test dependency chung; `pokepoke.kover`; `verifyModuleDependencies` |
| `pokepoke.kover` | Kover: loại class sinh tự động khỏi báo cáo; luật ngưỡng line coverage cho module khớp `[kover].modules` |

Quy ước: AGP 9 đã có Kotlin tích hợp, plugin **không** áp `org.jetbrains.kotlin.android`.

## 2. Yêu cầu

- Gradle 9.6.0 (`gradle/wrapper/gradle-wrapper.properties`).
- AGP 9.5.2, Kotlin 2.4.21, JDK 21 cho toolchain của project (`jvm = "21"` trong catalog; cần JDK 21 cài sẵn hoặc plugin
  `org.gradle.toolchains.foojay-resolver-convention` trong `settings.gradle.kts` để Gradle tự tải). Bản thân `build-logic` biên dịch ở JVM 17.
- Android SDK có platform `compileSdk = 37`, minor `1` (API 37.1), đặt `sdk.dir` trong `local.properties` hoặc biến `ANDROID_HOME`.
- Các Gradle plugin dưới đây là `compileOnly` trong `build-logic`: **project dùng phải tự khai báo** chúng trên classpath
  (qua catalog ở bước 3.1 và `plugins { ... apply false }` ở `build.gradle.kts` gốc): AGP, Kotlin (KGP), Kotlin Compose compiler,
  KSP, Hilt, Room, Kover, Roborazzi. Thiếu plugin nào thì plugin pokepoke dùng nó báo `ClassNotFoundException` / `Plugin ... was not found`.
  `tomlj` (đọc file TOML) thì `build-logic` tự mang theo.

## 3. Setup từng bước

Ví dụ dưới đây tạo project mới `Demo` có `:app` và `:debugtool`; `<REPO>` là đường dẫn tới thư mục chứa `build-logic/` này.

### 3.1. Catalog `pokepoke` và `includeBuild`

Sao chép catalog `pokepoke.versions.toml` (trong thư mục `gradle/` cạnh `build-logic/`) vào `gradle/pokepoke.versions.toml` của project:
nó chứa version SDK / JVM, artifact các Gradle plugin, thư viện mà plugin tự thêm và alias của chính các plugin `pokepoke.*`.
Thư viện riêng của project (retrofit, coil...) để ở catalog `libs` (`gradle/libs.versions.toml`) của bạn; **một artifact chỉ nằm ở một catalog**.

`settings.gradle.kts`:

```kotlin
pluginManagement {
    includeBuild("<REPO>/build-logic")   // hôm nay dùng includeBuild; Maven: xem mục 7
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("pokepoke") {
            from(files("gradle/pokepoke.versions.toml"))
        }
    }
}

rootProject.name = "Demo"
include(":app", ":debugtool")
```

`build.gradle.kts` gốc (đưa Gradle plugin lên classpath, mục 2):

```kotlin
plugins {
    alias(pokepoke.plugins.android.application) apply false
    alias(pokepoke.plugins.android.library) apply false
    alias(pokepoke.plugins.kotlin.jvm) apply false
    alias(pokepoke.plugins.kotlin.compose) apply false
    alias(pokepoke.plugins.kotlin.serialization) apply false
    alias(pokepoke.plugins.ksp) apply false
    alias(pokepoke.plugins.hilt) apply false
    alias(pokepoke.plugins.room) apply false
    alias(pokepoke.plugins.kover) apply false
    alias(pokepoke.plugins.roborazzi) apply false
}
```

`app/build.gradle.kts`:

```kotlin
plugins {
    alias(pokepoke.plugins.pokepoke.android.application)
}

android {
    namespace = "com.example.demo"
    defaultConfig { applicationId = "com.example.demo" }
}

dependencies {
    // Công cụ debug chỉ cho dev / qa, không bao giờ cho prod (mục 5).
    "devImplementation"(project(":debugtool"))
    "qaImplementation"(project(":debugtool"))
}
```

Module khác: `alias(pokepoke.plugins.pokepoke.android.library)`, `...pokepoke.android.feature.impl`, ... Thư viện mà plugin đã thêm
sẵn không cần khai báo lại; thư viện khác thì `implementation(libs.xxx)` (catalog của bạn) hoặc `implementation(pokepoke.junit)` (catalog `pokepoke`).

Lưu ý: `:core:*` là phần dùng chung giữa các project (sẽ publish được), nên chỉ dùng catalog `pokepoke` và `coreLibs` (thư viện ngoài của core, `build-logic` không đọc), không dùng `libs.`; thư viện riêng của app mới đặt ở `libs`.

Cần thêm cho `:app`:

- `app/src/main/AndroidManifest.xml` (có `<application>`) và `app/src/main/res/resources.properties` chứa `unqualifiedResLocale=en-US`
  (plugin bật `generateLocaleConfig`, AGP đòi file này).
- `version.properties` ở thư mục gốc:

  ```properties
  versionMajor=1
  versionMinor=0
  versionPatch=0
  versionCode=1
  ```

  `versionName` = `major.minor.patch` (cộng hậu tố flavor / build type, ví dụ `1.0.0-dev-debug`).
- `debugtool/build.gradle.kts` (module công cụ debug; ví dụ này để trống, thực tế là code debug của bạn). Mỗi project trong
  `include(...)` phải có thư mục thật, và phải áp một plugin Android / JVM để `:app` phụ thuộc được vào nó:

  ```kotlin
  plugins {
      alias(pokepoke.plugins.pokepoke.android.library)
  }

  android {
      namespace = "com.example.demo.debugtool"
  }
  ```

### 3.2. `configs/build/structure.toml`: cấu trúc module

Plugin không hardcode tên module: danh sách module tự thêm, luật kiến trúc và ngưỡng coverage đều đọc từ file này (ở thư mục gốc
của project dùng). Mọi đường dẫn phải là project có trong `settings.gradle.kts` (gõ sai thì build fail ngay); mẫu có ký tự đại diện
phải khớp ít nhất một project.

Pattern: một đường dẫn cụ thể (`:core:network`) khớp đúng nó; `*` khớp **đúng một** đoạn (`:feature:*:impl` khớp `:feature:home:impl`);
`**` khớp **một đoạn trở lên** (`:core:**` khớp `:core:network`, `:core:storage:api`); riêng `**` đứng một mình khớp mọi module.

File tối thiểu cho ví dụ Demo (copy được; mọi section và key đều bắt buộc, trừ `[[rules]]`):

```toml
[library]                          # pokepoke.android.library
testImplementation = []            # module tự thêm `testImplementation(project(x))` cho mọi library
excludeAutoDependencies = []       # library KHÔNG nhận dòng trên (mẫu có ký tự đại diện được)

[feature.api]                      # pokepoke.android.feature.api
api = []                           # `api(project(x))` tự thêm

[feature.impl]                     # pokepoke.android.feature.impl
implementation = []                # `implementation(project(x))` tự thêm
testFixtures = []                  # `testImplementation(testFixtures(project(x)))` tự thêm

[app]
debugTool = ":debugtool"           # module công cụ debug; phải tồn tại và KHÁC chính :app (mục 5)

[kover]
minLineCoverage = 70               # ngưỡng line coverage (0..100)
modules = []                       # module phải đạt ngưỡng (mẫu có ký tự đại diện được), ví dụ [":core:*:impl", ":feature:*:impl"]

# Luật kiến trúc (tùy chọn, nhiều khối): module khớp `from` (trừ `except`) không được phụ thuộc module khớp `to`.
[[rules]]
from = ["**"]
except = [":app"]
to = [":debugtool"]               # mẫu có ký tự đại diện, ví dụ ":**:impl", cũng được nhưng phải khớp ít nhất một project
reason = "chỉ :app được phụ thuộc :debugtool"
```

Với project thật điền danh sách module vào các mảng, ví dụ `testImplementation = [":core:testing"]`,
`api = [":core:common-kotlin", ":core:navigation"]`. Mọi `[[rules]]` tương tự: thêm khối, không cần sửa plugin.

### 3.3. `configs/env/`: config theo môi trường

Ba file TOML, bốn section `[common]`, `[dev]`, `[qa]`, `[prod]` (đủ cả bốn, được để rỗng):

| File | Git | Nội dung |
|---|---|---|
| `configs/env/config.toml` | commit | giá trị không bí mật |
| `configs/env/secrets.example.toml` | commit | danh sách key secret bắt buộc, **giá trị luôn là `""`** |
| `configs/env/secrets.toml` | **gitignore** | secret thật, cùng cấu trúc với file mẫu |

`configs/env/config.toml`:

```toml
[common]                                   # mọi flavor; flavor ghi đè key trùng tên
BASE_URL = "https://example.com/api/"

[dev]
DEEPLINK_SCHEME = "demo-dev"

[qa]
DEEPLINK_SCHEME = "demo-qa"

[prod]
DEEPLINK_SCHEME = "demo"
```

`configs/env/secrets.example.toml` (chưa có secret nào):

```toml
[common]

[dev]

[qa]

[prod]
```

Muốn thêm secret `API_KEY`: ghi `API_KEY = ""` vào section tương ứng của `secrets.example.toml` rồi
`cp configs/env/secrets.example.toml configs/env/secrets.toml` và điền giá trị thật.

Luật: tên key `UPPER_SNAKE_CASE`; sau khi gộp `[common]` với phần ghi đè, ba flavor phải có **cùng bộ key**; kiểu lấy từ TOML
(chuỗi -> `String`, số nguyên -> `long`, `true`/`false` -> `boolean`; ghi đè phải cùng kiểu; secret luôn là `String`);
một key không được nằm ở cả `config.toml` lẫn secret. Mỗi key thành field `BuildConfig.KEY` và manifest placeholder `${KEY}`
(secret **không** thành placeholder). Lưu ý: secret trong `BuildConfig` vẫn nằm trong APK và dịch ngược được.

### 3.4. `configs/signing/`: khóa ký

Hai bộ file, mỗi bộ gồm `<tên>.keystore` và `<tên>.properties` (4 key: `storeFile` là đường dẫn **tương đối với `configs/signing/`**,
`storePassword`, `keyAlias`, `keyPassword`):

| Bộ | Git | Dùng cho |
|---|---|---|
| `debug.*` | commit (khóa dùng chung cho cả team / CI, bản debug cài đè được giữa các máy) | mọi variant trừ `prodRelease` (kể cả `qaRelease`) |
| `release.*` | **gitignore, không bao giờ commit hoặc in ra** | chỉ `prodRelease` |

Tạo khóa debug (bắt buộc, vì plugin đọc ngay lúc cấu hình):

```sh
mkdir -p configs/signing
keytool -genkeypair -v -keystore configs/signing/debug.keystore -storepass android -alias androiddebugkey \
  -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
printf 'storeFile=debug.keystore\nstorePassword=android\nkeyAlias=androiddebugkey\nkeyPassword=android\n' > configs/signing/debug.properties
```

Tạo khóa release (chỉ máy / CI ký bản phát hành; đổi mật khẩu thật, đừng dùng lại `android`):

```sh
keytool -genkeypair -v -keystore configs/signing/release.keystore -alias release -keyalg RSA -keysize 2048 -validity 10000
printf 'storeFile=release.keystore\nstorePassword=<mật khẩu>\nkeyAlias=release\nkeyPassword=<mật khẩu>\n' > configs/signing/release.properties
```

`release.*` chỉ được đọc khi task của `prodRelease` nằm trong task graph, nên máy không có file này vẫn build `devDebug`, `qaRelease`... bình thường.

### 3.5. `.gitignore`

Các dòng bắt buộc:

```gitignore
*.keystore
/configs/signing/release.*
!/configs/signing/debug.keystore
/configs/env/secrets.toml
```

### 3.6. Chạy thử

```sh
./gradlew :app:assembleDevDebug
```

(`./gradlew` do `gradle wrapper --gradle-version 9.6.0` tạo ra, hoặc chép `gradlew` + `gradle/wrapper/` từ project khác.)

## 4. Quy ước cố định

- Flavor dimension duy nhất `env`: `dev` (applicationId `.dev`, versionName `-dev`), `qa` (`.qa`, `-qa`), `prod` (không hậu tố).
  Build type `debug` thêm `.debug` / `-debug`. Variant có: `devDebug`, `qaDebug`, `qaRelease`, `prodDebug`, `prodRelease`;
  **`devRelease` bị tắt** (không có task).
- Mọi variant ký bằng khóa `debug.*`, **trừ `prodRelease` ký bằng `release.*`**. Vì vậy `qaRelease` cài đè lên `qaDebug` được, `prodRelease` thì không.
- `BuildConfig` bật sẵn; giá trị theo môi trường chỉ đọc từ `configs/env/`, không có trong plugin. Module khác `:app` không có `BuildConfig` theo flavor:
  nhận giá trị qua DI (ví dụ Hilt qualifier).
- Release bật R8 (`optimization { enable = true }`); `androidResources.generateLocaleConfig = true`.

## 5. Task được thêm

| Task | Khi nào chạy | Fail khi nào, sửa thế nào |
|---|---|---|
| `verifyModuleDependencies` (mọi module dùng plugin library / application / jvm.library) | Trước `preBuild` và trong `check` | Module khai báo phụ thuộc vào module bị `[[rules]]` cấm: `Module ':core:network' must not depend on ':feature:home:impl': <reason>`. Bỏ dependency đó hoặc sửa luật trong `structure.toml` nếu luật sai |
| `verifyReleaseHasNoDebugTool` (`:app`) | Trong `check` và mọi assemble / bundle của flavor `prod` | `debugTool` của `structure.toml` nằm trong classpath runtime của `prodDebug` / `prodRelease`: `A prod variant ... contains :debugtool`. Chỉ khai báo bằng `"devImplementation"` / `"qaImplementation"` |
| Kover verify (`koverVerify`, `koverHtmlReport`) | Chạy tay hoặc trong CI | Module trong `[kover].modules` có line coverage dưới `minLineCoverage`. Thêm test, hoặc bỏ module khỏi danh sách |

## 6. Lỗi thường gặp

| Triệu chứng (message mẫu) | Nguyên nhân / cách sửa |
|---|---|
| `Thiếu configs/signing/release.properties — chỉ cần cho prodRelease ...` | Build `prodRelease` mà thiếu `release.properties` (hoặc `release.keystore`). Tạo theo mục 3.4. Các variant khác không bị ảnh hưởng. Thiếu key: `configs/signing/release.properties: thiếu key 'keyAlias'.` |
| `Thiếu configs/signing/debug.properties` hoặc `storeFile '...' không tồn tại` lúc áp plugin | Quên tạo khóa debug (mục 3.4); phải có ở mọi máy / CI |
| `Thiếu secret 'API_KEY' cho flavor 'prod' trong configs/env/secrets.toml.` (hoặc `đang rỗng`, `có key thừa`) | `secrets.example.toml` có key bắt buộc mà `secrets.toml` thiếu: `cp configs/env/secrets.example.toml configs/env/secrets.toml` rồi điền. Chỉ variant cần secret đó fail |
| `configs/env/secrets.example.toml [common]: 'KEY' phải là ""` | File mẫu được commit nên không chứa giá trị thật; đặt `""` |
| `configs/env/config.toml: flavor 'prod' thiếu key 'X'; ba flavor phải có cùng bộ key` | Key ghi ở `[dev]` / `[qa]` mà không có ở `[prod]` (hoặc `[common]`). Thêm vào đủ ba flavor |
| `configs/env/...: thiếu section [qa]` | Thiếu một trong bốn section, kể cả rỗng |
| `configs/build/structure.toml [app] debugTool = ':debugtool': no such project in settings.gradle.kts (typo?)` | Đường dẫn trong `structure.toml` không có trong `include(...)`. Sửa chính tả / thêm project; mẫu có ký tự đại diện thì báo `the pattern matches no project` |
| `Cannot query the value of this provider because it has no value available.` lúc áp plugin | Thiếu hẳn một file bắt buộc: `configs/build/structure.toml`, `configs/env/config.toml` hoặc `configs/env/secrets.example.toml` (message không nêu tên file, kiểm tra ba file này) |
| `Missing version.properties at ...` | Thiếu `version.properties` ở thư mục gốc (mục 3.1) |
| `Configuring project ':debugtool' without an existing directory is not allowed` | Project trong `include(...)` chưa có thư mục |
| `No matching variant of project :debugtool ... No variants exist` | Module được phụ thuộc chưa áp plugin Android / JVM (thiếu `build.gradle.kts`) |
| `No resources.properties file found` | Thiếu `app/src/main/res/resources.properties` (mục 3.1) |
| `A prod variant ... runtime classpath contains :debugtool` | Xem mục 5 |
| `SDK location not found` | Đặt `sdk.dir` trong `local.properties` hoặc `ANDROID_HOME` |
| Cảnh báo / lỗi phiên bản lạ khi cùng thư viện nằm ở hai catalog (Gradle âm thầm chọn bản cao hơn) | Mỗi artifact `group:name` chỉ được ở **một** trong `pokepoke.versions.toml`, `core.versions.toml` (nếu có) và `libs.versions.toml` (ví dụ `junit` đã ở `pokepoke`, đừng thêm vào `libs`). Repo gốc có test `CatalogConsistencyTest` fail với `group:name nằm ở cả pokepoke và libs: [junit:junit]` |
| `ClassNotFoundException` / `Plugin ... was not found` với plugin Kover, KSP, Hilt... | Chưa đưa Gradle plugin tương ứng lên classpath (mục 2, `build.gradle.kts` gốc) |

## 7. Publish (chưa hỗ trợ)

Hiện chỉ dùng được qua `includeBuild` như trên; chưa publish lên Maven. Hướng đi: publish plugin và catalog `pokepoke`
(artifact riêng, plugin `version-catalog`) **cùng một version**, project dùng khai báo
`create("pokepoke") { from("<group>:catalog:<version>") }` và `pluginManagement` lấy plugin từ Maven thay cho `includeBuild`.
Khi đó plugin luôn chạy với đúng bộ version đã được test, còn `libs` hoàn toàn của project.
Các file cấu hình (`configs/build/structure.toml`, `configs/env/`, `configs/signing/`, `version.properties`) được đọc theo thư mục gốc của
project dùng, không nằm trong jar, nên vẫn đúng sau khi publish. Cập nhật mục này khi làm thật.
