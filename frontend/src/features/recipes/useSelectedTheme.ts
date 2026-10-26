import { useCallback, useState } from 'react'

const KEY = 'selectedThemeId'

/** The day theme the user picked, remembered across reloads. */
export function useSelectedTheme() {
  const [themeId, setThemeId] = useState<number | null>(() => {
    const stored = localStorage.getItem(KEY)
    return stored ? Number(stored) : null
  })

  const select = useCallback((id: number | null) => {
    setThemeId(id)
    if (id === null) localStorage.removeItem(KEY)
    else localStorage.setItem(KEY, String(id))
  }, [])

  return [themeId, select] as const
}
