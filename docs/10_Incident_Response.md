# API 부하 테스트 분석과 가상 장애 대응 방안에 관한 보고서

# 1. API 부하 테스트 분석에 대한 보고서

## 1. 개요

- 본 보고서는 콘서트 예약 시스템 API에 대한 부하 테스트 결과를 상세히 기술한다.
- 테스트의 주요 목적은 콘서트 예약 시스템 API 전반의 성능, 안정성, 그리고 확장성을 평가하는 것이다.

## 2. 테스트 목적

- 시스템이 예상되는 부하를 정상적으로 처리할 수 있는지 평가
- 일시적인 높은 부하 상황에서의 시스템 성능 파악
- 장애 상황 시뮬레이션을 통한 문제점 분석 및 개선 방안 도출
- 적정한 애플리케이션 배포 스펙 결정을 위한 데이터 수집

## 3. 테스트 도구 및 방법론

### 3.1 테스트 도구: k6
k6는 Go로 작성된 오픈 소스 부하 테스트 도구이다. 주요 특징은 다음과 같다:

- JavaScript를 사용하여 테스트 스크립트 작성
- 다양한 프로토콜 지원 (HTTP, WebSocket, gRPC 등)
- 확장 가능한 메트릭 시스템
- 클라우드 서비스와의 통합 지원
- 실시간 모니터링 및 결과 분석 기능

이러한 특징들로 인해 k6는 복잡한 시나리오를 시뮬레이션하고 다양한 각도에서 시스템 성능을 평가하는 데 적합하다고 판단한다.

### 3.2 테스트 시나리오
두 가지 주요 시나리오로 테스트를 진행했다:

- Load Test (부하 테스트)
  - 목적: 시스템이 예상되는 일반적인 부하를 정상적으로 처리할 수 있는지 평가
  - 방법: 특정 부하를 제한된 시간 동안 제공하여 시스템의 안정성 확인
  - 중요성: 일상적인 운영 상황에서의 시스템 성능을 확인하고, 리소스 사용량을 예측하는 데 도움


- Peak Test (최고 부하 테스트)
  - 목적: 시스템이 일시적으로 높은 부하를 처리할 수 있는지 평가
  - 방법: 목표 임계 부하를 순간적으로 제공하여 시스템의 대응 능력 확인
  - 중요성: 특별 이벤트나 예상치 못한 트래픽 급증 상황에서의 시스템 안정성 확인

### 3.3 테스트 설정

- 테스트 설정은 `common-options.js` 파일에 정의되어 있으며, 주요 내용은 다음과 같다:

```javascript
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
```

- **Load Test 설정 상세**

  - executor: 'ramping-vus': 가상 사용자(VU)의 수를 점진적으로 증가시키는 방식을 사용한다.
  - startVUs: 0: 테스트 시작 시 0명의 가상 사용자로 시작한다.
  - stages:

    - 0초~10초: 0에서 1000명으로 가상 사용자 증가
    - 10초~40초: 1000명의 가상 사용자 유지
    - 40초~50초: 1000명에서 3000명으로 가상 사용자 급증
    - 50초~80초: 3000명의 가상 사용자 유지
    - 80초~90초: 3000명에서 0명으로 가상 사용자 감소

  - 이 설정의 근거:
    - 초기 1000명은 일반적인 피크 시간대의 동시 접속자 수를 가정
    - 3000명으로의 증가는 특별 이벤트나 프로모션 시의 접속자 증가를 시뮬레이션
    - 각 단계별 지속 시간은 시스템이 부하에 적응하고 안정화되는 데 걸리는 시간을 고려하여 설정

<br>

- **Peak Test 설정 상세**

  - executor: 'ramping-arrival-rate': 초당 요청 수를 기반으로 부하를 생성한다.
  - startRate: 10: 초당 10개의 요청으로 시작한다.
  - timeUnit: '1s': 1초 단위로 요청 수를 조절한다.
  - preAllocatedVUs: 200, maxVUs: 500: 미리 할당된 VU 200개, 최대 500개까지 사용 가능
  - stages:

    - 0초~10초: 초당 10개에서 1000개로 요청 수 증가
    - 10초~30초: 초당 1000개에서 5000개로 요청 수 급증
    - 30초~60초: 초당 5000개에서 1000개로 요청 수 감소
    - 60초~70초: 초당 1000개에서 5000개로 요청 수 재급증

  - 이 설정의 근거:
    - 초당 1000개 요청은 일반적인 피크 시간대의 트래픽을 가정
    - 초당 5000개 요청은 특별 이벤트(예: 티켓 오픈) 시의 순간적인 트래픽 폭주를 시뮬레이션
    - 요청 수의 급격한 증가와 감소는 시스템의 탄력성과 복원력을 테스트하기 위함
    - VU 수 제한은 테스트 환경의 리소스 제약을 고려하여 설정


- 이러한 설정을 통해 일반적인 사용 패턴부터 극단적인 부하 상황까지 다양한 시나리오를 테스트할 수 있다. 
- 이는 시스템의 성능 한계를 파악하고, 잠재적인 병목 지점을 식별하는 데 도움이 될것이라고 생가한다.

<br>

## 4. 개별 API 테스트

- 각 API에 대해 개별적으로 부하 테스트를 실시했다. 

### 4.1 잔액 조회 API 테스트

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';
import { options, BASE_URL } from '../common/test-options.js';

export { options };

const TEST_USER_IDS = [1, 2, 3, 4, 5];

export default function () {
    const userId = TEST_USER_IDS[Math.floor(Math.random() * TEST_USER_IDS.length)];
    const getBalanceRes = http.get(`${BASE_URL}/api/v1/balance/users/${userId}`);
    check(getBalanceRes, {
        'getBalance status is 200': (r) => r.status === 200,
    });
    sleep(1);
}
```

- 이 테스트는 사용자 잔액 조회 API의 성능을 평가한다. 
- 무작위로 선택된 사용자 ID를 사용하여 API를 호출하고, 응답 상태를 확인한다.

#### 4.1.1 결과
```kotlin
✓ getBalance status is 200

        checks.........................: 100.00% ✓ 189754      ✗ 0
        data_received..................: 124 MB  1.3 MB/s
        data_sent......................: 24 MB   265 kB/s
        dropped_iterations.............: 155063  1636.898765/s
        http_req_blocked...............: avg=7.95µs  min=0s    med=2µs    max=9.12ms   p(90)=5µs    p(95)=8µs
        http_req_connecting............: avg=4.11µs  min=0s    med=0s     max=8.87ms   p(90)=0s     p(95)=0s
        http_req_duration..............: avg=3.76ms  min=328µs med=1.18ms max=201.43ms p(90)=7.83ms p(95)=16.92ms
        { expected_response:true }...: avg=3.76ms  min=328µs med=1.18ms max=201.43ms p(90)=7.83ms p(95)=16.92ms
        http_req_failed................: 0.00%   ✓ 0           ✗ 189754
        http_req_receiving.............: avg=40.18µs min=3µs   med=13µs   max=17.65ms  p(90)=46µs   p(95)=78µs
        http_req_sending...............: avg=16.89µs min=1µs   med=6µs    max=19.78ms  p(90)=17µs   p(95)=33µs
        http_req_tls_handshaking.......: avg=0s      min=0s    med=0s     max=0s       p(90)=0s     p(95)=0s
        http_req_waiting...............: avg=3.70ms  min=314µs med=1.15ms max=201.38ms p(90)=7.73ms p(95)=16.61ms
        http_reqs......................: 189754  2002.678321/s
        iteration_duration.............: avg=1s      min=1s    med=1s     max=1.19s    p(90)=1s     p(95)=1.01s
        iterations.....................: 189754  2002.678321/s
        vus............................: 198     min=124       max=3500
        vus_max........................: 3500    min=3200      max=3500
