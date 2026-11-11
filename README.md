# Naurok AI Android project

## Build APK through GitHub Actions (phone only)

1. Create a **public** GitHub repository, for example `NaurokAI`.
2. Extract this ZIP on your phone.
3. In the repository choose **Add file → Upload files** and upload the extracted project files, including the hidden `.github/workflows/android.yml`.
4. If your file picker hides `.github`, create a new file in GitHub named `.github/workflows/android.yml` and paste the workflow contents from the ZIP.
5. Commit the files.
6. Open **Actions** → **Build Android APK** → **Run workflow** → **Run workflow**.
7. After it finishes, open the run and download the `NaurokAI-debug-apk` artifact. Extract that artifact ZIP to get `app-debug.apk`.

Do not put API keys or passwords in the repository.

This starter app explains pasted study questions using an OpenAI-compatible Chat Completions API. It does not capture the screen, perform OCR, or automatically select answers.
