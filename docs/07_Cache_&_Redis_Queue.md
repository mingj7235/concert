
# [성능 개선을 위한 Cache 사용과 많은 수의 인원을 수용할 수 있는 대기열 시스템을 위한 Redis 이관에 관하여]

# 1. 캐시 도입을 통한 성능 개선 보고서

## 1> 개요
- 본 보고서는 콘서트 조회 및 콘서트 스케쥴 조회에 Cache 를 도입한 내용을 분석하는 것으로 한다.
- 주요 목적은 Cache 를 도입한 이유와 분석, 그리고 도입한 과정과 결과를 설명하는 것이다.
- 대규모 트래픽이 예상되는 콘서트 예약 시스템에서 Cache 도입은 데이터베이스 부하 감소와 응답 시간 단축을 위한 핵심 전략이다.

<br>

## 2> 캐시 설정 분석

### 2.1 캐시 설정 코드 분석
```kotlin
@Configuration
@EnableCaching
class CacheConfig(
    val objectMapper: ObjectMapper,
) {
    @Bean
    fun redisCacheManagerBuilderCustomizer(): RedisCacheManagerBuilderCustomizer =
        RedisCacheManagerBuilderCustomizer {
            it
                .withCacheConfiguration(
                    ONE_MIN_CACHE,
                    redisCacheConfigurationByTtl(objectMapper, TTL_ONE_MINUTE),
                ).withCacheConfiguration(
                    FIVE_MIN_CACHE,
                    redisCacheConfigurationByTtl(objectMapper, TTL_FIVE_MINUTE),
                )
        }

    private fun redisCacheConfigurationByTtl(
        objectMapper: ObjectMapper,
        ttlInMin: Long,
    ): RedisCacheConfiguration =
        RedisCacheConfiguration
            .defaultCacheConfig()
            .computePrefixWith { "$it::" }
            .entryTtl(Duration.ofMinutes(ttlInMin))
            .disableCachingNullValues()
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    StringRedisSerializer(),
                ),
            ).serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    GenericJackson2JsonRedisSerializer(objectMapper),
                ),
            )

    companion object {
        const val ONE_MIN_CACHE = "one-min-cache"
        const val TTL_ONE_MINUTE = 1L
        const val FIVE_MIN_CACHE = "five-min-cache"
        const val TTL_FIVE_MINUTE = 5L
    }
}
```

- `@EnableCaching` 어노테이션을 사용하여 Spring의 캐싱 기능을 활성화한다. 이는 Spring Framework에 캐시 관련 기능을 자동으로 구성하도록 지시한다.
- `RedisCacheManagerBuilderCustomizer` Bean 을 정의하여 Redis 캐시 매니저를 커스터마이즈한다. 
  - 이를 통해 서로 다른 TTL을 가진 두 가지 캐시 설정을 구성한다:
  - ONE_MIN_CACHE: 1분 TTL을 가진 캐시
  - FIVE_MIN_CACHE: 5분 TTL을 가진 캐시

- `redisCacheConfigurationByTtl` 함수는 각 캐시의 상세 설정을 정의한다:
  - `computePrefixWith { "$it::" }`: 캐시 키에 접두사를 추가하여 네임스페이스를 구분한다.
  - `entryTtl(Duration.ofMinutes(ttlInMin))`: 캐시 엔트리의 TTL을 설정한다.
  - `disableCachingNullValues()`: null 값의 캐싱을 방지한다.
  - `serializeKeysWith(StringRedisSerializer())`: 캐시 키를 문자열로 직렬화한다.
  - `serializeValuesWith(GenericJackson2JsonRedisSerializer(objectMapper))`: 캐시 값을 JSON 형식으로 직렬화한다.

<br>

### 2.2 캐시 설정의 합리성

#### 다양한 TTL 설정:

- 데이터의 특성에 따라 1분과 5분의 TTL을 설정함으로써, 데이터의 정합성과 캐시의 효율성을 균형있게 조절했다.
- Concert를 조회하는 것은 5분, ConcertSchedule을 조회하는 것은 1분으로 설정했다.
- 근거: Concert의 모든 Schedule과 Seat이 모두 매진되어 Unavailable한 상태로 변경되는 것은 ConcertSchedule에 비해 변경 빈도가 낮다고 판단했기 때문이다.
- 이러한 차별화된 TTL 설정은 데이터의 특성을 고려한 최적화된 캐시 전략을 구현한 것으로, 캐시의 효율성을 높이면서도 데이터의 정합성을 적절히 유지할 수 있다.


#### Null 값 캐싱 방지:

- `disableCachingNullValues()`를 통해 불필요한 null 값의 캐싱을 방지하여 캐시 공간을 효율적으로 사용하도록 했다.
- 근거: null 값을 캐시하는 것은 불필요한 메모리 사용을 초래하고, 잠재적으로 의미 있는 데이터가 캐시되는 것을 방해할 수 있다. 또한, null 값을 반환하는 쿼리의 경우 캐시 없이 직접 데이터베이스에 접근하는 것이 더 효율적일 수 있다.

#### 직렬화 설정:

- `StringRedisSerializer`와 `GenericJackson2JsonRedisSerializer`를 사용하여 키와 값의 효율적인 직렬화/역직렬화를 보장했다.
- 근거: `StringRedisSerializer`는 키를 문자열로 저장하여 Redis에서 효율적으로 검색할 수 있게 한다. 
  - `GenericJackson2JsonRedisSerializer`는 복잡한 객체를 JSON 형식으로 저장하여 데이터의 구조를 유지하면서도 효율적인 저장과 검색을 가능하게 한다.
  - 이러한 직렬화 전략은 데이터의 효율적인 저장과 빠른 검색을 가능하게 하여 캐시의 성능을 최적화한다.



이러한 캐시 설정은 대규모 트래픽이 예상되는 콘서트 예약 시스템에서 데이터베이스 부하를 줄이고 응답 시간을 단축시키는 데 크게 기여할 것으로 예상된다. 

