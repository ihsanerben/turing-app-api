# Turing App API

Spring Boot tabanlı backend uygulaması.

## Gereksinimler

- Java 21
- Docker ve Docker Compose

## Çalıştırma

Docker Compose yalnız PostgreSQL ve MinIO altyapısını çalıştırır. Backend ve frontend
host üzerinde manuel başlatılır:

```bash
docker compose up -d postgres minio
cp .env.example .env # yalnız ilk kurulumda
./run.sh
```

`run.sh`, Git'e girmeyen `.env` dosyasını yükler ve zorunlu değerleri doğrular. Frontend yan
repoda `npm run dev` ile çalışır.

Servisler:

- API: `http://localhost:8080/api/health`
- Web: `http://localhost:5173`
- MinIO Console: `http://localhost:9001`
