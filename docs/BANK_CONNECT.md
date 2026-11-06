# Bank connect (Account Aggregator): guide

Bank connect brings your bank spending into the app automatically, using India's Account Aggregator
(AA) framework: you give consent, the app reads your accounts and transactions, and money that went
out becomes expenses marked **bank**.

**What exists today is a complete demo.** The consent flow, the data import, duplicate protection,
categorising, expiry and stopping are all built and tested, but the "bank" is a built-in pretend one
that invents accounts and transactions. Nothing connects to a real bank. Connecting a real provider
is the next step and needs a provider account, see [Going real](#going-real).

## Try the demo

1. Start the API with the demo provider:

   ```bash
   export AA_PROVIDER=mock          # PowerShell: $env:AA_PROVIDER = 'mock'
   mvn spring-boot:run
   ```

   The log says `Bank connect is running in DEMO mode`.

2. In the app: **Spending, Bank connect, Link a bank account**.
3. You see a consent screen listing what is asked for (purpose, date range, how long access lasts).
   Tap **Approve**. In a real flow this step happens in your Account Aggregator app instead.
4. Tap **Fetch my transactions**. About 80 invented expenses appear in **Spending**, marked **bank**,
   with categories guessed from the merchant. Incoming payments such as salary are left out.
5. Tap **Sync now** again: nothing is added twice.
6. **Stop sharing** ends access. The expenses already added stay.

Without `AA_PROVIDER` (the default is `none`) the screen says bank connect is not switched on, and
the API answers 503 for everything except `GET /aa/status`.

## How the flow works

```
 You tap "Link a bank account"
        |
 App asks the API  --->  API asks the provider to create a CONSENT REQUEST
        |                  (purpose, date range, valid for 30 days)
        |
 You approve it in your AA app  (demo: the Approve button)         status PENDING -> ACTIVE
        |
 "Sync now"  --->  API fetches accounts + transactions through the consent
        |
 Each money-out transaction becomes an expense (source BANK_SYNC)
 Each one gets a fingerprint, so syncing again never duplicates it
        |
 "Stop sharing" (or 30 days pass)                                   status REVOKED / EXPIRED
```

## What is stored

| Stored | Not stored |
| --- | --- |
| The consent: purpose, date range, status, when it was approved, when it expires | Full account numbers (only a masked form like `XXXX4321`) |
| Linked accounts: bank name, masked number, type, latest balance | Statements, or the bank's raw transaction text beyond a short note (the merchant) |
| Expenses made from money-out transactions, with a fingerprint | Incoming payments (the app tracks spending, not income) |

Only one connection can be pending or active at a time. A consent lasts 30 days
(`AA_CONSENT_DAYS`) and a new one covers the last 90 days of transactions (`AA_HISTORY_DAYS`).
Deleting your account removes the consent and linked accounts.

## Settings

| Variable | Default | Meaning |
| --- | --- | --- |
| `AA_PROVIDER` | `none` | `none` keeps bank connect off, `mock` is the demo. A real provider needs its own adapter. |
| `AA_CONSENT_DAYS` | `30` | How long an approved consent lasts. |
| `AA_HISTORY_DAYS` | `90` | How far back a new consent reaches. |

## API

All under `/api/v1/aa` and requiring a login. `{id}` is a consent id belonging to you; anyone else's id is a 404.

| Call | What it does |
| --- | --- |
| `GET /status` | `{configured, mock, provider}`. Works even when bank connect is off. |
| `GET /consents` | Your connections with their linked accounts. Also notices expiry. |
| `POST /consents` | Starts a connection (optional body `{"historyDays": 90}`). 409 if one is already pending or active. |
| `POST /consents/{id}/mock-approve`, `/mock-reject` | Demo only: stands in for your AA app. 404 with a real provider. |
| `POST /consents/{id}/sync` | Fetches and imports. Returns `{imported, skipped, creditsIgnored, accounts}`. 409 unless the consent is active. |
| `DELETE /consents/{id}` | Stops sharing. Safe to repeat. |

## What the demo is, and is not

- **Invented, but consistent.** The data is generated from the consent and the date, so fetching the same day
  twice gives the same transactions, and a longer range only adds days. It never creates transactions in the
  future. Two accounts are returned, one with a salary credit each month.
- **Not the real protocol.** A real Account Aggregator involves encrypted data transfer, signed consent
  artefacts and a regulated participant on your side. None of that is simulated here; the demo exercises
  the app's half of the flow.
- If the server restarts while a request is waiting for approval, just approve it again.

## Going real

Everything in the app talks to a provider through one interface, `AaProvider`
(`backend/src/main/java/com/siva/assistant/aa/`). Connecting a real aggregator means writing one more
implementation and selecting it in `AaConfig`. The screens, import, categorising, duplicate protection,
expiry and stopping do not change.

What a real adapter has to do:

1. **Create a consent request** with the provider and return the **redirect URL** where the user approves
   (the app already shows that step for non-demo providers).
2. **Report the consent's status**, by polling or by receiving the provider's notification. A notification
   needs a public HTTPS URL for the server.
3. **Fetch the data** for an active consent and return it as the neutral `AaModel` types, handling whatever
   encryption the provider uses.
4. **Revoke** the consent at the provider.

Things to know before starting. These come from general knowledge of the framework and **have not been
checked against any provider's current documentation**, so confirm them first:

- Apps that read bank data through Account Aggregators are generally "Financial Information Users", a
  regulated role. A personal project usually cannot hold it directly, which is why providers offer
  **sandboxes with mock data** for development. Live access to real accounts typically needs the
  provider's onboarding as a regulated entity, or a partnership with one.
- Provider sandboxes (for example Setu, Finvu, OneMoney) need a signup and API credentials. Onboarding
  details change, so read the provider's current guide.
- Credentials belong on the server only, like the other secrets (see `.env.example`).

## Troubleshooting

| What you see | Meaning and fix |
| --- | --- |
| *Bank connect is not switched on* | `AA_PROVIDER` is not set on the server. Set `AA_PROVIDER=mock` for the demo. |
| *You already have a bank connection in progress or active* (409) | Finish or stop the current one first. |
| *This connection has not been approved yet* (409) | Approve it first. |
| *This connection has expired* (409) | Consents last 30 days. Start a new one; imported expenses stay. |
| Balances or transactions look random | They are invented in demo mode. |
| Expenses landed in "Other" | The merchant did not match a known keyword. Recategorise it in the app. Keywords live in `CategoryGuesser.java`. |
