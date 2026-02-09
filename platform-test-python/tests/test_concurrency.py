import asyncio

import httpx
import pytest


@pytest.mark.integration
async def test_ping_concurrency(async_client: httpx.AsyncClient):
    async def one() -> None:
        r = await async_client.get("/demo/ping")
        assert r.status_code == 200
        assert r.json() == {"ok": True}

    await asyncio.gather(*(one() for _ in range(50)))

