# Turing App API

Spring Boot tabanlı backend uygulaması.

## Docker ile tüm uygulamayı başlatma

Docker Desktop açık olmalı; `turing-app-api` ve `turing-app-web` klasörleri yan yana bulunmalıdır.
Backend klasöründe mevcut `.env` kullanılır. İlk kurulumda `.env.example` dosyasını `.env`
olarak kopyalayıp JWT, profil şifreleme ve SMTP değerlerini doldurun. Mevcut `.env` dosyasını
örnek dosyayla değiştirmeyin.

```bash
docker compose up -d --build --wait
```

Bu komut PostgreSQL, MinIO, backend (`api`) ve frontend (`web`) servislerini aynı
`turing-app-api` Docker Compose grubunda başlatır. Backend veritabanı ve MinIO hazır olana,
frontend de backend sağlıklı olana kadar bekler. Veriler mevcut kalıcı volume'larda korunur.

İlk kurulumdan sonra Docker Desktop → Containers → `turing-app-api` grubundaki **Start/Stop**
düğmesiyle tüm uygulamayı açıp kapatabilirsiniz. Kod değişikliklerini image'lara almak için
üstteki `--build` komutunu yeniden çalıştırın. Frontend Docker içinde build edilmiş dosyaları
Nginx ile sunar; kaynak kod değişiklikleri otomatik yenilenmez.

| Servis | Adres |
|---|---|
| Frontend | http://localhost:5174 |
| Backend | http://localhost:8086 |
| API sağlık kontrolü | http://localhost:8086/api/health |
| Swagger UI | http://localhost:8086/swagger-ui.html |
| PostgreSQL | localhost:5432 |
| MinIO API / Console | http://localhost:9000 / http://localhost:9001 |

8086 veya 5174 portunda host üzerinde çalışan `./run.sh`, `npm run dev` ya da başka bir
uygulama varsa önce ilgili süreci durdurun. Portlar sabittir; alternatif porta geçilmez.

```bash
docker compose ps
docker compose logs -f api web
docker compose stop
```

Compose, backend `.env` dosyasını yalnız runtime'da kullanır. Container içindeki DB ve MinIO
adresleri `postgres:5432` / `minio:9000` olarak ayarlanır; tarayıcı API'ye
`http://localhost:8086` üzerinden bağlanır. Yerel `local` profili kullanılır. Docker
image'larına `.env` dosyaları kopyalanmaz. Bu Compose dosyası yerel geliştirme içindir.

## İsteğe bağlı host geliştirme

Java 21 ve frontend için Node.js/npm gerekir. Önce Docker'daki uygulama servislerini durdurun:

```bash
docker compose stop api web
docker compose up -d postgres minio
./run.sh
```

Bu eski host akışı backend 8080 ve frontend 5173 portlarını kullanır; bu portların da boş olması gerekir.
Frontend klasöründe `npm run dev` çalıştırılır. `run.sh`, `.env` dosyasını yükler ve
zorunlu değerleri doğrular. Aynı uygulamayı host ve Docker üzerinde aynı portlarda birlikte çalıştırmayın.

## Doğrulama

```bash
./mvnw verify
docker compose config --quiet
```