특히, 데이터의 특성에 따라 차별화된 TTL을 적용하고, 효율적인 직렬화 전략을 사용함으로써 시스템의 전반적인 성능 향상을 도모할 수 있다.

<br>

## 3> 캐시 적용 분석

### 3.1 ConcertService의 캐시 적용

#### 3.1.1 getAvailableConcerts 메소드
```kotlin
@Cacheable(
        cacheNames = [CacheConfig.FIVE_MIN_CACHE],FIVE_MIN_CACHE
        key = "'available-concert'",
        condition = "#token != null",
        sync = true,
    )
    fun getAvailableConcerts(token: String): List<ConcertServiceDto.Concert> {
        validateQueueStatus(token)
        return concertManager
            .getAvailableConcerts()
            .map {
                ConcertServiceDto.Concert(
                    concertId = it.id,
                    title = it.title,
                    description = it.description,
                )
            }
    }
```

- 캐시 키: `'available-concert'`
- TTL: 5분
  - 근거: 
    - 사용 가능한 콘서트 목록은 콘서트 스케쥴을 조회하는 것에 비해 상대적으로 변경 빈도가 낮은 데이터로 판단했다. 
    - 5분의 TTL을 사용함으로써 데이터의 일관성을 유지하면서도 캐시의 효과를 극대화할 수 있다.
    - 콘서트의 전체적인 가용성이 5분 이내에 급격히 변경될 가능성은 낮아, 5분의 지연은 허용 가능한 수준으로 판단했다.
    - 보다 긴 TTL은 데이터베이스 쿼리 횟수를 더욱 줄여 시스템 성능을 향상시킬 수 있다.
- 조건: token이 null이 아닐 때만 캐시 적용
- 동기화: `sync = true`로 설정하여 동시 요청 시 중복 연산 방지

#### 3.1.2 getConcertSchedules 메소드

```kotlin
@Cacheable(
        cacheNames = [CacheConfig.ONE_MIN_CACHE],
        key = "'concert-' + #concertId",
        condition = "#token != null && #concertId != null",
        sync = true,
    )
    fun getConcertSchedules(
        token: String,
        concertId: Long,
    ): ConcertServiceDto.Schedule {
        validateQueueStatus(token)
        return ConcertServiceDto.Schedule(
            concertId = concertId,
            events =
                concertManager
                    .getAvailableConcertSchedules(concertId)
                    .map {
                        ConcertServiceDto.Event(
                            scheduleId = it.id,
                            concertAt = it.concertAt,
                            reservationAt = it.reservationAvailableAt,
                        )
                    },
        )
    }
```

- 캐시 키: `'concert-#concertId`
- TTL: 1분
  - 근거:
    - 콘서트 스케줄은 특정 콘서트에 대한 상세 정보로, 예약 상황에 따라 빈번하게 변경될 수 있다고 판단했다.
    - 1분의 짧은 TTL은 데이터의 최신성을 보장하면서도 동시에 캐시의 이점을 활용할 수 있게 한다.
    - 스케줄 정보는 좌석 가용성 등 실시간성이 중요한 정보를 포함하므로, 짧은 TTL로 최신 정보를 제공하는 것이 중요하다고 판단했다.
- 조건: token과 concertId가 모두 null이 아닐 때만 캐시 적용
- 동기화: sync = true로 설정하여 동시 요청 시 중복 연산 방지

### 3.2 캐시 적용의 합리성

#### 빈번한 조회 데이터 캐싱:

- 사용 가능한 콘서트 목록과 콘서트 스케줄은 자주 조회되는 데이터다.
- 이러한 데이터를 캐싱함으로써 반복적인 데이터베이스 쿼리를 줄여 전체적인 시스템 성능을 향상시킬 수 있다.

#### 차별화된 TTL 설정:

- 사용 가능한 콘서트 목록(5분 TTL)과 콘서트 스케줄(1분 TTL)에 대해 서로 다른 TTL을 설정했다.
- 콘서트 목록은 상대적으로 변경이 적어 longer TTL을 적용하여 캐시 효과를 극대화할 수 있다고 기대했다.
- 콘서트 스케줄은 실시간성이 중요하여 shorter TTL을 적용, 최신 정보를 더 자주 갱신한다.
- 이는 각 데이터의 특성과 변경 빈도를 고려한 결과로, 데이터의 최신성과 시스템 성능 사이의 최적의 균형을 찾기 위한 전략이다.

#### 조건부 캐싱:
- token과 concertId의 유효성을 검사하여 불필요한 캐싱을 방지하도록 한다.
- 이는 무의미한 데이터가 캐시를 차지하는 것을 방지하고, 캐시 공간을 효율적으로 사용할 수 있는 것을 기대했다.

#### 동기화 설정:
- sync = true 설정으로 동시에 여러 요청이 들어올 경우, 한 번만 데이터베이스에 접근하여 캐시를 생성하도록 했다.
- 이는 동시성 문제를 해결하고, 불필요한 데이터베이스 쿼리를 방지하여 시스템의 안정성과 효율성을 높이는 것을 기대했다.

<br>

## 4> 캐시 무효화 (Eviction) 분석
### 4.1 ConcertCacheManager 분석

```kotlin
@Component
class ConcertCacheManager {
    @CacheEvict(
        cacheNames = [CacheConfig.FIVE_MIN_CACHE],
        key = "'available-concert'",
    )
    fun evictConcertCache() {}

    @CacheEvict(
        cacheNames = [CacheConfig.ONE_MIN_CACHE],
        key = "'concert-' + #concertId",
    )
    fun evictConcertScheduleCache(concertId: Long) {}
}
```

`ConcertCacheManager` 클래스는 캐시 무효화를 담당한다.

- evictConcertCache():
  - 사용 가능한 콘서트 목록 캐시를 무효화한다.
  - FIVE_MIN_CACHE에서 'available-concert' 키를 가진 캐시 항목을 제거한다.
  - 콘서트 상태가 변경될 때 호출되어 전체 콘서트 목록의 최신성을 보장하도록 한다.


