# Phone-only upgrade — Hermes Bridge0.4.0

APK compile/lint and46 real-socket protocol checks (fake Android handler) passed:
https://github.com/techwithrakibofficial/Hermes-Android-Bridge/actions/runs/40642075456
Python3.12 and3.14:59 tests each passed:
https://github.com/techwithrakibofficial/Hermes-Android-Bridge/actions/runs/40642075403
These do not establish actual phone screen-control or messaging success.

## 1. Install APK

https://github.com/techwithrakibofficial/Hermes-Android-Bridge/releases/download/dev-12/Hermes-Bridge-0.4.0.apk

APK SHA256:2671a19802bd77954172444eaef79c099e7655841cbab6c7315ca9321ac59fed

Update the existing Bridge app. If Android reports a signing-certificate mismatch,
remove/reinstall **only Hermes Bridge**, then enable its Accessibility again and
reselect your voice if needed. Preserve Termux, Ubuntu, Hermes and Telegram.
No root, ADB, Developer Options or EMI bypass is required.

## 2. Stop the current foreground gateway, pair the new bridge

In the terminal running the gateway, press Ctrl+C once and wait for the native
Termux `~ $` prompt. No new Termux session is required.
Open Bridge0.4.0 → Start local bridge → read disclosure → enable **Allow screen
reading & control**. The switch is off at every new session.
Tap **Copy Termux pairing command**, paste it **alone** at native Termux `$` and
press Enter. Do not paste it into an unfinished curl command or into Telegram.
Never share the copied token or .env.

## 3. Run this single pinned updater

This reuses the working private SDK and Hermes installation, backs up config,
verifies19 discovered tools, adds the requested companion preference block to
backed-up SOUL.md, then starts the existing gateway. Existing SOUL.md content
outside the marked block is preserved. Provider/Telegram credentials and memory
are retained. No check-in job is created by this command.

```sh
proot-distro login ubuntu -- env HERMES_BRIDGE_TOKEN="$HERMES_BRIDGE_TOKEN" python3 -c 'import hashlib,os,sys,urllib.request; from pathlib import Path; d=urllib.request.urlopen("https://raw.githubusercontent.com/techwithrakibofficial/Hermes-Android-Bridge/d616cfb14878b1a7652212e2de49207118f9490f/integrations/hermes_bridge/setup_android_bridge.py",timeout=30).read(); hashlib.sha256(d).hexdigest()=="8bb988ef62eda3b4c2828fb3f8d1cc707f130d635ccae946a4624b6b8e841908" or sys.exit("Download verification failed"); p=Path.home()/".local/share/hermes-android-bridge/setup_android_bridge.py"; p.parent.mkdir(parents=True,exist_ok=True); p.write_bytes(d); p.chmod(0o600); os.execv(sys.executable,[sys.executable,str(p),"--install-sdk","--companion"])'
```

If a new registration prompt appears, choose Y for all tools. A plain gateway
banner without an AI reply does not prove the Telegram round trip. If STOP is
shown, send only the visible CHECK/PASS/STOP lines, never secret exports.

## 4. Verify safely in Telegram

Send `/new` in your existing paired private bot chat so the new tools/preferences
are loaded. Then send:

> android_bridge_status এবং android_device_state দিয়ে version ও screen control
> পরীক্ষা করো। তারপর android_list_apps দিয়ে Hermes Bridge খুঁজে app খোলো। নতুন
> screen দেখে Open safe control test button চাপো। Test message-এ পরীক্ষা সফল
> লিখে, counter একবার বাড়িয়ে নতুন screen থেকে ফল যাচাই করো। কোনো message পাঠাবে
> না বা অন্য app-এর setting বদলাবে না।

Verify the phone actually shows the draft and **Counter: 1**. After each action,
Hermes must observe again, rather than reuse a node or claim success from accepted.
If nodes are unavailable/expired, report that exact result. Full acceptance tests
are in docs/COMPANION_0_4.md.

## 5. Choose a clear feminine voice

In Bridge → Voice output → বাংলা → Choose voice → Test selected voice. Preview the
installed voices, save the one you hear as feminine/clear, then leave the setup
screen and ask Hermes:

> android_speak দিয়ে বলো: আমি আছি, বলো আজ তোমার দিনটা কেমন গেল?

Listen for the selected voice and speed. Standard Android voices do not provide
a reliable gender field. Do not infer the sound from a Voice1/Voice2 label.
Offline installed voices avoid additional TTS service charges. Online voices may
send text to their engine provider. Existing inference provider limits still apply.

## Proactive contact and limits

Coding/planning/discussion use your existing Hermes/provider. The companion
profile requests warm, feminine, affectionate Bengali conversation when welcome.
It does not change the provider's policies or remove Android restrictions.

To activate proactive **Telegram** check-ins, use the timing/delivery prompt in
docs/COMPANION_0_4.md after confirming your timezone. The actual Hermes cron tool
must return job IDs/next run and a test delivery. None is scheduled here.
No actual incoming telephone calls, live AI conversation within carrier/WhatsApp
calls, standalone mic/chat UI or universal automatic phone repair is included.
General click/type tools allow broader tasks; each real app/send/call workflow
still needs correct recipient and outcome verification.

## Later restart

Keep the app bridge and Termux alive. If the same app session remains active:

```sh
proot-distro login ubuntu -- /root/.local/share/hermes-android-bridge/hermes-with-android gateway run
```

After app bridge restart, copy a fresh export and repeat the pinned updater.
Android can kill the processes, so background delivery has no uptime guarantee.
