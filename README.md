# demo-1

Spring Boot 3.5 + Embabel AI Agents + Vue 3 Frontend.

## Voraussetzungen

- Docker & Docker Compose

## Setup

```bash
cp .env.example .env
```

`OPENAI_API_KEY` in `.env` eintragen.

## Start

```bash
docker compose up --build
```

| Service  | URL                    |
|----------|------------------------|
| Frontend | http://localhost:3000  |
| Backend  | http://localhost:8088  |

Im Hintergrund:

```bash
docker compose up --build -d
```

## Stop

```bash
docker compose down
```

Mit Volumes aufräumen:

```bash
docker compose down -v
```

## Logs

```bash
# alle Services
docker compose logs -f

# nur Backend
docker compose logs -f backend

# nur Frontend
docker compose logs -f frontend
```

## Stack

- **Backend:** Java 21, Spring Boot 3.5, Embabel 0.4.0, OpenAI GPT-4.1 Mini
- **Frontend:** Vue 3, Vite, Nginx
