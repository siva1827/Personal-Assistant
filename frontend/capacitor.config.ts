import type { CapacitorConfig } from '@capacitor/cli'

// Set CAP_DEV=1 when syncing a development build that talks to the API on your computer over
// plain http (for example http://192.168.1.20:8080). Release builds must use https and leave it off.
const dev = process.env.CAP_DEV === '1'

const config: CapacitorConfig = {
  appId: 'com.siva.assistant',
  appName: 'Daily Assistant',
  webDir: 'dist',
  server: dev ? { cleartext: true } : undefined,
  android: dev ? { allowMixedContent: true } : undefined,
}

export default config
