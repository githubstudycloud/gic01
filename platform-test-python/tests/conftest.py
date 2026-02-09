import os

import httpx
import pytest


def _base_url() -> str:
    url = os.getenv("PLATFORM_BASE_URL", "http://localhost:8080").strip()
    return url[:-1] if url.endswith("/") else url


@pytest.fixture(scope="session")
def base_url() -> str:
    return _base_url()


@pytest.fixture()
def client(base_url: str):
    with httpx.Client(base_url=base_url, timeout=2.0) as c:
        yield c


@pytest.fixture()
async def async_client(base_url: str):
    async with httpx.AsyncClient(base_url=base_url, timeout=2.0) as c:
        yield c

