import { Capacitor, registerPlugin } from '@capacitor/core'

/** One received text message as the phone stores it. */
export interface RawSms {
  id: string
  address: string | null
  body: string | null
  /** Epoch milliseconds */
  date: number
}

export interface SmsInboxPlugin {
  /** Asks for the READ_SMS permission if needed, then returns messages received at or after sinceMs. */
  readInbox(options: { sinceMs?: number; limit?: number }): Promise<{ messages: RawSms[] }>
}

const SAMPLE_KEY = 'devSmsInbox'

/**
 * Stand-in used in a browser, where there is no inbox to read. It returns the sample messages
 * stored by loadSampleMessages(), so the import screen can be tried without a phone.
 */
class SmsInboxWeb implements SmsInboxPlugin {
  async readInbox(options: { sinceMs?: number; limit?: number }) {
    const stored = localStorage.getItem(SAMPLE_KEY)
    const all: RawSms[] = stored ? JSON.parse(stored) : []
    const messages = all
      .filter((m) => m.date >= (options.sinceMs ?? 0))
      .sort((a, b) => b.date - a.date)
      .slice(0, options.limit ?? 200)
    return { messages }
  }
}

export const SmsInbox = registerPlugin<SmsInboxPlugin>('SmsInbox', {
  web: () => Promise.resolve(new SmsInboxWeb()),
})

/** True on the Android app, where the real inbox is read. */
export const isRealInbox = () => Capacitor.getPlatform() === 'android'

/** Synthetic messages covering the cases the importer must handle. No real data. */
export function sampleMessages(now: number = Date.now()): RawSms[] {
  const minutes = (n: number) => now - n * 60_000
  return [
    {
      id: 's1',
      address: 'AX-DEMOBK',
      date: minutes(30),
      body: 'Rs.250.00 debited from A/c XX1234 to VPA swiggy@upi (UPI Ref 100000001). Not you? Call bank.',
    },
    {
      id: 's2',
      address: 'VM-DEMOBK',
      date: minutes(120),
      body: 'INR 1,299.50 spent on Bank Card XX9876 at AMAZON PAY INDIA. Avl limit INR 50,000.',
    },
    { id: 's3', address: 'JD-DEMOBK', date: minutes(200), body: 'Sent Rs.60.00 From A/C *1234 To UBER INDIA Ref 100000002.' },
    { id: 's4', address: 'AX-DEMOBK', date: minutes(240), body: 'Rs 5,000.00 credited to your A/c XX1234 by salary. Avl Bal Rs 20,000.' },
    { id: 's5', address: 'AX-DEMOBK', date: minutes(260), body: 'OTP for payment of Rs 500 at AMAZON is 123456. Do not share.' },
    { id: 's6', address: '+919800000000', date: minutes(300), body: 'Paid Rs 300 to you for dinner, thanks!' },
  ]
}

/** Browser only: stores the sample messages so the import screen has something to find. */
export function loadSampleMessages(): void {
  localStorage.setItem(SAMPLE_KEY, JSON.stringify(sampleMessages()))
}
