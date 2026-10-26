import { describe, expect, it } from 'vitest'
import type { InternalAxiosRequestConfig } from 'axios'
import { api, setAccessToken } from './client'

const runInterceptors = async () => {
  const config = { headers: new (await import('axios')).AxiosHeaders() } as InternalAxiosRequestConfig
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const handlers = (api.interceptors.request as any).handlers
  return handlers[0].fulfilled(config) as InternalAxiosRequestConfig
}

describe('api client', () => {
  it('adds the bearer token once set', async () => {
    setAccessToken('abc')
    expect((await runInterceptors()).headers.Authorization).toBe('Bearer abc')
  })

  it('sends no Authorization header after the token is cleared', async () => {
    setAccessToken(null)
    expect((await runInterceptors()).headers.Authorization).toBeUndefined()
  })
})
