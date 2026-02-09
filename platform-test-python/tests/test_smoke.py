import httpx


def test_ping_smoke(client: httpx.Client):
    r = client.get("/demo/ping")
    assert r.status_code == 200
    assert r.headers.get("X-Request-Id")
    assert r.json() == {"ok": True}


def test_lock_smoke(client: httpx.Client):
    r = client.get("/demo/lock", params={"name": "py", "ttlSeconds": 2})
    assert r.status_code == 200
    assert r.headers.get("X-Request-Id")
    body = r.json()
    assert body["name"] == "py"
    assert isinstance(body["acquired"], bool)

