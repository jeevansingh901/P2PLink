# P2PLink
P2PLink is a peer-to-peer style file sharing app that lets users share files directly using a simple invite code. It runs a lightweight Java HTTP server that spins up ephemeral per-share senders, and a modern Next.js frontend for a clean, minimal UI.

Project Structure
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
└─ ui/                                 # Next.js 14 app router UI
   ├─ src/app/                         # Routes (upload, receive, status)
   ├─ src/components/                  # Drag&drop, progress, invite-code, etc.
   └─ package.json


Note: The exact package names may differ in your repo; structure above mirrors the uploaded files and their responsibilities.

Key Features

⚡️ Drag & drop file upload

🔢 Invite code per share (human-readable code or short token)

🔐 Passphrase-protected shares (Option A: store only the hash)

⏳ Expiry & auto-cleanup jobs for stale/finished shares

⏸️ Pause / resume downloads (HTTP range + server state)

📡 Live status via SSE (progress, connected peers, state)

💻 Modern Next.js UI (responsive, minimal)

🌐 CORS-safe backend for local dev and hosted UI

🧰 No rate-limiting yet (intentionally omitted per request)

Prerequisites

Java 17+ (build/run the backend)

Maven 3.8+ (or Gradle, if you convert)

Node.js 18+ & npm (frontend)

Git (to version and deploy)

Getting Started
Quick Start (Dev)

Linux/macOS

./start.sh


Windows

start.bat


These scripts should:

Build & run the Java backend on :8080

Install UI deps and run Next.js on :3000

Adjust the scripts to match your folder layout if needed.

Manual Setup
Backend
cd backend
mvn clean package
java -jar target/peerlink-1.0-SNAPSHOT.jar


Defaults:

HTTP server on http://localhost:8080

Temporary file dir: ./.peerlink/tmp (configurable)

Share expiration: 30–60 minutes (configurable)

Frontend
cd ui
npm install
npm run dev


Open http://localhost:3000
.

How It Works
Create a Share (Upload)

User drops a file in the UI

UI POST /api/upload with file + optional passphrase

Backend creates a FileEntry in FileRegistry:

Generates invite code and download URL

Hashes passphrase (bcrypt/argon2) and stores hash only

Emits share.created over SSE

Backend is now ready to stream bytes on demand

Join a Share (Download)

Recipient enters invite code in UI

UI fetches metadata GET /api/shares/{code}/meta

If passphrase required, UI prompts user and will send it in a header

UI starts download GET /api/shares/{code}/download (supports range)

Progress events delivered via SSE

Download can pause/resume using HTTP range and server state

Architecture
┌────────────────┐          ┌──────────────────┐           ┌───────────────────┐
│    Next.js UI  │  HTTP    │   Java Backend   │   Local   │     File System   │
│ (upload/recv)  │◄────────►│ (Server + SSE)   │◄─────────►│ (temp storage)    │
└────────────────┘          └──────────────────┘           └───────────────────┘
         ▲                             │
         │ SSE (progress, state)       │
         └─────────────────────────────┘


Upload sequence (simplified)

UI            Backend                Registry/SSE
│ POST /upload ────────────────────► │ validate, hash pass, persist
│ ◄──────────── 201 + code/meta     │ FileEntry.new(...) in FileRegistry
│                                    │ SseHub.broadcast(share.created)


Download sequence (simplified)

UI              Backend                 Registry/SSE
│ GET /shares/{code}/meta ───────────► │ lookup, require pass? return meta
│ GET /shares/{code}/download ───────► │ stream via FileSenderHandler
│ ◄──────────────── file bytes         │ SseHub.broadcast(progress, state)

High-Level Design (HLD)
Components & Responsibilities

FileServer

Boots embedded HTTP server

Registers routes via FileController

Installs CorsHandler

FileController

Wires handlers to endpoints:

POST /api/upload → UploadHandler

GET /api/shares/{code}/meta → DownloadHandler

GET /api/shares/{code}/download → FileSenderHandler

GET /api/sse → SseHandler

UploadService

Validates file, computes metadata (size, mime)

Hashes passphrase (bcrypt/argon2)

Allocates invite code (short token)

Persists FileEntry in FileRegistry

Schedules expiry timestamp

DownloadService

Looks up by code