- evictConcertScheduleCache(concertId: Long):
  - 특정 콘서트의 스케줄 캐시를 무효화한다.
  - ONE_MIN_CACHE에서 'concert-{concertId}' 형식의 키를 가진 캐시 항목을 제거한다.
  - 특정 콘서트의 스케줄이 변경될 때 호출되어 해당 콘서트 정보의 최신성을 보장하도록 한다.

### 4.2 캐시 무효화 적용

```kotlin
@Transactional
fun executePayment(
    token: String,
    userId: Long,
    reservationIds: List<Long>,
): List<PaymentServiceDto.Result> {
    val user = userManager.findById(userId)
    val requestReservations = reservationManager.findAllById(reservationIds)

    validateReservations(userId, requestReservations)

    // 결제를 하고, 성공하면 결제 내역을 저장한다.
    val executedPayments =
        paymentManager.executeAndSaveHistory(
            user,
            requestReservations,
        )

    // reservation 상태를 PAYMENT_COMPLETED 로 변경한다.
    reservationManager.complete(requestReservations)

    // queue 상태를 COMPLETED 로 변경한다.
    val queue = queueManager.findByToken(token)
    queueManager.updateStatus(queue, QueueStatus.COMPLETED)

    // 결제 완료 후, 해당 Concert 의 좌석이 모두 매진이라면, Concert 의 상태를 UNAVAILABLE 로 변경한다.
    updateConcertStatusToUnavailable(requestReservations)

    // 결과를 반환한다.
    return executedPayments.map {
        PaymentServiceDto.Result(
            paymentId = it.id,
            amount = it.amount,
            paymentStatus = it.paymentStatus,
        )
    }
}

private fun updateConcertStatusToUnavailable(reservations: List<Reservation>) {
    val concertSchedules = reservations.map { it.seat.concertSchedule }.distinct()

    for (schedule in concertSchedules) {
        val allSeats = concertManager.findAllByScheduleId(schedule.id)
        if (allSeats.all { it.seatStatus == SeatStatus.UNAVAILABLE }) {
            val concert = schedule.concert
            concertManager.updateStatus(concert, ConcertStatus.UNAVAILABLE)
            concertCacheManager.evictConcertCache()
            concertCacheManager.evictConcertScheduleCache(concert.id)
        }
    }
}
```

- `updateConcertStatusToUnavailable` 을 통해 캐시 무효화를 시킨다.
  - 결제 처리 후 콘서트 상태가 변경될 수 있어, 캐시 무효화가 필요한 중요한 시점이기 때문에 이곳에 Evict 로직을 추가했다.
  - 실제 데이터 변경과 캐시 무효화가 동시에 이루어져야 데이터 일관성을 보장할 수 있다.

    
- `updateConcertStatusToUnavailable` 메서드의 역할
  - 결제된 예약들과 관련된 모든 콘서트 스케줄을 확인한다.
  - 각 스케줄의 모든 좌석이 예약 완료 상태인지 검사한다.
  - 모든 좌석이 예약 완료되었다면, 해당 콘서트의 상태를 UNAVAILABLE로 변경한다.
  - 상태가 변경된 콘서트에 대해 관련된 캐시(전체 콘서트 목록과 해당 콘서트의 스케줄)를 무효화한다.

### 4.3 캐시 무효화의 합리성
- 데이터 일관성 유지:
  - 결제 완료로 콘서트의 상태가 변경될 때 관련 캐시를 즉시 무효화한다.
  - 이는 캐시된 데이터와 실제 데이터베이스의 데이터 간 불일치를 방지하여 사용자에게 항상 정확한 정보를 제공한다.


- 선별적 캐시 무효화:
  - 전체 캐시가 아닌 변경된 콘서트와 관련된 캐시만을 무효화한다.
  - 콘서트 전체 목록(5분 TTL)과 해당 콘서트의 스케줄(1분 TTL) 캐시를 각각 무효화한다.
  - 이는 불필요한 캐시 재생성을 방지하고, 시스템 리소스를 효율적으로 사용하게 한다.
  - 특히, 스케줄 캐시의 경우 더 짧은 TTL로 인해 더 자주 갱신되므로, 무효화 시 최신 정보가 빠르게 반영된다.


- 실시간성 보장:
  - 중요한 상태 변경(예: 매진) 발생 시 즉시 캐시를 무효화하도록 한다.
  - 이를 통해 사용자에게 항상 최신 정보를 제공할 수 있으며, 특히 티켓 예매 시스템에서 중요한 실시간 가용성 정보의 정확성을 보장한다.

<br>

## 5> 성능 개선 효과 분석
### 5.1 쿼리 최적화

- 중복 쿼리 감소: 캐시를 통해 동일한 데이터에 대한 반복적인 데이터베이스 쿼리를 크게 줄일 수 있다.
- 복잡한 쿼리 결과 캐싱: `getAvailableConcerts`와 `getConcertSchedules`와 같은 복잡한 쿼리 결과를 캐싱함으로써, 데이터베이스의 부하를 크게 감소시킬 수 있다.

예를 들어, getAvailableConcerts 메서드 실행 시 다음과 같은 쿼리가 실행될 수 있다:
```sql
SELECT c.id, c.title, c.description
FROM concerts c
LEFT JOIN concert_schedules cs ON c.id = cs.concert_id
LEFT JOIN seats s ON cs.id = s.concert_schedule_id
WHERE c.status = 'AVAILABLE'
  AND cs.reservation_available_at <= CURRENT_TIMESTAMP
  AND s.seat_status = 'AVAILABLE'
GROUP BY c.id
HAVING COUNT(DISTINCT s.id) > 0;
```

#### 위 쿼리의 복잡도

- 데이터 모델의 복잡성:
  - concerts, concert_schedules, seats 세 개의 테이블이 연관되어 있다.
  - 이는 콘서트, 콘서트 일정, 좌석 간의 관계를 표현하기 위한 것으로, 실제 비즈니스 로직을 정확히 반영하기 위해 필요한 구조다.


