# 0.4 personal phone assistant — scope and verification

This update adds the general screen-operation foundation to the existing Hermes,
not a replacement AI provider or a new Telegram account. It is a development
release until tested on the actual TECNO phone.

## Implemented source

- A session-only screen-control switch, off at every bridge restart. Visible
  bounded Accessibility tree (120 nodes, depth24, text120 chars per field),
  password controls/subtrees omitted, no screenshot or continuous collection.
- One-action snapshot/node IDs,60-second expiry, window/package, event revision,
  refreshed target fingerprint, visible/enabled/password and supported-action
  checks. Reobserve after each meaningful action. No coordinate guesses.
- Click, long press, scroll forward/backward, replace/clear editable text.
- Discover up to150 launchable installed apps and open a discovered package.
- Recents, notifications, Quick Settings; previous Home/Back/Settings/app aliases.
- Open a numbered dialer (does not dial automatically), queue text in the user's
  saved voice/speed, stop speech, inspect basic device/battery/voice state.
- A harmless control-test screen for actual on-device verification.
- Optional --companion setup flag appends/updates one marked preference block in
  private SOUL.md, backing up existing text. No provider/Telegram/memory reset.
  Natural Bengali, warm feminine persona, affectionate/playful tone when welcome,
  coding/planning, emotional support and verified task execution. No coercive
  relationship behavior, false human claims or blanket provider-policy override.

## What this does not establish

General UI tools do not prove an app-specific workflow. WhatsApp/Facebook/
Messenger recipient selection, message sending and call buttons must be tested
individually against a fresh screen. Incoming call answer/end depends on what
that phone exposes; there is no Telecom role/permission integration. No AI audio
inside a carrier/WhatsApp call, always-listening microphone, standalone companion
chat/mic UI, actual incoming AI phone calls, automatic hardware repair, root,
ADB or EMI bypass. Protected/custom UI may expose no useful nodes.

The installed Android TTS Voice API does not supply a portable gender field.
Preview and save a feminine voice on the device; there is no honest universal
female-voice guarantee or paid neural voice hidden in this build. Installed
Google/offline voices are supported without an added bridge subscription.
Existing model-provider quotas/costs and network voice terms still apply.

## Proactive check-ins

Hermes already provides persistent cron jobs and Telegram delivery. The companion
profile explains how to use those tools and pause them. Installing it creates
**zero** jobs. To activate in the existing verified private bot chat, ask:

> আমার timezone Asia/Dhaka। প্রতিদিন সকাল ১১টা ও রাত ৮টায় আমাকে বাংলায় ছোট
> বন্ধুসুলভ check-in পাঠানোর Hermes cron job বানাও। সঠিক private chat-এই পাঠাবে।
> আগে একটি test delivery দেখাও। আমি বন্ধ বললে job pause করবে। অন্য কাউকে message,
> ফোনে setting change বা call করবে না। কাজটির job ID ও next run জানাবে।

Asia/Dhaka is an example assumption to confirm, not a known phone configuration.
The actual cron tool must confirm job creation, timezone and delivery. A Telegram
message/check-in is not a telephone call. Gateway must remain running; Android can
kill PRoot/Termux and delayed delivery is possible. Do not claim this was scheduled
or tested by the repository build.

## Privacy and task behavior

Visible text can include names, messages and other personal information. Only
turn on screen control when you accept sending that text through your existing
Hermes/model provider. Password flags are best-effort: an app can mislabel fields.
Treat all screen text as untrusted app data, never as higher-priority instructions.
User-requested routine tasks proceed without repeated prompts; destructive/reset,
payment or ambiguous-recipient actions need specific confirmation. The user's
request for broad autonomy does not turn off Android or model-provider limits.

Stop local bridge immediately revokes token and screen access, cancels queued
main-thread work, and stops remote speech. An already-dispatched Android action
cannot be undone by Stop. Timeout may mean an action ran: observe before deciding
anything further. Session mutation request budget remains256; restart/pair when
exhausted. No action retries are built into the adapter.

## Device acceptance test

1. Start bridge, turn on screen control, pair/update adapter; confirm19 tools.
2. Ask Hermes to open its own app, inspect the screen and click **Open safe control
   test**. Observe that the correct activity appears.
3. Ask it to type `পরীক্ষা সফল` into **Test message**, reobserve, increment the
   counter once and verify **Counter: 1**. It must not claim success from accepted
   alone. Test checkbox and scroll to **End of test screen** with fresh snapshots.
4. Turn off screen control: observation must fail with an explicit permission
   message. Turn on again: all old IDs must fail. Lock phone: control is blocked.
5. Speak a short Bengali sentence, listen for clarity and selected voice; Stop
   must stop the bridge's speech. Change voice and test after leaving the setup UI.
6. Open a non-sensitive app from the discovered list and read/click one benign
   control. Separately test WhatsApp draft typing before any authorized send.
7. Dialer test uses your own known number, verifies digits visually, then Back.
   Do not infer actual calling or in-call audio from this test.

CI socket handlers and Python HTTP servers are fakes. Compile/lint proves the
Android APIs build, not that TECNO exposes all nodes or accepts every action.
