"""A real ACP SDK fixture: no Hermes credentials, network inference or Android actions."""
import asyncio
import acp
from acp.schema import AgentCapabilities, InitializeResponse, NewSessionResponse, PromptResponse, PermissionOption

class Fixture(acp.Agent):
    def on_connect(self, conn): self.conn = conn
    async def initialize(self, protocol_version=None, **kwargs):
        return InitializeResponse(protocol_version=acp.PROTOCOL_VERSION, agent_capabilities=AgentCapabilities(load_session=True))
    async def new_session(self, cwd, **kwargs): return NewSessionResponse(session_id="sdk-session")
    async def load_session(self, cwd, session_id, **kwargs): return None
    async def cancel(self, session_id, **kwargs): return None
    async def prompt(self, session_id, prompt, **kwargs):
        response = await self.conn.request_permission(session_id=session_id,
            tool_call=acp.update_tool_call("permission-one",title="Fixture change",status="pending"),
            options=[PermissionOption(option_id="permit",kind="allow_once",name="Allow once")])
        if response.outcome.outcome == "selected":
            await self.conn.session_update(session_id,acp.update_agent_message_text("SDK permission and text work"))
        return PromptResponse(stop_reason="end_turn")

if __name__ == "__main__": asyncio.run(acp.run_agent(Fixture(),use_unstable_protocol=True))
