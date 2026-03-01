# Privacy Policy for Lekker

**Last updated: March 1, 2026**

## Overview

Lekker is a Dutch language learning app. We are committed to protecting your privacy. This policy explains what data the app handles and how.

## Data We Do NOT Collect

- No personal information (name, email, phone number, address)
- No account or login required
- No device hardware identifiers (IMEI, MAC address, advertising ID)
- No location data
- No contacts, photos, or media
- No analytics or behavioral tracking
- No advertising SDKs

## Data Stored Locally on Your Device

The following data is stored **only on your device** using Android SharedPreferences and never uploaded to any server:

- **Search history**: Words you look up and the AI-generated explanations
- **Favorites & flashcards**: Words you bookmark, along with spaced-repetition progress (review intervals, ease factors)
- **Cached responses**: Previously fetched explanations for faster access
- **App settings**: Language preference (Chinese/English), theme, cache toggle, and learning profile weights

You can clear this data at any time by clearing the app's data in your device settings or uninstalling the app.

## Data Transmitted to Our Server

When you search for a Dutch word, the app sends the following to our backend server via HTTPS:

- The **search query** (the Dutch word or phrase you typed)
- A **random device ID** (a randomly generated UUID, not linked to your identity or device hardware)
- Your **language preference** (Chinese or English)
- Your **learning profile weights** (four numbers controlling the emphasis of AI responses)

This is the **only** network request the app makes. No other data is ever transmitted.

## Third-Party Services

### Cloudflare Workers (Backend Hosting)
Our backend runs on Cloudflare Workers. It processes your search query and forwards it to the AI service. The random device ID is used solely for rate limiting (50 queries per day) and is stored in Cloudflare KV with automatic deletion after 24 hours. No query content is logged or stored on the server.

### Alibaba Cloud DashScope / Qwen AI
Your search queries are processed by Alibaba Cloud's Qwen AI language model to generate Dutch language explanations. Queries are sent to Alibaba Cloud's API via our backend server. Please refer to [Alibaba Cloud's Privacy Policy](https://www.alibabacloud.com/help/en/legal/latest/chinese-mainland-Chinese-version-Chinese-privacy-policy) for their data practices.

### Android Text-to-Speech
The app uses Android's built-in Text-to-Speech engine for Dutch pronunciation. This is an on-device system service. Note that some device manufacturers may use cloud-based TTS engines at the system level, which is outside the app's control.

## Permissions

The app requires only one permission:

- **INTERNET**: Required to fetch AI-generated word explanations from the backend server.

No other permissions (camera, microphone, location, storage, contacts, etc.) are requested.

## Data Security

- All network communication uses **HTTPS encryption** (TLS)
- No cleartext traffic is permitted
- API keys are stored as server-side secrets and never exposed to the client
- All user data remains in the app's private storage sandbox

## Children's Privacy

Lekker does not knowingly collect any personal information from anyone, including children under the age of 13. The app does not require registration or any personal data input.

## Changes to This Policy

We may update this Privacy Policy from time to time. Any changes will be reflected by updating the "Last updated" date at the top of this page.

## Contact

If you have questions about this Privacy Policy, please contact us at:

**Email**: jacquesqiu@outlook.com

---

*This privacy policy applies to the Lekker Android app available on Google Play.*
