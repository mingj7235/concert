# 동시성 문제를 극복해보자 - (feat. DB 락과 Redis 분산락)

# 🌱 0. 들어가며

### Chapter3 시작

6주차에 대한 회고를 이제야 쓴다.
2주가 지난 시점에서 회고하는 것이라 많이 늦었지만,
이번 주차까지 진행했던 항해 발제 내용을 통해 얻은 인사이트가 더해져서 연속성 있는 회고를 쓸 수 있을 것 같아 오히려 좋다고 생각했다.

6주차는 Chapter3 가 본격적으로 시작하는 주였다.
Chapter 3의 주된 내용은 Chapter2 를 통해 완성한 서버가 동시성 이슈와 대량의 트래픽을 소화해낼 수 있도록 설계와 구조를 변경해나가는 과정을 담는다.

### 동시성 문제와 극복
6주차의 주제는 Chapter3 의 큰 축중 하나인 '동시성 문제' 에 대해 중점을 두었다.

백엔드 엔지니어의 주요 역량중 하나인 동시성 문제.
이 문제를 해결하기 위해 6주차에서는 `DB 락`과 `Redis` 를 활용한 분산락을 내 어플리케이션에 어디에 어떻게 적용할 것인지 고민해야 했다.

저번 5주차 회고에서도 이야기 했지만 이 주제에 있어서 자신있게 대답할 수 없었다.
> 😃 : 동시성 이슈에 대해서 어떻게 해결할 수 있나요?
나: .....어... 동시성 이슈는 한 데이터에 여러 접근이 일어나는 거고.. 어.. 락을 걸면 되지 않을까요?

딱 위와 같이 대답할 수 있는 수준에서 이번 주차를 시작했다.
정말 중요한 내용이고 꼭 이해하고 자신있어야 하는 주제였지만, 결코 쉽지 않은 주제.
백엔드 면접에서 무조건 나오는 단골 질문인 동시성 문제.
이번 6주차에서 어떤 배움을 얻었고 성장을 경험했는지 정리해 보자.

<br>


# 🍐 1. 6주차 항해 회고

### 그래서 이번 주에는 무엇을 해야하나요?

