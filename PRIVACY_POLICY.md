# Privacy Policy for Lekker

**Last updated: October 28, 2026**

## Overview

Lekker is a Dutch language learning app. We are committed to protecting your privacy. This policy explains what data the app handles and how.

## Data We Do NOT Collect

- No registration information (name, email, phone number, address)
- No account or login required
- No device hardware identifiers (IMEI, MAC address, advertising ID)
- No location data
- No contacts, photos, or media
- No analytics or behavioral tracking
- No advertising SDKs

## Data Stored Locally on Your Device

The app stores the following data locally using Android SharedPreferences. It does not upload your saved history, favorites, flashcard progress, or stored responses to the Lekker backend. Search requests transmit the data described in the next section, and Android may back up local app data as described below.

- **Search history**: Words you look up and the AI-generated explanations
- **Favorites & flashcards**: Words you bookmark, along with spaced-repetition progress (review intervals, ease factors)
- **Saved explanations**: Previously fetched explanations that can be reopened from history and flashcards
- **App settings**: Language preference (Chinese/English), theme, cache toggle, and learning profile weights

You can remove the local copy by clearing the app's data in your device settings or uninstalling the app. System backups are managed separately through your device and backup provider settings, and may restore app data when you reinstall the app.

### Android System Backup and Restore

The app currently enables Android system backup and does not exclude its SharedPreferences from backup. Depending on your Android version, device manufacturer, and backup settings, this data may be included in a cloud backup to your Google Drive account or transferred to another device. This may include saved searches and explanations, favorites, flashcard progress, app settings, learning profile weights, and the random app-generated device ID.

These backups are handled by Android and the backup provider, rather than by the Lekker backend. See [Android's backup documentation](https://developer.android.com/identity/data/autobackup) and your device's backup settings for details.

## Data Transmitted to Our Server

When you search for a Dutch word, the app sends the following to our backend server via HTTPS:

- The **search query** (the Dutch word or phrase you typed)
- A **random device ID** (an app-generated UUID, not derived from hardware identifiers)
- Your **language preference** (Chinese or English)
- Your **learning profile weights** (four numbers controlling the emphasis of AI responses)

The app's AI lookup request includes the data listed above. Android system backup and text-to-speech services may make separate network requests, as described in this policy.

## Third-Party Services

### Cloudflare Workers (Backend Hosting)
Our backend runs on Cloudflare Workers. It processes your search query and forwards it to the AI service. The random device ID is used for rate limiting (50 queries per day) and is included in a dated Cloudflare KV counter key. Each counter expires 24 hours after its last update. The Worker does not save query text or generated explanations in KV. It logs upstream API error responses for troubleshooting; those messages may contain information returned by the AI provider. Log retention depends on the hosting configuration and provider policies.

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
- Local app data is stored in the app's private storage sandbox; Android system backups may contain a copy as described above

## Children's Privacy

Lekker does not knowingly collect any personal information from anyone, including children under the age of 13. The app does not require registration or any personal data input.

## Changes to This Policy

We may update this Privacy Policy from time to time. Any changes will be reflected by updating the "Last updated" date at the top of this page.

## Contact

If you have questions about this Privacy Policy, please contact us at:

**Email**: jacquesqiu@outlook.com

---

*This privacy policy applies to the Lekker Android app available on Google Play.*
