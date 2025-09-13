# P2PLink

P2PLink is a peer-to-peer style file sharing app that lets users share files directly using a simple invite code.  
It runs a lightweight Java HTTP server that spins up ephemeral per-share senders, and a modern Next.js frontend for a clean, minimal UI.

---

## Project Structure

```
/ (repo root)
├─ backend/
│  └─ src/main/java/p2plink/
│     ├─ Main.java                    # App entry point
│     ├─ FileServer.java              # HTTP server bootstrap (routing + lifecycle)
│     ├─ FileController.java          # Route registration / top-level wiring
│     ├─ handlers/
│     │  ├─ UploadHandler.java        # POST /api/upload (start a share)
│     │  ├─ DownloadHandler.java      # GET  /api/shares/{code}/meta, /download
│     │  ├─ FileSenderHandler.java    # Streams file bytes to client
│     │  ├─ SseHandler.java           # GET /api/sse (server-sent events)
│     │  └─ CorsHandler.java          # Cross-origin headers for UI
│     ├─ services/
│     │  ├─ UploadService.java        # Validates + persists share metadata
│     │  └─ DownloadService.java      # Read-side operations + pause/resume
│     ├─ core/
│     │  ├─ FileSharer.java           # Orchestrates per-share sender lifecycle
│     │  └─ FileRegistry.java         # In-memory registry (static inner class: FileEntry)
│     └─ sse/
│        └─ SseHub.java               # Broadcast hub for live updates
│
└─ ui/                                # Next.js 14 app router UI
   ├─ src/app/                        # Routes (upload, receive, status)
   ├─ src/components/                 # Drag&drop, progress, invite-code, etc.
   └─ package.json
```

---

## Key Features

- ⚡️ Drag & drop file upload  
- 🔢 Invite code per share (human-readable code or short token)  
- 🔐 Passphrase-protected shares (Option A: store only the hash)  
- ⏳ Expiry & auto-cleanup jobs for stale/finished shares  
- ⏸️ Pause / resume downloads (HTTP range + server state)  
- 📡 Live status via SSE (progress, connected peers, state)  
- 💻 Modern Next.js UI (responsive, minimal)  
- 🌐 CORS-safe backend for local dev and hosted UI  
- 🧰 No rate-limiting yet (intentionally omitted per request)  

---

## Prerequisites

- Java 17+ (build/run the backend)  
- Maven 3.8+ (or Gradle, if you convert)  
- Node.js 18+ & npm (frontend)  
- Git (to version and deploy)  

---

## Getting Started

### Quick Start (Dev)

**Linux/macOS**
```bash
./start.sh
```

**Windows**
```bat
start.bat
```

These scripts should:  
- Build & run the Java backend on `:8080`  
- Install UI deps and run Next.js on `:3000`  

> Adjust the scripts to match your folder layout if needed.

### Manual Setup

#### Backend

```bash
cd backend
mvn clean package
java -jar target/peerlink-1.0-SNAPSHOT.jar
```

Defaults:  
- HTTP server on http://localhost:8080  
- Temporary file dir: ./.peerlink/tmp (configurable)  
- Share expiration: 30–60 minutes (configurable)  

#### Frontend

```bash
cd ui
npm install
npm run dev
```

Open http://localhost:3000

---

## How It Works

### Create a Share (Upload)
1. User drops a file in the UI  
2. UI `POST /api/upload` with file + optional passphrase  
3. Backend creates a `FileEntry` in `FileRegistry`:  
   - Generates invite code and download URL  
   - Hashes passphrase (bcrypt/argon2) and stores hash only  
   - Emits `share.created` over SSE  
4. Backend is now ready to stream bytes on demand  

### Join a Share (Download)
1. Recipient enters invite code in UI  
2. UI fetches metadata `GET /api/shares/{code}/meta`  
3. If passphrase required, UI prompts user and will send it in a header  
4. UI starts download `GET /api/shares/{code}/download` (supports range)  
5. Progress events delivered via SSE  
6. Download can pause/resume using HTTP range and server state  

---

## Architecture (HLD)

```
┌────────────────┐          ┌──────────────────┐           ┌───────────────────┐
│    Next.js UI  │  HTTP    │   Java Backend   │   Local   │     File System   │
│ (upload/recv)  │◄────────►│ (Server + SSE)   │◄─────────►│ (temp storage)    │
└────────────────┘          └──────────────────┘           └───────────────────┘
         ▲                             │
         │ SSE (progress, state)       │
         └─────────────────────────────┘
```

**Upload Flow**
```
UI            Backend                Registry/SSE
│ POST /upload ────────────────────► │ validate, hash pass, persist
│ ◄──────────── 201 + code/meta     │ FileEntry.new(...) in FileRegistry
│                                    │ SseHub.broadcast(share.created)
```

