export interface ParsedSms {
  direction: 'DEBIT' | 'CREDIT'
  amount: number
  merchant: string | null
  /** yyyy-MM-dd, or null when the message has no readable date */
  date: string | null
}

const AMOUNT = /(?:rs\.?|inr|₹)\s*([\d,]+(?:\.\d{1,2})?)/i
const DEBIT_WORDS = /\b(debited|spent|sent|paid|purchase|withdrawn|payment of|deducted)\b/i
const CREDIT_WORDS = /\b(credited|received|deposited|refund(?:ed)?)\b/i

const MONTHS = ['jan', 'feb', 'mar', 'apr', 'may', 'jun', 'jul', 'aug', 'sep', 'oct', 'nov', 'dec']

function pad(n: number): string {
  return String(n).padStart(2, '0')
}

function fullYear(y: number): number {
  return y < 100 ? 2000 + y : y
}

/** dd-mm-yy, dd/mm/yyyy or dd-Mon-yy. Returns yyyy-MM-dd or null. */
export function parseSmsDate(text: string): string | null {
  const numeric = text.match(/\b(\d{1,2})[-/](\d{1,2})[-/](\d{2,4})\b/)
  if (numeric) {
    const [d, m, y] = [Number(numeric[1]), Number(numeric[2]), fullYear(Number(numeric[3]))]
    if (m >= 1 && m <= 12 && d >= 1 && d <= 31) return `${y}-${pad(m)}-${pad(d)}`
  }
  const named = text.match(/\b(\d{1,2})[-\s]([A-Za-z]{3})[a-z]*[-\s,]*(\d{2,4})\b/)
  if (named) {
    const m = MONTHS.indexOf(named[2].toLowerCase()) + 1
    const d = Number(named[1])
    if (m > 0 && d >= 1 && d <= 31) return `${fullYear(Number(named[3]))}-${pad(m)}-${pad(d)}`
  }
  return null
}

function cleanMerchant(raw: string): string | null {
  const merchant = raw
    .replace(/[@].*$/, '') // a UPI handle like swiggy@upi becomes swiggy
    .replace(/[^A-Za-z0-9 &'._-]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
    .replace(/[.\s-]+$/, '')
  return merchant.length >= 2 ? merchant : null
}

function findMerchant(text: string): string | null {
  const patterns = [
    /\bat\s+([A-Za-z0-9][A-Za-z0-9 &'._-]*?)(?=\s+(?:on|via|ref|using|txn)\b|[.,;]|\s*$)/i,
    /\bVPA\s+([A-Za-z0-9._-]+@[A-Za-z0-9]+)/i,
    /\bto\s+(?!a\/c|account|your)([A-Za-z0-9][A-Za-z0-9@ &'._-]*?)(?=\s+(?:on|via|ref|upi|txn)\b|[.,;]|\s*$)/i,
  ]
  for (const pattern of patterns) {
    const match = text.match(pattern)
    const merchant = match && cleanMerchant(match[1])
    if (merchant) return merchant
  }
  return null
}

/**
 * Reads the amount, direction, merchant and date out of an Indian bank/UPI alert.
 * Returns null when the text does not look like a transaction message.
 */
export function parseBankSms(text: string): ParsedSms | null {
  const message = text.replace(/\s+/g, ' ').trim()
  const amountMatch = message.match(AMOUNT)
  if (!amountMatch) return null
  const amount = Number(amountMatch[1].replace(/,/g, ''))
  if (!(amount > 0)) return null

  const debit = message.search(DEBIT_WORDS)
  const credit = message.search(CREDIT_WORDS)
  if (debit < 0 && credit < 0) return null
  // The first keyword wins: "debited ... credited to a/c" is still money going out
  const direction = credit < 0 || (debit >= 0 && debit < credit) ? 'DEBIT' : 'CREDIT'

  return { direction, amount, merchant: findMerchant(message), date: parseSmsDate(message) }
}

const KEYWORDS: Record<string, string[]> = {
  Food: ['swiggy', 'zomato', 'restaurant', 'cafe', 'dominos', 'mcdonald', 'kfc', 'pizza', 'starbucks'],
  Groceries: ['bigbasket', 'blinkit', 'zepto', 'dmart', 'grocery', 'supermarket', 'instamart', 'reliance fresh'],
  Transport: ['uber', 'ola', 'rapido', 'irctc', 'metro', 'redbus', 'fuel', 'petrol', 'indian oil', 'hpcl', 'bpcl'],
  Shopping: ['amazon', 'flipkart', 'myntra', 'ajio', 'meesho', 'nykaa'],
  Bills: ['electricity', 'airtel', 'jio', 'vodafone', 'bsnl', 'broadband', 'recharge', 'insurance', 'gas'],
  Health: ['pharmacy', 'apollo', 'medplus', 'hospital', 'clinic', 'pharmeasy', '1mg'],
  Entertainment: ['netflix', 'spotify', 'bookmyshow', 'hotstar', 'prime video', 'pvr', 'inox'],
}

/** Picks the user's category whose keywords appear in the merchant name, if any. */
export function guessCategoryId(
  merchant: string | null,
  categories: { id: number; name: string }[],
): number | null {
  if (!merchant) return null
  const haystack = merchant.toLowerCase()
  for (const [name, words] of Object.entries(KEYWORDS)) {
    if (words.some((w) => haystack.includes(w))) {
      const category = categories.find((c) => c.name.toLowerCase() === name.toLowerCase())
      if (category) return category.id
    }
  }
  return null
}
