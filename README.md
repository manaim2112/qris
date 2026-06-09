# QRIS Listener

Aplikasi Android (**minimal Android 7.0 / API 24+**) untuk memantau notifikasi pembayaran dari **GoPay Merchant** dan **Mandiri Merchant (Livin Merchant)**, lalu meneruskannya ke server via webhook.

## Alur Kerja

```
[GoPay/Mandiri Merchant] → Notifikasi Masuk
        ↓
[QRIS Listener] → Filter kata kunci (berhasil, payment, dll)
        ↓
[Webhook POST] → https://qris.yamitra.com/webhook/v1  ( + session token )
        ↓
[Server] → Proses & verifikasi data
```

## Login API

Endpoint: `POST https://yamitra.com/customer/loginWithApp`

### Request
```json
{
    "email": "user@example.com",
    "password": "********"
}
```

### Response Sukses
```json
{
    "status": "success",
    "session": "session_token_abc123",
    "name": "Nama Pengguna",
    "message": "Login berhasil"
}
```
Kode HTTP: `200`

### Response Gagal
```json
{
    "status": "error",
    "message": "Email atau kata sandi salah"
}
```
Kode HTTP: `4xx`

---

## Webhook Notifikasi

Endpoint: `POST https://qris.yamitra.com/webhook/v1`

Dikirim otomatis setiap kali aplikasi mendeteksi notifikasi pembayaran masuk.

### Request
```json
{
    "app": "id.co.bankmandiri.livinmerchant",
    "title": "Pembayaran Masuk",
    "text": "Rp50.000 dari Customer A",
    "session": "session_token_abc123"
}
```

| Field     | Tipe   | Deskripsi                                           |
|-----------|--------|------------------------------------------------------|
| `app`     | String | Package name sumber notifikasi                       |
| `title`   | String | Judul notifikasi                                     |
| `text`    | String | Isi notifikasi                                       |
| `session` | String | Session token dari login (identifikasi akun pengirim) |

### Response yang Diharapkan
```json
{
    "status": "success",
    "message": "Webhook diterima"
}
```
Kode HTTP: `200`

> **Catatan server**: Gunakan field `session` untuk mencocokkan notifikasi dengan akun user. Pastikan session divalidasi sebelum memproses data.

---

## Aplikasi yang Dipantau

| Aplikasi             | Package Name                         |
|----------------------|--------------------------------------|
| Mandiri Merchant       | `id.bmri.livinmerchant`              |
| GoPay Merchant       | `com.gojek.gopaymerchant`            |

Kata kunci pemicu webhook: `berhasil`, `masuk`, `diterima`, `pembayaran`, `success`, `received`, `payment`, `credit`.

---

## Persyaratan Aplikasi (Dashboard)

1. **Koneksi Internet** — untuk mengirim webhook ke server
2. **Izin Akses Notifikasi** — aktifkan di Settings → Notification Access
3. **Session Login** — login dengan akun yamitra.com
4. **Layanan Listener** — service berjalan di latar belakang
5. **Optimasi Baterai** — nonaktifkan agar listener tetap jalan saat HP sleep

### Catatan Battery Optimization
Beberapa HP (Xiaomi, OPPO, vivo, Huawei) memiliki sistem manajemen baterai agresif yang bisa mematikan service. Pada dashboard, ketuk item **Optimasi Baterai** untuk membuka pengaturan. Jika tidak cukup, atur manual:
- **Xiaomi/HyperOS**: Settings → Apps → Manage Apps → QRIS Listener → Battery Saver → pilih **No restrictions**
- **OPPO/Realme**: Settings → Battery → App Battery Management → QRIS Listener → pilih **Allow background activity**
- **Samsung**: Settings → Battery → Background usage limits → Never sleeping apps → tambahkan QRIS Listener

---

## Build & Install

```bash
git add .
git commit -m "Initial commit"
git push origin main

# Download APK dari GitHub Actions → Artifacts
# Install di Android 7.0+ (min SDK 24 / Nougat)
```