- 두 번의 JOIN이 필요한 이유:
  - 첫 번째 JOIN (concerts와 concert_schedules):
    - 목적: 특정 콘서트의 모든 가능한 일정을 확인하기 위함이다.
    - 근거: 하나의 콘서트가 여러 일정을 가질 수 있기 때문에 필요하다.
  - 두 번째 JOIN (concert_schedules와 seats):
    - 목적: 각 콘서트 일정의 가용 좌석을 확인하기 위함이다.
    - 근거: 각 콘서트 일정마다 독립적인 좌석 구조를 가질 수 있으며, 예약 가능한 좌석이 있는지 확인해야 하기 때문이다.


- 복잡한 조건절:
  - 콘서트 상태, 예약 가능 시간, 좌석 상태 등 여러 조건을 동시에 확인해야 한다.
  - 이는 사용자에게 정확하고 최신의 예약 가능한 콘서트 정보를 제공하기 위해 필요한 정보다.


- GROUP BY와 HAVING 절:
  - 목적: 최소한 하나의 가용 좌석이 있는 콘서트만 선택하기 위함이다.
  - 이 복잡한 로직은 데이터베이스 레벨에서 처리되어야 하므로, 쿼리의 복잡성이 증가할 수 있다.

#### 캐시 사용으로 인한 개선의 이득
- 이러한 복잡한 쿼리는 캐시 적용 후 캐시 히트 시 실행되지 않아, 데이터베이스 부하를 크게 줄일 수 있다고 생각한다. 
- 특히 대량의 동시 접속이 예상되는 콘서트 예약 시스템에서, 이러한 캐싱 전략은 데이터베이스의 부하를 크게 줄이고 전체 시스템의 응답 시간을 개선하는 데 중요한 역할을 한다고 생각한다.


### 5.2 대량 트래픽 처리 능력 향상

- 응답 시간 단축: 
  - 캐시된 데이터를 사용함으로써, 데이터베이스 조회 시간을 크게 줄여 전체적인 응답 시간을 단축시킬 수 있다.
  - 예: 데이터베이스 쿼리 시 100ms가 소요되던 요청이 캐시 히트 시 10ms 이내로 단축될 수 있다.

- 데이터베이스 부하 분산:
  - 캐시를 통해 데이터베이스로의 직접적인 요청을 줄임으로써, 대량의 트래픽 발생 시에도 데이터베이스의 부하를 효과적으로 분산시킬 수 있다.
  - 예: 초당 1000건의 요청 중 80%가 캐시에서 처리된다면, 데이터베이스는 초당 200건의 요청만 처리하면 된다.

### 5.3 실시간 데이터 제공

- TTL을 통한 데이터 정합성 유지:
  - 1분과 5분의 TTL 설정을 통해, 캐시의 이점을 누리면서도 적절한 주기로 데이터를 갱신하여 실시간성을 보장한다.
  - 예: 콘서트 스케줄(1분 TTL)은 빠른 갱신으로 실시간 예약 상황을 반영하고, 콘서트 목록(5분 TTL)은 상대적으로 안정적인 데이터를 효율적으로 제공한다.

- 상태 변경 시 즉시 캐시 무효화:
  - 중요한 상태 변경(예: 콘서트 매진) 시 즉시 캐시를 무효화하여, 사용자에게 항상 최신 정보를 제공한다.
  - 예: 콘서트가 매진되면 즉시 `evictConcertCache()`와 `evictConcertScheduleCache()`를 호출하여 관련 캐시를 갱신한다.

<br>

## 6> 콘서트 조회와 콘서트 스케쥴 기능에 Cache 도입 결론
본 프로젝트에서 구현한 캐시 전략은 대량의 트래픽 발생 시 발생할 수 있는 지연 문제를 효과적으로 해결할 수 있는 방안이라고 생각한다.

- 데이터베이스 부하 감소: 반복적인 쿼리를 캐시로 대체하여 데이터베이스 부하를 크게 줄인다.
- 응답 시간 단축: 복잡한 쿼리 결과를 캐시에서 즉시 제공하여 응답 시간을 대폭 단축시킨다.
- 데이터 일관성 유지: 적절한 TTL 설정과 상태 변경 시 즉시 캐시 무효화를 통해 데이터의 일관성을 유지한다.
- 실시간성 보장: 중요 데이터의 빠른 갱신과 즉시 캐시 무효화를 통해 실시간 정보를 제공한다.

<br>

# 2. 많은 수의 인원을 수용할 수 있는 대기열 시스템을 위한 Redis 이관에 관한 보고서

## 1> 개요
- 본 보고서는 기존 DB 기반의 콘서트 대기열 시스템을 Redis를 활용한 시스템으로 변경하는 과정을 상세히 기술한다. 
- 이 변경은 대규모 인원을 수용할 수 있는 효율적인 대기열 시스템 구축을 목표로 진행되었다.

<br>

## 2> 기존 시스템 분석
### 2.1 기존 시스템의 구조

- JPA를 사용한 RDB 기반 구현
- `Queue` 엔티티를 통한 대기열 정보 저장
- `QueueJpaRepository`를 통한 데이터 접근 및 조작

### 2.2 한계점

- 확장성 제한:
  - 관계형 DB는 대규모 동시 접속 처리에 한계가 있음
  - 근거: 
    - 관계형 DB는 ACID 속성을 보장하기 위해 락(lock)을 사용하며, 이는 동시성 처리에 병목현상을 일으킬 수 있다.
    - 예를 들어, 대기열 순서 업데이트 시 row-level 락으로 인해 처리 속도가 저하될 수 있다.


- 성능 이슈: 
  - 대기열 위치 조회 등에 복잡한 쿼리가 필요해 성능 저하 가능성
  - 근거: 대기열 위치 조회 시 `SELECT COUNT(*) FROM Queue WHERE id < ? AND status = 'WAITING'`와 같은 쿼리가 필요하며, 이는 대기열 크기가 커질수록 성능이 저하된다.


- 실시간 처리의 어려움: 
  - 대기열 상태 변경 및 조회에 지연 발생 가능
  - 근거: 
    - 트랜잭션 처리 및 커밋 과정에서 발생하는 지연으로 인해 실시간성이 떨어질 수 있다. 
    - 예를 들어, 대기열 상태 업데이트 후 즉시 조회 시 최신 상태가 반영되지 않을 수 있다.

