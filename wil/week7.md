# 대량의 트래픽이 몰려올 때 나는 어떻게 해야하나? - (feat. Cache, 대기열 구현)

# 🌱 0. 들어가며

### '대량 트래픽' 은 무섭다.

이번 주차는 내 어플리케이션에 대량의 트래픽이 들어올 때 어떻게 대응할 것인가에 대해 고민했던 주였다.
내가 만든 어플리케이션에 갑자기 사람들이 마구 몰려온다고 생각해보자.
가장 먼저 든 생각은 무엇인가?

>오.. 내 프로젝트가 흥하나보군..!!

위와 같은 생각이 먼저 든다면, 축하한다. 🎉
이미 대량의 트래픽이 들어올 것을 미리 대비를 해놓은 탄탄하고 멋진 백엔드 엔지니어일 가능성이 높다!

하지만 만약 그것이 아니라면, 당신은 백엔드 엔지니어가 아닐 가능성이 높다.
백엔드 엔지니어인데 대비 없는 내 어플리케이션에 몰려오는 트래픽을 보고 마냥 기뻐한다면..
괜찮다.
이제부터 그 트래픽을 감당할 수 있는 방법을 아래에서 함께 배워나가면 된다.


> 망했다.. 내 개복치같은 프로그램은 금방 터져버리고 말거야.. 😢

나는 아마 위와 같은 생각을 먼저 할 것 같다.
대량의 트래픽을 감당해본 경험이 아직 없었고, 그것을 대응해본 경험도 거의 없다.
Cache 를 도입해본 적은 있으나, 기술적인 수준에서 도입을 해본 것에 그쳤었고,
대용량 트래픽에 대해 어떻게 대응할 것인가에 대한 고려를 해본 적이 없다.

이제, 대용량 트래픽을 대비해보자.


<br>


# 🍐 1. 7주차 항해 회고