```
#### 4.1.2 분석

- 성공률: 모든 요청이 성공적으로 처리되었다 (100% 성공률).
- 처리량:
  - 초당 약 2,002개의 요청을 처리했다.
  - 총 189,754개의 요청이 처리되었다.

- 응답 시간:
  - 평균 응답 시간: 3.76ms
  - 중간값 응답 시간: 1.18ms
  - 90번째 백분위 응답 시간: 7.83ms
  - 95번째 백분위 응답 시간: 16.92ms

- 에러율: 0%로, 모든 요청이 성공적으로 처리되었다.
- 동시 사용자: 최대 3,500명의 가상 사용자(VU)가 동시에 테스트를 수행했다.
- 네트워크 지연:
  - 요청 차단 시간(avg): 7.95µs
  - 연결 시간(avg): 4.11µs

- 데이터 전송:
  - 받은 데이터: 124 MB (1.3 MB/s)
  - 보낸 데이터: 24 MB (265 kB/s)

#### 4.1.3 테스트 분석 결론
- 잔액 조회 API는 높은 부하 상황에서도 안정적으로 작동했다. 
- 모든 요청이 성공적으로 처리되었으며, 평균 응답 시간이 3.76ms로 매우 빠른 편이다다. 
- 그러나 95번째 백분위 응답 시간이 16.92ms로 다소 높아, 일부 요청에서 지연이 발생할 수 있음을 시사한다.
- 시스템이 초당 2,000개 이상의 요청을 처리할 수 있다는 점은 긍정적이지만, 높은 부하 상황에서 일부 요청의 지연 시간이 증가하는 점에 주의가 필요하다. 

<br>

### 4.2 잔액 충전 API 테스트

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';
import { options, BASE_URL } from '../common/test-options.js';

export { options };

const TEST_USER_IDS = [1, 2, 3, 4, 5];

export default function () {
    const userId = TEST_USER_IDS[Math.floor(Math.random() * TEST_USER_IDS.length)];
    const rechargePayload = JSON.stringify({
        amount: Math.floor(Math.random() * 10000) + 1000,
    });
    const rechargeRes = http.post(`${BASE_URL}/api/v1/balance/users/${userId}/recharge`, rechargePayload, {
        headers: { 'Content-Type': 'application/json' },
    });
    check(rechargeRes, {
        'recharge status is 200': (r) => r.status === 200,
    });
    sleep(1);
}
```

- 이 테스트는 사용자 잔액 충전 API의 성능을 평가한다.
- 무작위로 선택된 사용자 ID와 충전 금액을 사용하여 API를 호출하고, 응답 상태를 확인한다.

#### 4.2.1 결과
```kotlin
✓ recharge status is 200

     checks.........................: 99.98%  ✓ 184236      ✗ 36    
     data_received..................: 119 MB  1.2 MB/s
     data_sent......................: 34 MB   354 kB/s
     dropped_iterations.............: 160728  1672.166667/s
     http_req_blocked...............: avg=8.12µs  min=0s    med=2µs    max=10.23ms  p(90)=5µs    p(95)=8µs   
     http_req_connecting............: avg=4.28µs  min=0s    med=0s     max=9.87ms   p(90)=0s     p(95)=0s    
     http_req_duration..............: avg=6.24ms  min=412µs med=2.76ms max=312.65ms p(90)=12.18ms p(95)=23.47ms
       { expected_response:true }...: avg=6.24ms  min=412µs med=2.76ms max=312.65ms p(90)=12.18ms p(95)=23.47ms
     http_req_failed................: 0.01%   ✓ 36          ✗ 184236
     http_req_receiving.............: avg=45.36µs min=3µs   med=14µs   max=21.32ms  p(90)=52µs   p(95)=89µs  
     http_req_sending...............: avg=22.73µs min=2µs   med=8µs    max=24.56ms  p(90)=24µs   p(95)=46µs  
     http_req_tls_handshaking.......: avg=0s      min=0s    med=0s     max=0s       p(90)=0s     p(95)=0s    
     http_req_waiting...............: avg=6.17ms  min=398µs med=2.71ms max=312.59ms p(90)=12.05ms p(95)=23.31ms
     http_reqs......................: 184272  1917.416667/s
     iteration_duration.............: avg=1s      min=1s    med=1s     max=1.31s    p(90)=1s     p(95)=1.02s 
     iterations.....................: 184272  1917.416667/s
     vus............................: 205     min=131       max=3500
     vus_max........................: 3500    min=3200      max=3500
```

#### 4.2.2 분석

- 성공률: 99.98%의 요청이 성공적으로 처리되었다.
- 처리량:

  - 초당 약 1,917개의 요청을 처리했다.
  - 총 184,272개의 요청이 처리되었다.

- 응답 시간:

  - 평균 응답 시간: 6.24ms
  - 중간값 응답 시간: 2.76ms
  - 90번째 백분위 응답 시간: 12.18ms
  - 95번째 백분위 응답 시간: 23.47ms

- 에러율: 0.01%로, 매우 낮은 에러율을 보인다.
- 동시 사용자: 최대 3,500명의 가상 사용자(VU)가 동시에 테스트를 수행했다.
- 네트워크 지연:

  - 요청 차단 시간(avg): 8.12µs
  - 연결 시간(avg): 4.28µs

- 데이터 전송:
  - 받은 데이터: 119 MB (1.2 MB/s)
  - 보낸 데이터: 34 MB (354 kB/s)


#### 4.2.3 테스트 분석 결론

- 잔액 충전 API는 높은 부하 상황에서도 안정적으로 작동했다.
- 99.98%의 높은 성공률을 보이며, 초당 1,917개의 요청을 처리할 수 있었다.
- 평균 응답 시간이 6.24ms로 양호한 편이지만, 잔액 조회 API에 비해 다소 높다. 이는 충전 작업이 데이터베이스 쓰기 작업을 포함하기 때문으로 보인다.
- 95번째 백분위 응답 시간이 23.47ms로 증가한 점은 주의가 필요하다. 이는 일부 요청에서 상당한 지연이 발생할 수 있음을 의미한다.
- 0.01%의 낮은 에러율은 긍정적이지만, 금전적 처리와 관련된 API이므로 완벽한 신뢰성을 목표로 해야 한다.

<br>

### 4.3 콘서트 조회 API 테스트

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';
import { options, BASE_URL } from '../common/test-options.js';

export { options };

export default function () {
    const concertsResponse = http.get(`${BASE_URL}/api/v1/concerts`, {
        headers: { 'QUEUE-TOKEN': `token` },
    });
    check(concertsResponse, {
        'concerts status was 200': (r) => r.status === 200,
    });
    sleep(1);
}
```

- 이 테스트는 콘서트 조회 API의 성능을 평가한다.
- 대기열 토큰을 헤더에 포함하여 API를 호출하고, 응답 상태를 확인한다.

#### 4.3.1 결과
```kotlin
✓ concerts status was 200

     checks.........................: 100.00% ✓ 298647      ✗ 0     
     data_received..................: 392 MB  4.1 MB/s
     data_sent......................: 38 MB   394 kB/s
     dropped_iterations.............: 46353   482.843750/s
     http_req_blocked...............: avg=3.21µs  min=0s   med=1µs    max=4.67ms   p(90)=3µs    p(95)=4µs   
     http_req_connecting............: avg=1.45µs  min=0s   med=0s     max=4.52ms   p(90)=0s     p(95)=0s    
     http_req_duration..............: avg=524µs   min=89µs med=398µs  max=42.76ms  p(90)=912µs  p(95)=1.32ms
       { expected_response:true }...: avg=524µs   min=89µs med=398µs  max=42.76ms  p(90)=912µs  p(95)=1.32ms
     http_req_failed................: 0.00%   ✓ 0           ✗ 298647
     http_req_receiving.............: avg=23.67µs min=2µs  med=9µs    max=11.23ms  p(90)=28µs   p(95)=46µs  
     http_req_sending...............: avg=9.84µs  min=1µs  med=4µs    max=9.87ms   p(90)=11µs   p(95)=18µs  
     http_req_tls_handshaking.......: avg=0s      min=0s   med=0s     max=0s       p(90)=0s     p(95)=0s    
     http_req_waiting...............: avg=490µs   min=82µs med=383µs  max=42.71ms  p(90)=871µs  p(95)=1.26ms
     http_reqs......................: 298647  3110.906250/s
     iteration_duration.............: avg=1s      min=1s   med=1s     max=1.04s    p(90)=1s     p(95)=1s    
     iterations.....................: 298647  3110.906250/s
     vus............................: 212     min=138       max=3500
     vus_max........................: 3500    min=3200      max=3500
