#!/usr/bin/env python3
"""
k6 Load Test Results Checker

Validates k6 test results against defined thresholds.
Exit code 0 = passed, 1 = failed
"""

import json
import sys
from typing import Dict, Any, List, Tuple


def load_results(filepath: str) -> Dict[str, Any]:
    """Load k6 results from JSON file."""
    with open(filepath, 'r') as f:
        return json.load(f)


def check_thresholds(
    results: Dict[str, Any]
) -> Tuple[bool, List[str], List[str]]:
    """
    Check results against thresholds.
    Returns (passed, failures, warnings)
    """
    metrics = results.get('metrics', {})
    failures = []
    warnings = []

    # Define thresholds
    thresholds = {
        'http_req_duration_p95': {
            'value': 500,  # ms
            'severity': 'error',
            'message': 'p95 latency exceeds 500ms'
        },
        'http_req_duration_p99': {
            'value': 1000,  # ms
            'severity': 'warning',
            'message': 'p99 latency exceeds 1000ms'
        },
        'http_req_failed_rate': {
            'value': 0.01,  # 1%
            'severity': 'error',
            'message': 'Error rate exceeds 1%'
        },
        'errors_rate': {
            'value': 0.01,  # 1%
            'severity': 'error',
            'message': 'Custom error rate exceeds 1%'
        },
    }

    # Check p95 latency
    http_duration = metrics.get('http_req_duration', {}).get('values', {})
    p95 = http_duration.get('p(95)', 0)
    if p95 > thresholds['http_req_duration_p95']['value']:
        failures.append(
            f"p95 latency: {p95:.2f}ms > "
            f"{thresholds['http_req_duration_p95']['value']}ms"
        )

    # Check p99 latency
    p99 = http_duration.get('p(99)', 0)
    if p99 > thresholds['http_req_duration_p99']['value']:
        warnings.append(
            f"p99 latency: {p99:.2f}ms > "
            f"{thresholds['http_req_duration_p99']['value']}ms"
        )

    # Check error rate
    http_failed = metrics.get('http_req_failed', {}).get('values', {})
    error_rate = http_failed.get('rate', 0)
    if error_rate > thresholds['http_req_failed_rate']['value']:
        failures.append(
            f"Error rate: {error_rate:.2%} > "
            f"{thresholds['http_req_failed_rate']['value']:.2%}"
        )

    # Check custom error rate if present
    custom_errors = metrics.get('errors', {}).get('values', {})
    custom_error_rate = custom_errors.get('rate', 0)
    if custom_error_rate > thresholds['errors_rate']['value']:
        failures.append(
            f"Custom error rate: {custom_error_rate:.2%} > "
            f"{thresholds['errors_rate']['value']:.2%}"
        )

    passed = len(failures) == 0
    return passed, failures, warnings


def print_summary(results: Dict[str, Any]) -> None:
    """Print a summary of the test results."""
    metrics = results.get('metrics', {})

    print("\n" + "=" * 50)
    print("Load Test Results Summary")
    print("=" * 50 + "\n")

    # HTTP Requests
    http_reqs = metrics.get('http_reqs', {}).get('values', {})
    print(f"Total Requests: {http_reqs.get('count', 0):,}")
    print(f"Request Rate: {http_reqs.get('rate', 0):.2f} req/s")

    # Latency
    http_duration = metrics.get('http_req_duration', {}).get('values', {})
    print(f"\nLatency:")
    print(f"  Average: {http_duration.get('avg', 0):.2f}ms")
    print(f"  p50: {http_duration.get('med', 0):.2f}ms")
    print(f"  p95: {http_duration.get('p(95)', 0):.2f}ms")
    print(f"  p99: {http_duration.get('p(99)', 0):.2f}ms")
    print(f"  Max: {http_duration.get('max', 0):.2f}ms")

    # Error Rate
    http_failed = metrics.get('http_req_failed', {}).get('values', {})
    print(f"\nError Rate: {http_failed.get('rate', 0):.2%}")

    # VUs
    vus = metrics.get('vus', {}).get('values', {})
    print(f"\nVirtual Users:")
    print(f"  Max: {vus.get('max', 0)}")

    # Custom metrics
    if 'product_search_duration' in metrics:
        search_duration = metrics['product_search_duration']['values']
        print(f"\nProduct Search Duration:")
        print(f"  p95: {search_duration.get('p(95)', 0):.2f}ms")

    print("\n" + "=" * 50)


def main():
    if len(sys.argv) < 2:
        print("Usage: python check-results.py <results.json>")
        sys.exit(1)

    filepath = sys.argv[1]

    try:
        results = load_results(filepath)
    except FileNotFoundError:
        print(f"Error: File not found: {filepath}")
        sys.exit(1)
    except json.JSONDecodeError as e:
        print(f"Error: Invalid JSON in {filepath}: {e}")
        sys.exit(1)

    print_summary(results)

    passed, failures, warnings = check_thresholds(results)

    # Print warnings
    if warnings:
        print("\n⚠️  Warnings:")
        for warning in warnings:
            print(f"  - {warning}")

    # Print result
    if passed:
        print("\n✅ Load test PASSED")
        if warnings:
            print(f"   ({len(warnings)} warning(s))")
        sys.exit(0)
    else:
        print("\n❌ Load test FAILED:")
        for failure in failures:
            print(f"  - {failure}")
        sys.exit(1)


if __name__ == '__main__':
    main()
