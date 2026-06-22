# ServiceSphere Onboarding Optimization Plan

ServiceSphere is a field service management app for solo tradespeople and small service businesses. The onboarding experience should help a new user understand the app's value in the first 2 minutes and complete one useful action quickly.

The fastest value should be:

> Create client/job -> add quote or job details -> understand that ServiceSphere keeps field work organized.

## Activation Goal

A new user should reach this moment fast:

> I can put today's field work into ServiceSphere and keep the client, job details, quote, notes, photos, and paperwork organized in one place.

The first session should optimize for:

`Create client/job -> add quote or job details -> see organized job workspace`

## 1. Ideal Onboarding Flow

Use a short, action-first flow:

1. Welcome screen
2. Pick trade/business type
3. Optional business name
4. Create first client/job
5. Add one useful job detail or quote line
6. Land on the job detail screen with next-step actions

Recommended structure:

- No long tutorial.
- No forced account-completion wizard.
- No permissions upfront.
- No empty dashboard as the first experience.
- Let users skip setup and use sample/demo mode.

Primary path:

`Welcome -> Create my first job -> Client + job basics -> Add quote/details -> Job workspace`

Secondary path:

`Welcome -> Explore with sample job -> Demo job workspace`

## 2. First Screen Copy

Headline:

> Run your service jobs without losing the details.

Subcopy:

> Keep clients, quotes, job notes, photos, signatures, routes, and invoices organized from the first call to payment.

Primary CTA:

> Create my first job

Secondary CTA:

> Explore with a sample job

Tiny reassurance:

> Takes about 1 minute. You can change everything later.

Alternative shorter version:

> Organize your next job in under a minute.

## 3. Best First Action For The User

Best first action:

> Create a client and job together in one lightweight flow.

Do not make "create a business profile" the first action. That delays value.

The first action should create a real object the user recognizes:

- Client name
- Job title
- Optional address
- Optional phone
- Optional job date
- Optional estimated price

This immediately proves the app's core promise: field work becomes organized.

## 4. What To Ask During Onboarding

Ask only what improves the first job experience:

- Trade type: plumbing, electrical, HVAC, landscaping, cleaning, handyman, other
- Business name: optional
- First client name
- Job title
- Job address: optional but valuable
- Job date: default to today
- Job type/status: default to New
- Estimate/quote amount: optional

Good prompt:

> What job do you want to organize first?

Fields:

- Client name
- Job title
- Address
- Date
- Estimated price

Keep it to one screen if possible.

## 5. What Not To Ask Too Early

Avoid asking for these during first-run onboarding:

- Full business profile
- Logo upload
- Tax settings
- Invoice numbering
- Payment terms
- Full service catalog
- Team members
- Subscription/payment info
- Notification permissions
- Contacts permission
- Location permission
- Photo/camera permission
- Signature setup
- Route preferences
- Accounting integrations
- Complex quote templates

These are useful later, but they are not needed to feel first value.

## 6. Suggested Business Setup Flow

Business setup should happen after the first useful job is created.

Trigger it contextually:

After first job is saved:

> Want invoices and quotes to show your business info?

CTA:

> Add business details

Fields, split into small steps:

1. Business name
2. Phone/email
3. Address
4. Logo, optional
5. Tax/terms, optional
6. Invoice/quote defaults, later

Keep this as a checklist item, not a blocker.

Checklist copy:

> Finish business setup
> Add your details once so quotes and invoices are ready later.

## 7. Suggested Sample/Demo Job Flow

Offer demo mode for users who are hesitant or just browsing.

CTA:

> Explore with a sample job

Create a realistic demo:

Client:

> Sarah Miller

Job:

> Repair leaking kitchen sink

Job details:

- Address
- Scheduled for today
- Status: New
- Quote: Labor + parts
- Note: "Customer reports leak under sink."
- Photo placeholder
- Signature placeholder
- Invoice placeholder

Demo job screen should show the product's ecosystem:

- Client details
- Job notes
- Quote
- Photos
- Signature
- Route
- Invoice

Top banner:

> Sample job. Create your own when you're ready.

CTA:

> Create my real first job

## 8. Empty State Copy

Dashboard empty state:

> Your jobs will show up here
> Create your first job to keep the client, address, notes, quote, and invoice in one place.

CTA:

> Create first job

Clients empty state:

> No clients yet
> Add a client once, then attach jobs, quotes, notes, and invoices to them.

CTA:

> Add client

Jobs empty state:

> No jobs yet
> Start with the next job on your schedule. You only need a title and client to begin.

