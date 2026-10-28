# Product goal — phone-operating assistant

User clarification: the assistant should handle normal phone tasks a person can
perform, including investigating a reported phone problem, finding a solution,
opening the right app/settings, changing the requested option and verifying the
result. Previously requested app navigation, message composition and calling
remain part of the goal. The target is general screen-aware operation rather
than a permanent menu limited to Home/Back or six applications.

## Preserve completed foundations

Existing Hermes v0.15.2 and Telegram/provider/memory state, working private MCP
runtime, authenticated loopback and voice engine preferences remain the base.
No root, ADB, Developer Options unlock or EMI bypass. Free/phone-only builds.
The user indicated completion after Telegram status/Home testing; this is user
confirmation, not independent observation of the phone.

## Required operation loop

1. Understand the user's goal and identify the relevant app/device symptom.
2. Inspect the visible screen and supported device/app state with permission.
3. Choose a small plan and act using UI nodes, normal intents and allowed APIs.
4. Observe again after each meaningful action. Reject stale node references.
5. Verify the requested visible/state change, then report what was actually
   checked. Spoken confidence or an accepted Android request is insufficient.
6. If blocked or uncertain, stop safely and explain the specific missing access
   or evidence. Do not blindly repeat timed-out/ambiguous actions.

Routine authorized navigation and settings changes should proceed without
repeated confirmations. Confirmation is for consequential/ambiguous changes,
such as deletion/reset or choosing between multiple message/call recipients.
No silent reset, credential replacement or protected-restriction bypass.

## Milestones

- 0.3: normal Settings and supported app launches. The initial alias list is a
  bounded first implementation; it is not the final app-support ceiling.
- Next: explicit screen-observation permission, bounded UI-node snapshots,
  password-field redaction, stable snapshot/node IDs and stale-target checks.
- Next: node click, scroll and text entry; optional coordinate fallback only
  with a fresh observed target and clear gesture permission. Verify each step.
- Then: a task loop for settings/app workflows and software-problem diagnosis.
  Use reliable reference sources if the repair procedure is unknown. Persist
  task progress and support Stop/cancel.
- Then: tested chat-app workflows with correct recipient identity and visible
  result checks. Generic click support alone does not prove reliable messaging.
- Companion chat/voice UI must ultimately operate the existing Hermes directly,
  with visible listening/thinking/acting/stopped states and clear duration.

## Practical limits

Accessibility cannot guarantee access to every app/control. UI trees may be
missing/stale; apps may change layout. Android/EMI-protected operations remain
restricted. Hardware faults, carrier/server outages and tasks requiring unavailable
permissions cannot be promised as automatic software fixes. Report those limits
truthfully. The goal is broad useful autonomy with verified results, not a claim
that every human action can be automated successfully.

## Current implementation boundary

The staged 0.3 build opens Settings and allowlisted apps. It does not yet read
screens, click inside apps, type, change settings, send messages or make calls.
Do not present those future capabilities as working before individual tests.
