import { describe, expect, it } from 'vitest'
import { dueInfo } from './dueLabel'

const today = new Date(2026, 9, 26) // Mon 26 Oct 2026

describe('dueInfo', () => {
  it('flags past dates as overdue', () => {
    expect(dueInfo('2026-10-20', today)).toEqual({ label: 'Overdue · Tue, Oct 20', tone: 'overdue' })
  })

  it('uses friendly names for today and tomorrow', () => {
    expect(dueInfo('2026-10-26', today)).toEqual({ label: 'Today', tone: 'today' })
    expect(dueInfo('2026-10-27', today)).toEqual({ label: 'Tomorrow', tone: 'soon' })
  })

  it('shows a short date for later days', () => {
    expect(dueInfo('2026-10-30', today)).toEqual({ label: 'Fri, Oct 30', tone: 'later' })
  })
})
