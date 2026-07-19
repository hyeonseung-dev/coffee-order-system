import http from 'k6/http';
import { check } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const baseUrl = __ENV.BASE_URL || 'http://host.docker.internal:8080';
const duration = new Trend('popular_menu_measure_duration', true);
const failures = new Rate('popular_menu_measure_failure_rate');
const requests = new Counter('popular_menu_measure_requests');

export const options = {
  scenarios: {
    warmup: { executor: 'constant-vus', vus: 30, duration: '10s', exec: 'warmup' },
    measure: { executor: 'constant-vus', vus: 30, duration: '30s', startTime: '10s', exec: 'measure' },
  },
};

function request(phase) {
  const response = http.get(`${baseUrl}/api/menus/popular`, { tags: { phase } });
  const success = check(response, { 'popular menu returns 200': (r) => r.status === 200 });
  if (phase === 'measure') {
    duration.add(response.timings.duration);
    failures.add(!success);
    requests.add(1);
  }
}

export function warmup() { request('warmup'); }
export function measure() { request('measure'); }
