import importlib.util
import json
from pathlib import Path
import sys
import tempfile
import threading
import time
import unittest
import urllib.error
import urllib.request
import uuid

sys.path.insert(0, str(Path(__file__).parent))
import companion_server as server
TOKEN = "A" * 43


class Process:
    def poll(self): return None


class FakePeer:
    def __init__(self, command, update, permission):
        self.on_update, self.on_permission = update, permission
        self.closed = False; self.known = set(); self.stderr = ""; self.process = Process()
        self.calls = []; self.prompt = None; self.session = "session-one"
    def initialize(self): pass
    def call(self, method, params, timeout=90):
        self.calls.append(method)
        return {"sessionId": self.session} if method == "session/new" else {}
    def request(self, method, params):
        self.calls.append(method); self.prompt = server.Pending(); return self.prompt
    def finish(self, text="ঠিক আছে"):
        self.on_update({"sessionId": self.session, "update": {"sessionUpdate": "agent_message_chunk", "content": {"type": "text", "text": text}}})
        self.prompt.value = {"stopReason": "end_turn"}; self.prompt.event.set()
    def permission(self, key, option=None): self.calls.append(("permission", key, option))
    def notify(self, method, params):
        self.calls.append(method)
        if self.prompt:
            self.prompt.value = {"stopReason": "cancelled"}; self.prompt.event.set()
    def close(self): self.closed = True


def wait_for(test):
    until = time.monotonic() + 4
    while time.monotonic() < until:
        if test(): return
        time.sleep(.01)
    raise AssertionError("condition did not finish")


class CompanionTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(); self.addCleanup(self.temp.cleanup)
        self.journal = server.Journal(Path(self.temp.name)/"private/jobs.sqlite")
        self.manager = server.Manager(self.journal, lambda:TOKEN, lambda:True, [], Path(self.temp.name)/"workspace", FakePeer)
        self.http = server.Server(("127.0.0.1",0), self.manager, lambda:TOKEN)
        threading.Thread(target=self.http.serve_forever, daemon=True).start()
        self.addCleanup(self.cleanup)
    def cleanup(self):
        self.http.shutdown(); self.http.server_close(); self.manager.close()
        wait_for(lambda:not self.manager.worker_running)
        self.journal.close()
    def payload(self): return {"request_id":uuid.uuid4().hex, "conversation_id":uuid.uuid4().hex, "text":"বাংলা পরীক্ষা"}
    def request(self, path, body=None, token=TOKEN, extra=None):
        headers={"Authorization":"Bearer "+token}
        if body is not None: headers["Content-Type"]="application/json"
        headers.update(extra or {})
        req=urllib.request.Request("http://127.0.0.1:"+str(self.http.server_port)+path,
            data=json.dumps(body).encode() if body is not None else None, headers=headers)
        try:
            with urllib.request.urlopen(req, timeout=4) as response: return response.status,json.load(response)
        except urllib.error.HTTPError as error:
            with error: return error.code,json.load(error)
    def submit(self):
        payload=self.payload(); status,result=self.request("/v1/jobs",payload)
        self.assertEqual(status,200); wait_for(lambda:self.manager.peer and self.manager.peer.prompt)
        return payload,result
    def test_health_auth_origin_and_host(self):
        self.assertEqual(self.request("/v1/health",token="wrong")[0],401)
        self.assertEqual(self.request("/v1/health",extra={"Origin":"https://example.com"})[0],403)
        self.assertEqual(self.request("/v1/health",extra={"Host":"example.com"})[0],400)
        self.assertEqual(self.request("/v1/health")[1]["backend"],"ready")
    def test_exactly_one_prompt_despite_repeated_post(self):
        payload,_=self.submit()
        self.assertEqual(self.request("/v1/jobs",payload)[0],200)
        self.assertEqual(self.manager.peer.calls.count("session/prompt"),1)
        self.manager.peer.finish()
        wait_for(lambda:not self.manager.worker_running)
        self.assertEqual(self.request("/v1/jobs",payload)[1]["state"],"completed")
        self.assertEqual(self.manager.peer.calls.count("session/prompt"),1)
    def test_conflicting_nonce_and_concurrent_task_rejected(self):
        payload,_=self.submit(); changed=dict(payload,text="different")
        self.assertEqual(self.request("/v1/jobs",changed)[1]["error"],"request_id_conflict")
        self.assertEqual(self.request("/v1/jobs",self.payload())[1]["error"],"agent_busy")
    def test_visible_final_text_and_no_thought_leak(self):
        payload,_=self.submit()
        self.manager._update({"sessionId":"session-one","update":{"sessionUpdate":"agent_thought_chunk","content":{"type":"text","text":"PRIVATE THOUGHT"}}})
        self.manager.peer.finish("পরীক্ষা সফল")
        wait_for(lambda:not self.manager.worker_running)
        result=self.request("/v1/jobs/"+payload["request_id"])[1]
        self.assertEqual(result["reply"],"পরীক্ষা সফল"); self.assertNotIn("PRIVATE THOUGHT",json.dumps(result))
    def test_approval_is_once_and_never_always(self):
        payload,_=self.submit()
        self.manager._permission(81,{"sessionId":"session-one","toolCall":{"title":"Review change"},"options":[{"optionId":"yes","kind":"allow_once"},{"optionId":"always","kind":"allow_always"},{"optionId":"no","kind":"reject_once"}]})
        result=self.request("/v1/jobs/"+payload["request_id"])[1]
        nonce=result["approval"]["id"]
        self.assertEqual(self.request("/v1/jobs/"+payload["request_id"]+"/approval",{"approval_id":nonce,"allow":True})[0],200)
        self.assertIn(("permission",81,"yes"),self.manager.peer.calls)
        self.assertEqual(self.request("/v1/jobs/"+payload["request_id"]+"/approval",{"approval_id":nonce,"allow":True})[0],409)
    def test_cancel_is_scoped_to_this_session(self):
        payload,_=self.submit()
        self.request("/v1/jobs/"+payload["request_id"]+"/cancel",{})
        wait_for(lambda:not self.manager.worker_running)
        self.assertEqual(self.manager.get(payload["request_id"])["state"],"cancelled")
        self.assertIn("session/cancel",self.manager.peer.calls)
    def test_provider_limit_is_safe_no_new_request(self):
        payload,_=self.submit(); self.manager.peer.finish("Error: Error code: 402 insufficient credits secret-token")
        wait_for(lambda:not self.manager.worker_running)
        data=self.manager.get(payload["request_id"])
        self.assertEqual(data["error"],"provider_credit_limit"); self.assertEqual(data["reply"],"")
        self.assertEqual(self.manager.peer.calls.count("session/prompt"),1)
    def test_bridge_off_blocks_job_before_inference(self):
        self.manager.bridge_check=lambda:False
        self.assertEqual(self.request("/v1/jobs",self.payload())[1]["error"],"android_bridge_not_paired_or_running")
        self.assertIsNone(self.manager.peer)
    def test_extra_fields_and_oversize_not_accepted(self):
        payload=self.payload();payload["url"]="https://example.com"
        self.assertEqual(self.request("/v1/jobs",payload)[0],400)
        payload=self.payload();payload["text"]="x"*8001
        self.assertEqual(self.request("/v1/jobs",payload)[0],400)
    def test_journal_restart_marks_interrupted_no_replay(self):
        path=Path(self.temp.name)/"other/jobs.sqlite"; journal=server.Journal(path)
        job={"id":"a"*32,"state":"running","reply":"","approval":None}
        journal.save(job,"hash");journal.close();journal=server.Journal(path)
        self.assertEqual(journal.get("a"*32)[1]["state"],"interrupted")
        self.assertEqual(path.stat().st_mode & 0o777,0o600);journal.close()
    def test_failure_classifier_does_not_treat_explanations_as_errors(self):
        self.assertIsNone(server.provider_error("HTTP402 credits mean insufficient budget"))
        self.assertEqual(server.provider_error("Error: 429 rate limit"),"provider_rate_limit")
        self.assertNotIn("sk-abcdefghijklmno",server.safe_text("key sk-abcdefghijklmno"))

    def test_late_permission_cannot_undo_stop(self):
        payload,_=self.submit()
        self.manager.peer.notify=lambda method,params:self.manager.peer.calls.append(method)
        self.manager.cancel(payload["request_id"])
        self.manager._permission(99,{"sessionId":"session-one","toolCall":{"title":"Late approval"},"options":[{"optionId":"yes","kind":"allow_once"}]})
        self.assertEqual(self.manager.active["state"],"cancelling")
        self.assertIsNone(self.manager.active["approval"])
        self.assertIn(("permission",99,None),self.manager.peer.calls)
        self.manager.peer.finish()
        wait_for(lambda:not self.manager.worker_running)
        self.assertEqual(self.manager.get(payload["request_id"])["state"],"cancelled")

    def test_restart_keeps_nonce_tombstone_and_never_executes_again(self):
        import hashlib
        payload=self.payload(); path=Path(self.temp.name)/"restart/jobs.sqlite"
        journal=server.Journal(path)
        fingerprint=hashlib.sha256(json.dumps(payload,sort_keys=True).encode()).hexdigest()
        journal.save({"id":payload["request_id"],"state":"running","reply":"","approval":None},fingerprint)
        journal.close(); journal=server.Journal(path)
        restarted=server.Manager(journal,lambda:TOKEN,lambda:True,[],Path(self.temp.name)/"restart-work",FakePeer)
        try:
            self.assertEqual(restarted.submit(payload)["state"],"interrupted")
            self.assertIsNone(restarted.peer)
        finally: restarted.close();journal.close()


