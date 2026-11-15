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
| UI-01 | PARTIAL | Synthetic assigned-check list hides stale entries on refresh/error; own-only backend authorization and seven-angle E2E pending |
| UI-02 | PARTIAL | Emulator PASS: uploadedKinds restored after Activity recreation; real server E2E still required |
| UI-03 | PARTIAL | Synthetic cancelled camera callback cleans private capture file; external camera denial/unavailable untested |
| UI-04 | PARTIAL | FileProvider scope passed; external camera URI grants untested |
| UI-05 | PARTIAL | Emulator PASS: pending private file survives Activity recreation and explicit retry; live upload untested |
| UI-06 | PARTIAL | Emulator PASS: synthetic timeout and uploadedKinds reconciliation; private R2 upload and real HTTP remain deferred |
| UI-07 | PARTIAL (synthetic emulator PASS) | Synthetic support list and create/retry after Activity recreation; backend own-only HTTP E2E pending |
| UI-08 | PARTIAL | Emulator: offline retry after Activity recreation reuses key/body/revision; server replay acceptance untested |
| UI-09 | PARTIAL | Emulator: HTTP 409 refreshes case and next send uses new key/revision; server response contract not E2E |
| UI-10 | PARTIAL | Emulator PASS: closed case and server-side closure discovered after 409 disable composer; real backend E2E pending |
| UI-11 | PARTIAL (synthetic emulator PASS) | Synthetic Android UI: taken order verification gate disabled; backend assignment-phase and D4 gate E2E untested |
| UI-12 | PARTIAL (synthetic emulator PASS) | Synthetic Android UI: in_progress done, in-flight disabled, timeout retry same revision; deterministic key contract in JVM; real HTTP replay untested |
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

## Photo-control recovery test — pending CI

