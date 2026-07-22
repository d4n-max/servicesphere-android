# Trustworthy professional documents

ServiceSphere creates real A4 PDFs in app-private `files/pdfs` storage and exposes them only through its FileProvider. No broad storage permission or raw `file://` URI is used. PDFs contain configured business details, available client/job information, itemized totals, notes, payment instructions, signatures, and page numbers. Optional data is omitted cleanly.

PDF preview uses Android `PdfRenderer` against the generated file; it is not an imitation of the document. Android sharing uses one chooser-based PDF path with `application/pdf` and temporary read permission.

Quote statuses are Draft, Sent, Accepted, Declined, and Converted. Invoice statuses are Draft, Sent, Paid, Void, with Overdue calculated from an unpaid status and past due date. “Sent” is a manual local confirmation after share initiation; ServiceSphere does not track delivery, opening, viewing, or downloads.

Generated-PDF metadata is persisted with the source update time so callers can recognize stale output. Document activity records local lifecycle actions and never stores PDF bytes or document contents.
