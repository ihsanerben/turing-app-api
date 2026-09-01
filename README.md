# Turing App API

Spring Boot tabanlı backend uygulaması.

## Gereksinimler

- Java 21
- Docker ve Docker Compose

## Çalıştırma

Tüm sistemi (PostgreSQL, MinIO, Mailpit, API ve web) kaldırmak için frontend reposunu
bu repo ile aynı üst dizinde tutun ve çalıştırın:

```bash
docker compose up --build
```

Yalnız altyapıyı başlatıp backend'i host üzerinde çalıştırmak için:

```bash
docker compose up -d postgres minio mailpit
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Servisler:

- API: `http://localhost:8080/api/health`
- Web: `http://localhost:5173`
- MinIO Console: `http://localhost:9001`
- Mailpit: `http://localhost:8025`
