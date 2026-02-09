import httpx
import yaml


def test_openapi_yaml_is_present(client: httpx.Client):
    r = client.get("/openapi.yaml")
    assert r.status_code == 200
    assert "openapi" in r.text

    doc = yaml.safe_load(r.text)
    assert doc["openapi"].startswith("3.")
    assert "/demo/ping" in doc["paths"]
    assert "/demo/lock" in doc["paths"]

