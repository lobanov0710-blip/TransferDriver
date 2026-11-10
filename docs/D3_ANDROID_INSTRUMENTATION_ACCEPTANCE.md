# D3 Android — instrumentation acceptance matrix

Status: PARTIALLY EXECUTED (security subset PASS on Android emulator; full D3 UI/E2E NOT ACCEPTED). Scope: TransferDriver Java/XML, isolated debug instrumentation. No R2, production, real passenger data, or D4 liveness.

## Test cases

| ID | Setup | Action | Expected result |
|---|---|---|---|
| UI-01 | authenticated driver, own photo check | open photo-check list and detail | only own checks and seven required angles; no public object URL |
| UI-02 | pending photo check, server uploadedKinds subset | recreate Activity | accepted angles restored from server; no locally invented success |
| UI-03 | camera unavailable / denied | request capture | safe error; no completed/uploaded marker |
| UI-04 | camera temp file in app cache | launch camera through FileProvider | URI grant limited to camera recipient; no external storage path |
| UI-05 | photo pending during rotation | recreate Activity | pending private cache path retained, no duplicate upload |
| UI-06 | upload timeout | refresh backend uploadedKinds | server is source of truth; retry cannot mark success locally |
| UI-07 | authenticated driver | create own support case | correct category and idempotency key; only own case visible |
| UI-08 | support message network timeout | retry same pending message | same key, body and expectedRevision; no duplicate displayed as confirmed |
| UI-09 | support message returns HTTP 409 | refresh case | pending mutation cleared; server revision displayed |
| UI-10 | support case closed by server | attempt send | action disabled; backend rejection handled |
| UI-11 | assigned order in taken phase | open details | no self-take or direct trip start; verification gate remains disabled |
| UI-12 | own in_progress order | submit done twice | stable Idempotency-Key and expectedRevision; terminal result follows backend |
| UI-13 | screenshot/recents capture | open photo/support/details screens and confirmation dialog | FLAG_SECURE applied; dialog flag set before show |

## Actual execution — 2026-11-10

Instrumented test suite: **10/10 PASS**, Android API 35 emulator, code SHA
`060a06a250fdd6ee5335633105c854992b6caa04`.

- [D3 UI instrumentation run 42862576549](https://github.com/lobanov0710-blip/TransferDriver/actions/runs/42862576549): PASS.
- [Android branch tests run 42862576550](https://github.com/lobanov0710-blip/TransferDriver/actions/runs/42862576550): PASS.
- Real Android checks: FileProvider scope, non-exported screens, sanitized EXIF/GPS, EXIF mirror/rotation/transpose, invalid JPEG fail-closed, secure LoginActivity window after recreation and secure inheritance of sensitive screens.
- Earlier [run 42862000650](https://github.com/lobanov0710-blip/TransferDriver/actions/runs/42862000650) was 9/10: an overly strict assertion rejected absent EXIF orientation. The production image transform passed; test corrected and re-run.

Coverage against the **13-case matrix** (do not mark an entire case PASS from partial evidence):

| Case | Status | Evidence still required |
|---|---|---|
| UI-01 | NOT RUN | Own-check list/detail against controlled backend |
| UI-02 | NOT RUN | Server uploadedKinds recreation/recovery |
| UI-03 | NOT RUN | Camera denied/unavailable behavior |
| UI-04 | PARTIAL | FileProvider scope passed; external camera URI grants untested |
| UI-05 | NOT RUN | Pending private photo across Activity recreation |
| UI-06 | NOT RUN | Timeout and server reconciliation; private upload deferred while R2 off |
| UI-07 | NOT RUN | Own support case create/list interaction |
| UI-08 | PARTIAL | Emulator: offline retry after Activity recreation reuses key/body/revision; server replay acceptance untested |
| UI-09 | PARTIAL | Emulator: HTTP 409 refreshes case and next send uses new key/revision; server response contract not E2E |
| UI-10 | PARTIAL | Emulator PASS: closed case and server-side closure discovered after 409 disable composer; real backend E2E pending |
| UI-11 | NOT RUN | Assigned-only gate at runtime |
| UI-12 | NOT RUN | Terminal done UI replay interaction |
| UI-13 | PARTIAL | Login FLAG_SECURE + inheritance passed; real confirmation dialog runtime untested |

No staging/production calls were made by these emulator checks. R2 remains
unconfigured, so do not simulate a successful private photo upload.

## Synthetic support UI instrumentation — 2026-11-10

- [Android branch tests 42883503403](https://github.com/lobanov0710-blip/TransferDriver/actions/runs/42883503403): PASS at `c4f10984f8f5f404ccd288169f9856265c36f8af`.
- [Android emulator 42883503884](https://github.com/lobanov0710-blip/TransferDriver/actions/runs/42883503884): PASS at the same SHA.
- `SupportCaseUiInstrumentationTest` exercises UI-08 (offline replay across Activity recreation), UI-09 (409 refresh and new revision/key), UI-10 (closed case disabled and secure window).
- Uses a **debug-source-set-only** fake `SupportCaseGateway`; the release source set has a separate factory that always creates `DriverV1Repository`. No live API, STAGING, production or R2.
- Follow-up test for **server-side closure discovered on HTTP 409** added in `f820f6935cc8f05b453cb9211c9e2ac6f71e9ead`, and passed on Android emulator in the final run below.
- UI-08/09/10 are **partial** against full acceptance requirements until server-backed and UI-state edge cases are independently verified. Do not treat synthetic emulator PASS as STAGING E2E.

## Final verification of support UI tests and release source set

- Code SHA: `e8f43f9c053e6dca32cf6ee88c63e9aa9acdf8d7`.
- [Android branch tests 42884298313](https://github.com/lobanov0710-blip/TransferDriver/actions/runs/42884298313): PASS; Gradle executed `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:assembleDebugAndroidTest` and `:app:compileReleaseJavaWithJavac`.
- [Android emulator 42884298225](https://github.com/lobanov0710-blip/TransferDriver/actions/runs/42884298225): PASS; includes UI recovery from 409 into a **closed** case.
- Release Java compilation passed; no signed release artifact or production deploy was made.
- UI-08/09/10 still require backend-backed authorization/idempotency/replay E2E for full acceptance. Photo-control private upload remains blocked on deferred R2.

## Required execution evidence

- Gradle `:app:connectedDebugAndroidTest` result, Android API/device, exact commit SHA and CI run.
- Emulator/device screenshot assertions and Activity recreation outcomes.
- MockWebServer/controlled fake API traces with synthetic identifiers only; no Firebase credentials or real data.
- Separate STAGING support/chat E2E only when required, with authorization, isolation, 409 and replay checks; do not enable private uploads until R2 is approved.
- Update PROJECT_JOURNAL.md with PASS/FAIL/SKIP per case, defects and follow-up commits.

## Release gates

Do not claim UI runtime PASS from JVM unit tests or assembleDebug alone. No merge to master, production deploy, remote D1 migration, R2 provisioning or D4 face/liveness changes under this matrix.
