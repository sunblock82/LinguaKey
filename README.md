# LinguaKey — Korean → English learning IME

LinguaKey is an Android input method designed for language learning: the text field keeps the Korean text the user actually intends to send, while a learning bar above the keyboard continuously shows an English translation.

## What v1 includes

- Independent Android IME (`InputMethodService`)
- Korean 2-beolsik Hangul composition + English layout toggle
- Debounced Korean→English translation shown only in the keyboard UI
- ML Kit on-device translation after model download
- Password/PIN/private-field detection: learning panel disabled
- Manual private mode
- English TTS pronunciation
- Star/save phrase locally; no automatic message history
- Due-card review screen with simple spaced intervals
- Conservative curated expression tips
- No clipboard reading, no analytics SDK, no account, no cloud database

## Build

Recommended: Android Studio Quail or newer, JDK 17+, Android SDK 36.

1. Open this folder in Android Studio.
2. Let Gradle sync.
3. Build > Build APK(s).
4. Install on the phone.
5. Open LinguaKey > `1. 키보드 활성화` and enable LinguaKey.
6. Tap `2. 사용할 키보드 선택` and choose LinguaKey.
7. Connect to Wi-Fi the first time so the Korean/English translation model can download.

The ML Kit translation dependency is `com.google.mlkit:translate:17.0.3` and requires minSdk 23+.

## Privacy model

The keyboard deliberately does not persist typed text. It only stores a pair when the user presses ☆. Sensitive editor types (password/web-password/number-password) disable the learning bar. A manual private mode is also available.

ML Kit may use configured source/destination language metadata for diagnostics according to Google's ML Kit disclosure documentation; the app itself adds no analytics SDK.

## Known v1 limitations

- This is a fresh IME and has not yet been device-tested on every OEM/app combination.
- Autocorrection, swipe typing, emoji search, multilingual prediction, hardware keyboard handling, and accessibility polishing are future work.
- ML Kit translation is optimized for casual translation; subtle conversational nuance should be validated before treating every result as authoritative learning material.
- The symbol layer covers common punctuation and numbers; emoji search and advanced symbols are future work.

## Architecture

- `LinguaKeyImeService`: IME lifecycle + keyboard/learning bar
- `HangulComposer`: Korean composition state machine
- `TranslatorManager`: ML Kit model/translation lifecycle
- `SensitiveFieldDetector`: privacy gating
- `SentenceExtractor`: extracts only the active sentence near cursor
- `ExpressionCoach`: curated learning tips
- `PhraseStore` / `ReviewActivity`: opt-in local learning history and review
