# LinguaKey 2.1.0

Korean keyboard with full-draft Korean-to-English learning translations.

## This update
- Full editable draft extraction, including text after the cursor and paragraph breaks. Inputs over 6,000 characters fail explicitly, rather than being silently truncated. Offline chunks preserve all input.
- Removed phrase-rule replacement of entire translations. Translation output is scrollable.
- Optional OpenAI Responses / Gemini generateContent integration with personal encrypted API keys, cloud consent, manually configured context and tone, request cancellation and incomplete-response detection. Model names are editable. Defaults: gpt-4.1-mini and gemini-2.5-flash.
- Advanced settings: keyboard height, key spacing, side/bottom padding, font size/letter spacing, corners/borders/themes, touch-down or release input, key preview, vibration duration, sound volume, cursor sensitivity, backspace delay/repeat, double-space period.

## Installation
Install the APK from the Actions artifact. Enable LinguaKey in Android keyboard settings and select it. For cloud translation open Advanced Settings > AI translation, choose provider, save API key locally and enable explicit cloud consent. Set context/tone and run the test button. Personal API access/billing is required separately. Keys must not be committed to this repository.

## Validation
CI runs all unit tests before assembling the APK. Tests cover Hangul composing batches, phrase-preservation regression, lossless chunking, length limits and provider-response parsing. Actual cloud calls require a user API key and have not been live-tested in CI. OEM keyboard UX needs on-device checking.

## Build
JDK 17, Gradle 8.13, Android SDK 36; run `gradle :app:testDebugUnitTest :app:assembleDebug`. GitHub Actions caches the debug signing identity for subsequent builds; this is not a durable production release-signing system.
