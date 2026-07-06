# Help Desk — sistem de suport full-stack

Sistem de suport (help desk) cu ticketing, chat live (WebSocket/STOMP) și
notificări event-driven (RabbitMQ).

- **Backend:** Spring Boot 3.5 (Java 21) — `backend/api`
- **Frontend:** React + TypeScript + Vite — `frontend`
- **Bază de date:** PostgreSQL (Supabase)

## Structură

```
helpdesk/
├── backend/
│   └── api/            → Spring Boot (auth + tickets + chat)
├── frontend/           → React + Vite + TypeScript
└── README.md
```

## Cerințe

- Java 21, Node.js, Git
- Un proiect Supabase (PostgreSQL) — vezi `backend/api/.env.example`

## Rulare backend

```bash
cd backend/api
# copiază .env.example în .env și completează datele din Supabase + JWT_SECRET
./mvnw compile                  # Windows: .\mvnw.cmd compile
./mvnw spring-boot:run          # Windows: .\mvnw.cmd spring-boot:run
```

Verificare: `GET http://localhost:8080/api/health` → `{"status":"UP"}`

### Endpoint-uri de autentificare

- `POST /api/auth/register` — `{ email, password, fullName }` → token JWT
- `POST /api/auth/login` — `{ email, password }` → token JWT
- `GET /api/me` — necesită antet `Authorization: Bearer <token>`

## Rulare frontend

```bash
cd frontend
npm install
npm run dev
```

## Progres

- [x] Săpt. 1 — Setup: schelet backend + frontend, conexiune la bază de date
- [x] Săpt. 2 — Model de date + autentificare (JWT) — register/login/me
- [ ] Săpt. 3 — Canal asincron (formular de suport)
- [ ] Săpt. 4 — CRUD tichete (dashboard agent)
- [ ] Săpt. 5 — Roluri + workflow (MVP)
- [ ] Săpt. 6 — Notificări event-driven (RabbitMQ)
- [ ] Săpt. 7–8 — Chat live (WebSocket/STOMP)
- [ ] Săpt. 9 — Teste + CI
- [ ] Săpt. 10 — Finalizare
