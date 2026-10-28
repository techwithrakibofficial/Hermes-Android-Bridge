"""Actual pinned Hermes+ACP+MCP against a fake local LLM/Android endpoint; no external inference."""
import json
import os
from pathlib import Path
import sys
import tempfile
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/"integrations/hermes_bridge"))
from companion_server import Journal, Manager
TOKEN="A"*43
class Endpoint(BaseHTTPRequestHandler):
    bridge_calls=0
    tool_offered=False
    def log_message(self,*args): pass
    def answer(self,body,kind="application/json"):
        data=body if isinstance(body,bytes) else json.dumps(body).encode()
        self.send_response(200);self.send_header("Content-Type",kind);self.send_header("Content-Length",str(len(data)));self.end_headers();self.wfile.write(data)
    def do_GET(self):
        if self.path=="/bridge/v1/status":
            type(self).bridge_calls+=1
            return self.answer({"bridge":"ready","version":"0.5.0","allowed_actions":["home","back","settings","open_app"],"supported_apps":[]})
        return self.answer({"object":"list","data":[{"id":"companion-fixture","object":"model","owned_by":"fixture","context_length":65536}]})
    def do_POST(self):
        data=json.loads(self.rfile.read(int(self.headers.get("Content-Length","0"))))
        tools=data.get("tools") or []
        name=next((t["function"]["name"] for t in tools if t.get("function",{}).get("name","").endswith("android_bridge_status")),None)
        if name: type(self).tool_offered=True
        completed=any(m.get("role")=="tool" for m in data.get("messages",[]))
        call=name and not completed
        message={"role":"assistant","content":None if call else ("Fixture bridge ready." if name else "Fixture title")}
        if call: message["tool_calls"]=[{"id":"call_status","type":"function","function":{"name":name,"arguments":"{}"}}]
        if data.get("stream"):
            delta=dict(message);delta.pop("role",None)
            if call: delta["tool_calls"][0]["index"]=0
            chunks=[{"id":"fixture","object":"chat.completion.chunk","created":1,"model":"companion-fixture","choices":[{"index":0,"delta":delta,"finish_reason":None}]},
                    {"id":"fixture","object":"chat.completion.chunk","created":1,"model":"companion-fixture","choices":[{"index":0,"delta":{},"finish_reason":"tool_calls" if call else "stop"}],"usage":{"prompt_tokens":10,"completion_tokens":4,"total_tokens":14}}]
            return self.answer(("".join("data: "+json.dumps(c)+"\n\n" for c in chunks)+"data: [DONE]\n\n").encode(),"text/event-stream")
        return self.answer({"id":"fixture","object":"chat.completion","created":1,"model":"companion-fixture","choices":[{"index":0,"message":message,"finish_reason":"tool_calls" if call else "stop"}],"usage":{"prompt_tokens":10,"completion_tokens":4,"total_tokens":14}})

def main():
    endpoint=ThreadingHTTPServer(("127.0.0.1",0),Endpoint)
    threading.Thread(target=endpoint.serve_forever,daemon=True).start()
    with tempfile.TemporaryDirectory() as directory:
        home=Path(directory)/"hermes";home.mkdir(mode=0o700)
        url="http://127.0.0.1:"+str(endpoint.server_port)
        pairing=Path(directory)/"connection.json";pairing.write_text(json.dumps({"token":TOKEN}));pairing.chmod(0o600)
        wrapper=Path(directory)/"android_fixture.py"
        wrapper.write_text("import sys\nsys.path.insert(0,"+repr(str(ROOT/"integrations/hermes_bridge"))+")\nimport bridge_mcp\nbridge_mcp.BASE_URL="+repr(url+"/bridge")+"\nbridge_mcp.MCPAdapter(bridge_mcp.BridgeClient("+repr(str(pairing))+ ")).serve()\n")
        config={"model":{"default":"companion-fixture","provider":"custom","base_url":url+"/v1","api_key":"fixture-only","max_tokens":512,"context_length":65536},
                "mcp_servers":{"android-bridge":{"command":sys.executable,"args":[str(wrapper)],"enabled":True}},
                "agent":{"max_turns":4},"compression":{"enabled":False}}
        import yaml
        (home/"config.yaml").write_text(yaml.safe_dump(config))
        (home/".env").write_text("OPENAI_API_KEY=fixture-only\nOPENAI_BASE_URL="+url+"/v1\n")
        os.environ["HERMES_HOME"]=str(home)
        journal=Journal(Path(directory)/"native/jobs.sqlite")
        manager=Manager(journal,lambda:TOKEN,lambda:True,[sys.executable,"-m","acp_adapter"],Path(directory)/"workspace")
        request={"request_id":"a"*32,"conversation_id":"b"*32,"text":"Use android_bridge_status once and report bridge readiness. Do not use other tools."}
        try:
            manager.submit(request)
            until=time.monotonic()+180
            while manager.worker_running and time.monotonic()<until: time.sleep(.2)
            job=manager.get(request["request_id"])
            if job["state"]!="completed" or "Fixture bridge ready" not in job["reply"] or not Endpoint.tool_offered or Endpoint.bridge_calls<1:
                print("Actual Hermes integration failed:",job["state"],job.get("error"),"tool offered",Endpoint.tool_offered,"bridge calls",Endpoint.bridge_calls)
                if manager.peer: print(manager.peer.stderr[-9000:]) # Test-only keys and localhost fixtures only.
                raise AssertionError("actual Hermes/ACP/MCP tool roundtrip")
            print("PASS: actual Hermes v2026.5.29.2 ACP prompt -> configured MCP android_bridge_status -> fake Android -> reply. No real inference or device action.")
        finally:
            manager.close()
            endpoint.shutdown();endpoint.server_close()
if __name__=="__main__":main()