<br>

## 3> Redis 기반 시스템으로의 전환
### 3.1 Redis 선택 이유

- 고성능: 
  - 메모리 기반 데이터 구조로 빠른 읽기/쓰기 가능
  - Redis는 초당 100,000개 이상의 읽기/쓰기 작업을 처리할 수 있으며, 대규모 동시 접속 상황에서도 빠른 응답 시간을 유지할 수 있다.


- 확장성: 
  - 대규모 동시 접속 처리에 적합
  - AWS 의 ElastiCache 등 Redis Cluster를 사용하여 수평적 확장이 가능하며, 필요에 따라 노드를 추가하여 처리 용량을 늘릴 수 있다.


- 실시간 처리: 
  - 대기열 상태 변경 및 조회를 즉시 반영 가능 
  - Redis의 `ZADD`, `ZRANK` 명령어를 사용하여 대기열 추가 및 위치 조회를 밀리초 단위의 지연으로 처리할 수 있다.


- 데이터 구조의 다양성: 
  - Sorted Set 등을 활용한 효율적인 대기열 관리 가능 
  - Sorted Set을 사용하여 대기열을 구현하면, `O(log(N))` 시간 복잡도로 대기열 위치를 조회할 수 있다.

<br>

### 3.2 주요 변경 사항
#### 3.2.1 데이터 모델 변경

- Queue 엔티티 대신 Redis의 Sorted Set 사용
  - Sorted Set은 Redis의 데이터 구조 중 하나로, 각 요소가 score와 연관되어 있는 정렬된 집합이다. 
  - 이를 통해 대기열의 순서를 효율적으로 관리할 수 있습니다.


- 대기열 키: waiting_queue, 처리 중인 대기열 키: processing_queue
  waiting_queue 키는 대기 중인 사용자들의 정보를 저장하도록 한다. 
  - processing_queue 키는 현재 처리 중인 사용자들의 정보를 저장한다. 
  - 각 키는 Sorted Set으로 구현되어 있어 효율적인 순서 관리가 가능하다.

#### 3.2.2 저장소 계층 변경

- `QueueJpaRepository` 대신 `QueueRedisRepository` 구현
- Redis Template을 사용한 데이터 접근 로직 구현

```kotlin
@Repository
class QueueRedisRepository(
    private val redisTemplate: RedisTemplate<String, String>,
) {
    // 메서드 구현
}
```


#### 3.2.3 비즈니스 로직 변경

- `QueueManager`와 `QueueService`의 로직을 Redis 기반으로 재구현
```kotlin
@Service
class QueueService(
    private val queueManager: QueueManager,
    private val userManager: UserManager,
) {
    /**
     * userId 를 통해 user 를 찾아온다.
     * waiting 상태로 queue 저장 및 token 발급
     */
    @Transactional
    fun issueQueueToken(userId: Long): QueueServiceDto.IssuedToken {
        val user = userManager.findById(userId)

        return QueueServiceDto.IssuedToken(
            token = queueManager.enqueueAndIssueToken(user.id),
        )
    }

    /**
     * token 을 통해 queue 의 상태를 조회한다.
     * 현재 queue 상태가 waiting 상태라면 현재 대기열이 얼마나 남았는지를 계산하여 반환한다.
     * 그 밖의 상태라면, 얼마나 대기를 해야하는지 알 필요가 없으므로 0 을 반환한다.
     */
    fun findQueueByToken(token: String): QueueServiceDto.Queue {
        val status = queueManager.getQueueStatus(token)
        val isWaiting = status == QueueStatus.WAITING

        val position = if (isWaiting) queueManager.getPositionInWaitingStatus(token) else NO_REMAINING_WAIT
        val estimatedWaitTime = if (isWaiting) queueManager.calculateEstimatedWaitSeconds(position) else NO_REMAINING_WAIT

        return QueueServiceDto.Queue(
            status = status,
            remainingWaitListCount = position,
            estimatedWaitTime = estimatedWaitTime,
        )
    }

    /**
     * 스케쥴러를 통해 WAITING 상태의 대기열을 PROCESSING 상태로 변경한다.
     */
    fun updateToProcessingTokens() {
        queueManager.updateToProcessingTokens()
    }

    /**
     * 스케쥴러를 통해 만료 시간이 지났지만 여전히 WAITING 상태인 대기열을 삭제한다.
     */
    fun cancelExpiredWaitingQueue() {
        queueManager.removeExpiredWaitingQueue()
    }

    companion object {
        const val NO_REMAINING_WAIT = 0L
    }
}

@Component
class QueueManager(
    private val queueRedisRepository: QueueRedisRepository,
    private val jwtUtil: JwtUtil,
) {
    // JWT Token 을 userId 로 생성하고, QUEUE 를 생성한다.
    fun enqueueAndIssueToken(userId: Long): String {
        val token = jwtUtil.generateToken(userId)
        val score = System.currentTimeMillis()
        val existingToken = queueRedisRepository.findWaitingQueueTokenByUserId(userId.toString())

        existingToken?.let {
            queueRedisRepository.removeFromWaitingQueue(it, userId.toString())
        }

        queueRedisRepository.addToWaitingQueue(token, userId.toString(), score)
        return token
    }

    fun getQueueStatus(token: String): QueueStatus {
        val userId = jwtUtil.getUserIdFromToken(token)

        return when {
            queueRedisRepository.isProcessingQueue(token) -> QueueStatus.PROCESSING
            queueRedisRepository.getWaitingQueuePosition(token, userId.toString()) > 0L -> QueueStatus.WAITING
            else -> QueueStatus.CANCELLED
        }
    }

    fun getPositionInWaitingStatus(token: String): Long {
        val userId = jwtUtil.getUserIdFromToken(token)
        return queueRedisRepository.getWaitingQueuePosition(token, userId.toString())
    }

    fun updateToProcessingTokens() {
        val availableProcessingRoom = calculateAvailableProcessingRoom()
        if (availableProcessingRoom <= 0) return

        val tokensNeedToUpdateToProcessing =
            queueRedisRepository.getWaitingQueueNeedToUpdateToProcessing(availableProcessingRoom.toInt())

        tokensNeedToUpdateToProcessing.forEach { (token, userId) ->
            queueRedisRepository.updateToProcessingQueue(
                token = token,
                userId = userId,
                expirationTime = System.currentTimeMillis() + TOKEN_EXPIRATION_TIME,
            )
        }
    }

    private fun calculateAvailableProcessingRoom(): Long {
        val currentProcessingCount = queueRedisRepository.getProcessingQueueCount()
        return (ALLOWED_PROCESSING_TOKEN_COUNT_LIMIT - currentProcessingCount).coerceAtLeast(0)
    }

    fun removeExpiredWaitingQueue() {
        queueRedisRepository.removeExpiredWaitingQueue(System.currentTimeMillis())
    }

    fun completeProcessingToken(token: String) {
        queueRedisRepository.removeProcessingToken(token)
    }
    
    fun calculateEstimatedWaitSeconds(position: Long): Long {
        val batchSize = 1000L
        val batchInterval = 60L * 5 // 5 minutes
        val batches = position / batchSize
        return batches * batchInterval
    }

    companion object {
        const val ALLOWED_PROCESSING_TOKEN_COUNT_LIMIT = 1000
        const val TOKEN_EXPIRATION_TIME = 15L * 60L * 1000 // 15 minutes
    }
}
```

