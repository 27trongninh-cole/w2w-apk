# wav2wem-android (giai đoạn 1: build test aoTuV)

Mục tiêu giai đoạn này: xác nhận build được libvorbis bản vá aoTuV 6.03 (2020)
và encode ra file .ogg đúng vendor "aoTuV" — CHƯA build Android, CHƯA làm APK.
Nếu bước này thành công, sẽ làm tiếp giai đoạn 2 (build Android NDK + APK).

## Cách chạy

1. Push toàn bộ nội dung này lên GitHub (nhánh `main`).
2. Vào tab **Actions** trên GitHub, chờ workflow "Build Test (aoTuV native, giai đoạn 1)" chạy xong
   (khoảng 3-5 phút).
3. Nếu có dấu ✅ xanh: bấm vào lần chạy đó → kéo xuống mục **Artifacts** → tải `build-output.zip`
   về, giải nén sẽ có file `test.ogg` — gửi lại cho Claude để kiểm tra tiếp (đọc thử vendor
   string, thử match codebook).
4. Nếu có dấu ❌ đỏ: bấm vào lần chạy đó → bấm vào job `build` → copy toàn bộ log
   (đặc biệt đoạn có chữ `Error` màu đỏ) gửi lại cho Claude để sửa.
