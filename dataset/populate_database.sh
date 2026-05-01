#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"
cypher_file="$(pwd)/populate_database.cypher"

if [[ ! -f "$cypher_file" ]]; then
  echo "error: missing ${cypher_file}" >&2
  exit 1
fi

if ! docker exec neo4j true 2>/dev/null; then
  echo "error: Docker container 'neo4j' is not running (start it with docker compose first)" >&2
  exit 1
fi

NEO4J_USER="${NEO4J_USER:-neo4j}"
NEO4J_PASSWORD="${NEO4J_PASSWORD:-password123}"

exec docker exec -i neo4j cypher-shell \
  -a bolt://localhost:7687 \
  --non-interactive \
  -u "${NEO4J_USER}" \
  -p "${NEO4J_PASSWORD}" \
  < "${cypher_file}"