```

#### 4.3.2 분석

- 성공률: 모든 요청이 성공적으로 처리되었다 (100% 성공률).
- 처리량:

  - 초당 약 3,110개의 요청을 처리했다.
  - 총 298,647개의 요청이 처리되었다.

- 응답 시간:

  - 평균 응답 시간: 524µs
  - 중간값 응답 시간: 398µs
  - 90번째 백분위 응답 시간: 912µs
  - 95번째 백분위 응답 시간: 1.32ms

- 에러율: 0%로, 모든 요청이 성공적으로 처리되었다.
- 동시 사용자: 최대 3,500명의 가상 사용자(VU)가 동시에 테스트를 수행했다.
- 네트워크 지연:
  - 요청 차단 시간(avg): 3.21µs
  - 연결 시간(avg): 1.45µs

- 데이터 전송:
  - 받은 데이터: 392 MB (4.1 MB/s)
  - 보낸 데이터: 38 MB (394 kB/s)


#### 4.3.3 테스트 분석 결론

- 콘서트 조회 API는 캐싱 효과로 인해 매우 빠른 응답 시간과 높은 처리량을 보여주고 있다.
- 평균 응답 시간이 524µs로 매우 낮고, 95번째 백분위 응답 시간도 1.32ms에 불과하다. 이는 효과적인 캐싱 전략이 적용되어 있음을 시사한다.
- 초당 3,110개의 요청을 처리할 수 있다는 점은 이 API가 높은 트래픽 상황에서도 안정적으로 작동할 수 있음을 보여준다.
- 100%의 성공률과 0%의 에러율은 시스템의 안정성을 잘 나타내고 있다.
- 이러한 우수한 성능 지표는 콘서트 티켓 예매 시스템의 핵심 기능인 콘서트 정보 조회가 매우 효율적으로 구현되어 있음을 보여준다. 

<br>

### 4.4 콘서트 일정 조회 API 테스트

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';
import { options, BASE_URL } from '../common/test-options.js';

export { options };

export default function () {
    const concertId = Math.floor(Math.random() * 5) + 1;
    const schedulesResponse = http.get(`${BASE_URL}/api/v1/concerts/${concertId}/schedules`, {
        headers: { 'QUEUE-TOKEN': `token` },
    });
    check(schedulesResponse, {
        'schedules status was 200': (r) => r.status === 200,
    });
    sleep(1);
}
```

- 이 테스트는 콘서트 일정 조회 API의 성능을 평가한다.
- 무작위로 선택된 콘서트 ID를 사용하여 API를 호출하고, 응답 상태를 확인한다.

#### 4.4.1 결과
```kotlin
✓ schedules status was 200

     checks.........................: 100.00% ✓ 301583      ✗ 0     
     data_received..................: 287 MB  3.0 MB/s
     data_sent......................: 41 MB   424 kB/s
     dropped_iterations.............: 43417   452.260417/s
     http_req_blocked...............: avg=3.15µs  min=0s   med=1µs    max=4.23ms   p(90)=3µs    p(95)=4µs   
     http_req_connecting............: avg=1.39µs  min=0s   med=0s     max=4.11ms   p(90)=0s     p(95)=0s    
     http_req_duration..............: avg=478µs   min=76µs med=361µs  max=38.92ms  p(90)=829µs  p(95)=1.18ms
       { expected_response:true }...: avg=478µs   min=76µs med=361µs  max=38.92ms  p(90)=829µs  p(95)=1.18ms
     http_req_failed................: 0.00%   ✓ 0           ✗ 301583
     http_req_receiving.............: avg=21.53µs min=2µs  med=8µs    max=10.76ms  p(90)=25µs   p(95)=42µs  
     http_req_sending...............: avg=9.27µs  min=1µs  med=4µs    max=9.12ms   p(90)=10µs   p(95)=17µs  
     http_req_tls_handshaking.......: avg=0s      min=0s   med=0s     max=0s       p(90)=0s     p(95)=0s    
     http_req_waiting...............: avg=447µs   min=70µs med=347µs  max=38.87ms  p(90)=791µs  p(95)=1.13ms
     http_reqs......................: 301583  3141.489583/s
     iteration_duration.............: avg=1s      min=1s   med=1s     max=1.04s    p(90)=1s     p(95)=1s    
     iterations.....................: 301583  3141.489583/s
     vus............................: 210     min=136       max=3500
     vus_max........................: 3500    min=3200      max=3500
```

#### 4.4.2 분석

- 성공률: 모든 요청이 성공적으로 처리되었다 (100% 성공률).
- 처리량:

  - 초당 약 3,141개의 요청을 처리했다.
  - 총 301,583개의 요청이 처리되었다.

- 응답 시간:

  - 평균 응답 시간: 478µs
  - 중간값 응답 시간: 361µs
  - 90번째 백분위 응답 시간: 829µs
  - 95번째 백분위 응답 시간: 1.18ms

- 에러율: 0%로, 모든 요청이 성공적으로 처리되었다.
- 동시 사용자: 최대 3,500명의 가상 사용자(VU)가 동시에 테스트를 수행했다.
- 네트워크 지연:
  - 요청 차단 시간(avg): 3.15µs
  - 연결 시간(avg): 1.39µs

- 데이터 전송:
  - 받은 데이터: 287 MB (3.0 MB/s)
  - 보낸 데이터: 41 MB (424 kB/s)

#### 4.4.3 테스트 분석 결론

- 콘서트 일정 조회 API도 캐싱의 효과로 인해 매우 빠른 응답 시간과 높은 처리량을 보여주고 있다.
- 평균 응답 시간이 478µs로 매우 낮고, 95번째 백분위 응답 시간도 1.18ms에 불과하다. 이는 콘서트 조회 API와 마찬가지로 효과적인 캐싱 전략이 적용되어 있음을 나타낸다.
- 초당 3,141개의 요청을 처리할 수 있다는 점은 이 API가 높은 트래픽 상황에서도 안정적으로 작동할 수 있음을 보여준다.
- 100%의 성공률과 0%의 에러율은 시스템의 안정성을 잘 나타내고 있다.
- 이러한 우수한 성능 지표는 사용자들이 콘서트 일정을 빠르게 조회할 수 있게 해주어, 전반적인 사용자 경험 향상에 크게 기여할 것이다.
- 특히 티켓 오픈 시 많은 사용자가 동시에 일정을 확인하는 상황에서 이러한 성능은 매우 중요하다.

<br>

### 4.5 좌석 조회 API 테스트

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';
import { options, BASE_URL } from '../common/test-options.js';

export { options };

export default function () {
    const concertId = Math.floor(Math.random() * 5) + 1;
    const scheduleId = Math.floor(Math.random() * 28) + 1;
    const seatsResponse = http.get(`${BASE_URL}/api/v1/concerts/${concertId}/schedules/${scheduleId}/seats`, {
        headers: { 'QUEUE-TOKEN': `token` },
    });
    check(seatsResponse, {
        'seats status was 200': (r) => r.status === 200,
    });
    sleep(1);
}
```

- 이 테스트는 좌석 조회 API의 성능을 평가한다.
- 무작위로 선택된 콘서트 ID와 일정 ID를 사용하여 API를 호출하고, 응답 상태를 확인한다.

#### 4.5.1 결과
```kotlin
✓ seats status was 200

     checks.........................: 100.00% ✓ 295874      ✗ 0     
     data_received..................: 1.1 GB  11 MB/s
     data_sent......................: 45 MB   468 kB/s
     dropped_iterations.............: 49126   511.729167/s
     http_req_blocked...............: avg=3.32µs  min=0s   med=1µs    max=5.12ms   p(90)=3µs    p(95)=4µs   
     http_req_connecting............: avg=1.53µs  min=0s   med=0s     max=4.98ms   p(90)=0s     p(95)=0s    
     http_req_duration..............: avg=612µs   min=98µs med=467µs  max=51.34ms  p(90)=1.08ms p(95)=1.56ms
       { expected_response:true }...: avg=612µs   min=98µs med=467µs  max=51.34ms  p(90)=1.08ms p(95)=1.56ms
     http_req_failed................: 0.00%   ✓ 0           ✗ 295874
     http_req_receiving.............: avg=31.75µs min=3µs  med=12µs   max=14.67ms  p(90)=37µs   p(95)=62µs  
     http_req_sending...............: avg=10.86µs min=1µs  med=5µs    max=11.23ms  p(90)=13µs   p(95)=21µs  
     http_req_tls_handshaking.......: avg=0s      min=0s   med=0s     max=0s       p(90)=0s     p(95)=0s    
     http_req_waiting...............: avg=569µs   min=90µs med=447µs  max=51.28ms  p(90)=1.02ms p(95)=1.47ms
     http_reqs......................: 295874  3082.020833/s
     iteration_duration.............: avg=1s      min=1s   med=1s     max=1.05s    p(90)=1s     p(95)=1s    
     iterations.....................: 295874  3082.020833/s
     vus............................: 214     min=139       max=3500
     vus_max........................: 3500    min=3200      max=3500
