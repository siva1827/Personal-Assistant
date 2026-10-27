import { describe, expect, it } from 'vitest'
import { describeSuggestError } from './suggestErrors'

describe('describeSuggestError', () => {
  it('sends people with an empty pantry to the pantry', () => {
    const problem = describeSuggestError(400, 'Add some ingredients to your pantry first')
    expect(problem.action).toBe('pantry')
    expect(problem.message).toContain('pantry')
  })

  it('treats other bad requests as not retryable', () => {
    expect(describeSuggestError(400, 'count must be at most 5').action).toBe('none')
  })

  it('shows the server wording for the daily limit', () => {
    const problem = describeSuggestError(429, 'You have used all 20 AI suggestions for today. Try again tomorrow')
    expect(problem.message).toContain('20')
    expect(problem.action).toBe('none')
  })

  it('offers a retry when the AI fails, ending the sentence neatly', () => {
    const problem = describeSuggestError(502, 'The AI service quota or rate limit was reached. Try again later')
    expect(problem.action).toBe('retry')
    expect(problem.message).toBe('The AI service quota or rate limit was reached. Try again later.')
    expect(describeSuggestError(502, 'Already ends with a stop.').message).toBe('Already ends with a stop.')
    expect(describeSuggestError(502).message).toContain('try again')
  })

  it('explains that AI is not switched on yet', () => {
    const problem = describeSuggestError(503)
    expect(problem.message).toContain('API key')
    expect(problem.action).toBe('none')
  })

  it('covers network failures', () => {
    expect(describeSuggestError(undefined).action).toBe('retry')
    expect(describeSuggestError(500).message).toContain('server')
  })
})
