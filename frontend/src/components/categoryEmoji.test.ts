import { describe, expect, it } from 'vitest'
import { categoryEmoji } from './categoryEmoji'

describe('categoryEmoji', () => {
  it('maps known categories, ignoring case', () => {
    expect(categoryEmoji('Vegetable')).toBe('🥦')
    expect(categoryEmoji('dairy')).toBe('🥛')
  })

  it('falls back for unknown or missing categories', () => {
    expect(categoryEmoji('mystery')).toBe('🍽️')
    expect(categoryEmoji(null)).toBe('🍽️')
  })
})