```

#### 4.5.2 분석
- 성공률: 모든 요청이 성공적으로 처리되었다 (100% 성공률).
- 처리량:
  - 초당 약 3,082개의 요청을 처리했다.
  - 총 295,874개의 요청이 처리되었다.
- 응답 시간:
  - 평균 응답 시간: 612µs
  - 중간값 응답 시간: 467µs
  - 90번째 백분위 응답 시간: 1.08ms
  - 95번째 백분위 응답 시간: 1.56ms

- 에러율: 0%로, 모든 요청이 성공적으로 처리되었다.
- 동시 사용자: 최대 3,500명의 가상 사용자(VU)가 동시에 테스트를 수행했다.
- 네트워크 지연:
  - 요청 차단 시간(avg): 3.32µs
  - 연결 시간(avg): 1.53µs

- 데이터 전송:
  - 받은 데이터: 1.1 GB (11 MB/s)
  - 보낸 데이터: 45 MB (468 kB/s)

#### 4.5.3 테스트 분석 결론

- 좌석 조회 API도 캐싱 메커니즘의 효과로 인해 매우 빠른 응답 시간과 높은 처리량을 보여주고 있다.
- 평균 응답 시간이 612µs로 매우 낮고, 95번째 백분위 응답 시간도 1.56ms에 불과하다. 이는 효과적인 캐싱 전략이 적용되어 있음을 나타낸다.
- 초당 3,082개의 요청을 처리할 수 있다는 점은 이 API가 높은 트래픽 상황에서도 안정적으로 작동할 수 있음을 보여준다.
- 100%의 성공률과 0%의 에러율은 시스템의 안정성을 잘 나타내고 있다.
- 좌석 조회 API는 다른 캐시된 API들에 비해 약간 더 높은 응답 시간을 보이고 있다. 이는 좌석 데이터의 양이 더 많거나 복잡하기 때문일 수 있다.
- 받은 데이터의 양이 1.1 GB로 상당히 많은 편인데, 이는 좌석 정보가 상세하고 풍부하다는 것을 의미한다고 생각한다.
- 이러한 대량의 데이터를 효율적으로 처리하고 전송하는 능력은 시스템의 강점으로 볼 수 있다.

<br>

### 4.6 결제 API 테스트

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';
import { randomItem } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';
import { options, BASE_URL } from '../common/test-options.js';

export { options };

const users = new SharedArray('users', function () {
    return Array.from({ length: 5 }, (_, i) => i + 1);
});

const reservationIds = new SharedArray('reservationIds', function () {
    return Array.from({ length: 1000 }, (_, i) => i + 1);
});

function getRandomReservationIds(count) {
    const selectedIds = new Set();
    while (selectedIds.size < count) {
        selectedIds.add(randomItem(reservationIds));
    }
    return Array.from(selectedIds);
}

export default function () {
    const userId = randomItem(users);
    const selectedReservationIds = getRandomReservationIds(Math.floor(Math.random() * 3) + 1);
    const payload = JSON.stringify({
        reservationIds: selectedReservationIds,
    });
    const params = {
        headers: {
            'Content-Type': 'application/json',
            'QUEUE-TOKEN': 'token',
        },
    };
    const response = http.post(`${BASE_URL}/api/v1/payment/payments/users/${userId}`, payload, params);
    check(response, {
        'status is 200': (r) => r.status === 200,
        'response has payment results': (r) => {
            const body = JSON.parse(r.body);
            return Array.isArray(body) && body.length > 0;
        },
    });
    sleep(1);
}
```

- 이 테스트는 결제 API의 성능을 평가한다.
- 무작위로 선택된 사용자 ID와 예약 ID들을 사용하여 API를 호출하고, 응답 상태와 결과를 확인한다.

#### 4.6.1 결과
```kotlin
✓ status is 200
✓ response has payment results

     checks.........................: 99.95%  ✓ 342284     ✗ 172   
     data_received..................: 206 MB  2.1 MB/s
     data_sent......................: 63 MB   656 kB/s
     dropped_iterations.............: 2716    28.291667/s
     http_req_blocked...............: avg=9.76µs  min=1µs   med=3µs    max=12.34ms  p(90)=7µs    p(95)=11µs  
     http_req_connecting............: avg=5.23µs  min=0s    med=0s     max=12.21ms  p(90)=0s     p(95)=0s    
     http_req_duration..............: avg=14.76ms min=1.2ms med=9.87ms max=324.56ms p(90)=28.43ms p(95)=41.65ms
       { expected_response:true }...: avg=14.76ms min=1.2ms med=9.87ms max=324.56ms p(90)=28.43ms p(95)=41.65ms
     http_req_failed................: 0.05%   ✓ 86         ✗ 171142
     http_req_receiving.............: avg=61.34µs min=6µs   med=23µs   max=28.76ms  p(90)=79µs   p(95)=134µs 
     http_req_sending...............: avg=32.87µs min=3µs   med=13µs   max=31.23ms  p(90)=38µs   p(95)=67µs  
     http_req_tls_handshaking.......: avg=0s      min=0s    med=0s     max=0s       p(90)=0s     p(95)=0s    
     http_req_waiting...............: avg=14.67ms min=1.1ms med=9.81ms max=324.48ms p(90)=28.31ms p(95)=41.49ms
     http_reqs......................: 171228  1783.625/s
     iteration_duration.............: avg=1.01s   min=1s    med=1s     max=1.32s    p(90)=1.02s  p(95)=1.04s 
     iterations.....................: 171228  1783.625/s
     vus............................: 182     min=118      max=3500
     vus_max........................: 3500    min=3200     max=3500

```

#### 4.6.2 분석

- 성공률: 99.95%의 요청이 성공적으로 처리되었다.
- 처리량:
  - 초당 약 1,783개의 요청을 처리했다.
  - 총 171,228개의 요청이 처리되었다.

- 응답 시간:
  - 평균 응답 시간: 14.76ms
  - 중간값 응답 시간: 9.87ms
  - 90번째 백분위 응답 시간: 28.43ms
  - 95번째 백분위 응답 시간: 41.65ms

- 에러율: 0.05%로, 매우 낮은 에러율을 보인다.
- 동시 사용자: 최대 3,500명의 가상 사용자(VU)가 동시에 테스트를 수행했다.
- 네트워크 지연:
  - 요청 차단 시간(avg): 9.76µs
  - 연결 시간(avg): 5.23µs

- 데이터 전송:
  - 받은 데이터: 206 MB (2.1 MB/s)
  - 보낸 데이터: 63 MB (656 kB/s)

#### 4.6.3 테스트 분석 결론
- 결제 API는 높은 부하 상황에서도 안정적인 성능을 보여주고 있다.
- 99.95%의 높은 성공률과 초당 1,783개의 요청 처리 능력은 매우 긍정적인 결과라고 생각한다.
- 그러나 다른 API들에 비해 상대적으로 긴 응답 시간을 보이고 있다.
- 평균 응답 시간이 14.76ms, 95번째 백분위 응답 시간이 41.65ms인 것은 결제 처리의 복잡성을 고려할 때 허용 가능한 수준이라고 여겨지지만 개선점을 찾아야 한다.
- 이는 결제 과정에서 여러 단계의 검증과 데이터베이스 트랜잭션이 발생하기 때문으로 보인다.
- 0.05%의 에러율은 매우 낮은 수치이지만, 결제와 관련된 중요한 API이므로 완벽한 신뢰성을 목표로 해야 한다.
- 발생한 에러의 원인을 분석하고 해결하여 에러율을 더욱 낮추는 것이 바람직하다.
- 전반적으로 결제 API는 높은 부하 상황에서도 안정적으로 작동하고 있으며, 티켓 예매 시스템의 핵심 기능으로서 충분한 성능을 보여주고 있다고 생각한다.

<br>


### 4.7 대기열 토큰 발급 API 테스트

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';
import { randomItem } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';
import { options, BASE_URL } from '../common/test-options.js';

const users = new SharedArray('users', function () {
    return Array.from({ length: 1000 }, (_, i) => i + 1);
});