![](https://velog.velcdn.com/images/joshuara7235/post/931fe88e-01bb-4ac3-81a9-156815a94b93/image.png)


보고서 작성..! 📜
지금까지는 주로 코드를 구현하고 코드를 작성하는 과제였다.
물론, Chapter2 에서 설계를 하면서 시퀀스다이어그램을 작성했었지만, 그것은 보고서라기보다 설계를 위한 내용이었다.

해야 할 내용을 정리하면 다음과 같다.
> #### TODO LIST
1. 개발한 어플리케이션의 어떤 유즈케이스에서 동시성 이슈가 발생 될 수 있는지 분석
2. 기존의 로직의 문제점은 무엇인지 분석
3. 동시성 이슈를 해결하기 위해 다양한 방법을 적용
4. 시도해본 내용을 바탕으로 가장 적절한 것을 선택하고 근거를 작성
5. 코드레벨로 실제로 구현하고 테스트를 작성
6. 위의 내용을 모두 정리하여 보고서 형태로 작성




<br>

### 내가 공부하고 배운것은 정리를 할 때 진짜 내 것이 된다.

![](https://velog.velcdn.com/images/joshuara7235/post/5584b3b1-ecd7-4cae-910d-83350997897b/image.png)

PR 에서 확인할 수 있듯이 보고서를 작성하기 위해 내 어플리케이션에 여러가지 시도를 했다.
이 내용을 바탕으로 보고서를 작성해 보자.


#### 1. 내 어플리케이션에서 어떤 녀석이 동시성 이슈가 날꼬? 🧐

> **좌석 예약 기능**
- 좌석 예약을 할 때, 동시에 여러명이 하나의 좌석을 두고 예약하려고 한다면 단 1명만이 그 좌석을 예약할 수 있어야 한다.
- 만약, 한 좌석을 여러명이 예약이 가능하다면 이 어플리케이션은.. 망한다.. ㅠ



>**잔액 충전 기능**
- 한 명의 유저가 자신의 잔액을 충전을 할 때, 실수로 여러번 다발적으로 호출했을 때 1회만 가능하도록 해야한다.
- 네트워크 지연 혹은 사용자의 실수로 충전버튼이 여러 번 클릭 된 경우 중복 충전이 발생하면 안된다.


내가 만든 콘서트 예약 시스템에서 동시성 이슈가 발생할 것 같은 기능은 위의 두 가지였다.
동시성 이슈를 제어함으로서 내가 기대하는 결과는 다음과 같다.

> ** 개선된 좌석 예약 기능**
- 특정 좌석에 대해 최초로 예약 요청을 완료한 사용자만 해당 좌석을 성공적으로 예약을 한다.
- 다른 사용자들의 동일 좌석 예약 시도는 실패하고, 적절한 오류 메시지를 받아야 한다.

> ** 개선된 잔액 충전 기능**
- 사용자가 여러 번 충전 요청을 보내더라도 단 한 번만 잔액이 증가해야 한다.
- 충전 금액은 정확히 한 번만 사용자의 계정에 반영되어야 하며, 금액 오차가 없어야 한다.

<br>

#### 2. 기존 로직의 한계는 무엇인가?

해결에 앞서서, 기존 내 로직은 어떻게 되어있는지 확인이 필요하다.
지금 보면.. 상당히 허술하고 비효율적이라고 느껴진다.
(그렇게 보인다는 것은, 내가 또 그만큼 성장했다는 것을 반증한다고 믿는다. 😇)


```kotlin
// 좌석 예약

@Service  
class ReservationService(  
    private val userManager: UserManager,  
    private val queueManager: QueueManager,  
    private val concertManager: ConcertManager,  
    private val reservationManager: ReservationManager,  
) {  
    @Transactional  
    fun createReservations(  
        token: String,  
        reservationRequest: ReservationServiceDto.Request,  
    ): List<ReservationServiceDto.Result> {  
        validateQueueStatus(token)  
        validateUser(reservationRequest.userId)  
        validateReservationRequest(  
            requestConcertId = reservationRequest.concertId,  
            requestScheduleId = reservationRequest.scheduleId,  
            requestSeatIds = reservationRequest.seatIds,  
        )  
  
        return reservationManager  
            .createReservations(reservationRequest)  
            .map {  
                ReservationServiceDto.Result(  
                    reservationId = it.id,  
                    concertId = reservationRequest.concertId,  
                    concertName = it.concertTitle,  
                    concertAt = it.concertAt,  
                    seat =  
                        ReservationServiceDto.Seat(  
                            seatNumber = it.seat.seatNumber,  
                            price = it.seat.seatPrice,  
                        ),  
                    reservationStatus = it.reservationStatus,  
                )  
            }  
    }

@Component  
class ReservationManager(  
    private val reservationRepository: ReservationRepository,  
    private val userRepository: UserRepository,  
    private val concertRepository: ConcertRepository,  
    private val concertScheduleRepository: ConcertScheduleRepository,  
    private val seatRepository: SeatRepository,  
) {  
    /**  
     * 1. Reservation 을 PaymentPending 상태로 생성한다.  
     * 2. 좌석 상태를 Unavailable 로 변경한다.  
     */    fun createReservations(reservationRequest: ReservationServiceDto.Request): List<Reservation> {  
        val user =  
            userRepository.findById(reservationRequest.userId)  
                ?: throw BusinessException.NotFound(ErrorCode.User.NOT_FOUND)  
        val concert =  
            concertRepository.findById(reservationRequest.concertId)  
                ?: throw BusinessException.NotFound(ErrorCode.Concert.NOT_FOUND)  
        val concertSchedule =  
            concertScheduleRepository.findById(reservationRequest.scheduleId)  
                ?: throw BusinessException.NotFound(ErrorCode.Concert.SCHEDULE_NOT_FOUND)  
        val seats = seatRepository.findAllById(reservationRequest.seatIds)  
  
        val reservations =  
            seats.map { seat ->  
                val reservation =  
                    Reservation(  
                        user = user,  
                        concertTitle = concert.title,  
                        concertAt = concertSchedule.concertAt,  
                        seat = seat,  
                        reservationStatus = ReservationStatus.PAYMENT_PENDING,  
                        createdAt = LocalDateTime.now(),  
                    )  
                reservationRepository.save(reservation)  
            }  
  
        seatRepository.updateAllStatus(reservationRequest.seatIds, SeatStatus.UNAVAILABLE)  
  
        return reservations  
    }	
```

```kotlin
// 잔액 충전
@Service  
class BalanceService(  
    private val balanceManager: BalanceManager,  
) {  
    @Transactional  
    fun recharge(  
        userId: Long,  
        amount: Long,  
    ): BalanceServiceDto.Detail {  
        if (amount < 0) throw BusinessException.BadRequest(ErrorCode.Balance.BAD_RECHARGE_REQUEST)  
  
        val rechargedBalance =  
            balanceManager.updateAmount(  
                userId = userId,  
                amount = amount,  
            )  
  
        return BalanceServiceDto.Detail(  
            userId = userId,  
            currentAmount = rechargedBalance.amount,  
        )  
    }

@Component  
class BalanceManager(  
    private val userRepository: UserRepository,  
    private val balanceRepository: BalanceRepository,  
) {  
    fun updateAmount(  
        userId: Long,  
        amount: Long,  
    ): Balance {  
        val user = userRepository.findById(userId) ?: throw BusinessException.NotFound(ErrorCode.User.NOT_FOUND)  
        return balanceRepository.findByUserId(user.id)?.apply {  
            updateAmount(amount)  
        } ?: balanceRepository.save(  
            Balance(  
                user = user,  
                amount = amount,  
                lastUpdatedAt = LocalDateTime.now(),  
            ),  
        )  
    }	
```

위의 코드는 개선 전의 좌석 예약과 잔액 충전 로직이다.

모두 Service 계층, 즉 Usecase 에서 `@Transactional` 을 통해 트랜잭션을 관리했다.
이렇게 로직 자체를 하나의 트랜잭션에 따 때려넣었던 나름의 이유는 다음과 같다.
>1. 원자성 보장: 하나의 트랜잭션 내에서 Service 레이어의 모든 로직이 원자성을 가지고 실행되어야 한다고 판단했다.
2. 단순성: 서비스 계층에 트랜잭션을 적용함으로써 모든 데이터베이스 연산이 하나의 트랜잭션으로 묶이도록 했다.
3. 일관성: 모든 비즈니스 로직이 하나의 트랜잭션 내에서 실행되므로, 데이터의 일관성을 유지하기 쉽다고 생각했다.

하지만, 위와 같이 로직을 구현한다면 다음의 문제점들이 생긴다.

> **기존 로직의 문제점**
**1. 트랜잭션 범위가 너무 넓음** : 서비스 계층의 메서드 전체가 하나의 트랜잭션으로 묶여 있어, 불필요하게 긴 시간 동안 데이터베이스 리소스를 점유할 수 있다.
**2. 동시성 제어의 어려움** : 넓은 트랜잭션 범위로 인해 동시에 여러 요청이 처리될 때 데드락이 발생하거나 성능이 저하될 수 있다.
**3.세밀한 제어의 부재** : 특정 연산에 대해서만 트랜잭션을 적용하거나, 다른 격리 수준을 설정하는 등의 세밀한 제어가 어렵다.
**4.성능 저하** : 모든 연산이 하나의 큰 트랜잭션으로 묶여 있어, 데이터베이스 연결이 오래 유지되면서 전반적인 시스템 성능이 저하될 수 있다.

<br>

#### 3. 동시성 이슈를 해결하기 위해 다양한 방법을 적용

위의 이슈를 해결하기 위해 나는 `DB 락`과 `Redis` 를 사용한 분산락을 시도해봤다.

우선 각각의 방법에서 공통적으로 적용한 것이 있는데, 그것은 '트랜잭션 범위를 축소' 한 것 이었다.
트랜잭션의 범위를 `Service` 에서 `Manager` 로 내려 더 작은 단위로 제어하도록 변경했다.

** DB 락 **

1. 낙관적 락
- 낙관적 락은 동시 업데이트가 드물게 발생한다는 가정 하에 동작한다.
- 이 방식은 데이터 수정 시 충돌이 발생하지 않을 것이라고 이름 그대로 '낙관적으로' 가정하고, 충돌이 발생했을 때 이를 감지하고 처리한다.
- 그렇기 때문에, 100회 이상의 동시 요청 테스트에서는 실패가 발생했다.
- 낙관적 락은 직접적으로 DB 에 락을 거는 방법이 아니기에 간단하고 효과적이지만, 높은 동시성 환경에서는 한계가 있음을 확인했다.


2. 비관적 락
- 비관적 락은 동시 업데이트가 빈번하게 발생할 것이라고 '비관적으로' 가정하고, 데이터를 읽는 시점에 락을 걸어 다른 트랜잭션의 접근을 차단한다.
- 이 방식은 데이터 무결성을 강하게 보장하지만, 동시성 처리 성능이 낮아질 수 있다.

```kotlin
@Lock(LockModeType.PESSIMISTIC_WRITE)  
@Query("SELECT s FROM Seat s WHERE s.id IN :seatIds")  
fun findAllByIdWithPessimisticLock(seatIds: List<Long>): List<Seat>
```

위와 같이 좌석을 찾아오는 쿼리에 비관적 락을 걸어서 적용시켰다.
```kotlin
@Test  
fun `1000개의 동시 예약 요청 중 하나만 성공해야 한다`() {  
    // Given  
    val startTime = System.nanoTime()  
    val concert =  
        concertRepository.save(  
            Concert(  
                title = "Test Concert",  
                description = "Test Description",  
                concertStatus = ConcertStatus.AVAILABLE,  
            ),  
        )  
    val schedule =  
        concertScheduleRepository.save(  
            ConcertSchedule(  
                concert = concert,  
                concertAt = LocalDateTime.now().plusDays(1),  
                reservationAvailableAt = LocalDateTime.now().minusHours(1),  
            ),  
        )  
    val seat =  
        seatRepository.save(  
            Seat(schedule, 1, SeatStatus.AVAILABLE, 10000),  
        )  
  
    val threadCount = 1000  
    val executorService = Executors.newFixedThreadPool(threadCount)  
    val latch = CountDownLatch(threadCount)  
  
    val successfulReservations = mutableListOf<ReservationServiceDto.Result>()  
    val failedReservations = mutableListOf<Throwable>()  
  
    // When  
    repeat(threadCount) { index ->  
        executorService.submit {  
            try {  
                val token = "test_token_$index"  
                val user = userRepository.save(User(name = "Test User$index"))  
                queueRepository.save(  
                    Queue(  
                        user = user,  
                        token = token,  
                        joinedAt = LocalDateTime.now(),  
                        queueStatus = QueueStatus.PROCESSING,  
                    ),  
                )  
  
                val reservationRequest =  
                    ReservationServiceDto.Request(  
                        userId = user.id,  
                        concertId = concert.id,  
                        scheduleId = schedule.id,  
                        seatIds = listOf(seat.id),  
                    )  
  
                val result = reservationService.createReservations(token, reservationRequest)  
                synchronized(successfulReservations) {  
                    successfulReservations.addAll(result)  
                }  
            } catch (e: Exception) {  
                synchronized(failedReservations) {  
                    failedReservations.add(e)  
                }  
            } finally {  
                latch.countDown()  
            }  
        }  
    }    latch.await()  
  
    val endTime = System.nanoTime()  
    val duration = Duration.ofNanos(endTime - startTime)  
  
    // Then  
    assertEquals(1, successfulReservations.size, "1개의 예약만 성공해야 합니다.")  
    assertEquals(999, failedReservations.size, "999개의 예약은 실패해야 합니다.")  
    assertTrue(failedReservations.all { it is BusinessException.BadRequest }, "실패한 예약들은 모두 BusinessException.BadRequest 예외여야 합니다.")  
  
    val updatedSeat = seatRepository.findById(seat.id)!!  
    assertEquals(SeatStatus.UNAVAILABLE, updatedSeat.seatStatus, "좌석 상태가 UNAVAILABLE로 변경되어야 합니다.")  
    println("테스트 실행 시간: ${duration.toMillis()} 밀리초")  
}
```

>![](https://velog.velcdn.com/images/joshuara7235/post/ff8cbf4f-4818-48d5-a4a2-013bb542ad3a/image.png)
- 1000번을 동시에 예약 요청을 했고, 1회의 요청만 성공하는 것을 확인했다.

- 확실히, 비관적락은 높은 동시성 환경에서도 데이터 무결성을 보장해줬다.
- 하지만, 동시에 처리할 수 있는 트랜잭션의 수가 제한되므로 높은 동시성 환경에서는 전체적인 시스템 처리량이 낮아질 수 있다.
- 그렇기에 데이터 정합성이 매우 중요하고 충돌이 자주 발생하는 환경에서 유용하다고 생각한다.

<br>

** Redis 를 이용한 분산 락 **

분산 락을 적용하기 위해, 커스텀 어노테이션과 함께 `AOP` 를 통해 구현했다.
![](https://velog.velcdn.com/images/joshuara7235/post/6906b581-a74d-4717-8072-90de3d6a537b/image.png)



1. Simple Lock

우선, 커스텀 어노테이션과 `Simple Lock` 을 `AOP` 를 통해 적용하기 위한 코드는 아래와 같이 구현했다.

```kotlin
@Target(AnnotationTarget.FUNCTION)  
@Retention(AnnotationRetention.RUNTIME)  
annotation class DistributedSimpleLock(  
    val key: String,  
    val waitTime: Long = 5,  
    val leaseTime: Long = 10,  
    val timeUnit: TimeUnit = TimeUnit.SECONDS,  
)

@Aspect  
@Component  
class DistributedSimpleLockAspect(  
    private val redisSimpleLock: RedisSimpleLock,  
) {  
    @Around("@annotation(com.hhplus.concert.common.annotation.DistributedSimpleLock)")  
    fun around(joinPoint: ProceedingJoinPoint): Any? {  
        val signature = joinPoint.signature as MethodSignature  
        val method = signature.method  
        val distributedLock = method.getAnnotation(DistributedSimpleLock::class.java)  
  
        val lockKey = distributedLock.key  
        val lockValue = UUID.randomUUID().toString()  
  
        try {  
            val acquired =  
                redisSimpleLock.tryLock(  
                    lockKey,  
                    lockValue,  
                    distributedLock.leaseTime,  
                    distributedLock.timeUnit,  
                )  
            if (!acquired) {  
                throw BusinessException.BadRequest(ErrorCode.Common.BAD_REQUEST)  
            }  
            return joinPoint.proceed()  
        } finally {  
            redisSimpleLock.releaseLock(lockKey, lockValue)  
        }  
    }  
}

@Component  
class RedisSimpleLock(  
    private val redisTemplate: RedisTemplate<String, String>,  
) {  
    fun tryLock(  
        key: String,  
        value: String,  
        leaseTime: Long,  
        timeUnit: TimeUnit,  
    ): Boolean =  
        redisTemplate  
            .opsForValue()  
            .setIfAbsent(key, value, leaseTime, timeUnit) ?: false  
  
    fun releaseLock(  
        key: String,  
        value: String,  
    ): Boolean {  
        val ops = redisTemplate.opsForValue()  
        val lockValue = ops.get(key)  
  
        if (lockValue == value) {  
            redisTemplate.delete(key)  
            return true  
        }  
        return false  
    }  
}
```
> Simple Lock 의 구현 <br>
어노테이션 정의
- `@DistributedSimpleLock` 어노테이션을 만들어 분산 락을 적용할 메서드를 지정한다. <br>
AOP를 이용한 락 적용
- `DistributedSimpleLockAspect` 클래스에서 어노테이션이 적용된 메서드 실행 전후로 락을 획득하고 해제한다.
- `'락 획득 → 트랜잭션 시작 → 비즈니스 로직 → 트랜잭션 종료(커밋 or 롤백) → 락 반납'` 순서로 동작하도록 의도했다. <br>
Redis를 이용한 락 구현
- `RedisSimpleLock` 클래스에서 `Redis`의 `setIfAbsent` 명령어를 이용해 락을 구현한다.
- 락 획득과 해제 로직을 제공한다.




위의 내용이 실제로 사용된 내용은 아래와 같다.

```kotlin
@Service  
class BalanceService(  
    private val balanceManager: BalanceManager,  
) {
    fun recharge(  
        userId: Long,  
        amount: Long,  
    ): BalanceServiceDto.Detail {  
        if (amount < 0) throw BusinessException.BadRequest(ErrorCode.Balance.BAD_RECHARGE_REQUEST)  
  
        return rechargeWithSimpleLock(  
            userId = userId,  
            amount = amount,  
        )  
    }  

	@DistributedSimpleLock(  
        key = "'user:' + #userId",  
        waitTime = 5,  
        leaseTime = 10,  
    ) 
    fun rechargeWithSimpleLock(  
        userId: Long,  
        amount: Long,  
    ): BalanceServiceDto.Detail {  
        val rechargedBalance =  
            balanceManager.updateAmount(  
                userId = userId,  
                amount = amount,  
            )  
  
        return BalanceServiceDto.Detail(  
            userId = userId,  
            currentAmount = rechargedBalance.amount,  
        )  
    }

@Component  
class BalanceManager(  
    private val userRepository: UserRepository,  
    private val balanceRepository: BalanceRepository,  
) {  
    @Transactional  
    fun updateAmount(  
        userId: Long,  
        amount: Long,  
    ): Balance {  
        val user = userRepository.findById(userId) ?: throw BusinessException.NotFound(ErrorCode.User.NOT_FOUND)  
        return balanceRepository.findByUserId(user.id)?.apply {  
            updateAmount(amount)  
        } ?: balanceRepository.save(  
            Balance(  
                user = user,  
                amount = amount,  
                lastUpdatedAt = LocalDateTime.now(),  
            ),  
        )  
    }
```

위의 내용은 처음에 실패했다.
문제는 `AOP` 에 대한 이해도가 부족했기 때문이다.
하나의 클래스 내`(BalanceService)`에서 `AOP`를 적용하려 했으나, 프록시 기반의 `AOP` 특성상 동일 객체 내 메서드 호출에서는 `AOP`가 적용되지 않았다.
이것으로 인해 얼마나 삽질을 했는지 모른다.
삽질을 계속 하다가 도저히 이해가 되지 않아서 항해 여정중 처음으로 코치님께 DM 을 드려봤다 🥹

>![](https://velog.velcdn.com/images/joshuara7235/post/5180686f-b046-43f8-8492-6f997ee37f2f/image.png)
shout to 빛허재 코치님...
너무나 확실하게, 그리고 친절하게 문제를 말쓰해주시고 해결방안을 자세하게 설명해주셨다.


코치님의 조언을 참고하여 해결 하기위해, `BalanceLockManager`라는 중간 레이어를 추가하여 락을 관리하는 로직을 분리했다.

변경된 로직은 아래와 같다.

```kotlin
@Service  
class BalanceService(  
    private val balanceManager: BalanceManager,  
    private val balanceLockManager: BalanceLockManager,  
) {  
    fun recharge(  
        userId: Long,  
        amount: Long,  
    ): BalanceServiceDto.Detail {  
        if (amount < 0) throw BusinessException.BadRequest(ErrorCode.Balance.BAD_RECHARGE_REQUEST)  
  
        val rechargedBalance =  
            balanceLockManager.rechargeWithLock(userId, amount)  
  
        return BalanceServiceDto.Detail(  
            userId = userId,  
            currentAmount = rechargedBalance.amount,  
        )  
    }
@Component  
class BalanceLockManager(  
    private val balanceManager: BalanceManager,  
) {  
    @DistributedSimpleLock(  
        key = "'user:' + #userId",  
        waitTime = 5,  
        leaseTime = 10,  
    )  
    fun rechargeWithLock(  
        userId: Long,  
        amount: Long,  
    ): Balance =  
        balanceManager.updateAmount(  
            userId = userId,  
            amount = amount,  
        )  
}
@Component  
class BalanceManager(  
    private val userRepository: UserRepository,  
    private val balanceRepository: BalanceRepository,  
) {  
    @Transactional  
    fun updateAmount(  
        userId: Long,  
        amount: Long,  
    ): Balance {  
        val user = userRepository.findById(userId) ?: throw BusinessException.NotFound(ErrorCode.User.NOT_FOUND)  
        return balanceRepository.findByUserId(user.id)?.apply {  
            updateAmount(amount)  
        } ?: balanceRepository.save(  
            Balance(  
                user = user,  
                amount = amount,  
                lastUpdatedAt = LocalDateTime.now(),  
            ),  
        )  
    }

```

<br>

#### 4. 내가 실제로 선택한 방법은 ?

** 1) 예약 기능 구현 **

- 이중 락 전략 (분산 락 + 비관적 락) 을 사용했다.
    - Redis 분산 락으로 1차 동시성 제어를 수행한다.
    - 비관적 락으로 2차 안전장치를 마련하여 데이터 정합성을 보장한다.

```kotlin
@Service  
class ReservationService(  
    private val userManager: UserManager,  
    private val queueManager: QueueManager,  
    private val concertManager: ConcertManager,  
    private val reservationManager: ReservationManager,  
    private val reservationLockManager: ReservationLockManager,  
) {   
    fun createReservations(  
        token: String,  
        reservationRequest: ReservationServiceDto.Request,  
    ): List<ReservationServiceDto.Result> {  
        validateQueueStatus(token)
        validateUser(reservationRequest.userId)
        validateReservationRequest(  
            requestConcertId = reservationRequest.concertId,  
            requestScheduleId = reservationRequest.scheduleId,  
            requestSeatIds = reservationRequest.seatIds,  
        )
        return reservationLockManager  
            .createReservations(reservationRequest)  
            .map {  
                ReservationServiceDto.Result(  
                    reservationId = it.id,  
                    concertId = reservationRequest.concertId,  
                    concertName = it.concertTitle,  
                    concertAt = it.concertAt,  
                    seat =  
                        ReservationServiceDto.Seat(  
                            seatNumber = it.seat.seatNumber,  
                            price = it.seat.seatPrice,  
                        ),  
                    reservationStatus = it.reservationStatus,  
                )  
            }  
    }

@Component  
class ReservationLockManager(  
    private val reservationManager: ReservationManager,  
) {  
    @DistributedSimpleLock(  
        key =  
            "'user:' + #reservationRequest.userId + " +  
                "'concert:' + #reservationRequest.concertId + " +  
                "':schedule:' + #reservationRequest.scheduleId",  
        waitTime = 5,  
        leaseTime = 10,  
    )  
    fun createReservations(reservationRequest: ReservationServiceDto.Request): List<Reservation> =  
        reservationManager.createReservations(reservationRequest)  
}

@Transactional  
fun createReservations(reservationRequest: ReservationServiceDto.Request): List<Reservation> {  
    val user =  
        userRepository.findById(reservationRequest.userId)  
            ?: throw BusinessException.NotFound(ErrorCode.User.NOT_FOUND)  
    val concert =  
        concertRepository.findById(reservationRequest.concertId)  
            ?: throw BusinessException.NotFound(ErrorCode.Concert.NOT_FOUND)  
    val concertSchedule =  
        concertScheduleRepository.findById(reservationRequest.scheduleId)  
            ?: throw BusinessException.NotFound(ErrorCode.Concert.SCHEDULE_NOT_FOUND)  
    val seats = seatRepository.findAllByIdAndStatusWithPessimisticLock(reservationRequest.seatIds, SeatStatus.AVAILABLE)  
  
    val reservations =  
        seats.map { seat ->  
            val reservation =  
                Reservation(  
                    user = user,  
                    concertTitle = concert.title,  
                    concertAt = concertSchedule.concertAt,  
                    seat = seat,  
                    reservationStatus = ReservationStatus.PAYMENT_PENDING,  
                    createdAt = LocalDateTime.now(),  
                )  
            reservationRepository.save(reservation)  
        }  
  
    seatRepository.updateAllStatus(reservationRequest.seatIds, SeatStatus.UNAVAILABLE)  
  
    return reservations  
}

@Lock(LockModeType.PESSIMISTIC_READ)  
@Query("SELECT s FROM Seat s WHERE s.id IN :seatIds and s.seatStatus = :seatStatus")  
fun findAllByIdAndStatusWithPessimisticLock(  
    seatIds: List<Long>,  
    seatStatus: SeatStatus,  
): List<Seat>
```


** 2) 잔액 충전의 동시성 제어 **
- 분산 락만을 사용하여 동시 충전 요청을 제어한다.
- 여러 번의 충전 요청 중 한 번만 성공하도록 하여 우발적인 중복 충전을 방지한다.

```kotlin
@Service  
class BalanceService(  
    private val balanceManager: BalanceManager,  
    private val balanceLockManager: BalanceLockManager,  
) {  
    fun recharge(  
        userId: Long,  
        amount: Long,  
    ): BalanceServiceDto.Detail {  
        if (amount < 0) throw BusinessException.BadRequest(ErrorCode.Balance.BAD_RECHARGE_REQUEST)  
  
        val rechargedBalance =  
            balanceLockManager.rechargeWithLock(userId, amount)  
  
        return BalanceServiceDto.Detail(  
            userId = userId,  
            currentAmount = rechargedBalance.amount,  
        )  
    }

@Component  
class BalanceLockManager(  
    private val balanceManager: BalanceManager,  
) {  
    @DistributedSimpleLock(  
        key = "'user:' + #userId",  
        waitTime = 5,  
        leaseTime = 10,  
    )  
    fun rechargeWithLock(  
        userId: Long,  
        amount: Long,  
    ): Balance =  
        balanceManager.updateAmount(  
            userId = userId,  
            amount = amount,  
        )  
}

@Component  
class BalanceManager(  
    private val userRepository: UserRepository,  
    private val balanceRepository: BalanceRepository,  
) {  
    @Transactional  
    fun updateAmount(  
        userId: Long,  
        amount: Long,  
    ): Balance {  
        val user = userRepository.findById(userId) ?: throw BusinessException.NotFound(ErrorCode.User.NOT_FOUND)  
        return balanceRepository.findByUserId(user.id)?.apply {  
            updateAmount(amount)  
        } ?: balanceRepository.save(  
            Balance(  
                user = user,  
                amount = amount,  
                lastUpdatedAt = LocalDateTime.now(),  
            ),  
        )  
    }
```


자세한 시도와 분석 내용은 [제출한 보고서](https://github.com/mingj7235/concert/blob/main/docs/06_ConcurrencyReport.md) 에 나와 있다.
해당 내용은 [프로젝트의 Readme](https://github.com/mingj7235/concert) 에서도 확인이 가능하다.



<br>


# 💎 2. 이번 주차에 난 무엇을 배웠나


### 무분별한 Transactional 의 사용이 얼마나 위험한가

여태까지 몰랐다.
아니, 분명 나도 이론적으로는 알고 있었다.
`@Transactional` 을 사용하면 하나의 트랜잭션으로 묶이고 원자적으로 로직이 수행된다는 것을 분명히 알고 있었다.
그런데, 이것이 실제로 어떤 문제를 야기하고, 실제 어플리케이션에서 어떤 고민을 해야하는가에 대한 깊은 고민은 없었다. 부끄럽다. 🥲

이번 주차의 깨달음으로, `@Transactional` 을 어떻게 사용해야하고, 어떤 고민을 해야하는지 새로운 깨달음을 얻었다.


### AOP 의 동작원리
위에서 언급했듯이, AOP 의 동작원리를 제대로 깨닫게 되었다.
Proxy 기반으로 동작하므로, 한 클래스에서 같은 위상의 메서드를 호출하면 제대로 작동하지 않음을 삽질을 통해 체감했다.
~~역시 삽질을 해야 기억에 잘 남는것인가~~


### 다양한 Lock 에 대해 이해도가 생겼다.
내가 구현한 로직에 여러가지 Lock 을 구현하면서 장단점을 익히고, 실제 분석까지 하면서 이해도가 많이 생겼다.
당연히 완벽하지는 않지만, DB 락과 분산락을 왜 사용하고, 어떤 경우에 장단이 있는지 알게 되었다.

<br>


# 🙏🏻 3. 글을 마치며

Chapter3 를 이제 시작한다.
이번 챕터는 내가 정말 항해에서 많은 것을 얻고 배울 수 있는 챕터라고 생각한다.
매 주차를 지나오며 피로도가 지속적으로 누적되고 있지만, 이 과정을 모두 지났을 때 얻을 달콤한 열매를 기대하며 정진해본다.

화이팅!


### 지난 회고 보러가기
1주차 회고 - [테스트코드를 모르던 내게 찾아온 TDD](https://velog.io/@joshuara7235/%ED%85%8C%EC%8A%A4%ED%8A%B8%EC%BD%94%EB%93%9C%EB%A5%BC-%EB%AA%A8%EB%A5%B4%EB%8D%98-%EB%82%B4%EA%B2%8C-%EC%B0%BE%EC%95%84%EC%98%A8-TDD)
2주차 회고 - [코딩에 정답을 찾지말자. 고민을 통해 더 나아짐을 시작하자.](https://velog.io/@joshuara7235/%EC%BD%94%EB%94%A9%EC%97%90-%EC%A0%95%EB%8B%B5%EC%9D%84-%EC%B0%BE%EC%A7%80%EB%A7%90%EC%9E%90.-%EA%B3%A0%EB%AF%BC%EC%9D%84-%ED%86%B5%ED%95%B4-%EB%8D%94-%EB%82%98%EC%95%84%EC%A7%90%EC%9D%84-%EC%8B%9C%EC%9E%91%ED%95%98%EC%9E%90)
3주차 회고 - [좋은 코드를 위해서는 좋은 설계가 우선되어야 한다.](https://velog.io/@joshuara7235/%EC%A2%8B%EC%9D%80-%EC%BD%94%EB%93%9C%EB%A5%BC-%EC%9C%84%ED%95%B4%EC%84%9C%EB%8A%94-%EC%A2%8B%EC%9D%80-%EC%84%A4%EA%B3%84%EA%B0%80-%EC%9A%B0%EC%84%A0%EB%90%98%EC%96%B4%EC%95%BC-%ED%95%9C%EB%8B%A4)
4주차 회고 - [어플리케이션은 완벽할 수 없다. 다만 완벽을 지향할 뿐.](https://velog.io/@joshuara7235/%EC%96%B4%ED%94%8C%EB%A6%AC%EC%BC%80%EC%9D%B4%EC%85%98%EC%9D%80-%EC%99%84%EB%B2%BD%ED%95%A0-%EC%88%98-%EC%97%86%EB%8B%A4.-%EB%8B%A4%EB%A7%8C-%EC%99%84%EB%B2%BD%EC%9D%84-%EC%A7%80%ED%96%A5%ED%95%A0-%EB%BF%90)
5주차 회고 - [항해의 중간지점, 나는 얼마나 성장했나.](https://velog.io/@joshuara7235/%ED%95%AD%ED%95%B4%EC%9D%98-%EC%A4%91%EA%B0%84%EC%A7%80%EC%A0%90-%EB%82%98%EB%8A%94-%EC%96%BC%EB%A7%88%EB%82%98-%EC%84%B1%EC%9E%A5%ED%96%88%EB%82%98)

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



