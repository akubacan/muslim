# Personal Doa Library (Android native)

Kotlin + Jetpack Compose + Room + Hilt + DataStore. Min SDK 26. Tanpa izin INTERNET.

## Cara dapat APK

### A. Tanpa Android Studio (cukup HP/browser)
1. Buat akun GitHub, buat repository baru (private juga boleh).
2. Upload SEMUA isi folder ini (termasuk folder .github).
3. Buka tab **Actions** > **Build APK** > **Run workflow**.
4. Setelah hijau, buka run tersebut, unduh artifact **doa-library-debug-apk** (zip berisi app-debug.apk).
5. Ekstrak, kirim ke Redmi, install (izinkan "install dari sumber tidak dikenal").

### B. Android Studio
1. File > Open > pilih folder ini, tunggu Gradle sync.
2. Build > Build APK(s). Hasil: app/build/outputs/apk/debug/app-debug.apk

### APK release
Build > Generate Signed Bundle / APK > APK, buat keystore sendiri.

## Status
Tahap 1-4 selesai (kode): database, navigasi, CRUD, sampah, favorit, pencarian,
drag-and-drop urutan, pembaca dengan swipe, pengaturan ukuran/jarak/perataan, tema terang/gelap.
Belum: Font Manager, font bawaan, tema custom, backup/restore ZIP, ikon launcher kustom.
Proyek ini BELUM pernah dikompilasi; kemungkinan ada error kecil saat build pertama.
