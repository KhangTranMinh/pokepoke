# Deeplink / App link / Push notification

## 1. Cấu hình

| Loại | Dạng | Ghi chú |
|---|---|---|
| Deeplink (custom scheme) | `<scheme>://<path>` | Host là segment đầu: `pokepoke://pokemon/25` → `["pokemon","25"]` |
| App link | `https://<host>/<path>` | Domain **tạm thời**; chưa có `/.well-known/assetlinks.json` nên Android không verify được (xem §4) |

`<scheme>` / `<host>` **theo môi trường**, lấy từ `configs/env/config.toml` (build.md §4b), key `DEEPLINK_SCHEME` / `DEEPLINK_HOST`. Tạm thời gắn tên môi trường để các bản cài song song không tranh link:

| Flavor | Scheme | Host |
|---|---|---|
| dev | `pokepoke-dev` | `dev.pokepoke.com` |
| qa | `pokepoke-qa` | `qa.pokepoke.com` |
| prod | `pokepoke` | `pokepoke.com` |

- Manifest dùng placeholder `${DEEPLINK_SCHEME}` / `${DEEPLINK_HOST}`; `:app` cung cấp `DeepLinkConfig(setOf(BuildConfig.DEEPLINK_SCHEME), setOf(BuildConfig.DEEPLINK_HOST))`. Không hardcode scheme / host ở đâu khác (kể cả debugtool: chip mẫu dựng từ `DeepLinkConfig` inject qua Hilt).
- Ví dụ trong tài liệu dùng giá trị prod (`pokepoke://`, `https://pokepoke.com`); test dùng `DeepLinkConfig` tự tạo, không phụ thuộc flavor.
| Push notification | Notification chứa URI ở trên | Mở `MainActivity` với `data = uri` qua `DeepLinkIntentFactory`; đi chung đường deeplink |

Manifest `MainActivity`:

```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="${DEEPLINK_SCHEME}" />
</intent-filter>
<intent-filter android:autoVerify="true">
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="https" android:host="${DEEPLINK_HOST}" />
</intent-filter>
```

## 2. Bảng route

| Path (segments) | Handler (module) | NavRequest | Cần đăng nhập |
|---|---|---|---|
| `` (rỗng) / `home` | home | `ResetTo([HomeNavKey])` | Không |
| `favourite` | favourite | `ResetTo([FavouriteNavKey])` | Không (tab tự hiện nút đăng nhập) |
| `me` | profile | `ResetTo([ProfileNavKey])` | Không (tab tự hiện nút đăng nhập) |
| `pokemon/{id}` (id nguyên dương) | details | `ResetTo([HomeNavKey, DetailsNavKey(id)])` | **Có** (`DetailsNavKey` là `RequiresAuth`) |
| khác / id không hợp lệ | — | `dispatch` trả `false` | — |

Path match không phân biệt hoa thường; bỏ dấu `/` cuối; query string bỏ qua.

## 3. Luồng xử lý

```
Intent(data = uri)
  → DeepLinkDispatcher.dispatch(uri)
      parse + chuẩn hóa → DeepLink(segments, query)
      handler đầu tiên trả NavRequest != null
  → Navigator.execute(request)
      OnboardingInterceptor (0): chưa xem onboarding → ResetTo([OnboardingNavKey(pending = request)])
      AuthInterceptor (100):     có RequiresAuth & chưa login → chèn LoginNavKey(pending = request)
  → back stack mới
```

- **Cold start** (`savedInstanceState == null`, stack rỗng): có URI → dispatch; dispatch `false` hoặc không có URI → `ResetTo([HomeNavKey])`.
- **App đang chạy** (`onNewIntent`): dispatch; `false` → giữ nguyên màn hiện tại.
- Ví dụ `pokepoke://pokemon/25` khi lần đầu cài, chưa login:
  1. `[OnboardingNavKey(pending = ResetTo([HomeNavKey, DetailsNavKey(25)]))]`
  2. Xong onboarding → execute pending → auth chặn → `[HomeNavKey, LoginNavKey(pending = ResetTo([HomeNavKey, DetailsNavKey(25)]))]`
  3. Login thành công → goBack → execute pending → `[HomeNavKey, DetailsNavKey(25)]`
  4. Back → Home.

URI mẫu (giá trị prod; debugtool thay scheme / host bằng `DeepLinkConfig` của bản đang chạy): `pokepoke://home`, `pokepoke://favourite`, `pokepoke://me`, `pokepoke://pokemon/25`, `pokepoke://pokemon/133`, `https://pokepoke.com/pokemon/6`, `https://pokepoke.com/me`, `pokepoke://unknown`.

## 4. Kiểm tra thủ công

```sh
# devDebug
adb shell am start -a android.intent.action.VIEW -d "pokepoke-dev://pokemon/25" com.khangtm.pokepoke.dev.debug
adb shell am start -a android.intent.action.VIEW -d "https://dev.pokepoke.com/pokemon/25" com.khangtm.pokepoke.dev.debug
# qaRelease: pokepoke-qa:// / https://qa.pokepoke.com, package com.khangtm.pokepoke.qa
```

App link chưa verify: mở từ trình duyệt sẽ không tự vào app. Khi test tay có thể bật trong Settings → Apps → PokePoke → Open by default → thêm link của host môi trường đó, hoặc dùng lệnh `adb` ở trên (chỉ định package). Khi có domain thật: host `assetlinks.json` với SHA-256 của chứng chỉ release.
