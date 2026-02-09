# platform-test-python

Python test harness for the `platform-*` foundation.

Focus:
- black-box HTTP tests (smoke / integration)
- lightweight contract checks for `/openapi.yaml`
- simple concurrency checks

## Prereqs

- Python 3.11+ (tested with 3.13)
- Backend running locally:

```bash
mvn -q -pl platform-sample-app spring-boot:run
```

## Setup

```bash
cd platform-test-python
python -m venv .venv
.venv\\Scripts\\activate
pip install -r requirements.txt
```

## Run

Default base URL:
- `http://localhost:8080`

Override:

```bash
set PLATFORM_BASE_URL=http://localhost:8080
```

Run all tests:

```bash
pytest
```

