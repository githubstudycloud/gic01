import httpx


def test_todo_crud_smoke(client: httpx.Client):
    create = client.post("/crud/todos", json={"title": "hello"})
    assert create.status_code == 201
    assert create.headers.get("X-Request-Id")
    item = create.json()
    assert item["title"] == "hello"
    assert item["done"] is False
    todo_id = item["id"]

    get_one = client.get(f"/crud/todos/{todo_id}")
    assert get_one.status_code == 200
    assert get_one.headers.get("X-Request-Id")
    assert get_one.json()["id"] == todo_id

    update = client.put(f"/crud/todos/{todo_id}", json={"done": True})
    assert update.status_code == 200
    assert update.headers.get("X-Request-Id")
    assert update.json()["done"] is True

    listing = client.get("/crud/todos")
    assert listing.status_code == 200
    assert listing.headers.get("X-Request-Id")
    ids = {t["id"] for t in listing.json()}
    assert todo_id in ids

    delete = client.delete(f"/crud/todos/{todo_id}")
    assert delete.status_code == 200
    assert delete.headers.get("X-Request-Id")
    assert delete.json()["deleted"] is True

