import { describe, expect, it } from 'vitest'
import { apiPlatform, describeTestResult } from './testResult'

describe('describeTestResult', () => {
  it('points at the server setup when Firebase is not configured', () => {
    const outcome = describeTestResult({ configured: false, devices: 0, delivered: 0 })
    expect(outcome.ok).toBe(false)
    expect(outcome.message).toContain('FIREBASE_SETUP')
  })

  it('asks to register again when the phone is unknown to the server', () => {
    const outcome = describeTestResult({ configured: true, devices: 0, delivered: 0 })
    expect(outcome.ok).toBe(false)
    expect(outcome.message).toContain('not registered')
  })

  it('suspects mismatched Firebase files when nothing was accepted', () => {
    const outcome = describeTestResult({ configured: true, devices: 1, delivered: 0 })
    expect(outcome.ok).toBe(false)
    expect(outcome.message).toContain('same Firebase project')
  })

  it('reports success when at least one device accepted it', () => {
    expect(describeTestResult({ configured: true, devices: 2, delivered: 1 }).ok).toBe(true)
  })
})

describe('apiPlatform', () => {
  it('maps Capacitor platform names to the API enum', () => {
    expect(apiPlatform('android')).toBe('ANDROID')
    expect(apiPlatform('ios')).toBe('IOS')
    expect(apiPlatform('web')).toBe('WEB')
  })
})
