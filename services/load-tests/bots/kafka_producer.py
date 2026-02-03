"""
Kafka Load Test Bot
- Publishes messages at a specified rate
- Exposes Prometheus metrics on :8000/metrics
"""
import argparse
import json
import time
import random
import uuid
from datetime import datetime

from kafka import KafkaProducer
from prometheus_client import start_http_server, Counter, Histogram, Gauge

messages_sent = Counter('kafka_bot_messages_sent_total', 'Total messages sent', ['topic'])
send_duration = Histogram('kafka_bot_send_duration_seconds', 'Send duration', ['topic'])
send_errors = Counter('kafka_bot_send_errors_total', 'Send errors', ['topic'])
target_rate_gauge = Gauge('kafka_bot_target_rate', 'Target messages per second')
actual_rate_gauge = Gauge('kafka_bot_actual_rate', 'Actual messages per second')


def create_order_event():
    return {
        'eventType': 'ORDER_CREATED',
        'orderId': str(uuid.uuid4()),
        'userId': random.randint(1, 10000),
        'totalAmount': round(random.uniform(1000, 500000), 0),
        'items': [
            {'productId': random.randint(1, 1000), 'quantity': random.randint(1, 5)}
        ],
        'createdAt': datetime.now().isoformat(),
    }


def run(bootstrap_servers, topic, rate, duration):
    producer = KafkaProducer(
        bootstrap_servers=bootstrap_servers,
        value_serializer=lambda v: json.dumps(v).encode('utf-8'),
        acks='all',
        retries=3,
    )

    target_rate_gauge.set(rate)
    interval = 1.0 / rate
    sent_count = 0
    start_time = time.time()

    print(f'Starting: {rate} msg/s for {duration}s to {topic}')

    while time.time() - start_time < duration:
        loop_start = time.time()
        try:
            event = create_order_event()
            with send_duration.labels(topic=topic).time():
                producer.send(topic, value=event)
            messages_sent.labels(topic=topic).inc()
            sent_count += 1
        except Exception as e:
            send_errors.labels(topic=topic).inc()
            print(f'Error: {e}')

        elapsed = time.time() - start_time
        if elapsed > 0:
            actual_rate_gauge.set(sent_count / elapsed)

        sleep_time = interval - (time.time() - loop_start)
        if sleep_time > 0:
            time.sleep(sleep_time)

    producer.flush()
    producer.close()
    total_time = time.time() - start_time
    print(f'Done: {sent_count} messages in {total_time:.1f}s ({sent_count / total_time:.1f} msg/s)')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Kafka Load Test Bot')
    parser.add_argument('--bootstrap-servers', default='localhost:9092')
    parser.add_argument('--topic', default='order-created')
    parser.add_argument('--rate', type=int, default=100, help='Messages per second')
    parser.add_argument('--duration', type=int, default=60, help='Duration in seconds')
    parser.add_argument('--metrics-port', type=int, default=8000)
    args = parser.parse_args()

    start_http_server(args.metrics_port)
    print(f'Prometheus metrics on :{args.metrics_port}/metrics')
    run(args.bootstrap_servers, args.topic, args.rate, args.duration)
