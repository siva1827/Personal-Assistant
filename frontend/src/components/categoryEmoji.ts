const EMOJI: Record<string, string> = {
  vegetable: '🥦',
  fruit: '🍎',
  dairy: '🥛',
  meat: '🍗',
  seafood: '🐟',
  grain: '🌾',
  legume: '🫘',
  spice: '🌶️',
  nut: '🥜',
  sauce: '🥫',
  staple: '🧂',
}

/** Emoji used as a small avatar for an ingredient category. */
export function categoryEmoji(category: string | null | undefined): string {
  return (category && EMOJI[category.toLowerCase()]) || '🍽️'
}
