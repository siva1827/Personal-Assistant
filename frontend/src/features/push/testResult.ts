/** What POST /devices/test returns. */
export interface PushTestResult {
  /** False when the server has no Firebase credentials. */
  configured: boolean
  /** Devices registered for this account. */
  devices: number
  /** Devices Firebase accepted the message for. */
  delivered: number
}

export interface TestOutcome {
  ok: boolean
  message: string
}

/** Turns the server's answer into the one thing the user should do or know next. */
export function describeTestResult(result: PushTestResult): TestOutcome {
  if (!result.configured) {
    return {
      ok: false,
      message: 'The server is not set up for push yet. Add the Firebase service-account key (see docs/FIREBASE_SETUP.md).',
    }
  }
  if (result.devices === 0) {
    return { ok: false, message: 'This phone is not registered yet. Switch push off and on again, then retry.' }
  }
  if (result.delivered === 0) {
    return {
      ok: false,
      message:
        'Firebase did not accept the message. Check that google-services.json and the service-account key belong to the same Firebase project.',
    }
  }
  return { ok: true, message: 'Sent. It should appear in a moment, even if you leave the app.' }
}

/** Capacitor reports "android" or "ios"; the API expects upper case names. */
export function apiPlatform(capacitorPlatform: string): 'ANDROID' | 'IOS' | 'WEB' {
  if (capacitorPlatform === 'android') return 'ANDROID'
  if (capacitorPlatform === 'ios') return 'IOS'
  return 'WEB'
}
