export interface SuggestProblem {
  message: string
  /** What the screen can offer next: go and fill the pantry, or simply try again. */
  action: 'pantry' | 'retry' | 'none'
}

/** Turns the API's status and message into wording for the person, plus the next step. */
export function describeSuggestError(status: number | undefined, serverMessage?: string): SuggestProblem {
  switch (status) {
    case 400:
      if (serverMessage && /pantry/i.test(serverMessage)) {
        return { message: 'Add some ingredients to your pantry first, then come back for ideas.', action: 'pantry' }
      }
      return { message: 'That request could not be understood. Please check it and try again.', action: 'none' }
    case 429:
      return {
        message: serverMessage || 'You have used all of today\'s suggestions. Try again tomorrow.',
        action: 'none',
      }
    case 502:
      return {
        message: serverMessage ? `${serverMessage}.`.replace(/\.\.$/, '.') : 'The AI had trouble. Please try again.',
        action: 'retry',
      }
    case 503:
      return {
        message: 'AI suggestions are not switched on yet. The server needs an API key (see docs/AI_SETUP.md).',
        action: 'none',
      }
    default:
      return { message: 'Could not reach the server. Check your connection and try again.', action: 'retry' }
  }
}
