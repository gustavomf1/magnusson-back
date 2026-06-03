# magnossao-backend

Backend Spring Boot do e-commerce Magnossão.

## Rodar local

```bash
docker compose up -d        # sobe o Postgres
java -jar target/magnossao-backend-*.jar  # ou: ./mvnw spring-boot:run
```

Verificar: `curl localhost:8080/api/health` → `{"status":"ok","db":"ok",...}`

## Testes

```bash
./mvnw test
```

## Variáveis de ambiente

Ver `.env.example`. Em produção, injetar `DATABASE_URL`, `DATABASE_USER`,
`DATABASE_PASSWORD`, `APP_CORS_ORIGIN` e `PORT`.