class ACPWireTests(unittest.TestCase):
    def test_real_subprocess_initialize_update_permission_cancel(self):
        events=[];permissions=[]
        peer=server.ACPPeer([sys.executable,__file__,"--fake-acp"],events.append,lambda key,value:permissions.append((key,value)))
        try:
            peer.initialize()
            self.assertEqual(peer.call("session/new",{"cwd":"/tmp","mcpServers":[]})["sessionId"],"wire-session")
            result=peer.request("session/prompt",{"sessionId":"wire-session","prompt":[{"type":"text","text":"hi"}]})
            wait_for(lambda:len(permissions)>0)
            peer.permission(permissions[0][0],"allow-once")
            self.assertEqual(result.result(3)["stopReason"],"end_turn")
            self.assertEqual(events[-1]["update"]["content"]["text"],"wire reply")
        finally: peer.close()


class ActualSDKTests(unittest.TestCase):
    @unittest.skipUnless(importlib.util.find_spec("acp"), "ACP SDK not present on local host; installed explicitly in CI")
    def test_pinned_real_sdk_protocol_permission_roundtrip(self):
        events=[]; permissions=[]
        peer=server.ACPPeer([sys.executable,str(Path(__file__).with_name("acp_sdk_fixture.py"))],events.append,lambda key,value:permissions.append((key,value)))
        try:
            peer.initialize()
            self.assertEqual(peer.call("session/new",{"cwd":"/tmp","mcpServers":[]})["sessionId"],"sdk-session")
            result=peer.request("session/prompt",{"sessionId":"sdk-session","prompt":[{"type":"text","text":"fixture"}]})
            wait_for(lambda:len(permissions)>0)
            peer.permission(permissions[0][0],"permit")
            self.assertEqual(result.result(3)["stopReason"],"end_turn")
            self.assertEqual(events[-1]["update"]["content"]["text"],"SDK permission and text work")
        finally: peer.close()


def fake_acp():
    pending=None
    def emit(value): print(json.dumps(value),flush=True)
    for line in sys.stdin:
        value=json.loads(line); method=value.get("method"); key=value.get("id")
        if method=="initialize": emit({"jsonrpc":"2.0","id":key,"result":{"protocolVersion":1}})
        elif method=="session/new": emit({"jsonrpc":"2.0","id":key,"result":{"sessionId":"wire-session"}})
        elif method=="session/prompt":
            pending=key;emit({"jsonrpc":"2.0","id":90,"method":"session/request_permission","params":{"sessionId":"wire-session","options":[{"optionId":"allow-once","kind":"allow_once"}]}})
        elif key==90:
            emit({"jsonrpc":"2.0","method":"session/update","params":{"sessionId":"wire-session","update":{"sessionUpdate":"agent_message_chunk","content":{"type":"text","text":"wire reply"}}}})
            emit({"jsonrpc":"2.0","id":pending,"result":{"stopReason":"end_turn"}})

if __name__=="__main__":
    if "--fake-acp" in sys.argv: fake_acp()
    else: unittest.main()
