# CLAUDE.md

## Git / GitHub authentication (BẮT BUỘC)

- Remote: `https://github.com/KhangTranMinh/pokepoke.git` (branch chính: `main`).
- **Chỉ được dùng token trong `configs/credentials/.env` (biến `GITHUB_TOKEN`)** cho mọi thao tác với GitHub của project này. KHÔNG dùng token/credential global (osxkeychain, `gh auth`, `GITHUB_TOKEN`/`GH_TOKEN` có sẵn trong shell, ...).
- `git push/pull/fetch`: repo đã được cấu hình credential helper local (`git config --local credential.helper`) đọc token từ `configs/credentials/.env` và đã tắt helper global. Không sửa / không thêm helper khác; không nhúng token vào remote URL.
- `gh` CLI / GitHub API: luôn truyền token từ file, ví dụ:
  ```sh
  GH_TOKEN=$(grep '^GITHUB_TOKEN=' configs/credentials/.env | cut -d= -f2-) gh pr create ...
  ```
- Không bao giờ in token ra output, commit file `.env`, hay ghi token vào file nào khác. `configs/credentials/` đã nằm trong `.gitignore`.