- Commit `61fe2138771388c7495c9ab4b705f0a14bb33db2`: fixes false-completed UI when assigned-only check is absent; adds debug-only fake gateway and Android emulator recovery tests.
- [Android branch tests 42886168284](https://github.com/lobanov0710-blip/TransferDriver/actions/runs/42886168284) — PASS.
- [Android instrumentation 42886168323](https://github.com/lobanov0710-blip/TransferDriver/actions/runs/42886168323) — PASS.
- No actual photo upload, camera intent, R2 or production calls; do not mark UI-02 fully accepted from this synthetic test.

## Required execution evidence

- Gradle `:app:connectedDebugAndroidTest` result, Android API/device, exact commit SHA and CI run.
- Emulator/device screenshot assertions and Activity recreation outcomes.
- MockWebServer/controlled fake API traces with synthetic identifiers only; no Firebase credentials or real data.
- Separate STAGING support/chat E2E only when required, with authorization, isolation, 409 and replay checks; do not enable private uploads until R2 is approved.
- Update PROJECT_JOURNAL.md with PASS/FAIL/SKIP per case, defects and follow-up commits.

## Release gates

Do not claim UI runtime PASS from JVM unit tests or assembleDebug alone. No merge to master, production deploy, remote D1 migration, R2 provisioning or D4 face/liveness changes under this matrix.

## Photo pending-file and timeout recovery — verified 2026-11-11

- SHA `4191f9e0223637f52d83eff3ecd74f6b0a1e869d`.
- [Android branch tests 42888790438](https://github.com/lobanov0710-blip/TransferDriver/actions/runs/42888790438): **PASS**.
- [Android emulator 42888790491](https://github.com/lobanov0710-blip/TransferDriver/actions/runs/42888790491): **PASS**.
- UI-05: private pending file survives Activity recreation; explicit retry after simulated timeout uses the same file and kind, with no automatic upload on recreation.
- UI-06: simulated server acknowledgement in `uploadedKinds` removes pending file and disables duplicate capture/upload; live network/R2 E2E remains deferred.
- These are synthetic UI tests, not evidence of camera URI grants, real upload or backend authorization.

## Camera cancel and pending refresh regression — CI verified

- SHA `59c7a982fd64f922367c58ce9d74b5d28e83eb6e`.
- [Android branch tests 42963137921](https://github.com/lobanov0710-blip/TransferDriver/actions/runs/42963137921): **PASS**, verified 2026-11-14.
- [Android emulator 42963137998](https://github.com/lobanov0710-blip/TransferDriver/actions/runs/42963137998): **PASS**, verified 2026-11-14.
- Tests hold the synthetic refresh response to verify buttons are disabled during network loading, then re-enabled on success; cancel callback removes the private capture file without uploading.
- Actual camera app absence, URI grant behavior and R2 upload remain untested.

## Assigned photo list fail-closed — CI verified

- Code SHA `d5d0e2569bd2d1943b084a872ac0fd4ecd3560ef`.
- Synthetic Android UI test holds a list refresh and injects an error: old list items must disappear immediately and remain unavailable until a successful server response.
- UI-01 partial only; no real backend own-only authorization, camera, R2, or production operations.

## 2026-11-14 — latest verified D3 gates and support list hardening

- Previous Android code SHA `d5d0e2569bd2d1943b084a872ac0fd4ecd3560ef`:
  [Android branch tests 43020402371](https://github.com/lobanov0710-blip/TransferDriver/actions/runs/43020402371) **PASS**,
  [Android emulator 43020402391](https://github.com/lobanov0710-blip/TransferDriver/actions/runs/43020402391) **PASS**.
- Backend `uber-v3` on SHA `1f9a263d40f06122ea301951c397c2c53c077b05`:
  [Backend tests 43032988844](https://github.com/lobanov0710-blip/uber-v3/actions/runs/43032988844) **PASS**.
  This verifies SQLite terminal-mutation authorization, idempotency and audit tests, **not** an Android-to-HTTP end-to-end journey.
- New Android support list hardening on SHA `b676dd94df036a66008fcd94ed79bf4284b4e975`:
  `SupportCasesActivity` clears stale entries during refresh and errors; ignores responses from superseded requests;
  support category dialog applies `FLAG_SECURE` before show.
  Debug-only `SupportCasesGatewayFactory` enables synthetic list/create tests;
  release always uses `DriverV1Repository`.
- New synthetic emulator tests: `failedRefreshClearsPreviouslyVisibleSupportCases`,
  `lateResponseCannotRestoreSupersededSupportList`,
  `supportCreateTimeoutRetriesSameCategoryAndKeyAfterRecreation`.
- New CI [Android branch tests 43584663155](https://github.com/lobanov0710-blip/TransferDriver/actions/runs/43584663155) and
  [D3 UI instrumentation 43584663103](https://github.com/lobanov0710-blip/TransferDriver/actions/runs/43584663103)
  **PENDING at time of writing**. Do not mark new tests PASS until results verified.
- UI-07 remains PARTIAL even if synthetic tests pass: real driver identity, backend ownership and HTTP retry must be independently verified.
- UI-11, UI-12 remain NOT RUN at the Android UI layer; UI-02/04/05/06 and photo R2 upload remain PARTIAL/deferred.
- No production deploy, R2 provisioning, D4 liveness, merge or remote migrations.

## 2026-11-15 — UI-11/UI-12 and stale photo callback regression

Code SHA `d35fc2fc333cecd9428b34be986104bae383fb25` on
`fix/d3-ui-instrumentation-ci`.

- UI-11: synthetic `taken` assigned order shows disabled verification
  gate and has no enabled self-start/terminal action. This does not
  approve or implement D4 liveness.
- UI-12: synthetic `in_progress` order sends `done` with
  `expectedRevision`; while request is pending the button is disabled;
  after timeout an explicit retry keeps the same order/status/revision,
  and `IdempotencyKeyFactory` has a deterministic JVM contract.
  Synthetic server acknowledgement refreshes the order into `done`
  and hides the terminal action. The fake does not intercept a real
  HTTP Idempotency-Key header, so HTTP replay remains unverified.
- Order details are hidden during assigned-only refresh; the previous
  revision is invalidated until server revalidation. Disabled actions
  cannot open confirmation programmatically.
- Photo list/detail `getPhotoChecks` callbacks are guarded by request
  generation. Stale success/error cannot re-enable already uploaded
  angles or restore revoked list items. Existing buttons are disabled
  and `currentCheck` cleared during refresh. Tests use synthetic
  debug-only `PhotoCheckGateway`.
- Previous Android CI [43584663155](https://github.com/lobanov0710-blip/TransferDriver/actions/runs/43584663155) and
  [43584663103](https://github.com/lobanov0710-blip/TransferDriver/actions/runs/43584663103)
  **FAIL** due to Java variable shadowing in `SupportCasesActivity`
  (`dialog` lambda vs local `AlertDialog dialog`). Fixed at
  `301c82d42bbe9f9f64a16c8cbb58eb10f16b957c`.
- CI [Android branch tests 43722312807](https://github.com/lobanov0710-blip/TransferDriver/actions/runs/43722312807)
  **PASS**; [D3 UI instrumentation 43722312783](https://github.com/lobanov0710-blip/TransferDriver/actions/runs/43722312783)
  **FAIL** (28/30 passed). Both failures were Espresso dialog root focus errors,
  not production assertion failures. See verified retry below.
- UI-11 and UI-12 are **PARTIAL** against the full acceptance matrix:
  real assigned-only authorization, production-equivalent backend
  transition and end-to-end idempotency/audit remain untested.
- No owner phone setup, credentials, production or STAGING calls, R2,
  D4, merge, remote migration or deploy.

## 2026-11-15 — UI-07, UI-11, UI-12 and stale photo callbacks: verified CI

- Final code SHA `3a4dba477e34efab7f126eefa236ed2a077a1277`.
- [Android branch tests 43722768440](https://github.com/lobanov0710-blip/TransferDriver/actions/runs/43722768440):
  **PASS** (JVM, debug and release Java compilation).
- [D3 UI instrumentation 43722768431](https://github.com/lobanov0710-blip/TransferDriver/actions/runs/43722768431):
  **PASS**, Android emulator `:app:connectedDebugAndroidTest`.
- UI-07: synthetic support list fail-closed, superseded callbacks ignored,
  create timeout/retry retains idempotency key across recreation — emulator PASS.
- UI-11: synthetic taken assigned order cannot self-start or trigger done;
  verification gate remains disabled — emulator PASS.
- UI-12: synthetic in_progress done request disables concurrent user action;
  timeout permits explicit replay with unchanged order/status/revision;
  accepted done refresh hides action — emulator PASS. Stable
  `IdempotencyKeyFactory` and Retrofit header contract are JVM-tested,
  but actual HTTP header/replay is not tested by the fake gateway.
- Photo list and detail: superseded success/error responses cannot restore
  stale assignments or re-enable already uploaded angles — emulator PASS.
- UI-07/UI-11/UI-12 remain **PARTIAL** against full E2E acceptance;
  synthetic UI PASS is not evidence of real driver identity, server
  authorization, HTTP replay, audit persistence, or D4 liveness.
- No production/STAGING traffic, owner phone setup, R2, D4,
  merge, remote migrations or deploy.
