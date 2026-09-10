# Research/design notes (2026-09-10)

1. Android's supported architecture for a system-wide keyboard is an app containing a service derived from `InputMethodService`. Text is exchanged with editors through `InputConnection`, including `getTextBeforeCursor`, `commitText`, and composing text APIs.
2. `EditorInfo.inputType` exposes editor content type and can be used to detect password modes. LinguaKey treats those inputs as sensitive and disables learning behavior.
3. ML Kit translation supports 50+ languages, dynamic on-device model download, and Android API 23+. Current documented Android dependency: `com.google.mlkit:translate:17.0.3`. Language packs are roughly 30 MB.
4. Android `TextToSpeech` provides immediate speech synthesis; LinguaKey uses it only on an explicit tap.
5. Current Android Studio Quail releases support modern AGP generations. This project pins AGP 8.13.2 / Gradle 8.13-compatible configuration rather than chasing the newest plugin, to reduce compatibility risk.

Official references:
- https://developer.android.com/develop/ui/views/touch-and-input/creating-input-method
- https://developer.android.com/reference/android/inputmethodservice/InputMethodService
- https://developer.android.com/reference/android/view/inputmethod/EditorInfo
- https://developers.google.com/ml-kit/language/translation/android
- https://developers.google.com/ml-kit/android-data-disclosure
- https://developer.android.com/reference/android/speech/tts/TextToSpeech
