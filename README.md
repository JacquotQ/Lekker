<div align="center">
  <img src="https://github.com/user-attachments/assets/408b787f-17e3-4f8b-98a7-55ed2012c96f" width="200" alt="Lekker Logo">
  <p>An AI-powered Dutch learning app for iOS and Android</p>

  ![Swift](https://img.shields.io/badge/Swift-5.0-orange?logo=swift)
  ![Kotlin](https://img.shields.io/badge/Kotlin-Compose-7F52FF?logo=kotlin)
  ![Cloudflare Workers](https://img.shields.io/badge/Cloudflare-Workers-F38020?logo=cloudflare)
  [![License: CC BY-NC-SA 4.0](https://img.shields.io/badge/License-CC%20BY--NC--SA%204.0-lightgrey.svg)](https://creativecommons.org/licenses/by-nc-sa/4.0/)
</div>

---

## Overview

Lekker helps learners study Dutch vocabulary through AI-generated explanations. Type any Dutch word or phrase and get bilingual definitions, example sentences, memory tricks, etymological connections, and cultural context — all powered by a serverless backend.

## Features

- **AI Explanations** — Bilingual (Chinese + English) definitions with IPA pronunciation
- **Usage Examples** — Everyday spoken vs. formal written, presented in comparison tables
- **Memory Aids** — Root analysis, phonetic mnemonics, and visual associations
- **Language Connections** — Etymological links to English and German
- **Adaptive Learning** — Thumbs up/down feedback adjusts future explanation emphasis
- **Flashcards** — Spaced repetition review for saved vocabulary
- **Favorites & History** — Bookmark words and revisit past lookups
- **Smart Cache** — Identical queries are served locally, reducing API calls
- **Text-to-Speech** — Native pronunciation playback on both platforms

## Architecture

```
┌─────────────┐       ┌────────────────────────┐       ┌───────────┐
│  iOS App    │──────▶│  Cloudflare Workers    │──────▶│  Qwen API │
│  (SwiftUI)  │  HTTPS │  - rate limiting (KV)  │  HTTPS │  (Alibaba)│
├─────────────┤       │  - system prompt build  │       └───────────┘
│ Android App │──────▶│  - SSE streaming proxy  │
│  (Compose)  │       └────────────────────────┘
└─────────────┘
```

Users never handle API keys — everything is proxied through the backend.

## Project Structure

```
├── DutchLearner_iOS/          # SwiftUI iOS app (Xcode 16+)
├── LekkerAndroid/             # Jetpack Compose Android app
├── backend/                   # Cloudflare Workers (TypeScript)
│   ├── src/
│   │   ├── index.ts           # Request routing, rate limiting, SSE proxy
│   │   └── prompt.ts          # System prompt builder
│   ├── wrangler.toml          # Worker configuration
│   └── package.json
├── app.py                     # Legacy web version (Flask)
└── templates/                 # Legacy web frontend
```

## Getting Started

### Backend (Cloudflare Workers)

```bash
cd backend
npm install
npx wrangler secret put QWEN_API_KEY    # Enter your DashScope API key
npx wrangler deploy
```

The worker runs on Cloudflare's free tier (100k requests/day). Rate limiting is set to 50 queries per device per day.

### iOS

Open `DutchLearner_iOS/DutchLearner.xcodeproj` in Xcode 16+ and run on a simulator or device (iOS 17+).

### Android

Open `LekkerAndroid/` in Android Studio and run on an emulator or device (API 26+).

## Tech Stack

| Component | Technology |
|-----------|-----------|
| iOS | SwiftUI, async/await, URLSession SSE |
| Android | Jetpack Compose, OkHttp, Gson |
| Backend | Cloudflare Workers, TypeScript, KV |
| AI Model | Qwen (via DashScope API) |
| Rate Limiting | Cloudflare KV, per-device UUID |

## License

This project is licensed under the [Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International License](https://creativecommons.org/licenses/by-nc-sa/4.0/).

[![CC BY-NC-SA 4.0](https://licensebuttons.net/l/by-nc-sa/4.0/88x31.png)](https://creativecommons.org/licenses/by-nc-sa/4.0/)