CTA:

> Create job

Quotes empty state:

> No quotes yet
> Build a simple quote from a job and keep it attached to the client.

CTA:

> Create quote

Invoices empty state:

> No invoices yet
> When a job is ready to bill, turn the job details into an invoice.

CTA:

> Create invoice

Photos empty state:

> No job photos yet
> Add before-and-after photos or site details when you're on the job.

CTA:

> Add photo

## 9. Activation Event Definition

Primary activation event:

> User creates a real client/job and adds at least one meaningful job detail or quote item within the first session.

Suggested event name:

`activation_first_job_organized`

Fire when all are true:

- `client_created` or selected
- `job_created`
- At least one of:
  - job note added
  - quote line added
  - estimated price added
  - address added
  - photo added
  - invoice draft created

Activation should not require invoice creation. That is too late for first-time activation.

Secondary activation events:

- `first_client_created`
- `first_job_created`
- `first_quote_created`
- `first_job_note_added`
- `first_photo_added`
- `first_invoice_created`

Core funnel:

`signup/open -> onboarding started -> first job started -> first job saved -> detail/quote added -> activation`

## 10. First 5-Minute User Journey

Minute 0-1:

- User opens app.
- Sees clear value message.
- Taps "Create my first job."

Minute 1-2:

- Enters client name and job title.
- Optional address/date/price.
- Saves job.

Minute 2-3:

- Lands on job detail screen.
- Sees organized sections: notes, quote, photos, signature, invoice.
- Prompt says: "Add one detail so this job is ready to work from."

Minute 3-4:

- User adds a note or quote line.
- App confirms: "Job organized."

Minute 4-5:

- Show next best actions:
  - Add quote
  - Add photo
  - Get signature
  - Create invoice
- Present lightweight checklist:
  - Create first job: done
  - Add job details: done
  - Add business info: optional
  - Create first quote: optional

## 11. Permission Timing Strategy

Ask permissions only when the user takes an action that requires them.

Camera/photos:

- Ask when user taps "Add photo."
- Pre-permission copy:
  > Add job photos to keep site details with the job.

Location:

- Ask when user taps route/map/address navigation.
- Pre-permission copy:
  > Use your location to help route you to jobs.

Contacts:

- Avoid early.
- Ask only if importing client contacts.
- Pre-permission copy:
  > Import clients from your contacts instead of typing them manually.

Notifications:

- Ask after first job is created or when setting a reminder.
- Pre-permission copy:
  > Get reminders before scheduled jobs.

Never ask all permissions on first launch.

## 12. Best CTAs

Primary CTAs:

- Create my first job
- Add first client
- Save job
- Add job details
- Create quote
- Add quote line
- Add job note
- Create invoice
- Explore sample job

Contextual CTAs:

- Add address for routing
- Add price estimate
- Add photos
- Get customer signature
- Turn into invoice
- Finish business setup

Avoid vague CTAs:

- Get started
- Continue
- Next
- Learn more
- Complete setup

Use action-specific CTAs wherever possible.

## 13. Best Microcopy

On welcome:

> Start with one job. Add more details when you need them.

On first job form:

> You only need a client and job title to begin.

Under optional fields:

> Optional. Useful for routes, quotes, and invoices later.

After save:

> First job created. Now add one detail so it's ready to work from.

After adding quote/details:

> Nice. This job now has the details you need in the field.

For business setup:

> Add this once. We'll use it on future quotes and invoices.

For skipped fields:

> You can add this later from the job screen.

For demo mode:

> This is sample data. Nothing here affects your real jobs.

## 14. How To Reduce Friction

Use progressive disclosure:

- Start with minimum fields.
- Hide advanced job fields behind "Add more details."
- Default date to today.
- Default status to New.
- Let users save incomplete jobs.
- Do not require address.
- Do not require price.
- Do not require business setup.
- Do not require invoice setup.
- Do not ask for permissions until needed.
- Offer sample data.
- Show a visible "Skip for now."

Recommended first job form:

Required:

- Client name
- Job title

Optional:

- Address
- Date
- Estimated price
- Notes

That is enough.

## 15. How To Guide User To Create First Client/Job/Quote

Combine client and job creation.

Instead of:

`Create client -> Save -> Create job -> Pick client -> Save`

Use:

`Create first job -> Client name + job details -> Save`

After save, land on the job workspace.

Then guide with one prompt:

> What do you want to add next?

Options:

- Add quote
- Add note
- Add photo
- Create invoice

For quote guidance:

