# Blender Resource Library

This context covers a resource library for Blender-related content. It defines the domain language for works, assets, plugins, and their review state around external delivery links.

## Language

**Resource**:
A managed item in the library that can be published and reviewed. In this project, a Resource is one of Artwork, Asset, or Plugin.
_Avoid_: Item, content, entry

**Artwork**:
A Resource representing a published work with preview media and model files.
_Avoid_: Work, model showcase

**Preview Media**:
The locally stored preview image for an Artwork. It remains on local storage after the Artwork's Quark Link is approved, and can still be displayed and downloaded after the Artwork becomes Synced.
_Avoid_: Cover pack, archive preview

**Asset**:
A Resource representing reusable files that can be downloaded and reused.
_Avoid_: Material, pack, attachment

**Plugin**:
A Resource representing an installable Blender extension package tied to a Blender version. It follows the same local archive flow as an Asset and does not have Preview Media.
_Avoid_: Addon, extension package

**Quark Link**:
The external Quark share link associated with a Resource for review and delivery. It is provided after local upload output is ready and before the Resource enters Manual Review, and remains visible in ordinary Resource details after the Resource becomes Synced.
_Avoid_: Netdisk link, share URL

**Extraction Code**:
The Quark access code submitted together with a Quark Link so a reviewer can open the shared content. A Resource cannot enter Syncing unless both the Quark Link and Extraction Code are filled in, and it remains visible with the Quark Link after the Resource becomes Synced.
_Avoid_: Password, token, pickup code

**Pending Link**:
The state of a Resource after local files are stored but before its Quark Link and Extraction Code are submitted. In this state, edits can be saved without entering review, and incomplete Quark information can be kept as a draft; the Resource can enter review only after both fields are complete and submission is confirmed. Legacy Resources that do not yet have Quark review data also default to Pending Link.
_Avoid_: Draft sync, unsynced, temporary upload

**Local Archive**:
The local zip file stored under the upload directory for a Resource before Quark review is approved. Each Resource has only one current Local Archive at a time. When a Resource is approved as Synced, its Local Archive is deleted.
_Avoid_: Package cache, temp file, backup zip

**Local Info**:
The non-download local metadata shown for a Resource before it becomes Synced, such as titles, descriptions, file names, file sizes, and preview information. Local Info does not expose Local Archive download links.
_Avoid_: Local download, attachment links, archive access

**Post-Upload Edit Scope**:
In the current workflow phase, a Resource in Pending Link or Sync Failed can still edit titles, descriptions, Quark Link, and Extraction Code, but it cannot replace or add local files after the initial upload.
_Avoid_: Full reupload edit, archive rebuild editing, file replacement

**Sync Status**:
The review state of a Resource around Quark review. A newly created Resource starts as Pending Link, moves to Syncing after its Quark Link and Extraction Code are submitted, can move to Synced after manual review in the Management Page, and can move to Sync Failed when the reviewed link is rejected.
_Avoid_: Availability, publish status, audit status

**Read-Only Review State**:
The rule that a Resource in Syncing cannot be edited from ordinary Resource pages while it is waiting for Manual Review.
_Avoid_: Editable syncing, live draft review, review-in-progress edits

**Failure Reason**:
A short explanation recorded when a Resource is marked as Sync Failed. It is required when a reviewer rejects a Resource, and it is shown during Resubmission so the Quark Link can be corrected. When a Resource is resubmitted and returns to Syncing, the old Failure Reason is cleared.
_Avoid_: Error log, review note, exception

**Management Page**:
The page where Resources in Syncing are checked and moved to Synced or Sync Failed. It is a single mixed review queue for Artwork, Asset, and Plugin, distinguished by Resource type. Resources in Pending Link, Synced, or Sync Failed do not appear there.
_Avoid_: Admin queue, backend page, review dashboard

**Manual Review**:
The review step performed in the Management Page by opening a Quark Link and deciding whether the linked content is correct. Manual Review does not use automatic link validation.
_Avoid_: Auto validation, sync check, crawler verification

**Resubmission**:
The act of editing a Resource that is in Sync Failed and submitting corrected Quark information so the Resource returns to Syncing and re-enters the Management Page for another manual review. A Sync Failed Resource may also be saved without resubmission, in which case it remains Sync Failed.
_Avoid_: Retry, reopen, refresh
