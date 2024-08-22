// common-options.js
export const options = {
    scenarios: {
        load_test: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '10s', target: 1000 },
                { duration: '30s', target: 1000 },
                { duration: '10s', target: 3000 },
                { duration: '30s', target: 3000 },
                { duration: '10s', target: 0 },
            ],
        },
        peak_test: {
            executor: 'ramping-arrival-rate',
            startRate: 10,
            timeUnit: '1s',
            preAllocatedVUs: 200,
            maxVUs: 500,
            stages: [
                { duration: '10s', target: 1000 },
                { duration: '20s', target: 5000 },
                { duration: '30s', target: 1000 },
                { duration: '10s', target: 5000 },
            ],
        },
    },
};

export const BASE_URL = 'http://localhost:8080';