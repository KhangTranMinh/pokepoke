# Deeplink / App link / Push notification

## 1. Cấu hình

| Loại | Dạng | Ghi chú |
|---|---|---|
| Deeplink (custom scheme) | `pokepoke://<path>` | Host là segment đầu: `pokepoke://pokemon/25` → `["pokemon","25"]` |
| App link | `https://pokepoke.com/<path>` | Domain **tạm thời**; chưa có `/.well-known/assetlinks.json` nên Android không verify được (xem §4) |
| Push notification | Notification chứa URI ở trên | Mở `MainActivity` với `data = uri` qua `DeepLinkIntentFactory`; đi chung đường deeplink |

Manifest `MainActivity`:

```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="pokepoke" />
</intent-filter>
<intent-filter android:autoVerify="true">
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="https" android:host="pokepoke.com" />
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

URI mẫu (dùng trong debugtool và test): `pokepoke://home`, `pokepoke://favourite`, `pokepoke://me`, `pokepoke://pokemon/25`, `pokepoke://pokemon/133`, `https://pokepoke.com/pokemon/6`, `https://pokepoke.com/me`, `pokepoke://unknown`.

## 4. Kiểm tra thủ công

```sh
adb shell am start -a android.intent.action.VIEW -d "pokepoke://pokemon/25" com.khangtm.pokepoke.debug
adb shell am start -a android.intent.action.VIEW -d "https://pokepoke.com/pokemon/25" com.khangtm.pokepoke.debug
```

App link chưa verify: mở từ trình duyệt sẽ không tự vào app. Khi test tay có thể bật trong Settings → Apps → PokePoke → Open by default → thêm link `pokepoke.com`, hoặc dùng lệnh `adb` ở trên (chỉ định package). Khi có domain thật: host `assetlinks.json` với SHA-256 của chứng chỉ release.
