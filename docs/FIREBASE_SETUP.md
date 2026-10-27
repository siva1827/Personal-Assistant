# Firebase push notifications: setup guide

This switches on push messages that reach your phone **even when the app is closed**: a task due
today, a budget near its limit, food about to expire.

You need a free Google account and about 15 minutes. Firebase Cloud Messaging (FCM) is free and
does **not** require a billing plan.

The code is already in place. Without the two files below the app behaves exactly as before and
the Profile screen says push is not set up. Nothing breaks.

## How it works

```
 task due / budget near / food expiring
                |
        backend creates a notification  ---- shown in the app (bell) as always
                |
        backend asks Firebase to deliver it   <-- needs the SERVICE-ACCOUNT KEY (server only)
                |
        Firebase wakes the phone              <-- needs google-services.json (inside the app)
                |
        Android shows the notification; tapping it opens the right screen
```

Two separate files connect the two ends, and they must come from the **same Firebase project**:

| File | Goes where | Secret? |
| --- | --- | --- |
| `google-services.json` | `frontend/android/app/google-services.json` (inside the Android app) | Not secret, but git ignores it |
| Service-account key (`...firebase-adminsdk....json`) | On the machine that runs the backend, **outside the repo** | **Yes. Treat like a password** |

## Step 1. Create the Firebase project

1. Go to https://console.firebase.google.com and sign in.
2. Click **Create a project** (or **Add project**). Name it, for example, `daily-assistant`.
3. Google Analytics is not needed: switch it off, then **Create project**.

## Step 2. Add the Android app and get `google-services.json`

1. On the project home page click the **Android** icon (**Add app**).
2. **Android package name**: type exactly `com.siva.assistant` (it must match the app).
3. App nickname is optional. Leave SHA-1 empty (not needed for push). Click **Register app**.
4. Click **Download google-services.json**.
5. Copy it to **`frontend/android/app/google-services.json`**.
6. Click **Next** through the remaining screens. The Gradle setup they describe is already done.

## Step 3. Create the service-account key for the server

1. Click the **gear icon** next to *Project Overview*, then **Project settings**.
2. Open the **Service accounts** tab.
3. Click **Generate new private key**, then **Generate key**. A `.json` file downloads.
4. Move it somewhere safe **outside this repository**, for example
   `C:\secrets\firebase-service-account.json` or `~/secrets/firebase-service-account.json`.
5. In **Project settings, Cloud Messaging**, check that **Firebase Cloud Messaging API (V1)** says
   *Enabled* (it is by default for new projects). The old "Cloud Messaging API (Legacy)" is not
   used and can stay disabled.

> Never commit, email or paste this key. It can send messages as your project. If it ever leaks,
> delete it under **Project settings, Service accounts, Manage service account permissions**, open
> the account's **Keys** tab, delete the key, and generate a new one.

## Step 4. Start the backend with the key

Set `FIREBASE_CREDENTIALS_FILE` to the path of the key file, in the same terminal that runs the API.

**macOS / Linux / Git Bash**

```bash
export FIREBASE_CREDENTIALS_FILE="$HOME/secrets/firebase-service-account.json"
mvn spring-boot:run
```

**Windows PowerShell**

```powershell
$env:FIREBASE_CREDENTIALS_FILE = 'C:\secrets\firebase-service-account.json'
mvn spring-boot:run
```

On startup the log must say:

```
Push notifications are on for Firebase project daily-assistant
```

If it says `Push notifications are off: no Firebase credentials configured`, the variable is not set
in that terminal. If the file path is wrong the API refuses to start and tells you it cannot read
the credentials, so a typo cannot go unnoticed.

Hosting somewhere files are awkward (Railway, Render)? Put the **whole contents** of the key file
in an environment variable called `FIREBASE_CREDENTIALS_JSON` instead.

## Step 5. Rebuild the Android app

`google-services.json` is read when the app is built, so rebuild and reinstall after adding it.
The commands are in [LOCAL_SETUP.md](LOCAL_SETUP.md), section 10.3 (`CAP_DEV=1 npm run android:sync`,
then `gradle assembleDebug`, then install the APK).

## Step 6. Turn it on and test

1. Open the app on the phone and sign in.
2. **Profile, Push notifications**: switch it on and tap **Allow** when Android asks.
3. Tap **Send a test notification**. Within a few seconds the phone should show *Test notification*.
   Try it once with the app open and once after closing it.
4. For a real reminder: add a task due **today**, then open the app (or wait up to 30 minutes for
   the scheduled run). A push arrives, and tapping it opens the Tasks screen.

The Profile screen tells you what is wrong if the test fails:

| Message | Meaning and fix |
| --- | --- |
| *The server is not set up for push yet* | Step 4 not done, or set in a different terminal. |
| *This phone is not registered yet* | Switch the toggle off and on. The phone must reach the API (see LOCAL_SETUP, section 9). |
| *Firebase did not accept the message* | `google-services.json` and the service-account key come from different projects, or the key was revoked. Download both again from the same project. |
| *Could not set up push* | The app was built without `google-services.json` (redo Step 5), the phone has no Google Play services (an emulator needs a "Google Play" system image), or there is no network. |

## Troubleshooting

- **Nothing appears on the phone but the test says Sent.** Check Android settings, Apps, Daily
  Assistant, Notifications, are allowed. Some phone makers (Xiaomi, Oppo, Samsung power saving)
  stop background apps: set the app's battery use to *Unrestricted*.
- **Server log shows `Could not send via FCM: ... 400` or `401`.** The key file is wrong or revoked,
  or the computer's clock is far off (the key signs a time-limited token). Fix the clock or
  generate a new key.
- **A device stops receiving after reinstalling the app.** Expected: the old token dies and is
  deleted automatically the first time Firebase reports it. Open the app and the new one registers.
- **Signing out** removes this phone from your account on the server, so the next person using it
  does not receive your reminders. Signing back in registers it again.

## What is not included

- **iPhone**: needs an Apple developer account and an APNs key uploaded to Firebase, plus an iOS
  build. The server side already accepts iOS tokens, but this has not been set up or tested.
- **Browsers**: web push needs a service worker and a VAPID key. Use *Device alerts* on the Profile
  screen, which works while the app is open.
- Messages are sent for the same events that create in-app notifications; there are no quiet hours
  or per-type switches yet.
