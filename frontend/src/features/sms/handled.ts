const KEY = 'handledSmsRefs'
const MAX = 500

/** Fingerprints of messages the user already added or skipped, so they are not offered again. */
export function loadHandled(): Set<string> {
  try {
    return new Set<string>(JSON.parse(localStorage.getItem(KEY) ?? '[]'))
  } catch {
    return new Set()
  }
}

export function markHandled(ref: string): void {
  const refs = [...loadHandled().add(ref)]
  // Keep the newest entries only; the server still refuses true duplicates beyond this window
  localStorage.setItem(KEY, JSON.stringify(refs.slice(-MAX)))
}
