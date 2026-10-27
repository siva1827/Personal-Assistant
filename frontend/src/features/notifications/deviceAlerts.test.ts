import { describe, expect, it } from 'vitest'
import type { AppNotification } from '../../api/types'
import { unannounced } from './deviceAlerts'

const n = (id: number, read = false): AppNotification => ({
  id,
  type: 'TASK_DUE',
  title: 't',
  body: 'b',
  link: null,
  read,
  createdAt: '2026-10-27T00:00:00Z',
})

describe('unannounced', () => {
  it('returns only unread notifications newer than the last announced one, oldest first', () => {
    const items = [n(9), n(8, true), n(7), n(3)]
    expect(unannounced(items, 5).map((x) => x.id)).toEqual([7, 9])
  })

  it('returns nothing when everything was announced', () => {
    expect(unannounced([n(2), n(1)], 2)).toEqual([])
  })
})
