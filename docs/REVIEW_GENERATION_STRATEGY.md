# ServiceSphere Review Generation Strategy

## Trigger Moments

ServiceSphere checks review eligibility only after meaningful successful actions:

- Invoice created successfully.
- Job marked as Completed.
- Client signature collected successfully.
- Quote shared successfully through quote PDF share or quote-related client message share.

No prompt is shown on first launch, during onboarding, during business setup, in empty states, or before the user has successful work recorded.

## Eligibility Rules

- At least 2 meaningful success moments must be completed.
- At least 2 app sessions must be recorded, which prevents first-session prompts.
- Do not prompt more than once every 30 days.
- Stop prompting after 3 dismissals.
- If the user dismisses the prompt, save the dismissal time and respect the cooldown.
- If the user chooses the positive path or private feedback path, save the prompt attempt time and app version.

## In-App Copy

Title:
“How is ServiceSphere working for you?”

Body:
“A quick rating helps us understand what is working well for your service business.”

Positive action:
“It’s working well”

Neutral/negative action:
“I have feedback”

Dismiss action:
“Maybe later”

## Routing

- Positive users are routed to the Google Play In-App Review flow when available.
- If the in-app review flow cannot start, ServiceSphere opens the Play Store listing for `com.servicesphere.app`, with browser fallback.
- Neutral or unhappy users are routed to private email feedback with subject `ServiceSphere feedback`.
- Neutral or unhappy users are not sent to Google Play.

## Manual QA Checklist

- Fresh install: complete onboarding and business setup; verify no review prompt appears.
- First app session: complete two success moments; verify no review prompt appears.
- Second app session: complete the second or next success moment; verify prompt can appear.
- Tap “It’s working well”; verify Play review or store fallback opens.
- Tap “I have feedback”; verify email intent opens and Play Store does not open.
- Tap “Maybe later”; verify prompt does not reappear immediately.
- Set dismissal count to 3 in test/local state; verify prompt does not appear.
- Set last prompt time within 30 days; verify prompt does not appear.
- Verify invoice creation, job completed status, signature save, and quote share triggers still complete their original flows.

## Early Play Store Review Response Playbook

5-star review:
“Thank you for using ServiceSphere. Glad it is helping you keep jobs, clients, quotes, and invoices organized.”

4-star review:
“Thank you for the thoughtful feedback. We are continuing to improve ServiceSphere for small service businesses and appreciate you trying it.”

3-star review:
“Thank you for the feedback. We would like to understand what slowed you down so we can improve the workflow in a future update.”

1–2 star review:
“Thank you for trying ServiceSphere. We are actively improving the app and would appreciate details about what did not work well so we can fix it.”
