import { describe, expect, it } from 'vitest'
import { guessCategoryId, parseBankSms, parseSmsDate } from './smsParser'

// Synthetic messages in the common Indian bank / UPI formats (no real account data)
describe('parseBankSms', () => {
  it('reads a UPI debit with a VPA', () => {
    const sms = 'Rs.250.00 debited from A/c XX1234 on 26-10-26 to VPA swiggy@upi (UPI Ref 123456789). Not you? Call bank.'
    expect(parseBankSms(sms)).toEqual({ direction: 'DEBIT', amount: 250, merchant: 'swiggy', date: '2026-10-26' })
  })

  it('reads a card spend at a merchant, with commas and a named month', () => {
    const sms = 'INR 1,299.50 spent on Bank Card XX9876 at AMAZON PAY INDIA on 03-Oct-26. Avl limit INR 50,000.'
    expect(parseBankSms(sms)).toEqual({
      direction: 'DEBIT',
      amount: 1299.5,
      merchant: 'AMAZON PAY INDIA',
      date: '2026-10-03',
    })
  })

  it('reads a "sent ... to" message', () => {
    const sms = 'Sent Rs.120.00 From Bank A/C *1234 To ZOMATO On 26/10/26 Ref 987654. Not You? Call 1800'
    const parsed = parseBankSms(sms)
    expect(parsed).toMatchObject({ direction: 'DEBIT', amount: 120, merchant: 'ZOMATO', date: '2026-10-26' })
  })

  it('treats credits as money received', () => {
    const sms = 'Rs 5,000.00 credited to your A/c XX1234 on 26-10-2026 by salary. Avl Bal Rs 20,000.'
    expect(parseBankSms(sms)).toMatchObject({ direction: 'CREDIT', amount: 5000, date: '2026-10-26' })
  })

  it('counts debited-then-credited transfers as outgoing', () => {
    const sms = 'Rs 800 debited from A/c XX1234 and credited to A/c XX5678 on 26-10-26.'
    expect(parseBankSms(sms)?.direction).toBe('DEBIT')
  })

  it('handles the rupee symbol and a missing merchant or date', () => {
    expect(parseBankSms('₹75 paid')).toEqual({ direction: 'DEBIT', amount: 75, merchant: null, date: null })
  })

  it('ignores OTPs, failed payments and upcoming-debit reminders', () => {
    expect(parseBankSms('OTP for payment of Rs 500 at AMAZON is 123456. Do not share.')).toBeNull()
    expect(parseBankSms('Your payment of Rs.250.00 to SWIGGY has failed. Amount not debited.')).toBeNull()
    expect(parseBankSms('Transaction of Rs 900 declined due to insufficient balance in A/c XX1234')).toBeNull()
    expect(parseBankSms('Rs 499 will be debited from your A/c XX1234 on 28-10-26 for Netflix mandate')).toBeNull()
    expect(parseBankSms('Reminder: card payment due. Pay Rs 5,200 by 05-11-26 to avoid charges')).toBeNull()
  })

  it('returns null for messages that are not transactions', () => {
    expect(parseBankSms('Your OTP is 123456. Do not share it with anyone.')).toBeNull()
    expect(parseBankSms('Hello, how are you?')).toBeNull()
    expect(parseBankSms('Rs 100 offer just for you, apply now')).toBeNull()
    expect(parseBankSms('')).toBeNull()
  })
})

describe('parseSmsDate', () => {
  it('reads numeric and named dates', () => {
    expect(parseSmsDate('on 05/01/2026')).toBe('2026-01-05')
    expect(parseSmsDate('on 5-Jan-26')).toBe('2026-01-05')
    expect(parseSmsDate('on 12 March 2026')).toBe('2026-03-12')
  })

  it('rejects impossible dates', () => {
    expect(parseSmsDate('on 40-13-26')).toBeNull()
    expect(parseSmsDate('no date here')).toBeNull()
  })
})

describe('guessCategoryId', () => {
  const categories = [
    { id: 1, name: 'Food' },
    { id: 2, name: 'Transport' },
    { id: 3, name: 'Shopping' },
  ]

  it('matches merchant keywords to the user categories', () => {
    expect(guessCategoryId('SWIGGY', categories)).toBe(1)
    expect(guessCategoryId('Uber India', categories)).toBe(2)
    expect(guessCategoryId('amazon pay', categories)).toBe(3)
  })

  it('returns null when unknown, or when the user has no such category', () => {
    expect(guessCategoryId('Corner shop', categories)).toBeNull()
    expect(guessCategoryId('netflix', categories)).toBeNull()
    expect(guessCategoryId(null, categories)).toBeNull()
  })
})
