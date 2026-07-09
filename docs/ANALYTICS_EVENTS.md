# ServiceSphere Analytics Events

Last updated: July 9, 2026

Firebase Analytics / GA4 is used for production product analytics. Do not log client names, phone numbers, emails, addresses, notes, invoice text, signatures, photo paths, or generated file paths.

## GA4 Key Event

Mark this event as the primary GA4 key event and import it into Google Ads:

| Event | Trigger | Parameters |
| --- | --- | --- |
| `activation_first_job_organized` | Fires once when a real job has enough setup to prove activation: a created job plus a client, schedule, photo/detail, address, description, estimated price, or notes. | `source_screen`, `has_client`, `has_schedule`, `has_details`, `job_status` |

## Activation Events

| Event | Trigger | First-only | Parameters |
| --- | --- | --- | --- |
| `first_client_created` | First successful client creation from onboarding or client form. | Yes | `source_screen` |
| `first_job_created` | First successful real job creation from onboarding or job form. | Yes | `source_screen`, `has_client`, `has_schedule`, `has_details`, `job_status` |
| `activation_first_job_organized` | First job becomes meaningfully usable from onboarding, job form, or job detail enrichment. | Yes | `source_screen`, `has_client`, `has_schedule`, `has_details`, `job_status` |
| `first_quote_created` | First successful quote creation. | Yes | `source_screen`, `has_client`, `item_count`, `value_bucket`, `currency` |
| `first_invoice_created` | First successful invoice creation. | Yes | `source_screen`, `has_client`, `item_count`, `value_bucket`, `currency` |
| `first_photo_proof_added` | First successful job photo proof added from camera or gallery. | Yes | `source_screen`, `photo_source` |
| `first_signature_captured` | First successful job or invoice signature capture. | Yes | `source_screen`, `signature_target` |
| `first_pdf_generated` | First successful quote or invoice PDF generation/share. | Yes | `source_screen`, `document_type` |
| `data_export_created` | Successful local export creation. | No | `source_screen`, `export_type` |

## Paywall And Purchase Events

These are logged by `AnalyticsTracker` and are production Firebase Analytics events.

| Event | Trigger | Parameters |
| --- | --- | --- |
| `paywall_viewed` | Paywall screen is shown. | `screen`, `source`, `plan` |
| `premium_cta_clicked` | Upgrade CTA is tapped. | `screen`, `source`, `plan` |
| `purchase_started` | Purchase flow starts. | `screen`, `source`, `plan` |
| `purchase_success` | Purchase completes successfully. | `screen`, `source`, `plan`, `result` |
| `purchase_failed` | Purchase flow fails or is cancelled. | `screen`, `source`, `plan`, `result`, `error_type` |

## Parameter Notes

`value_bucket` intentionally avoids exact prices. Buckets are `zero`, `under_100`, `100_499`, `500_999`, `1000_4999`, and `5000_plus`.

`currency` is the app's business profile currency code only. It does not include customer or invoice details.