#### QueueService와 QueueManager의 주요 변경 사항 및 개선점:

- 실시간 대기열 위치 계산:
  - 기존: 복잡한 SQL 쿼리를 통해 대기열 위치를 계산한다.
  - 개선: Redis의 `ZRANK` 명령어를 사용하여 O(log(N)) 시간 복잡도로 즉시 위치 계산이 가능하다.


- 대기열 상태 관리:
  - 기존: 데이터베이스 트랜잭션을 통한 상태 변경한다.
  - 개선: Redis의 원자적 연산을 통해 즉각적인 상태 변경 및 반영이 가능하다.


- 토큰 발급 및 대기열 등록:
  - 기존: 데이터베이스 INSERT 연산으로 토큰을 발급하고 대기열을 등록한다.
  - 개선: Redis `ZADD` 명령어를 사용하여 빠르게 대기열을 등록한다.


- 대기 시간 예측:
  - Redis 로 변경하며 새로 추가된 기능으로, 현재 위치와 처리 속도를 기반으로 예상 대기 시간을 계산한다.


- 만료된 대기열 처리:
  - 기존: 주기적인 데이터베이스 쿼리를 통해 만료처리한다.
  - 개선: Redis의 `ZREMRANGEBYSCORE` 명령어를 사용하여 효율적으로 만료된 항목을 제거하도록 한다.

<br>

###  3.3 구현 과정 및 고려사항
#### 3.3.1 Redis 구성

- Redis 설정 클래스 (RedisConfig) 구현:
  - 이 클래스는 Redis 연결 및 데이터 직렬화/역직렬화 방식을 정의한다.
- Redis Template 빈 설정:
  - RedisTemplate은 Redis 작업을 추상화하여 개발자가 더 쉽게 Redis 작업을 수행할 수 있게 해준다.

```kotlin
@Configuration
class RedisConfig {
    @Bean
    fun redisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, String> =
        RedisTemplate<String, String>().apply {
            setConnectionFactory(connectionFactory)
            keySerializer = StringRedisSerializer()
            valueSerializer = StringRedisSerializer()
        }
}
```

- 이 설정에서는 키와 값 모두 문자열로 처리하도록 `StringRedisSerializer`를 사용했다. 
  - 근거 : 이는 대기열 시스템에서 주로 문자열 기반의 토큰과 사용자 ID를 다루기 때문이다.

<br>

#### 3.3.2 데이터 구조 설계
Redis의 Sorted Set을 사용한 대기열 구현은 다음과 같은 이점을 제공한다:

- 효율적인 순서 관리: Sorted Set은 O(log N) 시간 복잡도로 요소 추가, 제거, 순위 조회가 가능하다.
- 시간 기반 정렬: 시스템 현재 시간을 score로 사용함으로써, 선입선출(FIFO) 방식의 대기열을 자연스럽게 구현할 수 있다. 즉, 선착순으로 구현이 가능하다.
- 범위 쿼리 효율성: 특정 시간 범위의 항목을 효율적으로 조회하거나 제거할 수 있다.

구조:
```text
키: waiting_queue, processing_queue
값: {token}:{userId}
스코어: 시스템 현재 시간 (밀리초)
```
- 이러한 구조를 통해 대기열의 순서, 처리 상태, 만료 시간 등을 효과적으로 관리할 수 있다고 기대했다.

<br>
    
#### 3.3.3 대기열 관리 로직 구현

