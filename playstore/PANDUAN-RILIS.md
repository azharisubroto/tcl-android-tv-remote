# Panduan rilis ke Google Play

Isi folder ini:

| File | Untuk apa |
|---|---|
| `tcl-remote-1.0.aab` | File yang diunggah ke Play Console (sudah ditandatangani dengan upload key) |
| `tcl-remote-1.0.apk` | APK rilis yang sama, untuk dipasang langsung ke HP tester |
| `icon-512.png` | Ikon aplikasi 512×512 |
| `feature-graphic-1024x500.png` | Feature graphic |
| `privacy-policy.html` | Kebijakan privasi (isi `EMAIL_ANDA` lalu pasang di URL publik) |
| `PANDUAN-RILIS.md` | Dokumen ini, termasuk teks listing dan jawaban formulir |

Yang belum ada: screenshot HP. Play butuh minimal 2 screenshot (rasio 16:9 atau 9:16, sisi 320–3840 px). Ambil dari HP saat aplikasi tersambung ke TV.

---

## 1. Buat akun developer

1. Buka https://play.google.com/console/signup dan pilih akun **Personal**.
2. Bayar biaya sekali USD 25 (tidak bisa dikembalikan).
3. Verifikasi identitas: nama dan alamat sesuai KTP/paspor, nomor HP dan email diverifikasi dengan OTP. Nama di profil harus persis sama dengan dokumen.
4. Verifikasi bisa makan beberapa hari.

Alternatif gratis: Google menyediakan akun **Limited Distribution** (gratis, tanpa KTP) untuk pelajar/hobi, tapi hanya untuk dibagikan ke maksimal 20 perangkat tertentu dan **tidak** untuk rilis publik di Play Store. Cocok kalau aplikasinya cuma untuk diri sendiri dan keluarga.

## 2. Buat aplikasi di Play Console

*Create app* → nama aplikasi, bahasa default **Indonesia**, jenis **App**, **Free**, centang deklarasi.

Package `com.azhari.tclremote` terkunci selamanya setelah AAB pertama diunggah.

## 3. Play App Signing

Saat unggah AAB pertama, biarkan Google membuat dan menyimpan **app signing key** (opsi default). Keystore `release/tclremote-release.jks` kita jadi **upload key**. Kalau upload key hilang, bisa minta reset ke Google; app signing key tetap aman di Google.

Tetap backup `tclremote-release.jks` dan `keystore.properties` di tempat aman (bukan di GitHub).

Bonus: dengan Play App Signing, aplikasinya otomatis terdaftar untuk aturan verifikasi developer Android yang sudah berlaku di Indonesia sejak 30 September 2026.

## 4. Closed testing wajib (akun personal baru)

Akun personal yang dibuat setelah 13 Nov 2023 tidak bisa langsung rilis Production. Syaratnya:

1. Buat track **Closed testing**, unggah `tcl-remote-1.0.aab`, kirim untuk review.
2. Undang minimal **12 tester** (lewat daftar email Google atau Google Group), mereka harus klik link opt-in dan memasang aplikasinya.
3. Minimal 12 tester tetap ikut selama **14 hari berturut-turut**. Kalau jumlahnya turun di bawah 12, hitungannya bisa mulai ulang.
4. Google juga melihat apakah tester benar-benar memakai aplikasi, jadi minta mereka mencoba beberapa kali.
5. Setelah 14 hari, isi formulir *Apply for production* (pertanyaan tentang testing dan kesiapan aplikasi). Review akses produksi biasanya sekitar seminggu.

Tester tidak harus punya TV TCL; Android TV/Google TV merek lain juga bisa dipakai.

## 5. App content (menu *Policy and programs → App content*)

**Privacy policy**: isi URL publik dari `privacy-policy.html`. Repo GitHub-nya private, jadi pilih salah satu: Google Sites, GitHub Gist publik, atau repo publik kecil dengan GitHub Pages.

**Ads**: Tidak ada iklan.

**App access**: *All functionality is available without special access*. Tambahkan catatan bahwa aplikasi butuh Android TV/Google TV di Wi-Fi yang sama.

**Content rating** (kuesioner IARC): kategori *Utility, Productivity, Communication or Other*; jawab **Tidak** untuk semua pertanyaan kekerasan, seksual, judi, dll. Pertanyaan "pengguna bisa berinteraksi/berbagi konten" → Tidak. Hasilnya biasanya Semua Umur / 3+.

