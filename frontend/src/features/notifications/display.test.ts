import { describe, expect, it } from 'vitest'
import { notificationEmoji, timeAgo } from './display'

const now = new Date('2026-10-27T12:00:00Z')

describe('timeAgo', () => {
  it('uses relative wording for recent times', () => {
    expect(timeAgo('2026-10-27T11:59:40Z', now)).toBe('just now')
    expect(timeAgo('2026-10-27T11:55:00Z', now)).toBe('5 min ago')
    expect(timeAgo('2026-10-27T09:00:00Z', now)).toBe('3 h ago')
  })

  it('says yesterday for the day before, then a short date', () => {
    expect(timeAgo('2026-10-26T08:00:00Z', now)).toBe('yesterday')
    expect(timeAgo('2026-10-20T08:00:00Z', now)).toBe('Oct 20')
  })
})

describe('notificationEmoji', () => {
  it('has an icon for each type', () => {
    expect(notificationEmoji('TASK_DUE')).toBe('⏰')
    expect(notificationEmoji('BUDGET_OVER')).toBe('🚨')
    expect(notificationEmoji('PANTRY_EXPIRING')).toBe('🥬')
  })
})
