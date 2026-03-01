# Deploying SKSE Inventory to Render

This guide covers deploying the app to **Render** (free web hosting) with a cloud database, plus how to export your data to local MySQL anytime.

---

## Free Tier Reality (as of 2025)

| Service | Free Tier Status | Notes |
|---------|------------------|-------|
| **Render (app)** | ✅ Ongoing free | 750 instance hours/month, resets each calendar month. Spins down after 15 min idle (~1 min to wake). Enough for 4–5 visits/day. |
| **PlanetScale** | ❌ No longer free | Free tier ended April 2024. Paid plans only. |
| **Neon (PostgreSQL)** | ✅ Ongoing free | 0.5 GB storage, generous compute. No time limit. |
| **Railway** | ⚠️ $5 credit/month | Resets monthly. Often enough for 1 user + small DB. |
| **Render Postgres** | ⚠️ 30-day expiry | Free Postgres expires after 30 days. Not ideal for long-term data. |

**Recommended setup:** **Render** (app) + **Neon** (PostgreSQL, free ongoing).  
If you prefer MySQL: **Railway** ($5 credit) for both app and MySQL, or a paid PlanetScale plan.

---

## 1. Create a Free Database

### Option A: Neon (PostgreSQL) – recommended

1. Sign up at [neon.tech](https://neon.tech)
2. Create a project and database
3. Copy the connection string (e.g. `postgresql://user:pass@host/dbname?sslmode=require`)
4. Parse it for env vars:
   - `SPRING_DATASOURCE_URL` = full URL
   - `SPRING_DATASOURCE_USERNAME` = user from URL
   - `SPRING_DATASOURCE_PASSWORD` = password from URL

### Option B: Railway (MySQL or Postgres)

1. Sign up at [railway.app](https://railway.app)
2. New Project → Add MySQL or Postgres
3. In Variables, copy `DATABASE_URL` or the individual host/user/password
4. For MySQL: `SPRING_DATASOURCE_URL=jdbc:mysql://host:port/db?sslMode=REQUIRED`

### Option C: PlanetScale (MySQL, paid)

If you use PlanetScale (paid), use their connection string. Add `?sslMode=REQUIRED` for JDBC.

---

## 2. Deploy to Render

1. Push this repo to **GitHub**
2. Go to [render.com](https://render.com) → **New** → **Web Service**
3. Connect your GitHub repo
4. Render will detect `render.yaml` (or configure manually):
   - **Runtime:** Docker
   - **Build Command:** (uses Dockerfile)
   - **Start Command:** (uses Dockerfile)
5. Add **Environment Variables** in the Render dashboard:
   - `SPRING_PROFILES_ACTIVE` = `render`
   - `SPRING_DATASOURCE_URL` = your DB URL (JDBC format)
   - `SPRING_DATASOURCE_USERNAME` = your DB user
   - `SPRING_DATASOURCE_PASSWORD` = your DB password
6. Deploy. Your app will be at `https://your-app.onrender.com`

---

## 3. Export Data to Local MySQL

You can always export your cloud data and import it into local MySQL.

### If using PostgreSQL (Neon)

```bash
# Export from Neon (replace with your connection string)
pg_dump "postgresql://user:pass@host/dbname?sslmode=require" > backup.sql

# Import into local MySQL (requires conversion - see below)
```

PostgreSQL and MySQL use different SQL dialects. For a full migration:

1. Use a tool like [pgloader](https://pgloader.io) to convert, or
2. Export to CSV from the app/DB and re-import into MySQL, or
3. Use a migration tool (e.g. [pg2mysql](https://github.com/lanyrd/mysql-postgresql-converter))

### If using MySQL (Railway, PlanetScale)

```bash
# Export (replace host, user, db)
mysqldump -h your-db-host -u your-user -p your-database > backup.sql

# Import into local MySQL
mysql -u inventory_user -p inventory_db < backup.sql
```

### Using a GUI

- **MySQL:** MySQL Workbench, DBeaver, or TablePlus → connect to cloud DB → Export Data
- **PostgreSQL:** DBeaver, pgAdmin, or TablePlus → connect → Export

### Backup before switching providers

Before changing DB providers or shutting down the cloud DB, always run an export so you have a local copy.

---

## 4. Local Development

- **Dev (H2 in-memory):** `mvn spring-boot:run` (default)
- **Local MySQL:** `mvn spring-boot:run -Dspring-boot.run.profiles=prod`

---

## Summary

| What | Where |
|------|-------|
| App hosting | Render (free, 750 hrs/month) |
| Database | Neon (Postgres, free) or Railway (MySQL, $5 credit) |
| Data export | `pg_dump` / `mysqldump` or GUI → import into local MySQL |
