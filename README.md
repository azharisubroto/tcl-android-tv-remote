# TCL Remote

Aplikasi Android sederhana untuk mengontrol TV TCL (Android TV / Google TV) lewat Wi‑Fi. Tanpa infrared.

## Fitur

- **Cari TV otomatis** di jaringan Wi‑Fi yang sama, atau masukkan alamat IP manual.
- **Pairing sekali saja**: TV menampilkan kode 6 karakter, ketik di HP. Setelah itu HP tersambung otomatis.
- **Remote**: Power, Input, Setelan, panah + OK (tahan untuk mengulang), Kembali, Beranda, Menu.
- **Volume & saluran**: Vol +/−, Bisukan, CH +/−. Tombol volume di HP juga mengatur volume TV.
- **Media**: sebelumnya, mundur, putar/jeda, berhenti, maju, berikutnya.
- **Touchpad**: geser untuk berpindah, ketuk untuk OK.
- **Angka**: 0–9, saluran terakhir, info, tombol merah/hijau/kuning/biru, panduan acara, subtitle.
- **Aplikasi**: buka YouTube, Netflix, Prime Video, Disney+, Spotify, YouTube Music, Play Store, atau tambah sendiri. Aplikasi yang sedang terbuka di TV bisa disimpan dengan satu ketukan.
- **Keyboard**: ketik teks ke kolom pencarian di TV. Keyboard muncul otomatis saat TV meminta input.
- **Suara**: tahan tombol mikrofon untuk bicara ke pencarian suara TV (jika TV mendukung).

## Cara pakai

1. Nyalakan TV dan sambungkan HP ke Wi‑Fi yang sama.
2. Buka aplikasi, pilih TV dari daftar (atau ketuk **Masukkan IP**).
3. Masukkan kode yang muncul di layar TV.
4. Selesai. Lain kali aplikasi langsung tersambung ke TV terakhir.

Catatan: menyalakan TV dari kondisi mati total lewat Wi‑Fi hanya bisa jika TV mengaktifkan opsi seperti
"Network standby" / "Wake on LAN" di setelan TV.

## Build

Butuh Android Studio (atau JDK 17 + Android SDK 35).

```
./gradlew assembleDebug
```

APK ada di `app/build/outputs/apk/debug/app-debug.apk`.

## Teknis

- Kotlin + Jetpack Compose, minSdk 24.
- Protokol Android TV Remote v2: pairing TLS di port 6467, perintah di port 6466,
  penemuan TV lewat mDNS `_androidtvremote2._tcp`.
- Kode protokol ada di `app/src/main/java/com/azhari/tclremote/protocol/` dan tidak bergantung pada Android,
  sehingga bisa diuji di JVM biasa.