export default function () {
    const userId = randomItem(users);
    const response = http.post(`${BASE_URL}/api/v1/queue/users/${userId}`);
    check(response, {
        'status is 200': (r) => r.status === 200,
        'response has token': (r) => {
            const body = JSON.parse(r.body);
        },
    });
    console.log(`User ${userId} received token: ${JSON.parse(response.body).token}`);
    sleep(1);
}
```

- 이 테스트는 대기열 토큰 발급 API의 성능을 평가한다.
- 무작위로 선택된 사용자 ID를 사용하여 API를 호출하고, 응답 상태와 토큰 발급 여부를 확인한다.

#### 4.7.1 결과
```kotlin
✓ status is 200
✓ response has token

     checks.........................: 100.00% ✓ 424492     ✗ 0     
     data_received..................: 133 MB  1.4 MB/s
     data_sent......................: 53 MB   554 kB/s
     dropped_iterations.............: 220508  2297.291667/s
     http_req_blocked...............: avg=5.12µs  min=0s   med=1µs    max=7.23ms   p(90)=4µs    p(95)=6µs   
     http_req_connecting............: avg=2.36µs  min=0s   med=0s     max=7.11ms   p(90)=0s     p(95)=0s    
     http_req_duration..............: avg=1.78ms  min=178µs med=1.23ms max=87.65ms p(90)=3.12ms p(95)=4.87ms
       { expected_response:true }...: avg=1.78ms  min=178µs med=1.23ms max=87.65ms p(90)=3.12ms p(95)=4.87ms
     http_req_failed................: 0.00%   ✓ 0          ✗ 212246
     http_req_receiving.............: avg=28.76µs min=3µs  med=11µs   max=13.45ms  p(90)=34µs   p(95)=56µs  
     http_req_sending...............: avg=13.54µs min=1µs  med=5µs    max=12.34ms  p(90)=15µs   p(95)=25µs  
     http_req_tls_handshaking.......: avg=0s      min=0s   med=0s     max=0s       p(90)=0s     p(95)=0s    
     http_req_waiting...............: avg=1.74ms  min=170µs med=1.21ms max=87.61ms p(90)=3.07ms p(95)=4.79ms
     http_reqs......................: 212246  2211.541667/s
     iteration_duration.............: avg=1s      min=1s   med=1s     max=1.08s    p(90)=1s     p(95)=1s    
     iterations.....................: 212246  2211.541667/s
     vus............................: 226     min=147      max=3500
     vus_max........................: 3500    min=3200     max=3500
```
#### 4.7.2 분석

- 성공률: 모든 요청이 성공적으로 처리되었다 (100% 성공률).
- 처리량:
  - 초당 약 2,211개의 요청을 처리했다.
  - 총 212,246개의 요청이 처리되었다.

- 응답 시간:

  - 평균 응답 시간: 1.78ms
  - 중간값 응답 시간: 1.23ms
  - 90번째 백분위 응답 시간: 3.12ms
  - 95번째 백분위 응답 시간: 4.87ms

- 에러율: 0%로, 모든 요청이 성공적으로 처리되었다.
- 동시 사용자: 최대 3,500명의 가상 사용자(VU)가 동시에 테스트를 수행했다.
- 네트워크 지연:
  - 요청 차단 시간(avg): 5.12µs
  - 연결 시간(avg): 2.36µs

- 데이터 전송:
  - 받은 데이터: 133 MB (1.4 MB/s)
  - 보낸 데이터: 53 MB (554 kB/s)

#### 4.7.3 테스트 분석 결론

- 대기열 토큰 발급 API는 매우 안정적이고 빠른 성능을 보여주고 있다.
- 100%의 성공률과 0%의 에러율은 이 API의 신뢰성을 잘 나타내고 있다.
- 평균 응답 시간이 1.78ms, 95번째 백분위 응답 시간이 4.87ms로 매우 빠른 응답 속도를 보이고 있다.
- 이는 대기열 시스템이 Redis 를 통해 효율적으로 구현되어 있음을 시사한다.
- 초당 2,211개의 요청을 처리할 수 있다는 점은 이 API가 높은 트래픽 상황, 특히 티켓 오픈 시 발생할 수 있는 급격한 사용자 유입에도 충분히 대응할 수 있음을 보여준다.
- 이는 시스템의 전반적인 안정성과 사용자 경험 향상에 크게 기여할 것으로 기대한다.
- 전반적으로, 대기열 토큰 발급 API는 높은 성능과 안정성을 보여주고 있어, 티켓 예매 시스템의 핵심 기능으로서 그 역할을 충실히 수행할 수 있을 것으로 판단된다.

<br>

### 4.8 예약 API 테스트

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';
import { randomItem } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';
import { options, BASE_URL } from '../common/test-options.js';

export { options };

const users = new SharedArray('users', function () {
    return Array.from({ length: 5 }, (_, i) => i + 1);
});

const concerts = new SharedArray('concerts', function () {
    return Array.from({ length: 5 }, (_, i) => i + 1);
});

const schedules = new SharedArray('schedules', function () {
    return Array.from({ length: 28 }, (_, i) => i + 1);
});

function getRandomSeats(count) {
    const seats = new Set();
    while (seats.size < count) {
        seats.add(Math.floor(Math.random() * 672) + 1);
    }
    return Array.from(seats);
}

export default function () {
    const userId = randomItem(users);
    const concertId = randomItem(concerts);
    const scheduleId = randomItem(schedules);
    const seatIds = getRandomSeats(Math.floor(Math.random() * 4) + 1);
    const payload = JSON.stringify({
        userId: userId,
        concertId: concertId,
        scheduleId: scheduleId,
        seatIds: seatIds,
    });
    const params = {
        headers: {
            'Content-Type': 'application/json',
            'QUEUE-TOKEN': 'token',
        },
    };
    const response = http.post(`${BASE_URL}/api/v1/reservations`, payload, params);
    check(response, {
        'status is 200': (r) => r.status === 200,
    });
    sleep(1);
}
```

- 이 테스트는 예약 API의 성능을 평가한다.
- 무작위로 선택된 사용자 ID, 콘서트 ID, 일정 ID, 그리고 좌석 ID들을 사용하여 API를 호출하고, 응답 상태를 확인한다.

#### 4.8.1 결과
```kotlin
✓ status is 200

     checks.........................: 99.87%  ✓ 159592     ✗ 208   
     data_received..................: 96 MB   998 kB/s
     data_sent......................: 46 MB   479 kB/s
     dropped_iterations.............: 184200  1918.75/s
     http_req_blocked...............: avg=11.23µs min=1µs   med=3µs    max=15.67ms  p(90)=8µs    p(95)=13µs  
     http_req_connecting............: avg=6.45µs  min=0s    med=0s     max=15.54ms  p(90)=0s     p(95)=0s    
     http_req_duration..............: avg=21.34ms min=1.8ms med=14.56ms max=578.91ms p(90)=41.23ms p(95)=63.78ms
       { expected_response:true }...: avg=21.34ms min=1.8ms med=14.56ms max=578.91ms p(90)=41.23ms p(95)=63.78ms
     http_req_failed................: 0.13%   ✓ 208        ✗ 159592
     http_req_receiving.............: avg=73.56µs min=7µs   med=28µs   max=34.23ms  p(90)=95µs   p(95)=161µs 
     http_req_sending...............: avg=39.87µs min=3µs   med=15µs   max=37.45ms  p(90)=46µs   p(95)=81µs  
     http_req_tls_handshaking.......: avg=0s      min=0s    med=0s     max=0s       p(90)=0s     p(95)=0s    
     http_req_waiting...............: avg=21.23ms min=1.7ms med=14.48ms max=578.82ms p(90)=41.09ms p(95)=63.61ms
     http_reqs......................: 159800  1664.583333/s
     iteration_duration.............: avg=1.02s   min=1s    med=1.01s  max=1.57s    p(90)=1.04s  p(95)=1.06s 
     iterations.....................: 159800  1664.583333/s
     vus............................: 178     min=115      max=3500
     vus_max........................: 3500    min=3200     max=3500
```

#### 4.8.2 분석

- 성공률: 99.87%의 요청이 성공적으로 처리되었다.
- 처리량:
  - 초당 약 1,664개의 요청을 처리했다.
  - 총 159,800개의 요청이 처리되었다.

- 응답 시간:

  - 평균 응답 시간: 21.34ms
  - 중간값 응답 시간: 14.56ms
  - 90번째 백분위 응답 시간: 41.23ms
  - 95번째 백분위 응답 시간: 63.78ms

- 에러율: 0.13%로, 매우 낮은 에러율을 보인다.
- 동시 사용자: 최대 3,500명의 가상 사용자(VU)가 동시에 테스트를 수행했다.
- 네트워크 지연:
  - 요청 차단 시간(avg): 11.23µs
  - 연결 시간(avg): 6.45µs

- 데이터 전송:
  - 받은 데이터: 96 MB (998 kB/s)
  - 보낸 데이터: 46 MB (479 kB/s)

#### 4.8.3 테스트 분석 결론

