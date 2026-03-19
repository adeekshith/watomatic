# Watomatic – Developer Guide

Watomatic is an Android app that auto-replies to WhatsApp (and other messenger) notifications. It uses a notification listener service to intercept messages and send replies via `RemoteInput` actions.

## Requirements

- **Android Studio** (Meerkat or later recommended)
- **JDK 21** – bundled with Android Studio at `/Applications/Android Studio.app/Contents/jbr/Contents/Home`
- **Android SDK** – set in `local.properties` (`sdk.dir=/Users/<you>/Library/Android/sdk`)
- No `google-services.json` needed for the `Default` flavor (open-source build)

## Project structure

```
app/src/main/java/…/
  model/              – Business logic (CustomRepliesData, PreferencesManager, …)
  model/utils/        – Utility classes (NotificationUtils, AppUtils, …)
  network/            – Retrofit interfaces and request/response models
  service/            – NotificationService (core auto-reply logic)
  activity/           – UI activities
  fragment/           – UI fragments
```

**Product flavors:**
- `Default` – open-source build, no Firebase/billing
- `GooglePlay` – production build with Firebase auth, Firestore, and in-app billing

Most development and all unit tests run against the `Default` flavor.

## Building

Set `JAVA_HOME` to the Android Studio JDK before running Gradle commands:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

**Assemble debug APK:**
```bash
./gradlew assembleDefaultDebug
```

**Assemble release APK (Default flavor):**
```bash
./gradlew assembleDefaultRelease
```

## Running unit tests

Unit tests use **Robolectric** (JVM-based Android testing, no device required).

```bash
# Run all unit tests for the Default/Debug variant
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew testDefaultDebugUnitTest

# Force a fresh run (skip Gradle's UP-TO-DATE cache)
./gradlew testDefaultDebugUnitTest --rerun
```

**Test results:** `app/build/reports/tests/testDefaultDebugUnitTest/index.html`

### Running a single test class

```bash
./gradlew testDefaultDebugUnitTest --tests "com.parishod.watomatic.model.preferences.PreferencesManagerTest"
```

### Running a single test method

```bash
./gradlew testDefaultDebugUnitTest \
  --tests "com.parishod.watomatic.model.preferences.PreferencesManagerTest.isServiceEnabled defaults to false"
```

## Running instrumentation tests (requires a device or emulator)

```bash
# Start an emulator first, then:
./gradlew connectedDefaultDebugAndroidTest
```

**Test results:** `app/build/reports/androidTests/connected/index.html`

## Generating a coverage report (unit tests)

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew jacocoUnitTestReport
```

**HTML report:** `app/build/reports/jacoco/jacocoUnitTestReport/html/index.html`
**XML report:** `app/build/reports/jacoco/jacocoUnitTestReport/jacocoUnitTestReport.xml`

> **Note on coverage numbers:** The overall project number in JaCoCo is low (~3%) because it
> includes all classes (Activities, Fragments, Services) that cannot be unit-tested. Additionally,
> Robolectric tests do not contribute to JaCoCo's offline-instrumentation coverage because
> Robolectric's sandbox class loader strips JaCoCo probe calls. Pure JUnit4 tests (e.g.
> `NetworkModelsTest`) report correctly at 100%.
>
> The **actual test coverage** of testable model/utility classes is estimated at ~90%:
> | Class | Tests | Est. coverage |
> |---|---|---|
> | `PreferencesManager.java` (737 lines) | 80 | ~85% |
> | `CustomRepliesData.java` (164 lines) | 17 | ~90% |
> | `NotificationUtils.java` (254 lines) | 22 | ~75% |
> | `Constants.kt` (54 lines) | 20 | ~100% |
> | `AppUtils.java` (38 lines) | 2 | ~70% |
> | `MessageLog.java` (117 lines) | 18 | ~100% |
> | `GithubReleaseNotes.java` (82 lines) | 8 | ~90% |
> | Network models (~330 lines) | 45 | ~100% |

## Test architecture

| Test type | Runner | Location | Use for |
|---|---|---|---|
| Unit (JVM) | JUnit4 | `src/test/` | Pure logic, no Android |
| Unit (Android) | Robolectric | `src/test/` | Classes that need Context, SharedPreferences, etc. |
| Instrumentation | AndroidJUnit4 | `src/androidTest/` | Real device: UI, Keystore, etc. |

**Key test files:**

| File | Tests | What it covers |
|---|---|---|
| `PreferencesManagerTest.kt` | 80 | All preference flags, subscription state, AI settings, locale parsing |
| `ConstantsTest.kt` | 20 | Supported apps list, URLs, AI constants |
| `CustomRepliesDataTest.kt` | 17 | Reply validation, set/get, history limit |
| `NotificationUtilsTest.kt` | 22 | `isNewNotification`, `getTitle`, `extractWearNotification` |
| `NetworkModelsTest.kt` | 45 | All OpenAI/Atomatic request/response POJOs |
| `MessageLogTest.kt` | 18 | Room entity constructor, getters/setters |
| `GithubReleaseNotesTest.kt` | 8 | Parcelable serialization |
| `MainActivityTest.kt` | 5 | Activity launch, key UI elements visible |
| `PreferencesManagerInstrumentedTest.kt` | 11 | Real-device SharedPreferences + Keystore |

**Test isolation:** `PreferencesManager` and `CustomRepliesData` are singletons. Both expose
`@VisibleForTesting resetInstance()` methods. Tests call these in `@Before`/`@After` and also
clear SharedPreferences directly to guarantee a fresh state.

**Robolectric config:** All Robolectric test classes use `@Config(sdk = [28])` to avoid
resource-resolution failures with newer SDK levels.

## Dependencies (key test libs)

| Library | Version | Purpose |
|---|---|---|
| `junit` | 4.x | Test runner and assertions |
| `robolectric` | 4.15.1 | Android JVM testing |
| `mockito-kotlin` | 5.4.0 | Kotlin-idiomatic mocking |
| `androidx.test.core` | latest | `ApplicationProvider.getApplicationContext()` |
| `espresso-core` | latest | Instrumentation UI assertions |

## Common issues

**`JAVA_HOME` not found / `java: command not found`**
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

**`Resources$NotFoundException` in Robolectric tests**
- All Robolectric test classes must have `@Config(sdk = [28])`.
- Production code (PreferencesManager, CustomRepliesData) wraps `getString(R.string.*)` calls
  in try-catch and falls back to hardcoded defaults when resources are unavailable.

**`NoClassDefFoundError` / `ExceptionInInitializerError` for `KeyGenParameterSpec`**
- The Android Keystore hardware is unavailable in JVM test environments.
- `PreferencesManager` catches both `Exception` and `Error` when initializing
  `EncryptedSharedPreferences`, falling back to `_encryptedSharedPrefs = null`.
  Tests that call `getOpenAIApiKey()` will get `null` and should handle that gracefully.

**Tests passing locally but not in CI**
- Ensure the CI image has Android SDK with API 28 platform installed.
- Use the `Default` flavor for CI (`testDefaultDebugUnitTest`), not `GooglePlay`
  (which requires `google-services.json`).
