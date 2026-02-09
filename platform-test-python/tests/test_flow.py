import time

import httpx


def _wait_run_done(client: httpx.Client, flow_id: str, run_id: str, timeout_s: float = 5.0) -> str:
    deadline = time.time() + timeout_s
    while time.time() < deadline:
        r = client.get(f"/flows/{flow_id}/runs/{run_id}")
        assert r.status_code == 200
        status = r.json()["status"]
        if status in ("SUCCEEDED", "FAILED"):
            return status
        time.sleep(0.1)
    raise AssertionError(f"Timed out waiting for run: {run_id}")


def test_flows_list_and_run_artifacts(client: httpx.Client):
    r = client.get("/flows")
    assert r.status_code == 200
    assert r.headers.get("X-Request-Id")
    flows = r.json()
    ids = {f["id"] for f in flows}

    assert "demo.metrics" in ids
    assert "demo.workflow.release" in ids

    start = client.post("/flows/demo.metrics/runs", json={})
    assert start.status_code == 202
    assert start.headers.get("X-Request-Id")
    run_id = start.json()["runId"]

    status = _wait_run_done(client, "demo.metrics", run_id)
    assert status == "SUCCEEDED"

    art = client.get(f"/flows/demo.metrics/runs/{run_id}/artifacts")
    assert art.status_code == 200
    assert art.headers.get("X-Request-Id")
    snapshot = art.json()
    assert "metric.summary" in snapshot

