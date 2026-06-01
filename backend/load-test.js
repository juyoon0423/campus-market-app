import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        { duration: '10s', target: 20 },
        { duration: '20s', target: 20 },
        { duration: '10s', target: 0 },
    ],
    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<500'],
    },
};

export default function () {
    const BASE_URL = 'http://localhost:8080';

    // 한글 파라미터가 깨지지 않도록 encodeURI 적용
    const url = encodeURI(`${BASE_URL}/api/products/search?category=전자기기&status=SELLING`);
    const res = http.get(url);

    check(res, {
        'is status 200': (r) => r.status === 200,
    });

    sleep(1);
}