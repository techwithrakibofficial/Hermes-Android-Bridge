# OpenRouter402 during the0.4.1 phone UI test

Observed error: HTTP402 with limit_source openrouter_credits; requested maximum
output131072 exceeds current allowance. This is the present provider blocker,
not evidence of an Android bridge disconnect. Prior status/device/catalog phone
checks passed. Later tool-call banners alone do not establish actual UI changes.

## Minimal repair

Use model.max_tokens=4096, keep existing provider/model/key and all Android/Telegram
configuration. The backup+atomic-write command below validates config types and
preserves a legacy string model as model.default. It changes only the output cap,
not the conversation context_length. The backup is private mode600 and must not
be uploaded or shared. Temporary YAML tests passed; actual phone/API result pending.

1. Ctrl+C current foreground gateway, wait for native Termux prompt.
2. Run:

```sh
proot-distro login ubuntu -- python3 -c 'import shutil,sys,time,yaml; from hermes_cli.config import get_config_path; from utils import atomic_yaml_write; p=get_config_path(); p.is_file() and not p.is_symlink() or sys.exit("STOP: regular config file required"); d=yaml.safe_load(p.read_text(encoding="utf-8")) or {}; isinstance(d,dict) or sys.exit("STOP: config must be a mapping"); m=d.get("model"); m={} if m is None else ({"default":m} if isinstance(m,str) else m); isinstance(m,dict) or sys.exit("STOP: invalid model configuration; nothing changed"); b=p.with_name(p.name+".before-token-cap-"+str(time.time_ns())); shutil.copy2(p,b); b.chmod(0o600); m["max_tokens"]=4096; d["model"]=m; atomic_yaml_write(p,d,sort_keys=False); yaml.safe_load(p.read_text(encoding="utf-8"))["model"]["max_tokens"]==4096 or sys.exit("STOP: saved limit verification failed"); print("PASS: model.max_tokens = 4096; config backup saved.")'
```

3. Only on PASS restart existing private launcher:

```sh
proot-distro login ubuntu -- /root/.local/share/hermes-android-bridge/hermes-with-android gateway run
```

4. In verified Telegram bot chat send /new, then request a very short text-only
reply with no tools. If it responds, perform one read-only android_screen call,
report package/node count and stop. Resume the safe UI workflow only from a fresh
observed screen. The earlier attempted scrolls may have changed the view.

No APK update, bridge restart/token renewal, SDK install or credential reset is
needed if the current app bridge session remains active. Do not automatically
repeat the old multi-step action sequence. A smaller output cap can still receive
402 if current balance cannot cover input plus output. This does not grant free
credits or guarantee future requests. No paid upgrade was selected.

## Verified upstream references

- Exact installed release source: https://github.com/NousResearch/hermes-agent/blob/v2026.5.29.2/agent/agent_init.py
  reads positive integer model.max_tokens into agent.max_tokens.
- Config model.default/provider schema: https://github.com/NousResearch/hermes-agent/blob/v2026.5.29.2/cli-config.yaml.example
- Provider preflight explanation: https://github.com/OpenRouterTeam/docs/blob/main/api_reference/limits.mdx

The short-reply API test after restart is required before calling this resolved.
Never paste .env, config backups or full credential-bearing gateway logs into chat.
