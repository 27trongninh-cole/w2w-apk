# AoV Mod Installer

App Android đơn giản để cài mod (asset zip) vào **Liên Quân Mobile / Arena of Valor**
(`com.garena.game.kgvn`) bằng cách copy đè thư mục `Resources` từ file zip mod vào
`Android/data/com.garena.game.kgvn/files/Resources/`, dùng **Shizuku** để có quyền
ghi vào thư mục dữ liệu của app khác (không cần root).

## Yêu cầu trên máy chạy mod

1. Cài app **Shizuku** ([Play Store](https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api)
   hoặc [GitHub RikkaApps/Shizuku](https://github.com/RikkaApps/Shizuku)).
2. Khởi động Shizuku bằng một trong hai cách:
   - **Wireless debugging** (Android 11+): bật Developer options → Wireless debugging,
     làm theo hướng dẫn pair trong app Shizuku.
   - Hoặc **root**: khởi động qua root nếu máy đã root.
3. Cài app này (`AoV Mod Installer`), mở lên, bấm **"Cấp quyền Shizuku"** và đồng ý.

## Cách dùng

1. Bấm **"Chọn file mod (.zip)"**, chọn file mod đã tải (không cần giải nén trước,
   app tự nhận diện cấu trúc bên trong file zip dù ở dạng nào).
2. Bấm **"Cài mod"**.
3. Nếu thành công, sẽ hiện bảng tóm tắt các bước đã thực hiện.
   Nếu lỗi ở bất kỳ bước nào, app **luôn hiện bảng log lỗi chi tiết** (kèm nút Copy log)
   thay vì âm thầm bỏ qua — gửi log này khi cần hỗ trợ debug.

## Cấu trúc file mod hỗ trợ

App tự động quét (BFS) tìm thư mục tên `Resources` trong file zip, bất kể nó nằm ở
lớp nào, nên các cấu trúc sau đều được hỗ trợ:

```
TenFile.zip/.../Resources/
TenFile.zip/com.garena.game.kgvn/files/Resources/...
TenFile.zip/files/Resources/...
TenFile.zip/Resources/...
```

## Build từ source

### Build local
```bash
./gradlew :app:assembleDebug
```

### Build tự động qua GitHub Actions

Workflow `.github/workflows/build-release.yml` tự động:
- Chạy mỗi khi push lên `main` → build debug + release APK, upload làm **Artifacts**
  (vào tab Actions → chọn run → mục Artifacts để tải).
- Khi push **tag** dạng `v*` (vd `v1.0.0`) → build APK và tự động tạo **GitHub Release**
  đính kèm cả 2 file APK.

Tạo release mới:
```bash
git tag v1.0.0
git push origin v1.0.0
```

> APK được ký bằng debug keystore mặc định của Android (đủ để side-load cài trực tiếp
> trên máy). Nếu cần ký bằng key riêng, thêm signing config và secrets keystore vào
> workflow.

## Lưu ý

- Một số ROM tùy biến (MIUI, OneUI...) có thể chặn thêm bằng chính sách riêng khiến
  lệnh copy vẫn báo lỗi quyền dù Shizuku đã cấp quyền — bảng log lỗi sẽ hiển thị rõ
  exit code và stderr từ lệnh `cp` để xác định nguyên nhân.
- App không tự ý sửa gì ngoài thư mục `Resources` bên trong `files/` của game.