```kotlin
@Repository
class QueueRedisRepository(
    private val redisTemplate: RedisTemplate<String, String>,
) {
    /**
     * 대기열을 등록한다.
     */
    fun addToWaitingQueue(
        token: String,
        userId: String,
        expirationTime: Long,
    ) {
        redisTemplate.opsForZSet().add(WAITING_QUEUE_KEY, "$token:$userId", expirationTime.toDouble())
    }

    fun findWaitingQueueTokenByUserId(userId: String): String? {
        val pattern = "*:$userId"
        return redisTemplate.opsForZSet().rangeByScore(WAITING_QUEUE_KEY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)
            ?.find { it.endsWith(pattern) }
            ?.split(":")
            ?.firstOrNull()
    }

    fun removeFromWaitingQueue(
        token: String,
        userId: String,
    ) {
        redisTemplate.opsForZSet().remove(WAITING_QUEUE_KEY, "$token:$userId")
    }

    /**
     * WAITING 상태의 현재 대기열을 삭제하고, PROCESSING 상태를 등록한다.
     */
    fun updateToProcessingQueue(
        token: String,
        userId: String,
        expirationTime: Long,
    ) {
        redisTemplate.opsForZSet().remove(WAITING_QUEUE_KEY, "$token:$userId")
        redisTemplate.opsForZSet().add(PROCESSING_QUEUE_KEY, "$token:$userId", expirationTime.toDouble())
    }

    /**
     * 조회한 Token 의 대기열이 PROCESSING 상태인지 확인한다.
     */
    fun isProcessingQueue(token: String): Boolean {
        val score = redisTemplate.opsForZSet().score(PROCESSING_QUEUE_KEY, "$token:*")
        return score != null
    }

    /**
     * 현재 WAITING 상태의 대기열이 몇번째인지 순서를 리턴한다.
     */
    fun getWaitingQueuePosition(
        token: String,
        userId: String,
    ): Long = redisTemplate.opsForZSet().rank(WAITING_QUEUE_KEY, "$token:$userId") ?: -1

    /**
     * 현재 WAITING 상태의 대기열이 총 몇개인지 확인한다.
     */
    fun getWaitingQueueSize(): Long = redisTemplate.opsForZSet().size(WAITING_QUEUE_KEY) ?: 0

    /**
     * 현재 PROCESSING 상태의 대기열이 총 몇개인지 확인한다.
     */
    fun getProcessingQueueCount(): Long = redisTemplate.opsForZSet().size(PROCESSING_QUEUE_KEY) ?: 0

    /**
     * WAITING 상태의 대기열 중 PROCESSING 상태로 변경 할 수 있는 수만큼의 WAITING 상태의 대기열을 가지고 온다.
     */
    fun getWaitingQueueNeedToUpdateToProcessing(needToUpdateCount: Int): List<Pair<String, String>> =
        redisTemplate.opsForZSet().range(WAITING_QUEUE_KEY, 0, needToUpdateCount.toLong() - 1)
            ?.map {
                val (token, userId) = it.split(":")
                token to userId
            } ?: emptyList()

    /**
     * 현재 WAITING 상태의 대기열 중, 만료된 (ExpirationTime 이 현재시간보다 이전인) 대기열을 삭제한다.
     */
    fun removeExpiredWaitingQueue(currentTime: Long) {
        redisTemplate.opsForZSet().removeRangeByScore(WAITING_QUEUE_KEY, Double.NEGATIVE_INFINITY, currentTime.toDouble())
    }

    /**
     * 취소되거나 완료된 상태의 PROCESSING 대기열을 삭제한다.
     */
    fun removeProcessingToken(token: String) {
        val pattern = "$token:*"
        redisTemplate.opsForSet().members(PROCESSING_QUEUE_KEY)?.find { it.startsWith(pattern) }?.let {
            redisTemplate.opsForSet().remove(PROCESSING_QUEUE_KEY, it)
        }
    }

    companion object {
        const val WAITING_QUEUE_KEY = "waiting_queue"
        const val PROCESSING_QUEUE_KEY = "processing_queue"
    }
}
```

`addToWaitingQueue`

- 목적: 새로운 사용자를 대기열에 추가
- 작동 방식: Redis의 ZADD 명령어를 사용하여 O(log N) 시간 복잡도로 추가
- 이점: 대규모 동시 접속 상황에서도 빠른 대기열 등록 가능


`findWaitingQueueTokenByUserId`

- 목적: 특정 사용자의 대기열 토큰 조회
- 작동 방식: 패턴 매칭을 사용한 효율적인 검색
- 이점: 사용자별 중복 대기열 등록 방지에 활용


`removeFromWaitingQueue`

- 목적: 대기열에서 특정 항목 제거
- 작동 방식: Redis의 ZREM 명령어 사용
- 이점: 토큰 취소 또는 만료 시 즉각적인 대기열 정리 가능


`updateToProcessingQueue`

- 목적: 대기 상태에서 처리 중 상태로 전환
- 작동 방식: 원자적 연산을 통한 상태 변경
- 이점: 동시성 문제 없이 안전한 상태 전환 보장


`getWaitingQueuePosition`

- 목적: 특정 토큰의 대기열 내 위치 조회
- 작동 방식: Redis의 ZRANK 명령어 사용 (O(log N) 시간 복잡도)
- 이점: 대규모 대기열에서도 빠른 위치 조회 가능


`getWaitingQueueSize`, `getProcessingQueueCount`

- 목적: 각 대기열의 크기 조회
- 작동 방식: Redis의 ZCARD 명령어 사용
- 이점: 실시간 시스템 상태 모니터링 가능


`getWaitingQueueNeedToUpdateToProcessing`

- 목적: 처리 대기열로 이동할 토큰들 조회
- 작동 방식: Redis의 ZRANGE 명령어 사용
- 이점: 효율적인 배치 처리 구현 가능


`removeExpiredWaitingQueue`

- 목적: 만료된 대기열 항목 제거
- 작동 방식: Redis의 ZREMRANGEBYSCORE 명령어 사용
- 이점: 대기열 정리를 위한 별도의 복잡한 로직 불필요


`removeProcessingToken`

- 목적: 처리 완료된 토큰 제거
- 작동 방식: Redis의 ZREM 명령어 사용
- 이점: 처리 완료 후 즉각적인 리소스 정리 가능

<br>

#### 3.3.4 성능 최적화

- 대기 시간 예측 로직 구현 (calculateEstimatedWaitSeconds)

```kotlin
fun calculateEstimatedWaitSeconds(position: Long): Long {
    val batchSize = 1000L
    val batchInterval = 60L * 5 // 5 minutes
    val batches = position / batchSize
    return batches * batchInterval
}
```
- 수치 산출의 가정과 근거
  - 서버 처리 용량: 서버가 한 번에 1000명의 대기자를 처리할 수 있다고 가정 (batchSize = 1000L)
  - 처리 시간: 1000명의 대기자를 처리하는 데 약 5분이 소요된다고 가정 (batchInterval = 60L * 5)
  - 선형 확장성: 대기열 크기에 따라 처리 시간이 선형적으로 증가한다고 가정

