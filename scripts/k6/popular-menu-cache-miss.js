import http from 'k6/http';
import { check } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const baseUrl = __ENV.BASE_URL || 'http://host.docker.internal:8080';
const duration = new Trend('popular_menu_cold_request_duration', true);
const failures = new Rate('popular_menu_cold_request_failure_rate');

export const options = { vus: 1, iterations: 1 };

export default function () {
  const response = http.get(`${baseUrl}/api/menus/popular`);
  const success = check(response, { 'cold popular menu returns 200': (r) => r.status === 200 });
  duration.add(response.timings.duration);
  failures.add(!success);
}
