# Private MCP Python for Ubuntu PRoot

The phone's global Python3.14 is PEP668 externally managed. A previous attempt
to pip-install mcp==1.26.0 globally was rejected before MCP installation.
The existing Android pairing remains valid while its session is still running;
the MCP entry remains saved disabled until discovery succeeds.

The updated setup helper must run as a real saved .py file, not exec of an
in-memory download, because it restarts itself under a private interpreter.
It creates ~/.local/share/hermes-android-bridge/mcp-env using Python venv with
system_site_packages=True and with_pip=False. This exposes existing Hermes and
the already-installed global pip module. Running pip through the VENV interpreter
uses the venv installation scheme; no system-package override is used. Private
Python and pip visibility are checked before package installation. The exact
SDK dependency remains mcp==1.26.0, from Hermes v2026.5.29.2's pyproject.

Hermes's installed console script still has its global-Python shebang. Therefore
all setup registration and gateway calls explicitly execute:

    <private-venv>/bin/python <existing-hermes-console-script> <arguments>

The old owned MCP entry's base-interpreter path is recognized by resolved identity,
probed using a candidate pointing at the private Python, then saved only after
3 expected tools are discovered. Unrelated entries and tool filters are retained.
The adapter's token file and existing Hermes/Telegram/provider state are reused.

After success, a private launcher is saved:

```sh
proot-distro login ubuntu -- /root/.local/share/hermes-android-bridge/hermes-with-android gateway run
```

Use this launcher for future gateway restarts that need the MCP SDK. For example,
replace gateway run with mcp test android-bridge for the SDK-enabled CLI. The
global Hermes installation itself is not replaced or upgraded.

Local validation:47 tests pass on Python3.12, including actual venv creation,
active-prefix/system-pip visibility, refusal to install SDK into global Python,
base-interpreter entry migration rules and explicit-interpreter CLI dispatch.
No phone Python3.14 SDK installation/discovery has yet passed. Network/package
availability, ABI wheels and live Hermes discovery still require the phone run.
