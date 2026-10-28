#!/bin/bash
set -e

echo "Starting Postgres..."
docker run -d --name erpai-postgres -p 5432:5432 \
  -e POSTGRES_DB=erpai_platform -e POSTGRES_USER=erpai -e POSTGRES_PASSWORD=erpai_dev_password \
  postgres:16 2>/dev/null || echo "Postgres already running"

echo "Starting Kafka..."
docker run -d --name erpai-kafka -p 9092:9092 \
  -e KAFKA_NODE_ID=1 -e KAFKA_PROCESS_ROLES=broker,controller \
  -e KAFKA_CONTROLLER_QUORUM_VOTERS=1@localhost:9093 \
  -e KAFKA_LISTENERS=PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  -e KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
  -e CLUSTER_ID=MkU3OEVBNTcwNTJENDM2Qk \
  confluentinc/cp-kafka:7.7.0 2>/dev/null || echo "Kafka already running"

echo "Waiting for Kafka..."
sleep 15

echo "Starting POS service..."
POSTGRES_PORT=5432 KAFKA_BOOTSTRAP_SERVERS=localhost:9092 \
  POS_SALES_SERVICE_TOKEN=dev-token \
  ./gradlew :business:pos:application:bootRun &

echo "Starting Sales service..."
POSTGRES_PORT=5432 KAFKA_BOOTSTRAP_SERVERS=localhost:9092 \
  POS_SALES_SERVICE_TOKEN=dev-token \
  ./gradlew :business:sales:application:bootRun &

wait