**Target audience**: pilih **18+** (atau 13+). Jangan pilih usia anak supaya tidak masuk aturan Families.

**Data safety** (jawaban yang disarankan):
- *Does your app collect or share user data?* → **Yes** (karena audio mikrofon dikirim ke TV untuk pencarian suara).
- Data type: **Audio → Voice or sound recordings**.
  - Collected: Yes. Shared: No (dikirim ke perangkat milik pengguna sendiri, bukan pihak ketiga).
  - Processed ephemerally: **Yes**.
  - Required or optional: **Optional** (pengguna yang memilih menekan tombol suara).
  - Purpose: **App functionality**.
- Encrypted in transit: **Yes** (TLS).
- Way to request deletion: tidak ada data yang disimpan; pilih opsi yang sesuai/jelaskan di kebijakan privasi.
- Data lain (lokasi, kontak, ID, analitik, dll): tidak ada.

**Lain-lain**: News app → No. COVID → No. Government app → No. Financial features → None. Health → None.

## 6. Store listing

Kategori: **Tools** (Alat). Email kontak wajib diisi.

**Soal nama**: jangan pakai merek "TCL" di depan judul atau terkesan aplikasi resmi, karena bisa ditolak dengan kebijakan Impersonation/IP. Pola "untuk TCL" + disclaimer di deskripsi biasanya aman. Nama di HP saat ini masih "TCL Remote" (`strings.xml`); disarankan diganti jadi "Remote TV" agar konsisten.

**Judul** (maks 30 karakter):
```
Remote TV untuk TCL Android TV
```

**Deskripsi singkat** (maks 80 karakter):
```
Remote Wi-Fi untuk TV TCL dan Android TV/Google TV. Tanpa infrared.
```

**Deskripsi lengkap**:
```
Kendalikan TV TCL (Android TV / Google TV) dari HP lewat Wi-Fi. Tidak butuh infrared, cukup HP dan TV di jaringan Wi-Fi yang sama.

FITUR
• Cari TV otomatis di Wi-Fi, atau masukkan alamat IP manual
• Pairing sekali saja dengan kode yang muncul di layar TV
• Tombol lengkap: Power, Input, Setelan, panah + OK, Kembali, Beranda
• Volume dan saluran, termasuk tombol volume HP untuk mengatur volume TV
• Kontrol media: putar/jeda, maju, mundur, berikutnya, sebelumnya
• Touchpad: geser untuk berpindah, ketuk untuk OK
• Tombol angka, tombol warna, panduan acara, subtitle
• Buka YouTube, Netflix, Prime Video, Disney+, Spotify, dan aplikasi lain langsung dari HP
• Keyboard: ketik di kolom pencarian TV dari HP
• Pencarian suara lewat mikrofon HP (jika TV mendukung)

CARA PAKAI
1. Sambungkan HP dan TV ke Wi-Fi yang sama.
2. Pilih TV dari daftar.
3. Masukkan kode yang muncul di TV. Selesai.

Bekerja dengan TV TCL berbasis Android TV atau Google TV, dan juga Android TV/Google TV merek lain. TV TCL dengan sistem Roku atau Linux tidak didukung.

PRIVASI
Tanpa iklan, tanpa akun, tanpa pelacakan. Semua perintah dikirim langsung ke TV Anda dengan koneksi terenkripsi.

Aplikasi ini dibuat oleh pengembang independen dan tidak berafiliasi dengan TCL atau Google. TCL adalah merek dagang dari pemiliknya.
```

## 7. Rilis

1. Setelah akses Production disetujui: *Production → Create new release* → pakai AAB yang sama (atau versi lebih baru).
2. Pilih negara (misalnya Indonesia saja dulu, atau semua).
3. *Send for review*. Review aplikasi baru bisa beberapa hari.

## Update berikutnya

Naikkan `versionCode` (dan `versionName`) di `app/build.gradle.kts` setiap kali unggah build baru, lalu `./gradlew bundleRelease`. AAB ada di `app/build/outputs/bundle/release/app-release.aab`.

Aturan target API: aplikasi baru dan update wajib target **API 36 (Android 16)** sejak 31 Agustus 2026. Build ini sudah target API 36.