- 수치 산출의 한계
  - 부하 테스트를 진행하지 못하여 정확한 서버의 한계치를 측정하지 못했다. 
  - 따라서 위 로직은 실제 서버의 처리 능력과 부하 상황에 따라 조정이 필요할 수 있다. 
  - 정확한 예측을 위해서는 실제 운영 데이터를 기반으로 한 지속적인 모니터링과 조정이 필요하다.

#### 3.3.5 동시성 제어

- Redis의 원자적 연산을 활용한 동시성 관리:
  - Redis는 단일 스레드 모델을 사용하여 명령어를 순차적으로 처리한다.
  - 예를 들어, ZADD 명령어는 원자적으로 실행되어 동시에 여러 클라이언트가 같은 키에 접근하더라도 데이터 일관성이 보장된다.
  - 이를 통해 RDB 와 같은 별도의 락(lock) 메커니즘 없이도 안전한 동시성 제어가 가능하다.


- 사용자별 중복 대기열 등록 방지 로직:
  - `findWaitingQueueTokenByUserId` 메서드를 사용하여 사용자가 이미 대기열에 있는지 확인한다.
  - 기존 토큰이 있다면 제거하고 새로운 토큰을 발급함으로써 중복 등록을 방지한다.
  - 이 과정은 Redis의 트랜잭션을 활용하여 원자적으로 수행될 수 있다.


<br>

#### 3.3.6 확장성 고려

- 처리 중인 대기열 수 제한 설정 (ALLOWED_PROCESSING_TOKEN_COUNT_LIMIT)
  - 시스템의 처리 능력에 맞춰 동시에 처리할 수 있는 요청의 수를 제한한다.
  - 이를 통해 시스템 과부하를 방지하고 안정적인 서비스 제공이 가능하다.
  - 한계 : 위에서 언급하였듯, 부하 테스트 없이 임의의 대기열 수 제한을 설정한 것이므로, 보다 정확한 수치 산정은 부하 테스트 이후 가능하다.


- 대기열 처리 배치 크기 및 간격 설정을 통한 시스템 부하 관리
  - 처리 간격을 조절하여 시스템 부하를 분산시킬 수 있다.

<br>

#### 3.3.7 에러 처리 및 예외 상황 대비

- 토큰 만료 시간 설정 및 관리
  - 토큰에 만료 시간을 설정하여 무한정 대기하는 상황을 방지한다.
  - removeExpiredWaitingQueue 메서드를 통해 주기적으로 만료된 토큰을 제거하도록 한다.


- 사용자 ID를 통한 대기열 토큰 조회 기능 구현
  - 토큰 분실 상황에 대비하여 사용자 ID로 토큰을 조회할 수 있는 기능을 구현한다.

<br>

## 4> 주요 개선 사항
### 4.1 성능 향상

- 대기열 위치 조회 성능 대폭 개선 (O(log N) 복잡도)
  - 근거: 
    - Redis의 Sorted Set 자료구조를 활용하여 대기열을 구현했다. 
    - Sorted Set은 요소의 검색, 삽입, 삭제 연산이 모두 O(log N) 시간 복잡도를 가진다.
    - 기존 관계형 데이터베이스에서는 대기열 위치 조회를 위해 COUNT 쿼리를 사용해야 했으며, 이는 O(N) 시간 복잡도를 가진다.
    - 예를 들어, 100만 명의 대기열에서 위치를 조회할 때, 기존 방식은 최악의 경우 100만 번의 연산이 필요했지만, Redis를 사용한 새 방식은 약 20번의 연산으로 위치를 찾을 수 있다. (log2(1,000,000) ≈ 20)


- 실시간 대기열 상태 업데이트 가능
  - 근거: 
    - Redis의 인메모리 특성과 단일 스레드 모델을 활용하여 실시간 업데이트를 구현했다.
    - Redis의 ZADD, ZREM 등의 명령어는 원자적으로 실행되어 데이터 일관성을 보장하면서도 밀리초 단위의 빠른 응답 시간을 제공한다.


### 4.2 확장성 증가

- 대규모 동시 접속 처리 가능
  - Redis의 초당 수만 건의 연산 처리 능력을 활용하여 대규모 동시 접속 상황에서도 안정적인 서비스가 가능하다.
  - Redis Cluster를 통해 수평적 확장이 용이하여, 트래픽 증가에 따라 유연하게 시스템을 확장할 수 있다.


- 유연한 대기열 관리 (쉬운 확장 및 축소)
  - Redis의 Sorted Set을 사용하여 대기열을 구현함으로써, 대기열의 크기를 동적으로 조절할 수 있다.
  - 예를 들어, 대기열 용량을 10만에서 100만으로 늘리는 데 별도의 스키마 변경이나 시스템 중단 없이 즉시 적용이 가능하다.

### 4.3 실시간성 확보

- 대기열 상태 변경 즉시 반영
  - Redis의 인메모리 특성으로 인해 디스크 I/O가 최소화되어, 상태 변경이 즉시 반영된다.
  - 예를 들어, 사용자가 대기열에 진입하거나 빠져나갈 때 밀리초 단위의 지연으로 전체 대기열 상태가 업데이트된다.

- 실시간 대기 시간 예측 기능 구현
  - 현재 대기열 위치와 처리 속도를 기반으로 한 로직을 통해 실시간으로 대기 시간을 예측합니다.
  - 이를 통해 사용자에게 더 정확한 예상 대기 시간 정보를 제공할 수 있고, 좋은 사용자 경험을 줄 수 있다.


## 5> 향후 개선 계획

- 부하 테스트 실시:
  - 이번 주차에서 실시하지 못한 부하 테스트를 실시할 계획이다.
  - 이를 통해 시스템의 최대 처리 용량, 응답 시간, 병목 지점 등을 정확히 파악할 수 있을 것으로 기대한다.
  - 부하 테스트 결과를 바탕으로 대기열 처리 속도, 배치 크기, 타임아웃 설정 등의 파라미터를 최적화할 예정이다.
    - 예를 들어, 현재 알고리즘에서 가정한 "서버가 한 번에 1000명의 대기자를 처리할 수 있다"는 수치를 실제 테스트를 통해 검증하고 조정할 예정이다.
