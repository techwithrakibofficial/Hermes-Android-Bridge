# 0.5.0 — ফোনে upgrade ও acceptance guide

## Download ও প্রমাণ

[APK ডাউনলোড — Hermes Bridge 0.5.0](https://github.com/techwithrakibofficial/Hermes-Android-Bridge/releases/download/dev-20/Hermes-Bridge-0.5.0.apk) · [Release / checksum](https://github.com/techwithrakibofficial/Hermes-Android-Bridge/releases/tag/dev-20)

এটি development / phone acceptance build। APK তৈরি, lint, Java lifecycle/protocol tests এবং exact Hermes integration CI-তে পাস করেছে। নতুন microphone/TTS flow এখনো TECNO ফোনে পরীক্ষা করা হয়নি। APK SHA256:

```text
ed4ec5c67b2698261cf57ab9437fdb51da249a669fe5e4467efb92859b31ee9a
```

## ১. APK update এবং bridge pairing

1. উপরের APK download করে আগের Hermes Bridge-এর উপর Update করুন। শুধু app update করুন; Hermes/Termux/Telegram পুনরায় install/setup করবেন না।
2. নতুন home-এ **Connection → Open Bridge setup**। Local bridge Start করুন, screen reading/control চালু আছে যাচাই করুন। আগের TTS voice/speed একই app data-তে থাকবে।
3. Bridge-এর **Copy Termux export** ব্যবহার করুন। Termux-এ পুরোনো foreground gateway চললে **Ctrl+C একবার** দিয়ে prompt ফিরিয়ে আনুন। Export command-টি একা native Termux-এর `~ $` prompt-এ paste করে Enter দিন। কোনো curl/অন্য command-এর মাঝে paste করবেন না। Token কাউকে পাঠাবেন না।
4. App update-এর signature mismatch হলে থামুন এবং exact install error জানান। Cached debug key হারালে Bridge-only reinstall লাগতে পারে; এতে Bridge preferences/permission মুছবে। Termux/Hermes data মুছবেন না।

## ২. একবার private companion setup

APK একা Hermes চালায় না। Existing Ubuntu Hermes-এর পাশে একটি private localhost backend লাগবে। নিচের **পুরো command একবারে** native Termux prompt-এ দিন; export command-এর পর আলাদাভাবে। নতুন Termux session দরকার নেই।

```sh
proot-distro login ubuntu -- env HERMES_BRIDGE_TOKEN="$HERMES_BRIDGE_TOKEN" python3 -c 'import hashlib,os,sys,urllib.request; from pathlib import Path; d=urllib.request.urlopen("https://raw.githubusercontent.com/techwithrakibofficial/Hermes-Android-Bridge/c1bdca0215d5386ff096cc6cc5591d900e0fb5a3/integrations/hermes_bridge/setup_companion.py",timeout=30).read(); hashlib.sha256(d).hexdigest()=="b7f18b3e8c42a5dae33511078b8d924dc21f038021d7b4c49ed8ded0947e70cb" or sys.exit("Download verification failed"); p=Path.home()/".local/share/hermes-android-bridge/setup_companion.py"; p.parent.mkdir(parents=True,exist_ok=True); p.write_bytes(d); p.chmod(0o600); os.execv(sys.executable,[sys.executable,str(p),"--install-acp"])'
```

Expected milestones: existing Android pairing PASS, private ACP SDK0.9.0 imports PASS, backend SHA256 PASS, installed Hermes ACP initialize PASS, launchers saved, then **STARTING: native companion + existing Telegram gateway**। Optional SDK download-এ internet লাগবে। Setup inference/action চালিয়ে পরীক্ষা করে না। `STOP` হলে visible PASS/STOP lines জানান; token/.env/config backup পাঠাবেন না।

এটি existing private `mcp-env`-তে শুধু pinned `agent-client-protocol==0.9.0` dependency যোগ করে; Hermes upgrade, system Python override, provider/Telegram/pairing/cron reset করে না। Backend file বদলালে private backup রাখে। Existing 19-tool adapter অপরিবর্তিত। Setup-এর downloaded backend commit `3fad2328dea77584be2337744623ae5d3fe57d56`, SHA256 `7471d22ebf140e2f88b5022c9f65f26a198a422692827dedb2f3b3a114b3c35f`।

Termux foreground command চলতে দিন। Android app-এ **Connection → Reconnect / check current task** চাপুন। Android ও Hermes দুটো connected হলে text chat শুরু করুন। এটি একই Termux session-এ native backend এবং আগের Telegram gateway চালায়। নতুন gateway-এর পাশাপাশি পুরোনো আরেকটি gateway চালাবেন না। Cron-এর stored schedule অপরিবর্তিত থাকে।

পরের বার চালানোর command:

```sh
proot-distro login ubuntu -- /root/.local/share/hermes-android-bridge/hermes-companion-all
```

Bridge restart হলে token বদলায়: নতুন export নিয়ে Termux-এ paste করুন এবং নিচের saved installer command চালান; installer current pairing private file-এ save করে। Backend running থাকলে দ্বিতীয় gateway চালায় না। 
```sh
proot-distro login ubuntu -- env HERMES_BRIDGE_TOKEN="$HERMES_BRIDGE_TOKEN" python3 /root/.local/share/hermes-android-bridge/setup_companion.py --install-acp
```

Advanced separate-session use-এ backend-only launcher `hermes-companion` আছে; সাধারণভাবে `hermes-companion-all` ব্যবহার করুন।

## ৩. Text দিয়ে আগে পরীক্ষা

1. লিখুন: **“বাংলায় এক বাক্যে উত্তর দাও—তুমি কি প্রস্তুত?”** উত্তর দেখা মানে native app → existing Hermes → reply কাজ করছে।
2. লিখুন: **“শুধু bridge status ও device state পড়ে জানাও, কোনো setting বদলাবে না।”** Returned tool result যাচাই করুন।
3. **New chat** দিয়ে নতুন কথা শুরু, **History** দিয়ে পুরোনো conversation-এ ফেরা পরীক্ষা করুন। App history clear করলে Hermes archive/memory মুছে না।
4. Harmless control test: **“Hermes Bridge app-এর Connection → Open Bridge setup খুলে Open safe control test-এ যাও। নতুন snapshot নিয়ে test message-এ পরীক্ষা সফল লেখো, counter একবার বাড়াও, একটু অপেক্ষা করে নতুন snapshot-এ draft/counter যাচাই করো। Accepted-কে success ধরবে না; blind retry করবে না।”**

Agent চলাকালে ফোনে নিজে অন্য navigation করবেন না; target বদলালে fresh-screen guard action থামাতে পারে। Provider402/429 হলে app/provider message দেখুন; repeat tap করে request flood করবেন না। Free provider unlimited নয়।

## ৪. Voice mode

- **Voice** tab-এ বাংলা mic / English mic বাছুন। **কণ্ঠ, language ও speed বাছুন** দিয়ে আগের TTS voice preview করে clear voice নির্বাচন করুন। Feminine voice engine-এ থাকলে সেটি বাছুন; সব ফোনে একই voice/quality পাওয়া যাবে না।
- **Mic** চাপুন, disclosure পড়ুন এবং microphone permission দিন। Connection-এর notification permission allow করলে foreground task-এর Stop control notification-এ পাওয়া যায়।
- Default on-device recognition। বাংলা offline engine/model না থাকলে unavailable message আসবে। Android speech settings-এ language/service দেখুন; চাইলে **Network speech allow করি** opt-in দিন। এতে speech service audio online পাঠাতে পারে। Silent online fallback নেই।
- প্রথমে একবার বলুন, উত্তর শুনুন। পরে **উত্তরের পরে আবার শুনবে** toggle দিয়ে conversation mode পরীক্ষা করুন। Recognition চলাকালে app নিজের TTS শোনে না; সফল spoken reply শেষে একবার পরের turn শোনে।
- Silence, recognizer busy/error, call/audio-focus interruption বা session limit হলে pause হবে। Mic চাপলে resume; error থেকে অসীম auto-restart নেই। Session ১৫ মিনিটে pause হয়।
- **Interrupt & mic** দিয়ে speech থামিয়ে আপনার কথা বলা যায়। **Stop** local mic/TTS সঙ্গে সঙ্গে থামায়, চলতি Hermes কাজ cancel request দেয়। ইতিমধ্যে হওয়া click/message/action undo করে না। Notification Stop-ও আছে।
- App কোনো extra beep যোগ করে না। Android/OEM speech engine-এর নিজস্ব শুরু/শেষ sound পুরোপুরি সরানোর নিশ্চয়তা নেই। এটি turn-taking voice; Gemini-এর সব full-duplex behavior বা carrier-call audio নয়।

## ৫. ফোনে acceptance checklist

প্রতি ধাপে actual result লিখুন; ব্যর্থ হলে error/status-এর screenshot বা text দিন, credentials নয়। আগে harmless test activity ব্যবহার করুন।

| পরীক্ষা | প্রত্যাশিত ফল | বর্তমান অবস্থা |
|---|---|---|
| Native text + status tool | এক request, আসল reply/tool result | ফোনে বাকি |
| বাংলা/English mic final | একবার submit, duplicate নয় | ফোনে বাকি |
| TTS এবং automatic next turn | mic/TTS overlap নয়; একবার rearm | ফোনে বাকি |
| Mic দ্রুত দুবার; Stop during listening/speech | duplicate start নয়; late callback উপেক্ষা | ফোনে বাকি |
| ৩০ সেকেন্ড চুপ; recognizer error | pause, repeated beep/restart loop নয় | ফোনে বাকি |
| Permission deny/revoke, headset/call interruption | পরিষ্কার reason ও pause; crash নয় | ফোনে বাকি |
| Task চলাকালে bridge app ছাড়ে, তারপর ফিরে আসে | notification/progress থাকে; result reobserve | ফোনে বাকি |
| Network loss / provider402 বা429 | safe error/pause; inference auto-resend নয় | ফোনে বাকি |
| Backend বন্ধ করে ফের চালানো | interrupted task replay নয়; নতুন prompt চলে | ফোনে বাকি |
| Bridge Stop/restart/new pairing | পুরোনো token reject; নতুন pairing reconnect | ফোনে বাকি |
| Tool approval Deny / Allow once / Stop | একবারের decision; Stop-এ late approval কাজ নয় | ফোনে বাকি |
| History selection / process restart | persisted Hermes session ফিরে আসে | ফোনে বাকি |
| সকাল১১/রাত৮ cron | Telegram home-এ actual scheduled delivery | প্রথম recurring run বাকি |

## Scope ও troubleshooting

Existing screen/app tools, coding/planning/conversation এবং daily check-ins রাখা হয়েছে। Generic messaging/dialer workflow app UI/permissions অনুযায়ী observed verification চায়। Live telephone call-এর ভিতরে AI কথা বলা, নিজে AI incoming call, সব hardware/carrier fault repair, locked Developer Options bypass, offline unlimited LLM এবং unlimited free cloud service এই build-এর feature নয়।

Android/OEM battery policy Termux/app বন্ধ করতে পারে। Android থেকে সাধারণ battery settings-এ unrestricted/background allowance থাকলে প্রয়োজনমতো ব্যবহার করুন; কোনো protected policy bypass করবেন না। Device reboot-এর পরে নিজে bridge ও Termux launcher start করতে হবে।

Rollback: Ctrl+C combined launcher বন্ধ করুন। পুরোনো Telegram-only launcher `proot-distro login ubuntu -- /root/.local/share/hermes-android-bridge/hermes-with-android gateway run` দিয়ে আগের Telegram bridge চালানো যায়। Hermes config বা cron reset করবেন না। APK downgrade Android reject করতে পারে; আগে শুধু নতুন native flow বন্ধ রেখে পুরোনো Bridge setup/Telegram path ব্যবহার করুন।
