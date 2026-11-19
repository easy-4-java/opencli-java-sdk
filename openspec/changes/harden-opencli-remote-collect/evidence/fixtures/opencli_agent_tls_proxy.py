"""Run a TLS proxy and locked OpenCLI Agent on two IPv4 loopback sockets."""
import asyncio
import hmac
import socket
from pathlib import Path

import httpx
import uvicorn
from fastapi import FastAPI, Request, Response

from agent_server import app as agent


def listener():
    sock = socket.socket()
    sock.bind(("127.0.0.1", 0))
    return sock


async def main():
    backend, frontend = listener(), listener()
    agent_url = f"http://127.0.0.1:{backend.getsockname()[1]}/collect"
    Path("tls-port").write_text(str(frontend.getsockname()[1]))
    credential = "Bearer " + Path("token").read_text().strip()
    proxy = FastAPI()

    @proxy.post("/collect")
    async def collect(request: Request):
        supplied = request.headers.get("authorization", "")
        data = await request.body()
        if not hmac.compare_digest(supplied, credential):
            print("proxy rejected missing or invalid credential", flush=True)
            return Response(status_code=401, headers={"WWW-Authenticate": "Bearer"})
        assert credential.encode() not in data
        async with httpx.AsyncClient(timeout=10, trust_env=False) as client:
            response = await client.post(agent_url, content=data, headers={"Content-Type": "application/json"})
        print(f"proxy forwarded one accepted request, Agent HTTP {response.status_code}", flush=True)
        return Response(response.content, status_code=response.status_code, media_type="application/json")

    server = uvicorn.Server(uvicorn.Config(agent, access_log=False))
    tls = uvicorn.Server(uvicorn.Config(proxy, access_log=False, ssl_keyfile="key.pem", ssl_certfile="cert.pem"))
    local = asyncio.create_task(server.serve(sockets=[backend]))
    edge = asyncio.create_task(tls.serve(sockets=[frontend]))
    try:
        for _ in range(600):
            if Path("stop").exists() or local.done() or edge.done():
                break
            await asyncio.sleep(0.5)
    finally:
        server.should_exit = tls.should_exit = True
        await asyncio.gather(local, edge)


asyncio.run(main())
