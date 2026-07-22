# Today field-work cockpit QA

Use a local offline workspace for **Cedar & Stone Services**, with clients Ben Carter, Maya Patel, and Northline Cafe. Use EUR for all document fixtures.

## Today and follow-up checks

- [ ] No records: Today shows “No jobs scheduled today.” and one Create job action.
- [ ] Several jobs today: in-progress is first; then due scheduled jobs; then future scheduled jobs, ordered by time.
- [ ] An in-progress job is the prominent Next job.
- [ ] Completed and cancelled jobs are absent from active work; completed jobs appear under Completed today only.
- [ ] A job without an address has no Directions button.
- [ ] A valid address opens an Android-resolved maps/navigation app; no installed app shows the recoverable fallback message.
- [ ] An unpaid invoice due before today appears in Money requiring attention; paid and cancelled invoices do not.
- [ ] A sent quote older than seven days, or a quote expiring today/past expiry, appears in Quotes requiring follow-up; accepted/rejected/converted quotes do not.
- [ ] A due job reminder on a featured job is shown as an indicator rather than a second full reminder card; an unrelated due reminder remains visible.
- [ ] Pull/re-entry after a Room change reflects the new local state without simulated network loading.

## Existing completion workflow regression checks

- [ ] Complete a job with notes only, then restart the app and confirm status/notes persist.
- [ ] Complete a job with photo proof, with signature, and with both. Test camera denial and photo-picker cancellation without losing entered data.
- [ ] Replace a signature only after confirming the replacement.
- [ ] Confirm existing job-linked invoices open rather than creating duplicates; a job created from an accepted quote retains its relationship.
- [ ] Verify double taps do not create duplicate invoices and that the completed job leaves active Today immediately.

## Device/accessibility checks

- [ ] Test large font and a small phone; all primary actions remain visible and reachable.
- [ ] Test TalkBack order for Today, next-job directions, and job completion controls.
- [ ] Test fully offline, deleted client/quote relationships, and no compatible maps application.