Validates passphrase (hash compare)

Manages pause/resume via range headers + state

Emits progress events

FileSharer

Orchestrates per-share sender (open/close/lifecycle)

Coordinates with FileSenderHandler for byte range reads

FileRegistry

Thread-safe in-memory store of shares

Static inner class FileEntry holds all metadata/state

Periodic cleanup of expired/finished entries

SseHub / SseHandler

In-memory broadcaster for live updates

Multiplexes events per share and global events

CorsHandler

Adds Access-Control-* headers for UI origin

Data Storage

Ephemeral temp files on local FS

In-memory registry for share metadata

No external DB (simple + fast for single host)

Security

Option A: Passphrase per share (hash only, no plaintext)

Requires passphrase to download

Content served over HTTPS in production (terminate at proxy)

CORS restricted to configured UI origin

(Intentionally) no rate-limit yet

Observability

SSE stream for live progress + state

Structured logs for requests, errors, and cleanup runs

Low-Level Design (LLD)

Based on the uploaded classes; names/fields can be adjusted to your exact code.

FileRegistry & FileEntry (static inner class)

Purpose: central source of truth for shares.

Typical fields in FileEntry:

String id (UUID)

String code (invite code)

String filename

String mimeType

long sizeBytes

Path tempPath

String passHash (bcrypt/argon2)

Instant createdAt

Instant expiresAt

AtomicLong bytesSent

ShareState state (CREATED, READY, ACTIVE, PAUSED, COMPLETED, EXPIRED, FAILED)

volatile int activeConnections (for single/multi client policy)

Map<String,Object> extras (optional)

Core ops:

put(entry), getByCode(code), remove(id)

updateProgress(entry, delta), pause(entry), resume(entry)

Cleanup job: runs every N minutes → remove expired entries, delete temp files

Threading:

Use ConcurrentHashMap for registry

Use Atomic* for counters / progress

UploadService

createShare(UploadRequest req)

Validate file (size, mime allowlist)

Persist to temp dir

Generate code (4–8 chars)

Hash passphrase: passHash = bcrypt(pass)

Create FileEntry and store in FileRegistry

Emit share.created via SseHub

Return DTO: { code, filename, size }

cancelShare(code) (optional)

Mark as FAILED + cleanup

Errors: 400 for invalid input, 413 for too large, 500 on IO failure

DownloadService

getMeta(code)

Lookup FileEntry; return DTO { filename, size, requiresPassphrase }

authorizeAndOpen(code, pass?)

Verify passphrase (bcrypt verify against passHash)

If ok: return a stream handle + content length

Set state ACTIVE if first byte range starts

onBytesSent(code, n)

Increase bytesSent

Emit progress via SseHub

pause(code) / resume(code)

Update state and emit SSE (share.paused/share.resumed)

complete(code)

Set state COMPLETED

Schedule deletion & registry removal

FileSenderHandler

Reads Range headers for resume support

Uses FileChannel/RandomAccessFile for efficient range reads

Streams with sensible chunk size (e.g., 256 KiB)

Sets headers:

Content-Type, Content-Length, Accept-Ranges: bytes, Content-Disposition

On partial: 206 Partial Content + Content-Range

On each write:

Notifies DownloadService.onBytesSent(code, chunkSize)

UploadHandler

Multipart parse (file, optional passphrase)

Calls UploadService.createShare

Returns 201 Created + JSON body:

{
  "code": "AB7K2",
  "filename": "report.pdf",
  "sizeBytes": 1048576,
  "expiresAt": "2025-09-14T12:34:56Z"
}

DownloadHandler

GET /api/shares/{code}/meta

Returns metadata

GET /api/shares/{code}/download

Reads X-Passphrase header (or query/body if you chose)

Delegates to DownloadService.authorizeAndOpen

Streams using FileSenderHandler

SseHub & SseHandler

SseHub

Maintains list of connected sinks/emitters

Topics: share.created, progress, state, share.expired

API: broadcast(eventType, payload[, code])

SseHandler

Endpoint: GET /api/sse?code={optional}

If code provided → filter events for that share

Heartbeats/ping to keep connection alive

Proper text/event-stream headers

FileController

Central place to register routes:

POST /api/upload → UploadHandler

GET /api/shares/{code}/meta → DownloadHandler

