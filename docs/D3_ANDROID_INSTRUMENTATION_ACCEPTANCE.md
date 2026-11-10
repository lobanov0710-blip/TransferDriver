# D3 Android — instrumentation acceptance matrix

Status: planned, NOT EXECUTED. Scope: TransferDriver Java/XML, isolated debug instrumentation. No R2, production, real passenger data, or D4 liveness.

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

## Required execution evidence

- Gradle `:app:connectedDebugAndroidTest` result, Android API/device, exact commit SHA and CI run.
- Emulator/device screenshot assertions and Activity recreation outcomes.
- MockWebServer/controlled fake API traces with synthetic identifiers only; no Firebase credentials or real data.
- Separate STAGING support/chat E2E only when required, with authorization, isolation, 409 and replay checks; do not enable private uploads until R2 is approved.
- Update PROJECT_JOURNAL.md with PASS/FAIL/SKIP per case, defects and follow-up commits.

## Release gates

Do not claim UI runtime PASS from JVM unit tests or assembleDebug alone. No merge to master, production deploy, remote D1 migration, R2 provisioning or D4 face/liveness changes under this matrix.
