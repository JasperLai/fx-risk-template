#!/usr/bin/env bash
set -e
redis-server --daemonize yes || true
mvn -q -DskipTests package
java -jar risk-gateway/target/risk-gateway-0.1.0.jar
