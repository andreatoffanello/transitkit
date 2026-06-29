# Order Form Template — TransitKit

> Documento commerciale specifico per ogni deal. Si firma insieme all'MSA (prima volta) o da solo (rinnovi/cambi).
> Versione: 0.1 (draft 2026-05-25)

---

## STRUTTURA

Order Form **breve e leggibile** (1-2 pagine). Tutto il legalese sta nell'MSA. Qui solo: chi, cosa, quanto, quando.

---

# ORDER FORM #[NUMBER]

**Order Form Date:** [DATE]  
**Master Services Agreement Reference:** MSA dated [MSA DATE] between Provider and Customer  
**Order Form Term:** [12 months from Service Start Date]

---

## 1. Parties

**Provider:** [PROVIDER LEGAL NAME]  
**Customer:** [CUSTOMER LEGAL NAME]

## 2. Services ordered

| Service component | Quantity | Notes |
|-------------------|----------|-------|
| TransitKit iOS app (branded) | 1 | Published to App Store under [Customer name / Provider name as agent] |
| TransitKit Android app (branded) | 1 | Published to Google Play |
| Web stop pages (mobile) | 1 | Hosted at [subdomain.customer-domain] |
| GTFS static ingestion | 1 | From URL [CUSTOMER GTFS URL] |
| GTFS-realtime ingestion (VP/TU/SA) | 1 | From URL(s) [CUSTOMER GTFS-RT URLs] — via TransitKit realtime proxy |
| Operator console access | up to 3 admin users | https://console.transitkit.app/[customer-id] |
| Push notification quota | unlimited | APNs + FCM, fair-use ≤10 sends/day operator-wide |

## 3. Customer-specific configuration

- **Operator ID (internal):** [customer-slug]
- **Brand name (display in apps):** [BRAND NAME]
- **Bundle ID iOS:** com.transitkit.[customer-slug]
- **Package Android:** com.transitkit.[customer-slug]
- **Primary color:** [HEX]
- **Accent color:** [HEX]
- **Logo file received:** [yes/no — date]
- **App Store account ownership:** [Provider / Customer Apple Developer account]
- **Google Play account ownership:** [Provider / Customer Play Console account]
- **Custom domain for web stop pages:** [subdomain.customer-domain — CNAME to provided Vercel target]

## 4. Fees

| Item | Amount | Frequency |
|------|--------|-----------|
| Subscription fee | **USD $299** (or **EUR €299**) | Monthly, billed in advance |
| Setup fee | **USD $0** | One-time (waived for launch customers) |
| Onboarding (data integration + brand setup) | included | One-time, completed within 14 days of Service Start Date |

**Annual prepayment option:** USD $2,990 / EUR €2,990 (equivalent to 10 months — 2 months free).

**Currency:** Customer may elect to pay in USD or EUR. Currency selected at signing applies for the Order Form term.

**Payment method:** [Wire transfer / Paddle subscription / SEPA Direct Debit] — to be specified per deal.

**Invoicing:** [Paddle-handled automatically / Provider issues monthly invoice via PEC and SdI to Customer's electronic invoicing system / Wire transfer with manual invoice]

## 5. Service Start Date and Term

**Service Start Date:** [DATE] — defined as the date the Customer-branded apps are submitted to App Store + Google Play AND web stop pages go live.

**Initial Term:** 12 months from Service Start Date.

**Renewal:** Auto-renews for successive 12-month periods unless either Party gives 60 days' written notice before renewal date. Renewal fees may increase up to **5% per year** (CPI-aligned); above 5% requires Customer's written approval.

## 6. Milestones (target)

| Milestone | Target date | Status |
|-----------|-------------|--------|
| MSA + Order Form signed | T+0 | □ |
| Brand assets received | T+7 days | □ |
| GTFS feed validated | T+10 days | □ |
| iOS app submitted to App Store | T+21 days | □ |
| Android app submitted to Google Play | T+21 days | □ |
| Web stop pages live | T+14 days | □ |
| **Service Start Date (Go-live)** | T+30 days | □ |

## 7. Notices

| | Provider | Customer |
|---|----------|----------|
| Commercial email | [email] | [email] |
| Technical email | [email] | [email] |
| Billing email | [email] | [email] |
| PEC (if Italian customer) | [PEC] | [PEC] |

## 8. Order Form-specific terms

(Use this section for any one-off deviations from MSA. Examples:)

- *Service-level commitment override:* [e.g., "99.9% uptime instead of standard 99.5%, with penalty of 1 month free per 0.1% missed"]
- *Data residency requirement:* [e.g., "All EU passenger data hosted exclusively in EU regions"]
- *White-label exclusivity:* [e.g., "Provider will not contract with another transit operator within Customer's service area for the term"]
- *Co-marketing:* [e.g., "Customer agrees to be named as launch customer in Provider's marketing materials with prior approval of specific copy"]

If no Order Form-specific terms, write "None — MSA terms apply unchanged."

---

**Accepted by the Parties as of the Order Form Date above:**

**Provider — [PROVIDER LEGAL NAME]**

Name: ____________________  
Title: ____________________  
Date: ____________________  
Signature: ____________________

**Customer — [CUSTOMER LEGAL NAME]**

Name: ____________________  
Title: ____________________  
Date: ____________________  
Signature: ____________________

---

## CHECKLIST POST-FIRMA (interno Provider)

- [ ] Order Form firmato archiviato (PDF + signed scan)
- [ ] Customer record creato in CRM / spreadsheet
- [ ] Slot Paddle subscription attivato per il customer
- [ ] Slack/email kickoff inviato a Customer designated admins
- [ ] Branch operator creato in repo `transit-engine` (`shared/operators/[customer-slug]/`)
- [ ] Asset brand richiesti via email entro 24h
- [ ] CDN dataset pipeline schedulata
- [ ] Calendar reminder per primo invoice + per renewal -60 giorni