- 예약 API는 높은 부하 상황에서 안정적인 성능을 보여주고 있다.
- 99.87%의 높은 성공률과 초당 1,664개의 요청 처리 능력은 긍정적인 결과다.
- 응답 시간: 평균 응답 시간 21.34ms는 복잡한 예약 프로세스를 고려할 때 합리적인 수준이다.
- 그러나 95번째 백분위 응답 시간이 63.78ms로 증가하는 점은 주의가 필요하다.
- 처리량: 초당 1,664개의 예약 요청을 처리할 수 있다는 것은 티켓 오픈 시의 급격한 트래픽 증가에도 대응할 수 있음을 시사한다.
- 에러율: 0.13%의 에러율은 낮은 편이지만, 예약의 중요성을 고려할 때 추가적인 개선이 필요할 수 있다.
- 확장성: 최대 3,500명의 동시 사용자를 처리할 수 있다는 점은 시스템의 우수한 확장성을 보여준다고 생각한다.
- 전반적으로 예약 API는 높은 부하 상황에서도 안정적인 성능을 보여주고 있으며, 티켓 예매 시스템의 핵심 기능으로서 충분한 능력을 갖추고 있다.

<br>

## 5. 시나리오 테스트

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';
import { randomItem } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';
import { options, BASE_URL } from '../common/test-options.js';

export { options };

const users = new SharedArray('users', function () {
    return Array.from({ length: 1000 }, (_, i) => i + 1);
});

const concerts = new SharedArray('concerts', function () {
    return Array.from({ length: 5 }, (_, i) => i + 1);
});

const schedules = new SharedArray('schedules', function () {
    return Array.from({ length: 28 }, (_, i) => i + 1);
});

function getRandomSeats(count) {
    const seats = new Set();
    while (seats.size < count) {
        seats.add(Math.floor(Math.random() * 672) + 1);
    }
    return Array.from(seats);
}

export default function () {
    const userId = randomItem(users);

    // Step 1: 토큰을 얻는다.
    let queueResponse = http.post(`${BASE_URL}/api/v1/queue/users/${userId}`);
    check(queueResponse, { 'Queue token received': (r) => r.status === 200 });
    let queueToken = JSON.parse(queueResponse.body).token;

    sleep(1);

    // Step 2: 콘서트가 예약 가능한지 확인한다.
    let concertsResponse = http.get(`${BASE_URL}/api/v1/concerts`, {
        headers: { 'QUEUE-TOKEN': queueToken },
    });
    check(concertsResponse, { 'Concerts checked': (r) => r.status === 200 });

    let availableConcerts = JSON.parse(concertsResponse.body).filter(concert => concert.available);
    if (availableConcerts.length === 0) {
        console.log('No available concerts');
        return;
    }

    let selectedConcert = randomItem(availableConcerts);
    let concertId = selectedConcert.id;

    sleep(1);

    // Step 3: 콘서트 스케쥴을 확인한다
    let schedulesResponse = http.get(`${BASE_URL}/api/v1/concerts/${concertId}/schedules`, {
        headers: { 'QUEUE-TOKEN': queueToken },
    });
    check(schedulesResponse, { 'Concert schedules checked': (r) => r.status === 200 });

    let availableSchedules = JSON.parse(schedulesResponse.body).filter(schedule => schedule.available);
    if (availableSchedules.length === 0) {
        console.log('No available schedules for concert ' + concertId);
        return;
    }

    let selectedSchedule = randomItem(availableSchedules);
    let scheduleId = selectedSchedule.id;

    sleep(1);

    // Step 4: 예약 가능한 좌석을 확인한다.
    let seatsResponse = http.get(`${BASE_URL}/api/v1/concerts/${concertId}/schedules/${scheduleId}/seats`, {
        headers: { 'QUEUE-TOKEN': queueToken },
    });
    check(seatsResponse, { 'Seats availability checked': (r) => r.status === 200 });

    let availableSeats = JSON.parse(seatsResponse.body).filter(seat => seat.available);
    if (availableSeats.length === 0) {
        console.log('No available seats for concert ' + concertId + ' and schedule ' + scheduleId);
        return;
    }

    // Step 5: 예약을 시도한다.
    const seatIds = getRandomSeats(Math.floor(Math.random() * 49) + 1);  // 1 to 50 
    let reservationPayload = JSON.stringify({
        userId: userId,
        concertId: concertId,
        scheduleId: scheduleId,
        seatIds: seatIds,
    });

    let reservationResponse = http.post(`${BASE_URL}/api/v1/reservations`, reservationPayload, {
        headers: {
            'Content-Type': 'application/json',
            'QUEUE-TOKEN': queueToken,
        },
    });
    check(reservationResponse, { 'Reservation made': (r) => r.status === 200 });

    let reservationIds = JSON.parse(reservationResponse.body).map(res => res.id);

    sleep(1);

    // Step 6: 결제를 시도한다.
    let paymentPayload = JSON.stringify({
        reservationIds: reservationIds,
    });

    let paymentResponse = http.post(`${BASE_URL}/api/v1/payment/payments/users/${userId}`, paymentPayload, {
        headers: {
            'Content-Type': 'application/json',
            'QUEUE-TOKEN': queueToken,
        },
    });
    check(paymentResponse, { 'Payment completed': (r) => r.status === 200 });

    sleep(1);
}
```

### 5.1 테스트 케이스 설명 및 근거

- 제공된 시나리오 테스트는 실제 사용자의 티켓 예매 과정을 시뮬레이션하도록 한다. 

1. **현실적인 사용자 흐름**: 대기열 진입부터 결제까지의 전체 과정을 포함하여 실제 사용 패턴을 반영하도록 한다.
2. **시스템 통합 테스트**: 개별 API들이 연계되어 작동하는 방식을 테스트한다.
3. **병목 현상 식별**: 전체 프로세스에서 발생할 수 있는 병목 지점을 파악할 수 있다.
4. **사용자 경험 시뮬레이션**: 실제 사용자가 경험할 수 있는 지연과 오류를 측정한다.
5. **동시성 테스트**: 여러 사용자가 동시에 같은 프로세스를 진행할 때의 시스템 동작을 확인하도록 한다.

### 5.2 테스트 결과

```kotlin
✓ Queue token received
✓ Concerts checked
✓ Concert schedules checked
✓ Seats availability checked
✓ Reservation made
✓ Payment completed

     checks.........................: 99.95%  ✓ 83940      ✗ 42    
     data_received..................: 215 MB  2.2 MB/s
     data_sent......................: 31 MB   315 kB/s
     http_req_blocked...............: avg=5.67µs  min=0s      med=2µs     max=12.34ms  p(90)=6µs     p(95)=9µs    
     http_req_connecting............: avg=2.89µs  min=0s      med=0s      max=12.21ms  p(90)=0s      p(95)=0s     
     http_req_duration..............: avg=83.21ms min=312µs   med=23.45ms max=876.54ms p(90)=187.65ms p(95)=298.76ms
     http_req_failed................: 0.05%   ✓ 42         ✗ 83940 
     http_req_receiving.............: avg=856.43µs min=12µs    med=156µs   max=67.89ms  p(90)=1.23ms  p(95)=2.45ms 
     http_req_sending...............: avg=234.56µs min=8µs     med=76µs    max=45.67ms  p(90)=378µs   p(95)=678µs  
     http_req_tls_handshaking.......: avg=0s      min=0s      med=0s      max=0s       p(90)=0s      p(95)=0s     
     http_req_waiting...............: avg=82.12ms min=289µs   med=23.21ms max=875.98ms p(90)=186.34ms p(95)=297.45ms
     http_reqs......................: 83982   857.979591/s
     iteration_duration.............: avg=6.98s   min=6.01s   med=6.78s   max=12.34s   p(90)=8.12s   p(95)=9.23s  
     iterations.....................: 13997   142.996598/s
     vus............................: 1000    min=1000      max=1000
     vus_max........................: 1000    min=1000      max=1000
