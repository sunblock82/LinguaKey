# LinguaKey privacy design

LinguaKey is a keyboard, so privacy is a primary product requirement.

- Typed message text is not automatically written to disk by LinguaKey.
- Text is read only around the current cursor, and only to derive the active Korean sentence for the learning bar.
- Saved phrases are created only by an explicit press of the star button and are stored in app-private SharedPreferences.
- Password, visible-password, web-password, and numeric-password editor types disable translation, TTS, tips, and saving.
- A manual private mode is always available from the keyboard.
- The app does not request contacts, storage, microphone, location, camera, notification, accessibility, or clipboard permissions.
- Internet permission exists for downloading the ML Kit translation model. Translation runs on-device after the model is available.
- No advertising, analytics, crash-reporting, account, or proprietary cloud SDK is included in v1.