GET /api/shares/{code}/download → FileSenderHandler

GET /api/sse → SseHandler

Attaches CorsHandler to all

CorsHandler

Adds:

Access-Control-Allow-Origin: <UI_ORIGIN>

Access-Control-Allow-Headers: Content-Type, X-Passphrase, Range

Access-Control-Allow-Methods: GET, POST, OPTIONS

Access-Control-Expose-Headers: Content-Length, Content-Range, Content-Disposition

Handles OPTIONS preflights with 204

API Reference (typical)
Create Share
POST /api/upload
Content-Type: multipart/form-data

fields:
  file: <binary>
  passphrase: <string> (optional)


201 Response

{
  "code": "J7Q2Z",
  "filename": "demo.zip",
  "sizeBytes": 7340032,
  "expiresAt": "2025-09-14T13:45:00Z",
  "requiresPassphrase": true
}

Share Metadata
GET /api/shares/{code}/meta


200

{
  "filename": "demo.zip",
  "sizeBytes": 7340032,
  "requiresPassphrase": true,
  "state": "READY"
}

Download
GET /api/shares/{code}/download
Headers:
  X-Passphrase: <cleartext>    # verified against stored hash
  Range: bytes=1048576-        # optional for resume


Responses

200 OK (full) or 206 Partial Content (resumed)

401/403 if passphrase missing/invalid

410 Gone if expired

404 if not found

SSE
GET /api/sse?code={optional}
Accept: text/event-stream


Event samples

event: share.created
data: {"code":"J7Q2Z","filename":"demo.zip"}

event: progress
data: {"code":"J7Q2Z","bytesSent":1048576,"sizeBytes":7340032}

event: state
data: {"code":"J7Q2Z","state":"PAUSED"}

Frontend (Next.js 14)

Pages:

/ upload page (drag & drop, passphrase input, shows invite code)

/receive (enter code, prompt for passphrase if needed)

/status/[code] (live progress via SSE)

Components:

<DropZone /> (drag/drop + multipart POST)

<InviteCodeCard /> (share code + copy)

<DownloadForm /> (code + passphrase)

<ProgressBar /> (subscribes to /api/sse?code=...)

<ResumeToggle /> (pause/resume UI; client sends header/range logic)

Env:

NEXT_PUBLIC_API_BASE=http://localhost:8080

If you saw the “Unknown font Geist” error earlier, replace Geist with a built-in font (Inter) or install geist properly.

Configuration

Environment variables (typical):

PORT=8080 — backend port

UI_ORIGIN=http://localhost:3000 — for CORS

TEMP_DIR=.peerlink/tmp

SHARE_TTL_MINUTES=60

BCRYPT_COST=12

Expiry & Cleanup

A scheduled task runs every N minutes:

Marks entries past expiresAt as EXPIRED

Deletes temp file(s)

Removes entry from registry

Emits share.expired via SSE

Security Considerations

Passphrase stored as hash only (bcrypt/argon2)

Recommend HTTPS in prod (TLS at proxy—e.g., Nginx)

Validate mime & size on upload

Expose minimal headers

Consider future additions:

Rate limiting / IP throttling

Virus scanning (ClamAV)

Encrypted at rest (temp dir on encrypted volume)

Deployment

Local network: run backend on a host; share invite code across LAN

Docker: create Dockerfile and mount a writable temp volume

Cloud: Render, Fly.io, Railway, etc. (Nginx/Traefik as reverse proxy)

Static UI: Vercel/Netlify, pointing to your backend URL

For a full step-by-step Render + Nginx guide, see DEPLOYMENT.md (create next).

License

MIT

Repo Meta (example)
Languages
Java       ~51%
TypeScript ~30%
Shell      ~13.3%
JavaScript ~2.8%
CSS        ~1.7%
Batchfile  ~1.2%

Appendix: States
CREATED → READY → ACTIVE → (PAUSED ↔ ACTIVE)* → COMPLETED
                    │
                    └→ EXPIRED / FAILED


READY: uploaded & waiting for receiver

ACTIVE: bytes are flowing

PAUSED: receiver paused via UI

COMPLETED: all bytes sent

EXPIRED: TTL elapsed without completion

FAILED: unrecoverable error (I/O, etc.)