## 그래서 이번 주차는 뭘 해야하나요?
![](https://velog.velcdn.com/images/joshuara7235/post/f8218abd-cbe6-45c8-a12b-09dc410e92d1/image.png)

이번 주에 집중적으로 공부했던 부분은 다음과 같다.

>#### 1. Cache 를 어떤 로직에, 어떻게 적용 시킬 것인가.
#### 2. 기존에 DB 로 구현한 대기열 시스템을 Redis 로 어떻게 설계하고 구현할 것인가.


위의 내용을 바탕으로, 보고서를 작성해야 했다.
Chapter3 에서는 공부를 하고, 공부한 내용을 코드에 적용을 시킨 후, 분석한 내용을 보고서 작성을 계속 요구했다.
확실히 저번 주도 그렇고, 코드로 구현하기 위해 고민했던 내용을 문서로 정리하니까 확실히 내 것이 되는 것 같았다.


### 1> Cache 를 도입해보자.

#### 1) Cache 설정

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

콘서트 대기열 어플리케이션에서 Cache 를 붙이는 곳은 두곳으로 생각했다.
'예약이 가능한 콘서트 리스트 조회' 와 '특정 날짜의 예약이 가능한 콘서트 일정 조회' 였다.
이 두 로직은 특성에 맞게 TTL 이 조금 다르게 조정이 되어야 한다고 생각했다.
그렇기 때문에, 서로 다른 TTL 설정을 가지도록 `RedisCacheManagerBuilderCustomizer` 를 정의하여 커스터마이징 했다.

> **다양한 TTL 설정**
- 데이터의 특성에 따라 1분과 5분의 TTL을 설정함으로써, 데이터의 정합성과 캐시의 효율성을 균형있게 조절했다.
- Concert를 조회하는 것은 5분, ConcertSchedule을 조회하는 것은 1분으로 설정했다.
- 근거: Concert의 모든 Schedule과 Seat이 모두 매진되어 Unavailable한 상태로 변경되는 것은 ConcertSchedule에 비해 변경 빈도가 낮다고 판단했기 때문이다.


> **Null 값 캐싱을 방지**
- `disableCachingNullValues()`를 통해 불필요한 null 값의 캐싱을 방지하여 캐시 공간을 효율적으로 사용하도록 했다.
- 근거: null 값을 캐시하는 것은 불필요한 메모리 사용을 초래하고, 잠재적으로 의미 있는 데이터가 캐시되는 것을 방해할 수 있다.
- 또한, null 값을 반환하는 쿼리의 경우 캐시 없이 직접 데이터베이스에 접근하는 것이 더 효율적일 수 있다.

> **직렬화 설정**
- `StringRedisSerializer`와 `GenericJackson2JsonRedisSerializer`를 사용하여 키와 값의 효율적인 직렬화/역직렬화를 보장했다


<br>

#### 2) Cache 를 적용해보자

위에서 이야기 했듯, 나는 Cache 를 두 로직에 적용을 시켰다.

**콘서트 조회**
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

- 사용 가능한 콘서트 목록은 콘서트 스케쥴을 조회하는 것에 비해 상대적으로 변경 빈도가 낮은 데이터로 판단했다.
- 5분의 TTL을 사용함으로써 데이터의 일관성을 유지하면서도 캐시의 효과를 극대화할 수 있다.
- 콘서트의 전체적인 가용성이 5분 이내에 급격히 변경될 가능성은 낮아, 5분의 지연은 허용 가능한 수준으로 판단했다.
- 보다 긴 TTL은 데이터베이스 쿼리 횟수를 더욱 줄여 시스템 성능을 향상시킬 수 있다.


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
- 콘서트 스케줄은 특정 콘서트에 대한 상세 정보로, 예약 상황에 따라 빈번하게 변경될 수 있다고 판단했다.
- 1분의 짧은 TTL은 데이터의 최신성을 보장하면서도 동시에 캐시의 이점을 활용할 수 있게 한다.
- 스케줄 정보는 좌석 가용성 등 실시간성이 중요한 정보를 포함하므로, 짧은 TTL로 최신 정보를 제공하는 것이 중요하다고 판단했다.


#### 3) Eviction 전략

캐시를 만료 시키는 방법은 크게 두가지가 존재한다.

첫째, 캐시에 설정한 만료시간이 다 되어서 자연스럽게 만료되도록 하는 것.
둘째, Eviction 을 사용하여 강제로 해당 캐시를 만료 시키는 것.

강제적으로 캐시를 무효화 시키는 로직이 필요했기에, Evction 을 시키는 코드가 필요했다.

**Eviction 설정**

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
- `ConcertCacheManager` 은, Cache 관련된 책임을 가진 클래스다.
- 해당 Bean 을 통해 Cache 를 만료시키는 로직을 수행하도록 했다.


**Cache 만료 적용**
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

    val executedPayments =
        paymentManager.executeAndSaveHistory(
            user,
            requestReservations,
        )

    reservationManager.complete(requestReservations)

    val queue = queueManager.findByToken(token)
    queueManager.updateStatus(queue, QueueStatus.COMPLETED)

    updateConcertStatusToUnavailable(requestReservations)

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
>`updateConcertStatusToUnavailable` 을 통해 캐시 무효화를 시킨다.
- 결제 처리 후 콘서트 상태가 변경될 수 있어, 캐시 무효화가 필요한 중요한 시점이기 때문에 이곳에 Evict 로직을 추가했다.
- 실제 데이터 변경과 캐시 무효화가 동시에 이루어져야 데이터 일관성을 보장할 수 있다.


#### Cache 를 통한 성능 개선 효과 분석

** 쿼리 최적화 **

Cache 를 사용하는 가장 큰 이유는, 동일한 데이터에 대한 반복적인 DB 쿼리를 크게 줄일 수 있기 때문이다.
내가 Cache 를 도입하기로 결정한 로직에서 복잡한 쿼리라고 생각한 조회 로직은 `getAvailableConcerts` 와 `getConcertSchedules` 였다.

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
위의 쿼리는 `getAvailableConcerts` 를 수행했을 때, 실행되는 쿼리다.

>데이터 모델의 복잡성:
- concerts, concert_schedules, seats 세 개의 테이블이 연관되어 있다.
- 이는 콘서트, 콘서트 일정, 좌석 간의 관계를 표현하기 위한 것으로, 실제 비즈니스 로직을 정확히 반영하기 위해 필요한 구조다. <br>

>두 번의 JOIN이 필요한 이유:
- 첫 번째 JOIN (concerts와 concert_schedules):
    - 목적: 특정 콘서트의 모든 가능한 일정을 확인하기 위함이다.
    - 근거: 하나의 콘서트가 여러 일정을 가질 수 있기 때문에 필요하다.
- 두 번째 JOIN (concert_schedules와 seats):
    - 목적: 각 콘서트 일정의 가용 좌석을 확인하기 위함이다.
    - 근거: 각 콘서트 일정마다 독립적인 좌석 구조를 가질 수 있으며, 예약 가능한 좌석이 있는지 확인해야 하기 때문이다.

>복잡한 조건절:
- 콘서트 상태, 예약 가능 시간, 좌석 상태 등 여러 조건을 동시에 확인해야 한다.
- 이는 사용자에게 정확하고 최신의 예약 가능한 콘서트 정보를 제공하기 위해 필요한 정보다.

>GROUP BY와 HAVING 절:
- 목적: 최소한 하나의 가용 좌석이 있는 콘서트만 선택하기 위함이다.
- 이 복잡한 로직은 데이터베이스 레벨에서 처리되어야 하므로, 쿼리의 복잡성이 증가할 수 있다.


위와 같이 복잡한 쿼리는 캐시 적용 후, DB 부하를 크게 줄일 수 있다고 판단했기에, 해당 로직에 Cache 를 도입했다.

<br>


### 2> 대기열을 Redis 로 이관해보자
기존에 DB 로 구현되어있던 대기열 로직을 모두 Redis 로 이관했다.

#### 저장소 계층 변경 - `QueueRedisRepository` 구현
- QueueJpaRepository 대신 QueueRedisRepository 구현
- Redis Template을 사용한 데이터 접근 로직 구현

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

#### 대기 시간 예측 로직 구현

```kotlin
fun calculateEstimatedWaitSeconds(position: Long): Long {
    val batchSize = 1000L
    val batchInterval = 60L * 5 // 5 minutes
    val batches = position / batchSize
    return batches * batchInterval
}
```
이번 주에 부하테스트를 진행할 여력이 없었다.
그렇기에, 특정 가정을 기반으로 위의 대기시간 예측 로직을 구현했다.

>수치 산출의 가정과 근거
- 서버 처리 용량: 서버가 한 번에 1000명의 대기자를 처리할 수 있다고 가정 (batchSize = 1000L)
- 처리 시간: 1000명의 대기자를 처리하는 데 약 5분이 소요된다고 가정 (batchInterval = 60L * 5)
- 선형 확장성: 대기열 크기에 따라 처리 시간이 선형적으로 증가한다고 가정


#### 성능 향상

대기열에 대한 구현을 DB 에서 Redis 로 구현을 변경하면서 성능이 대폭 개선되었다.
- Redis의 `Sorted Set` 자료구조를 활용하여 대기열을 구현.
- `Sorted Set`은 요소의 검색, 삽입, 삭제 연산이 모두 `O(log N)` 시간 복잡도를 가진다.
- 기존 관계형 데이터베이스에서는 대기열 위치 조회를 위해 COUNT 쿼리를 사용해야 했으며, 이는 `O(N)` 시간 복잡도를 가진다.
- 예를 들어, 100만 명의 대기열에서 위치를 조회할 때, 기존 방식은 최악의 경우 100만 번의 연산이 필요했지만, Redis를 사용한 새 방식은 약 20번의 연산으로 위치를 찾을 수 있다. `(log2(1,000,000) ≈ 20)`


**전체적인 보고서 내용은 [이곳](https://github.com/mingj7235/concert/blob/main/docs/07_Cache_%26_Redis_Queue.md) 에서 확인할 수 있다.**
- 위의 블로그 내용보다 더 자세하고 상세하게 분석한 보고서의 내용이다.

<br>

# 🍇 2. 이번 주차에 난 무엇을 얻었나

### 코치님의 극찬을 받다 🥳

![](https://velog.velcdn.com/images/joshuara7235/post/1755be8c-4a16-4fc7-b604-9579fde7d576/image.png)

직장을 다니면서 항해를 소화하는 것은 힘들다.
퇴근 후, 밤늦게까지 과제를 해내는 것도 정말 힘들다.
주말에 발제를 듣고, 회고를 하며 지속적으로 글을 쓰는것도 정말 정말 힘들다.
물론, 당연히 배우고 성장하는 기쁨은 별개다.
힘든건 힘든거다!!!! 🤮

그런데, 그 힘든 것을 보상해주는 것 중 하나가, **'내가 열심히 시간을 갈아서 제출한 과제가 인정을 받았을 때'** 인 것 같다.
지금까지 항해플러스를 진행하면서 제출한 것이 '명예의 전당'에도 올라봤고, '우수' 도 몇번 받아봤지만,
이번 과제는 더 뭔가 특별했다.

코치님께 '최고' 라는 극찬을 받았고, 발제 강의중에서도 몸둘바를 모를 정도로 많은 칭찬을 해주셨다.

이 맛에 최선을 다해 과제를 하는 것 같다.

지금까지 ALL PASS!!!!! 🥳


### 트래픽 대응에 대해 찐하게 고민해보다.

보고서를 읽어보면 알겠지만, 정말 고민을 많이했고, 고민의 결과를 갈아 넣었다.
그 과정을 통해 Cache 와 Redis 에 대해 많이 공부를 했던 것 같다.
무엇보다 가장 좋았던 것은, '왜?' 라는 질문을 스스로에게 많이 했고, 그것에 대한 답을 계속 찾아갔었다.
그러다보니, 보고서 내용의 많은 부분이 '근거' 에 대한 내용이다.
공부하면서 나 스스로 이해하고 납득했던 부분을 적다보니 각 내용 별로 근거를 나열한 것들이 많았다.
그리고, 개인적으로 그 부분이 이번 과제를 통해 얻은 큰 수확이었던 것 같다.




<br>


# 🙏🏻 3. 글을 마치며

8월이 되었고, 벌써 항해플러스 7주차가 마무리 되었다.
항해를 하지 않았을 때는 어떻게 살았었는지 기억이 나지 않을 정도로, 이젠 퇴근 후 공부, 주말에 회고가 생활이 되었다.
(물론, 이게 안힘들다는 것은 당연히 아니다😇)
8주차는 Chapter3 의 마지막 부분이고, 이벤트 드리븐 기반의 설계를 고민하고 분석하는 것이다.
지금까지 해왔던 것처럼, 이번 주도 화이팅...!!



### 지난 회고 보러가기
1주차 회고 - [테스트코드를 모르던 내게 찾아온 TDD](https://velog.io/@joshuara7235/%ED%85%8C%EC%8A%A4%ED%8A%B8%EC%BD%94%EB%93%9C%EB%A5%BC-%EB%AA%A8%EB%A5%B4%EB%8D%98-%EB%82%B4%EA%B2%8C-%EC%B0%BE%EC%95%84%EC%98%A8-TDD)
2주차 회고 - [코딩에 정답을 찾지말자. 고민을 통해 더 나아짐을 시작하자.](https://velog.io/@joshuara7235/%EC%BD%94%EB%94%A9%EC%97%90-%EC%A0%95%EB%8B%B5%EC%9D%84-%EC%B0%BE%EC%A7%80%EB%A7%90%EC%9E%90.-%EA%B3%A0%EB%AF%BC%EC%9D%84-%ED%86%B5%ED%95%B4-%EB%8D%94-%EB%82%98%EC%95%84%EC%A7%90%EC%9D%84-%EC%8B%9C%EC%9E%91%ED%95%98%EC%9E%90)
3주차 회고 - [좋은 코드를 위해서는 좋은 설계가 우선되어야 한다.](https://velog.io/@joshuara7235/%EC%A2%8B%EC%9D%80-%EC%BD%94%EB%93%9C%EB%A5%BC-%EC%9C%84%ED%95%B4%EC%84%9C%EB%8A%94-%EC%A2%8B%EC%9D%80-%EC%84%A4%EA%B3%84%EA%B0%80-%EC%9A%B0%EC%84%A0%EB%90%98%EC%96%B4%EC%95%BC-%ED%95%9C%EB%8B%A4)
4주차 회고 - [어플리케이션은 완벽할 수 없다. 다만 완벽을 지향할 뿐.](https://velog.io/@joshuara7235/%EC%96%B4%ED%94%8C%EB%A6%AC%EC%BC%80%EC%9D%B4%EC%85%98%EC%9D%80-%EC%99%84%EB%B2%BD%ED%95%A0-%EC%88%98-%EC%97%86%EB%8B%A4.-%EB%8B%A4%EB%A7%8C-%EC%99%84%EB%B2%BD%EC%9D%84-%EC%A7%80%ED%96%A5%ED%95%A0-%EB%BF%90)
5주차 회고 - [항해의 중간지점, 나는 얼마나 성장했나.](https://velog.io/@joshuara7235/%ED%95%AD%ED%95%B4%EC%9D%98-%EC%A4%91%EA%B0%84%EC%A7%80%EC%A0%90-%EB%82%98%EB%8A%94-%EC%96%BC%EB%A7%88%EB%82%98-%EC%84%B1%EC%9E%A5%ED%96%88%EB%82%98
6주차 회고 - [동시성 문제를 극복해보자 - (feat. DB 락과 Redis 분산락)](https://velog.io/@joshuara7235/%EB%8F%99%EC%8B%9C%EC%84%B1-%EB%AC%B8%EC%A0%9C%EB%A5%BC-%EA%B7%B9%EB%B3%B5%ED%95%B4%EB%B3%B4%EC%9E%90-feat-DB-%EB%9D%BD%EA%B3%BC-Redis-%EB%B6%84%EC%82%B0%EB%9D%BD)


### 항해에 관심이 있으시다구요?

항해플러스에서 벌써 백엔드 6기 모집이 시작된다고해요. ~~(내가 벌써 선배..?)~~
제 회고글을 모두 읽어 보신 분들은 잘 아시겠지만, 이 과정을 통해 정말 많은 것을 누리고, 배우고, 경험하고, 느끼고 있습니다.

솔직히 말씀드리면, 이 과정은 마냥 즐겁지는 않아요.
고통스럽고, 힘들고, 많이 지칩니다. 😔

더군다나 직장을 다니면서 병행한다면 잠을 포기하고 시간을 많이 갈아 넣어야해요.
하지만, 지금 열심히 항해중인 제가 감히 자신있게 말씀드리자면, 이 과정을 통해 지금까지 경험하지 못했던 압축된 성장을 경험할 수 있습니다.

혹시, 관심이 있으시다면 [지원하실 때](https://hhplus-hub.oopy.io/) 추천인 코드(**HHPGS0893**)를 작성해주신다면 할인이 된다고 해요 ㅎㅎ
고민되시는 분은, 댓글로 달아주시면 커피챗을 통해 이야기 해도 좋을 것 같습니다.

성장을 위해 시간을 쏟을 준비가 되신 주니어 분들에게 정말 진심을 다해 추천합니다.