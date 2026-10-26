import { describe, expect, it } from 'vitest'
import { parseAllergies } from './useProfile'

describe('parseAllergies', () => {
  it('trims, lowercases and de-duplicates', () => {
    expect(parseAllergies(' Peanut, SHELLFISH ,peanut,, ')).toEqual(['peanut', 'shellfish'])
  })

  it('returns an empty list for blank input', () => {
    expect(parseAllergies('  ')).toEqual([])
  })
})