**Download Flow**
```
UI              Backend                 Registry/SSE
│ GET /shares/{code}/meta ───────────► │ lookup, require pass? return meta
│ GET /shares/{code}/download ───────► │ stream via FileSenderHandler
│ ◄──────────────── file bytes         │ SseHub.broadcast(progress, state)
```

---

## High-Level Design (HLD)

- **FileServer**: Boots embedded server, registers routes, installs CORS.  
- **FileController**: Central routing (upload, download, SSE).  
- **UploadService**: Validates, hashes passphrase, stores entry.  
- **DownloadService**: Validates pass, handles pause/resume, tracks progress.  
- **FileRegistry**: In-memory store of all shares (`FileEntry` inner class).  
- **FileSharer**: Lifecycle for per-share sender.  
- **SseHub**: Broadcasts events (share created, progress, state, expired).  
- **Handlers**:  
  - UploadHandler: file upload → UploadService  
  - DownloadHandler: meta + auth + start stream  
  - FileSenderHandler: streams file (supports Range)  
  - SseHandler: subscribes to events  
  - CorsHandler: adds `Access-Control-*`  

---

## Low-Level Design (LLD)

### FileRegistry & FileEntry

```text
Fields:
- id: UUID
- code: Invite code
- filename, mimeType, sizeBytes
- tempPath (Path)
- passHash (bcrypt/argon2)
- createdAt, expiresAt
- bytesSent (AtomicLong)
- state: CREATED, READY, ACTIVE, PAUSED, COMPLETED, EXPIRED, FAILED
- activeConnections (volatile int)
- extras (Map)
```

Ops: `put`, `getByCode`, `remove`, `updateProgress`, `pause`, `resume`.  
Cleanup job removes expired entries + deletes temp files.  

### UploadService

- `createShare(req)`: validate, persist file, generate code, hash pass, create entry, broadcast.  
- Errors: 400 invalid, 413 too large, 500 I/O.  

### DownloadService

- `getMeta(code)` returns filename, size, passphrase requirement.  
- `authorizeAndOpen(code, pass?)`: verifies passphrase, returns stream.  
- `onBytesSent(code, n)`: update progress, emit SSE.  
- `pause/resume`, `complete` → update state + cleanup.  

### Handlers

- **UploadHandler**: parses multipart, calls `UploadService`.  
- **DownloadHandler**: GET meta + download, validates headers.  
- **FileSenderHandler**: efficient ranged file streaming.  
- **SseHandler**: `GET /api/sse`, filters events by code if provided.  
- **CorsHandler**: handles OPTIONS, injects headers.  

---

## API Reference

### Create Share
```http
POST /api/upload
Content-Type: multipart/form-data
```
Fields: `file`, `passphrase` (optional).  
Response:
```json
{
  "code": "AB7K2",
  "filename": "report.pdf",
  "sizeBytes": 1048576,
  "expiresAt": "2025-09-14T12:34:56Z",
  "requiresPassphrase": true
}
```

### Share Metadata
```http
GET /api/shares/{code}/meta
```
Response:
```json
{
  "filename": "demo.zip",
  "sizeBytes": 7340032,
  "requiresPassphrase": true,
  "state": "READY"
}
```

### Download
```http
GET /api/shares/{code}/download
Headers:
  X-Passphrase: <cleartext>
  Range: bytes=1048576-
```

### SSE
```http
GET /api/sse?code={optional}
Accept: text/event-stream
```

---

## Frontend (Next.js 14)

- Pages: `/`, `/receive`, `/status/[code]`.  
- Components: `<DropZone />`, `<InviteCodeCard />`, `<DownloadForm />`, `<ProgressBar />`, `<ResumeToggle />`.  
- Env: `NEXT_PUBLIC_API_BASE=http://localhost:8080`.  

---

## Configuration

- `PORT=8080`  
- `UI_ORIGIN=http://localhost:3000`  
- `TEMP_DIR=.peerlink/tmp`  
- `SHARE_TTL_MINUTES=60`  
- `BCRYPT_COST=12`  

---

## Expiry & Cleanup

- Runs scheduled cleanup job.  
- Marks expired entries, deletes temp files, broadcasts `share.expired`.  

---

## Security Considerations

- Passphrase stored as hash only.  
- Recommend HTTPS (TLS at proxy).  
- Validate mime + size.  
- Restrict CORS to UI origin.  

---

## Deployment

- Local: run backend, access via LAN.  
- Docker: create container with writable volume.  
- Cloud: Render, Fly.io, Railway with Nginx proxy.  
- Static UI: Vercel/Netlify.  

---

## Appendix: State Transitions

```
CREATED → READY → ACTIVE → (PAUSED ↔ ACTIVE)* → COMPLETED
                    │
                    └→ EXPIRED / FAILED
```

---

## License

MIT