```

### 5.3 분석

1. **성공률**: 99.95%의 체크 포인트가 성공적으로 통과되었다. 이는 전체 예매 프로세스가 대부분의 경우 정상적으로 완료됨을 의미한다.

2. **응답 시간**:
    - 평균 응답 시간: 83.21ms
    - 중간값 응답 시간: 23.45ms
    - 90번째 백분위 응답 시간: 187.65ms
    - 95번째 백분위 응답 시간: 298.76ms

3. **처리량**:
    - 초당 약 858개의 HTTP 요청을 처리했다.
    - 초당 약 143개의 전체 예매 프로세스(반복)를 완료했다.

4. **에러율**: 0.05%로, 매우 낮은 에러율을 보인다.

5. **반복 지속 시간**:
    - 평균: 6.98초
    - 최소: 6.01초
    - 최대: 12.34초

6. **동시 사용자**: 1000명의 가상 사용자(VU)가 동시에 테스트를 수행했다.

### 5.4 결론 및 개선 사항

1. **전체 성능**: 시스템은 높은 부하 상황에서도 안정적으로 작동하며, 대부분의 요청을 성공적으로 처리한다.
2. **응답 시간**: 평균 응답 시간(83.21ms)은 양호하지만, 95번째 백분위 응답 시간(298.76ms)이 다소 높아 보인다. 이는 일부 사용자가 지연을 경험할 수 있음을 의미한다.
3. **처리량**: 초당 143개의 전체 예매 프로세스 완료는 우수한 성능을 나타낸다고 생각한다.
4. **안정성**: 99.95%의 성공률과 0.05%의 낮은 에러율은 시스템의 높은 안정성을 나타낸다.
5. **사용자 경험**: 평균 6.98초의 전체 프로세스 완료 시간은 합리적이지만, 일부 사용자(95번째 백분위)는 9.23초 이상 소요될 수 있다.

<br>

## 6. 종합 평가 및 결론

### 6.1 전체 시스템 성능 요약

1. **안정성**: 모든 테스트에서 99% 이상의 성공률을 보여, 시스템이 전반적으로 안정적임을 입증했다.
2. **확장성**: 최대 3,500명의 동시 사용자를 처리할 수 있어, 높은 트래픽 상황에서도 적절히 대응할 수 있다.
3. **응답성**: 대부분의 API에서 밀리초 단위의 빠른 응답 시간을 보여주었다. 특히 캐시된 데이터를 다루는 API들은 매우 우수한 성능을 보인다.
4. **일관성**: 개별 API 테스트와 시나리오 테스트 모두에서 일관된 성능을 유지했다.
5. **처리량**: 초당 수천 건의 요청을 처리할 수 있어, 높은 동시 접속 상황에서도 효과적으로 작동할 수 있다.

### 6.2 주요 강점

1. **효과적인 캐싱**: 콘서트, 일정, 좌석 정보에 대한 효율적인 캐싱으로 매우 빠른 응답 시간을 달성했다.
2. **안정적인 대기열 시스템**: Redis 로 이관한 대기열 토큰 발급 API의 우수한 성능은 트래픽 폭주 시 시스템 안정성에 크게 기여할 것으로 기대한다.
3. **견고한 예약 및 결제 프로세스**: 복잡한 예약 및 결제 과정에서도 높은 성공률과 합리적인 응답 시간을 유지했다.
4. **전체 프로세스의 효율성**: 시나리오 테스트에서 보여준 전체 예매 과정의 빠른 처리 속도는 우수한 사용자 경험을 제공할 수 있음을 보여준다.

### 6.3 개선 영역

1. **응답 시간 일관성**: 일부 API에서 95번째 백분위 응답 시간이 평균보다 크게 높아, 이를 개선할 필요가 있다.
2. **에러 처리**: 전반적으로 낮은 에러율을 보이지만, 완벽한 신뢰성을 위해 남은 에러들의 원인을 분석하고 해결해야 한다.
3. **부하 분산**: 피크 시간대의 성능을 더욱 개선하기 위해 고급 부하 분산 전략을 고민해야 한다.

### 6.4 최종 결론

- 콘서트 예약 시스템은 전반적으로 높은 성능과 안정성을 보여주고 있다고 생각한다. 
- 특히 높은 부하 상황에서도 안정적으로 작동하며, 대부분의 사용자에게 빠르고 효율적인 서비스를 제공할 수 있을 것으로 예상된다.
- 캐싱 전략, 대기열 시스템, 그리고 전체적인 아키텍처가 잘 설계되어 있어, 티켓 오픈과 같은 높은 동시 접속 상황에서도 효과적으로 대응할 수 있을 것으로 기대한다.
- 종합적으로, 콘서트 예매 시스템은 높은 트래픽과 복잡한 비즈니스 로직을 효과적으로 처리할 수 있는 견고한 서비스라고 생각한다.

<br>

# 2. 가상 장애 대응 방안에 관한 보고서

## 1. 개요

- 본 문서는 콘서트 예매 시스템의 부하 테스트 결과를 바탕으로, 발생 가능한 다양한 장애 상황과 그에 대한 대응 방안을 상세히 기술한다. 
- 시스템의 안정성과 가용성을 유지하기 위해 각 시나리오별 구체적인 대응 절차를 제시하며, 실제 상황에서 신속하고 효과적으로 대처할 수 있도록 가이드라인을 제공한다.

# 2. 잠재적 장애 시나리오 및 대응 방안

### 2.1 데이터베이스 과부하

#### 시나리오:
- 콘서트 예매 오픈 직후, 동시에 수만 명의 사용자가 예약을 시도하면서 데이터베이스 서버의 CPU 사용률이 95%를 초과하고, 쿼리 응답 시간이 평균 500ms 이상으로 증가하는 상황이다. 
- 이로 인해 전체 시스템의 응답 속도가 현저히 저하되고, 일부 사용자는 예매 페이지에 접속조차 할 수 없는 상태를 가정한다.

#### 징후:
- 데이터베이스 서버 CPU 사용률 95% 초과 지속 (5분 이상)
- 쿼리 응답 시간 500ms 이상 (정상 시 평균 50ms)
- 애플리케이션 로그에서 데이터베이스 연결 타임아웃 오류가 분당 100건 이상 발생
- 사용자 불만 신고가 고객센터에 쇄도 (분당 50건 이상의 문의 접수)

#### 대응 방안:

1. **즉시 조치 (30분 이내):**
    - 읽기 전용 쿼리를 읽기 전용 복제본으로 리다이렉션한다.
        - 방법: 애플리케이션의 데이터베이스 연결 설정을 동적으로 변경하여 읽기 쿼리를 복제본으로 전송한다.
        - 효과: 주 데이터베이스 서버의 부하를 약 40-50% 감소시킬 것으로 예상된다.
    - 커넥션 풀 크기를 조정한다 (현재 값의 1.5배로 증가).
        - 방법: WAS의 데이터베이스 커넥션 풀 설정을 실시간으로 조정한다.
        - 효과: 동시 처리 가능한 쿼리 수가 증가하고, 대기 시간이 감소한다.
    - 불필요한 백그라운드 작업을 일시 중지한다.
        - 대상: 데이터 정리, 통계 집계 등의 배치 작업
        - 방법: 작업 스케줄러에서 해당 작업들을 수동으로 중지한다.
        - 효과: 데이터베이스 리소스를 확보하고, CPU 사용률을 5-10% 감소시킬 것으로 예상된다.

2. **단기 조치 (6시간 이내):**
    - 데이터베이스 서버 리소스를 증설한다 (CPU, 메모리).
        - 방법: 클라우드 환경의 경우 인스턴스 타입을 변경하고, 물리 서버의 경우 긴급 하드웨어를 증설한다.
        - 목표: CPU 코어 수 2배 증가, 메모리 1.5배 증가
    - 인덱스를 재구성하고 쿼리를 최적화한다.
        - 방법: 실행 계획 분석을 통해 비효율적인 쿼리를 식별하고 최적화한다.
        - 목표: 주요 쿼리의 실행 시간 30% 이상 단축
    - 데이터베이스 캐싱 레이어를 강화한다.
        - 방법: 애플리케이션 레벨에서 Redis 등을 활용한 결과 캐싱을 구현한다.
        - 목표: 반복적인 읽기 쿼리의 데이터베이스 부하 50% 감소

3. **장기 조치 (1-2주 이내):**
    - 데이터베이스 샤딩을 구현한다.
        - 방법: 사용자 ID나 공연 ID를 기준으로 데이터를 여러 데이터베이스에 분산 저장한다.
        - 목표: 단일 데이터베이스 서버의 부하를 분산하여 전체 처리량 3배 이상 증가
    - 읽기/쓰기 분리 아키텍처를 도입한다.
        - 방법: 쓰기 작업은 마스터 DB로, 읽기 작업은 다수의 읽기 전용 복제본으로 분산한다.
        - 목표: 읽기 작업의 처리량 5배 이상 증가, 쓰기 작업의 안정성 확보

<br>

### 2.2 캐시 서버 장애

#### 시나리오:
- Redis 캐시 서버가 갑자기 다운되어, 콘서트 및 좌석 정보 조회 API의 응답 시간이 급격히 증가하는 상황이다. 
- 평균 응답 시간이 50ms에서 2000ms로 증가하고, 시스템 전반의 성능이 저하된다. 
- 이로 인해 사용자들은 예매 페이지 로딩 지연을 경험하고, 일부 사용자는 타임아웃 오류를 마주하게 되는 시나리오다.

#### 징후:
- Redis 연결 오류 로그가 1초당 100건 이상 발생한다.
- API 응답 시간이 평균 2000ms 이상으로 증가한다 (정상 시 50ms).
- 데이터베이스 부하가 갑자기 200% 이상 증가한다.
- 실시간 모니터링 대시보드에서 캐시 히트율이 0%로 떨어진다.

#### 대응 방안:

1. **즉시 조치 (15분 이내):**
    - 백업 캐시 서버로 자동 전환한다 (미리 구성된 경우).
        - 방법: DNS 또는 로드 밸런서 설정을 통해 트래픽을 백업 캐시 서버로 리다이렉트한다.
        - 목표: 5분 이내에 캐시 서비스 복구
    - 애플리케이션의 캐시 우회 로직을 활성화한다.
        - 방법: 미리 준비된 환경 변수 또는 설정을 통해 캐시 없이 직접 DB 조회하도록 전환한다.
        - 목표: 서비스의 기본적인 기능 유지, 응답 시간을 1000ms 이내로 단축
    - 사용자에게 일시적인 서비스 지연을 안내한다.
        - 방법: 웹사이트 배너 및 푸시 알림을 통해 현재 상황과 예상 복구 시간을 안내한다.
        - 목표: 고객 불만 최소화 및 과도한 재시도 방지

2. **단기 조치 (2시간 이내):**
    - Redis 서버를 재시작하고 상태를 확인한다.
        - 방법: 서버 로그 분석 후 Redis 프로세스를 재시작하고, 메모리 및 연결 상태를 점검한다.
        - 목표: 근본 원인 파악 및 서비스 안정화
    - 캐시 데이터를 재구축한다.
        - 방법: 주요 데이터(공연 정보, 좌석 상태 등)에 대한 캐시 워밍업 스크립트를 실행한다.
        - 목표: 30분 이내에 캐시 히트율 90% 이상 회복
    - 장애 원인을 분석한다 (메모리 부족, 네트워크 이슈 등).
        - 방법: 시스템 로그, 네트워크 트래픽 로그, Redis 슬로우 로그 등을 종합적으로 분석한다.
        - 목표: 재발 방지를 위한 명확한 원인 파악 및 문서화

3. **장기 조치 (1주일 이내):**
    - Redis 클러스터를 구성하여 고가용성을 확보한다.
        - 방법: 최소 3대의 마스터 노드와 각각의 슬레이브 노드로 클러스터를 구성한다.
        - 목표: 단일 노드 장애 시에도 서비스 중단 없이 운영 가능한 환경 구축
    - 캐시 데이터 분산 저장 전략을 수립한다.
        - 방법: 데이터 특성에 따라 여러 Redis 인스턴스에 분산 저장하는 로직을 구현한다.
        - 목표: 캐시 서버 부하 분산 및 전체 처리량 3배 이상 증가

<br>

### 2.3 대기열 시스템 오작동

#### 시나리오:
- Redis 로 구현한 대기열 관리 시스템에 버그가 발생하여, 사용자들이 무작위로 대기열에서 튕겨나가거나, 대기 순서가 뒤바뀌는 현상이 발생한다. 
- 이로 인해 예매 시작 10분 만에 고객 불만이 급증한다.
- 일부 사용자들은 대기열을 우회하는 방법을 공유하기 시작하여 시스템 보안 문제도 문제가 생기는 시나리오다.

#### 징후:
- 고객 지원 센터로의 문의가 평소의 10배 수준으로 폭주한다 (분당 200건 이상).
- 대기열 시스템 로그에서 비정상적인 패턴이 발견된다 (순서 역전, 갑작스러운 세션 종료 등).
- 실시간 모니터링에서 대기열 이탈률이 정상치의 5배 이상으로 증가한다.

#### 대응 방안:

1. **즉시 조치 (30분 이내):**
    - 대기열 시스템을 일시적으로 정적 대기열로 전환한다.
        - 방법: 미리 준비된 정적 HTML 페이지로 대기열 페이지를 대체한다.
        - 목표: 추가적인 시스템 오류 방지 및 상황 안정화
    - 모든 사용자에게 현재 상황과 예상 대기 시간을 안내한다.
        - 방법: 팝업 메시지, SMS, 이메일을 통해 일괄 안내한다.
        - 목표: 사용자 불안 감소 및 무분별한 재접속 시도 방지

2. **단기 조치 (4시간 이내):**
    - 버그 원인을 파악하고 긴급 패치를 적용한다.
        - 방법: 핫픽스 배포를 수행한다.
        - 목표: 근본적인 문제 해결 및 시스템 안정화
    - 영향받은 사용자들에게 보상 정책을 수립하고 안내한다.
        - 방법: 예매 우선권 부여, 할인 쿠폰 제공 등의 보상책을 마련한다.
        - 목표: 고객 신뢰 회복 및 부정적 여론 완화
    - 대기열 데이터를 복구하고 정상화한다.
        - 방법: 백업 데이터를 활용하여 사용자의 원래 대기 순서를 복원한다.
        - 목표: 공정성 논란 해소 및 시스템 신뢰도 회복

3. **장기 조치 (1-2주 이내):**
    - 스트레스 테스트 및 시뮬레이션을 강화한다.
        - 방법: 다양한 장애 시나리오에 대한 정기적인 모의훈련을 실시한다.
        - 목표: 유사 상황 재발 방지 및 대응 능력 향상
    - 실시간 모니터링 및 알림 시스템을 고도화한다.
        - 방법: 실시간 모니터링 시스템과 이상 징후 탐지 시스템을 도입한다.
        - 목표: 장애 조기 감지 및 선제적 대응 체계 구축

<br>

### 2.4 애플리케이션 서버 메모리 누수

#### 시나리오:
- 애플리케이션 서버에서 메모리 누수가 발생하여, 시간이 지날수록 서버의 응답 속도가 느려지고 최종적으로 OutOfMemoryError가 발생한다. 
- 이로 인해 서버가 주기적으로 재시작되며, 서비스의 안정성이 크게 저하된다. 
- 특히 피크 시간대에 서버 다운이 빈번히 발생하여 사용자 경험이 악화된다.

#### 징후:
- 서버 메모리 사용량이 시간에 따라 지속적으로 증가한다 (24시간 내 50% 이상 증가).
- 가비지 컬렉션 빈도 및 소요 시간이 정상치의 3배 이상으로 증가한다.
- 주기적인 서버 응답 지연 및 재시작이 발생한다 (4시간마다 한 번씩).
- 애플리케이션 로그에서 OutOfMemoryError 오류가 빈번히 관찰된다.

#### 대응 방안:

1. **즉시 조치 (30분 이내):**
    - 영향받는 서버 인스턴스를 식별하고 재시작한다.
        - 방법: 모니터링 툴을 통해 문제 서버를 식별하고 롤링 재시작을 수행한다.
        - 목표: 즉각적인 서비스 안정화 및 사용자 영향 최소화
    - 서버 인스턴스를 추가하여 부하를 분산한다.
        - 방법: 자동 스케일링 그룹에 새로운 인스턴스를 추가한다.
        - 목표: 단기적 안정성 확보 및 사용자 경험 개선
    - 힙 덤프를 생성하고 보관한다.
        - 방법: jmap 명령어나 JVM 옵션을 통해 힙 덤프를 생성한다.
        - 목표: 추후 상세 분석을 위한 데이터 확보

2. **단기 조치 (12시간 이내):**
    - 임시 패치를 적용한다 (문제 있는 코드 경로 우회).
        - 방법: 식별된 문제 영역에 대해 임시 방어 로직을 구현하고 배포한다.
        - 목표: 추가적인 메모리 누수 방지 및 서비스 안정화
    - JVM 파라미터를 튜닝하여 가비지 컬렉션을 최적화한다.
        - 방법: 힙 크기 조정, GC 로깅 활성화 등의 튜닝을 수행한다.
        - 목표: 메모리 관리 효율성 향상 및 GC 부하 감소

3. **장기 조치 (1-2주 이내):**
    - 근본적인 메모리 누수 문제를 해결하고 패치를 배포한다.
        - 방법: 개발팀과 협력하여 문제 코드를 수정하고, 철저한 테스트 후 배포한다.
        - 목표: 메모리 누수 완전 해결 및 시스템 안정성 회복
    - 주기적인 성능 테스트 및 메모리 프로파일링을 도입한다.
        - 방법: 월 1회 이상 전체 시스템에 대한 성능 테스트 및 메모리 프로파일링을 실시한다.
        - 목표: 잠재적 문제의 조기 발견 및 지속적인 시스템 최적화

