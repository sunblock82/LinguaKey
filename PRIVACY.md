# LinguaKey 2.1 privacy

- Offline translation uses ML Kit on-device after model download.
- Optional GPT/Gemini translation requires selecting a provider, explicit cloud consent and a personal API key. The complete current editable draft (up to 6,000 characters) and the manually configured context/tone are sent to that provider after an adjustable typing pause. No automatic reading of received chat messages, screenshots, clipboard or other apps.
- Cloud mode does not silently fall back to offline translations. Errors and incomplete API outputs are shown explicitly. OpenAI requests set store=false; provider retention and billing policies still apply.
- Personal API keys are stored encrypted with Android Keystore and excluded from backups. No developer key is bundled. This build is for personal use; a public multi-user service should use an authenticated backend.
- Input is not automatically stored. Only explicitly starred phrases are saved; optional statistics store counts. Configured context and tone persist until edited or cleared.
- Sensitive editor fields and manual private mode disable learning/translation. Pending cloud requests are cancelled on private-mode entry, input changes and keyboard dismissal; cancellation cannot retract data already sent.
- Network activity uses HTTPS; no analytics SDK is added.