> Add a simple quote
> Start with one line item. You can add more later.

Fields:

- Item/service
- Price
- Optional notes

Example placeholder:

> Labor, parts, callout fee, diagnostic visit

## 16. How To Make The App Feel Useful Immediately

The app should reward the first job creation with a complete-looking workspace.

After first job save, show:

- Client name
- Job title
- Date/status
- Address/map action if added
- Empty sections for quote, notes, photos, invoice
- Next-step checklist
- "Job organized" confirmation

Also show practical shortcuts:

- Call client
- Navigate
- Add note
- Add quote
- Add photo
- Create invoice

The user should feel:

> This is where I run the job from.

That is stronger than a generic dashboard.

## 17. Day 1 Retention Suggestions

Day 1 goal:

> Get the user back to continue or complete the first job.

Trigger-based messages:

If first job created but no detail added:

> Finish setting up your first job
> Add a note, quote, or address so it's ready when you're on-site.

If quote created but not sent/exported:

> Your quote is ready to review
> Open it, adjust details, or turn it into an invoice later.

If user used demo only:

> Ready to organize a real job?
> Create your first client/job in under a minute.

In-app Day 1 checklist:

- Add job note
- Add quote
- Add business details
- Add photo
- Create invoice

Keep it practical. Do not push upgrades yet.

## 18. Day 7 Retention Suggestions

Day 7 goal:

> Turn first use into a repeat workflow.

Suggested prompts:

- Create this week's jobs
- Add business details for cleaner quotes and invoices
- Turn completed jobs into invoices
- Back up/export your service records
- Add photos or signatures to completed jobs

Useful Day 7 feature discovery:

- Quote -> invoice flow
- Job photos
- Customer signatures
- Routes
- Data backup/export
- Recurring client/job reuse if available

Segment by behavior:

- Created job only: prompt quote/details
- Created quote: prompt invoice
- Created invoice: prompt business profile polish
- Demo only: prompt real first job

## 19. Onboarding QA Checklist

Core flow:

- New user can create first job in under 2 minutes.
- First job requires no more than client name and job title.
- User can skip optional fields.
- User can skip business setup.
- User can explore sample data.
- User never lands on a dead empty dashboard.
- First save lands on job detail/workspace screen.
- Next action is obvious after job creation.

Permissions:

- No permission prompts on first launch.
- Camera permission appears only after photo action.
- Location permission appears only after route/map action.
- Notification permission appears only after reminder/schedule action.

Copy:

- CTAs are action-specific.
- Optional fields are clearly marked.
- Empty states explain value and offer one primary action.
- No jargon like CRM, workflow automation, or dispatching unless users already expect it.

Analytics:

- Track every onboarding step.
- Track first job creation.
- Track first detail/quote/note/photo.
- Track time to activation.
- Track skipped setup.
- Track demo usage.
- Track permission accept/deny by timing.

Usability:

- Keyboard does not block important fields or save button.
- Save button remains accessible.
- Form works with one hand.
- Error states are clear.
- Offline or poor network state is handled if relevant.
- Back navigation does not lose entered data without warning.

## 20. Final Recommended Implementation Plan

### Phase 1: Fast Activation Flow

Build the first-run path around one outcome:

> Create first client/job and add one useful detail.

Implement:

- Welcome screen with two CTAs:
  - Create my first job
  - Explore sample job
- Combined client/job creation form
- Minimal required fields
- Optional job details
- Post-save job workspace
- Prompt to add quote, note, photo, or invoice

### Phase 2: Demo Job

Add sample job mode with realistic field-service data.

The demo should show the complete product shape without requiring setup:

- Client
- Job
- Quote
- Notes
- Photos placeholder
- Signature placeholder
- Invoice placeholder

### Phase 3: Contextual Setup

Move business setup after first value.

Add checklist:

- Create first job
- Add job details
- Add business info
- Create quote
- Create invoice

Make it dismissible.

### Phase 4: Permission Timing

Remove upfront permission prompts.

Ask permissions only after intent:

- Photo action -> camera/photos
- Route action -> location
- Reminder action -> notifications
- Import action -> contacts

### Phase 5: Measurement

Track activation as:

`first real job created + one meaningful detail added`

Measure:

- Activation rate
- Time to activation
- First job completion rate
- Quote creation rate
- Demo-to-real-job conversion
- Day 1 retention
- Day 7 retention

## Strongest Recommendation

Make "Create my first job" the center of onboarding, not "complete setup." For ServiceSphere, setup is only valuable after the user sees a real job workspace come together.
