# 동시성 문제와 극복에 관한 보고서

## 1. 콘서트 대기열 시스템에서 동시성 문제가 발생할 수 있는 로직

### 1) 좌석예약기능
- 동시에 여러명이 하나의 좌석을 두고 예약하려고 하면 단 1명만이 그 좌석을 예약할 수 있도록 해야한다.

#### 동시성 문제 발생 이유:
- 여러 사용자가 동시에 같은 좌석을 예약하려고 시도할 때, 시스템이 각 요청을 순차적으로 처리하지 않으면 중복 예약이 발생할 수 있다.
- 데이터베이스 트랜잭션이 적절히 관리되지 않으면, 한 사용자의 예약 과정 중 다른 사용자가 같은 좌석을 예약할 수 있다.

#### 기대하는 결과:
- 특정 좌석에 대해 최초로 예약 요청을 완료한 사용자만 해당 좌석을 성공적으로 예약을 한다.
- 다른 사용자들의 동일 좌석 예약 시도는 실패하고, 적절한 오류 메시지를 받아야 한다.

### 2) 잔액 충전
- 한 명의 유저가 자신의 잔액을 충전을 할 때, 실수로 여러번을 호출할 경우에 1회만 가능하도록 해야한다.

#### 동시성 문제 발생 이유:
- 사용자가 실수로 또는 네트워크 지연으로 인해 충전 버튼을 여러 번 클릭할 경우, 각 요청이 독립적으로 처리되어 중복 충전이 발생할 수 있다.
- 서버에서 요청을 처리하는 동안 클라이언트 측에서 추가 요청을 보내면, 서버가 이전 요청의 처리 상태를 확인하지 않고 새 요청을 처리할 수 있다.

#### 기대하는 결과:
- 사용자가 여러 번 충전 요청을 보내더라도 단 한 번만 잔액이 증가해야 한다.
- 충전 금액은 정확히 한 번만 사용자의 계정에 반영되어야 하며, 금액 오차가 없어야 한다.

<br>

## 2. 동시성 이슈 대응 이전의 로직

- 원래 로직은 Service 계층(Facade) 에 @Transactional 어노테이션을 적용하여 트랜잭션을 관리했다.
- 이러한 접근 방식을 선택한 이유는 아래와 같다.

### 트랜잭션 관리 방식 선택 이유

1. 원자성 보장: 하나의 트랜잭션 내에서 Service 레이어의 모든 로직이 원자성을 가지고 실행되어야 한다고 판단했다.
2. 단순성: 서비스 계층에 트랜잭션을 적용함으로써 모든 데이터베이스 연산이 하나의 트랜잭션으로 묶이도록 했다.
3. 일관성: 모든 비즈니스 로직이 하나의 트랜잭션 내에서 실행되므로, 데이터의 일관성을 유지하기 쉽다고 생각했다.


### 코드 설명

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

### 좌석 예약 로직 설명

1. ReservationService 클래스:
    - createReservations 메서드에 @Transactional 어노테이션이 적용되어 있다.
    - Facade 의 역할을한다.
    - 이 메서드는 예약 요청의 유효성을 검사한 후, ReservationManager를 통해 실제 예약을 생성한다.

2. ReservationManager 클래스:
    - createReservations 메서드에서 실제 예약 생성과 좌석 상태 업데이트를 수행한다.
    - 사용자, 콘서트, 스케줄, 좌석 정보를 조회한다.
    - Reservation 객체를 생성하고 저장한다.
    - 선택된 좌석들의 상태를 'UNAVAILABLE'로 업데이트한다.


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

### 잔액 충전 로직 설명

1. BalanceService 클래스:
    - recharge 메서드에 @Transactional 어노테이션이 적용되어 있다.
    - 충전 금액의 유효성을 검사한 후, BalanceManager를 통해 실제 잔액 업데이트를 수행한다.

2. BalanceManager 클래스:
    - updateAmount 메서드에서 사용자의 잔액을 업데이트한다.
    - 해당 사용자의 Balance 엔티티가 존재하면 금액을 갱신하고, 없으면 새로 생성한다.

### 이 접근 방식의 문제점

1. 트랜잭션 범위가 너무 넓음:
    - 서비스 계층의 메서드 전체가 하나의 트랜잭션으로 묶여 있어, 불필요하게 긴 시간 동안 데이터베이스 리소스를 점유할 수 있다.

2. 동시성 제어의 어려움:
    - 넓은 트랜잭션 범위로 인해 동시에 여러 요청이 처리될 때 데드락이 발생하거나 성능이 저하될 수 있다.

3. 세밀한 제어의 부재:
    - 특정 연산에 대해서만 트랜잭션을 적용하거나, 다른 격리 수준을 설정하는 등의 세밀한 제어가 어렵다.

4. 성능 저하:
    - 모든 연산이 하나의 큰 트랜잭션으로 묶여 있어, 데이터베이스 연결이 오래 유지되면서 전반적인 시스템 성능이 저하될 수 있다.

이러한 문제점들로 인해 동시성 이슈가 발생할 가능성이 높아지며, 특히 높은 트래픽 상황에서 시스템의 안정성과 성능이 저하될 수 있다.
<br>
따라서 동시성 문제에 대응하기 위해서는 트랜잭션의 범위를 좁히고, 더 세밀한 동시성 제어 메커니즘을 도입할 필요가 있다.

<br>

## 3. DB 락 구현


### 낙관적 락 (Optimistic Lock)

- 낙관적 락은 동시 업데이트가 드물게 발생한다는 가정 하에 동작한다.
- 이 방식은 데이터 수정 시 충돌이 발생하지 않을 것이라고 이름 그대로 '낙관적으로' 가정하고, 충돌이 발생했을 때 이를 감지하고 처리한다.

#### 코드 구현

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
  
        return runCatching {  
            val rechargedBalance = balanceManager.updateAmount(userId = userId, amount = amount)  
            BalanceServiceDto.Detail(  
                userId = userId,  
                currentAmount = rechargedBalance.amount,  
            )  
        }.getOrElse { exception ->  
            when (exception) {  
                is ObjectOptimisticLockingFailureException ->  
                    throw BusinessException.BadRequest(ErrorCode.Balance.CONCURRENT_MODIFICATION)  
                else -> throw exception  
            }  
        }  
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
        val balance = balanceRepository.findByUserIdWithLock(user.id)  
        return balance?.updateAmount(amount)  
            ?: balanceRepository.save(  
                Balance(  
                    user = user,  
                    amount = amount,  
                    lastUpdatedAt = LocalDateTime.now(),  
                ),  
            )  
    }
```
1. BalanceService 클래스:
    - `recharge` 메서드에서 트랜잭션 어노테이션을 제거했다.
    - `runCatching` 블록을 사용하여 예외 처리를 구현했다.
    - 낙관적 락 실패(`ObjectOptimisticLockingFailureException`)를 캐치하여 적절한 비즈니스 예외로 변환했다.

2. BalanceManager 클래스:
    - `updateAmount` 메서드에 `@Transactional` 어노테이션을 적용하여 트랜잭션 범위를 좁혔다.
    - `findByUserIdWithLock` 메서드를 사용하여 락이 걸린 상태로 `Balance` 엔티티를 조회한다.

3. Entity 클래스 :
    - Balance 엔티티에 `@Version` 어노테이션을 사용한 버전 필드를 추가했다.
    - 이 버전 필드는 JPA에 의해 자동으로 관리되며, 엔티티가 업데이트될 때마다 증가한다.

#### 주요 변경 사항

1. 트랜잭션 범위 축소:
    - 트랜잭션의 범위를 Service에서 Manager로 내려 더 작은 단위로 제어하도록 했다.
    - 이를 통해 트랜잭션 유지 시간을 줄이고, 리소스 점유를 최소화했다.

2. 낙관적 락 구현:
    - 엔티티에 버전 정보를 추가하여 JPA의 낙관적 락 기능을 활용했다.
    - 동시 수정 시 발생하는 충돌을 감지하고 예외를 발생시킨다.

3. 예외 처리:
    - 낙관적 락 실패 시 발생하는 예외를 커스텀한 예외를 뱉도록 명시적으로 처리하여 사용자에게 적절한 응답을 제공하도록 했다.

#### 테스트 코드 - 성공케이스 - 10번의 시도

```kotlin
@Test  
fun `1명의 유저가 10번의 동시 충전 요청이 오더라도 1번만 성공해야 한다`() {  
    // Given  
    val user = userRepository.save(User(name = "Test User"))  
    balanceRepository.save(  
        Balance(  
            user = user,  
            amount = 100,  
            lastUpdatedAt = LocalDateTime.now(),  
        ),  
    )  

    val threadCount = 10  
    val executorService = Executors.newFixedThreadPool(threadCount)  
    val latch = CountDownLatch(threadCount)  
    val rechargeAmount = 100L  

    // When  
    repeat(threadCount) {  
        executorService.submit {  
            try {  
                runCatching {  
                    balanceService.recharge(user.id, rechargeAmount)  
                }  
            } finally {  
                latch.countDown()  
            }  
        }  
    }        latch.await()  

    // Then  
    val finalBalance = balanceRepository.findByUserId(user.id)  
    assertEquals(200L, finalBalance?.amount, "최종 잔액은 200이어야 합니다.")  
}  

```
#### 테스트 코드 설명
- 10개의 스레드를 사용하여 동시에 잔액 충전을 시도한다.
- 각 스레드는 100원씩 충전을 시도한다.
- 테스트 결과, 최종 잔액이 200원(초기 100원 + 1회 성공한 100원)임을 확인한다.



#### 테스트 코드 - 실패케이스 - 100번의 시도

```kotlin
@Test  
fun `1명의 유저가 100번의 동시 충전 요청이 오더라도 1번만 성공해야 한다`() {  
    // Given  
    val user = userRepository.save(User(name = "Test User"))  
    balanceRepository.save(  
        Balance(  
            user = user,  
            amount = 100,  
            lastUpdatedAt = LocalDateTime.now(),  
        ),  
    )  
  
    val threadCount = 100  
    val executorService = Executors.newFixedThreadPool(threadCount)  
    val latch = CountDownLatch(threadCount)  
    val rechargeAmount = 100L  
  
    // When  
    repeat(threadCount) {  
        executorService.submit {  
            try {  
                runCatching {  
                    balanceService.recharge(user.id, rechargeAmount)  
                }  
            } finally {  
                latch.countDown()  
            }  
        }  
    }    latch.await()  
  
    // Then  
    val finalBalance = balanceRepository.findByUserId(user.id)  
    assertEquals(200L, finalBalance?.amount, "최종 잔액은 200이어야 합니다.")  
}
```
- 100회가 되었을 때 테스트가 깨지게 된다.

#### 결론 및 고찰

1. 낙관적 락의 효과와 한계:
    - 10회 정도의 동시 요청에 대해서는 낙관적 락이 효과적으로 동작함을 확인했다.
    - 하지만 100회의 동시 요청 테스트에서는 실패가 발생했다. 이는 낙관적 락의 한계를 보여준다.


2. 동시성 증가에 따른 문제:
    - 동시 요청 수가 증가함에 따라 충돌 발생 확률이 높아진다.
    - 충돌이 발생할 때마다 예외가 발생하고 재시도가 필요하므로, 로직이 복잡한경우 전체적인 처리 시간이 길어질 수 있다.
    - 극단적인 경우, 모든 요청이 계속 충돌하여 결과적으로 처리되지 못하는 상황(livelock)이 발생할 수 있다.


3. 성능과 정확성의 트레이드오프:
    - 낙관적 락은 충돌이 적은 환경에서는 높은 성능을 제공한다.
    - 그러나 충돌이 빈번한 환경에서는 재시도로 인한 오버헤드가 크게 증가할 수 있다.


4. 실제 운영 환경 고려사항:
    - 실제 서비스의 동시 요청 패턴과 빈도를 분석하여 적절한 동시성 제어 메커니즘을 선택해야 한다.


5. 재시도 로직 구현:
    - 낙관적 락 실패 시 즉시 에러를 반환하기보다는, 일정 횟수만큼 재시도하는 로직을 구현할 수 있을 것 같다 (시간이 없어서 하지 못함).
    - 이를 통해 일시적인 충돌로 인한 실패를 줄이고 성공률을 높일 수 있을 것으로 보인다.


결론적으로, 낙관적 락은 간단하고 효과적인 동시성 제어 방법이지만, 높은 동시성 환경에서는 한계가 있음을 확인했다.


### 비관적 락 (Pessimistic Lock)

- 비관적 락은 동시 업데이트가 빈번하게 발생할 것이라고 '비관적으로' 가정하고, 데이터를 읽는 시점에 락을 걸어 다른 트랜잭션의 접근을 차단한다.
- 이 방식은 데이터 무결성을 강하게 보장하지만, 동시성 처리 성능이 낮아질 수 있다.

#### 코드 구현

```kotlin
@Service  
class ReservationService(  
    private val userManager: UserManager,  
    private val queueManager: QueueManager,  
    private val concertManager: ConcertManager,  
    private val reservationManager: ReservationManager,  
) {  
    fun createReservations(  
        token: String,  
        reservationRequest: ReservationServiceDto.Request,  
    ): List<ReservationServiceDto.Result> {  
        validateQueueStatus(token)  
  
        userManager.findById(reservationRequest.userId)  
  
        validateReservationRequest(  
            requestConcertId = reservationRequest.concertId,  
            requestScheduleId = reservationRequest.scheduleId,  
            requestSeatIds = reservationRequest.seatIds,  
        )  
  
        return runCatching {  
            reservationManager  
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
        }.getOrElse { exception ->  
            when (exception) {  
                is PessimisticLockingFailureException,->  
                    throw BusinessException.BadRequest(ErrorCode.Concert.SEAT_ALREADY_RESERVED)  
                else -> throw exception  
            }  
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
        val seats = seatRepository.findAllByIdWithPessimisticLock(reservationRequest.seatIds)  
  
        seats.forEach { seat ->  
            if (seat.seatStatus != SeatStatus.AVAILABLE) {  
                throw BusinessException.BadRequest(ErrorCode.Concert.SEAT_ALREADY_RESERVED)  
            }  
            seat.updateStatus(SeatStatus.UNAVAILABLE)  
        }  
  
        return seats.map { seat ->  
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
    }
    
@Lock(LockModeType.PESSIMISTIC_WRITE)  
@Query("SELECT s FROM Seat s WHERE s.id IN :seatIds")  
fun findAllByIdWithPessimisticLock(seatIds: List<Long>): List<Seat>
```

1. ReservationService 클래스:
    - `createReservations` 메서드에서 트랜잭션 어노테이션을 제거했다.
    - `runCatching` 블록을 사용하여 예외 처리를 구현했다.
    - 비관적 락 실패(`PessimisticLockingFailureException`)를 캐치하여 적절한 비즈니스 예외로 변환 했다.

2. ReservationManager 클래스:
    - `createReservations` 메서드에 `@Transactional` 어노테이션을 적용하여 트랜잭션 범위를 좁혔다.
    - `findAllByIdWithPessimisticLock` 메서드를 사용하여 비관적 락이 걸린 상태로 Seat 엔티티를 조회하도록 했다.
    - 좌석 상태를 확인하고 업데이트하는 로직을 추가했다.

3. Repository 클래스:
    - `@Lock(LockModeType.PESSIMISTIC_WRITE)` 어노테이션을 사용하여 비관적 락을 구현했다.
    - 처음에는 `PESSIMISTIC_WRITE` 로 했다가 `PESSIMISTIC_READ`로 변경하여 성능을 개선했다.
    - 그 이유는, `PESSIMISTIC_WRITE` 를 사용할 만큼 데이터 무결성이 필요하다고 생각하지 않아서였다.



#### 테스트 코드

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
- 1000개의 스레드를 사용하여 동시에 좌석 예약을 시도한다.
- 테스트 결과, 단 하나의 예약만 성공하고 나머지는 실패함을 확인한다.
- 성공한 예약에 대해서는 좌석 상태가 'UNAVAILABLE'로 변경됨을 검증한다.
- 실행 시간을 측정하여 성능을 평가하도록 했다.


#### 결과 및 고찰

1. 정확성:
    - 비관적 락은 1000개의 동시 요청 중 정확히 1개만 성공하도록 보장했다.
    - 이는 비관적 락이 높은 동시성 환경에서도 데이터 무결성을 확실히 보장함을 보여주는 것이다.

2. 성능:
    - `PESSIMISTIC_WRITE`를 사용했을 때 752 밀리초가 소요되었다.
    - `PESSIMISTIC_READ`로 변경 후 695 밀리초로 약간의 성능 향상이 있었지만, 그 차이는 미미했다.

3. 트레이드오프:
    - 비관적 락은 데이터 정합성을 강력하게 보장하지만, 동시에 처리할 수 있는 트랜잭션의 수가 제한된다.
    - 높은 동시성 환경에서는 전체적인 시스템 처리량이 낮아질 수 있다.

4. 사용 시나리오:
    - 데이터 정합성이 매우 중요하고, 충돌이 자주 발생하는 환경에서 유용하다.
    - 예를 들어, 콘서트 티켓 예매와 같이 제한된 리소스에 대한 경쟁이 심한 경우에 적합한 것으로 보인다.

5. 확장성 고려:
    - 비관적 락은 데이터베이스 수준의 락을 사용하므로, 분산 환경에서의 확장성에 제한이 있을 수 있다.
    - 대규모 시스템에서는 분산 락과의 조합을 고려해볼 수 있고, 아래에서 그렇게 구현했다.


<br>

## 4. 분산 락 구현

- 분산 락은 여러 서버나 인스턴스에서 동시에 접근하는 리소스에 대한 동시성을 제어하기 위해 사용된다.
- Redis를 이용한 분산 락을 커스텀 어노테이션과 함께 AOP를 통해 구현했다.

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

### 구현 방식

1. 어노테이션 정의
    - `@DistributedSimpleLock` 어노테이션을 만들어 분산 락을 적용할 메서드를 지정한다.
    - 락의 키, 대기 시간, 임대 시간 등을 설정할 수 있도록 했다.

2. AOP를 이용한 락 적용
    - `DistributedSimpleLockAspect` 클래스에서 어노테이션이 적용된 메서드 실행 전후로 락을 획득하고 해제한다.
    - '락 획득 → 트랜잭션 시작 → 비즈니스 로직 → 트랜잭션 종료(커밋 or 롤백) → 락 반납' 순서로 동작하도록 의도했다.

3. Redis를 이용한 락 구현
    - `RedisSimpleLock` 클래스에서 Redis의 `setIfAbsent` 명령어를 이용해 락을 구현한다.
    - 락 획득과 해제 로직을 제공한다.

<br>

### 코드 설명

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

### 구현 과정에서의 문제점과 해결

1. 초기 구현의 문제:
    - 하나의 클래스 내(`BalanceService`)에서 AOP를 적용하려 했으나, 프록시 기반의 AOP 특성상 동일 객체 내 메서드 호출에서는 AOP가 적용되지 않았다.

2. 해결 방안:
    - `BalanceLockManager`라는 중간 레이어를 추가하여 락을 관리하는 로직을 분리했다.
    - 이를 통해 AOP가 정상적으로 적용되어 분산 락이 의도한 대로 동작하게 되었다.
    - 해결한 내용은 아래에 기술되어있다.

### 초기 테스트 실패
```kotlin
@BeforeEach  
fun setup() {  
    // 테스트 사용자 생성  
    testUser = userRepository.save(User(name = "Test User"))  
  
    // 초기 잔액 설정  
    balanceRepository.save(Balance(user = testUser, amount = 0, lastUpdatedAt = LocalDateTime.now()))  
}  
  
@Test  
fun `사용자가 동시에 1000 회 잔액을 충전할 때 1회만 충전이 정확히 반영되어야 한다`() {  
    val startTime = System.nanoTime()  
    val numberOfThreads = 1000  
    val rechargeAmount = 1000L  
  
    val executor = Executors.newFixedThreadPool(numberOfThreads)  
    val successfulRecharges = AtomicInteger(0)  
    val failedRecharges = AtomicInteger(0)  
  
    val totalTime =  
        measureTimeMillis {  
            try {  
                val futures =  
                    (1..numberOfThreads).map {  
                        executor.submit {  
                            try {  
                                balanceService.recharge(testUser.id, rechargeAmount)  
                                successfulRecharges.incrementAndGet()  
                            } catch (e: Exception) {  
                                failedRecharges.incrementAndGet()  
                            }  
                        }  
                    }  
                futures.forEach { it.get() } // 모든 작업이 완료될 때까지 대기  
            } finally {  
                executor.shutdown()  
                executor.awaitTermination(1, TimeUnit.MINUTES)  
            }  
        }  
    val endTime = System.nanoTime()  
    val duration = Duration.ofNanos(endTime - startTime)  
  
    // 결과 검증  
    val finalBalance = balanceRepository.findByUserId(testUser.id)!!  
    assertNotNull(finalBalance, "잔액이 존재해야 합니다")  
    assertEquals(rechargeAmount, finalBalance.amount, "최종 잔액이 예상 금액과 일치해야 합니다")  
    assertEquals(1, successfulRecharges.get(), "1회의 충전 시도만이 성공해야 합니다")  
    assertEquals(999, failedRecharges.get(), "실패한 충전이 없어야 합니다")  
  
    println("테스트 실행 시간: ${duration.toMillis()} 밀리초")  
    println("성공한 충전 횟수: ${successfulRecharges.get()}")  
    println("실패한 충전 횟수: ${failedRecharges.get()}")  
    println("최종 잔액: ${finalBalance.amount}")  
    println("총 실행 시간: $totalTime ms")  
}
```

### 테스트 결과

1. 초기 테스트 실패:
    - AOP 적용 문제로 인해 동시성 제어가 제대로 이루어지지 않았다.


### 해결 방안 - `BalanceLockManager` 를 통한 레이어 추가

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

### 테스트 통과
1. 로직 변경 후 테스트 통과:
    - 1000개의 동시 요청 중 1개만 성공하고 나머지는 실패하는 것을 확인했다.
    - 최종 잔액이 정확히 한 번의 충전만 반영되었음을 검증했다.

<br>

### 결론 및 고찰

1. 분산 환경 대응:
    - Redis를 이용한 분산 락은 여러 서버에서 동작하는 애플리케이션의 동시성 문제를 효과적으로 해결할 수 있다.

2. AOP의 활용:
    - AOP를 통해 비즈니스 로직과 동시성 제어 로직을 깔끔하게 분리할 수 있었다.
    - 하지만 AOP의 동작 방식을 정확히 이해하지 않으면 예상치 못한 문제가 발생할 수 있음을 확인했고 공부가 되었다.

3. 구조적 개선:
    - 중간 레이어(`BalanceLockManager`)를 도입함으로써 관심사를 명확히 분리하고 AOP 적용 문제를 해결했다.
    - 이는 코드의 가독성과 유지보수성을 향상시키는 결과를 가져왔다.

4. 성능과 신뢰성:
    - 분산 락을 통해 데이터의 정합성을 보장하면서도, Redis의 빠른 처리 속도로 인해 성능 저하를 최소화할 수 있었다.
    - 그러나 Redis 서버의 장애 상황에 대한 대비책도 고려해야 한다. (예컨데 ElasticCache 사망 등..)


<br>

## 5. 내가 선택한 콘서트 예약의 동시성 처리 방법

### 1) 예약 기능 구현

1. 아키텍처 설계
    - 서비스 레이어 (ReservationService): 전체적인 예약 로직 조정
    - 락 관리 레이어 (ReservationLockManager): 분산 락 적용
    - 트랜잭션 관리 및 도메인 레이어 (ReservationManager): 실제 예약 처리 및 트랜잭션 관리

<br>

2. 동시성 제어 전략
    - Redis를 이용한 분산 락 (DistributedSimpleLock): 대규모 동시 요청 처리
    - 비관적 락 (PESSIMISTIC_READ): 데이터베이스 수준에서의 동시성 제어
    - 이중 락 전략: Redis 장애 시 비관적 락으로 대체 가능하도록 함

<br>

3. 주요 구현 로직
    - 사용자 인증 및 요청 검증
    - 분산 락을 이용한 예약 요청 격리
    - 트랜잭션 내에서 좌석 상태 확인 및 업데이트
    - 예약 정보 생성 및 저장

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

<br>

### 2) 잔액 충전 기능 구현

1. 아키텍처 설계
    - 서비스 레이어 (BalanceService): 충전 로직 조정
    - 락 관리 레이어 (BalanceLockManager): 분산 락 적용
    - 트랜잭션 관리 레이어 (BalanceManager): 실제 잔액 업데이트 및 트랜잭션 관리

<br>

2. 동시성 제어 전략
    - Redis를 이용한 분산 락: 동시 충전 요청 제어
    - 트랜잭션 격리: 데이터베이스 수준에서의 동시성 제어

<br>

3. 주요 로직
    - 충전 요청 검증
    - 분산 락을 이용한 충전 요청 격리
    - 트랜잭션 내에서 잔액 업데이트 또는 새 잔액 정보 생성

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

### 구현 결정 사항 및 이유

1. 예약 기능의 동시성 제어 :
    - 이중 락 전략 (분산 락 + 비관적 락):
    - Redis 분산 락으로 1차 동시성 제어를 수행한다.
    - 비관적 락으로 2차 안전장치를 마련하여 데이터 정합성을 보장한다.

2. 잔액 충전의 동시성 제어:
    - 분산 락만을 사용하여 동시 충전 요청을 제어한다.
    - 여러 번의 충전 요청 중 한 번만 성공하도록 하여 우발적인 중복 충전을 방지한다.


### 테스트코드
```kotlin
@BeforeEach  
fun setup() {  
    // 콘서트, 스케줄, 좌석 생성  
    val concert =  
        concertRepository.save(  
            Concert(  
                title = "Test Concert",  
                description = "Test Description",  
                concertStatus = ConcertStatus.AVAILABLE,  
            ),  
        )  
    concertId = concert.id  
  
    val schedule =  
        concertScheduleRepository.save(  
            ConcertSchedule(  
                concert = concert,  
                concertAt = LocalDateTime.now().plusDays(1),  
                reservationAvailableAt = LocalDateTime.now().minusHours(1),  
            ),  
        )  
    scheduleId = schedule.id  
  
    val seat =  
        seatRepository.save(  
            Seat(  
                concertSchedule = schedule,  
                seatStatus = SeatStatus.AVAILABLE,  
                seatNumber = 1,  
                seatPrice = 10000,  
            ),  
        )  
    seatId = seat.id  
  
    // 1000명의 사용자와 큐 생성  
    repeat(1000) {  
        val user = userRepository.save(User(name = "User $it"))  
        userIds.add(user.id)  
        val token = UUID.randomUUID().toString()  
        tokens.add(token)  
        queueRepository.save(  
            Queue(  
                user = user,  
                token = token,  
                joinedAt = LocalDateTime.now(),  
                queueStatus = QueueStatus.PROCESSING,  
            ),  
        )  
    }  
}  
  
@Test  
fun `1000명의 사용자가 동시에 한 좌석 예약 시 한 명만 성공해야 한다`() {  
    val numberOfUsers = 1000  
    val executorService = Executors.newFixedThreadPool(numberOfUsers)  
    val latch = CountDownLatch(numberOfUsers)  
    val successCount = AtomicInteger(0)  
    val failCount = AtomicInteger(0)  
  
    repeat(numberOfUsers) { index ->  
        executorService.submit {  
            val userId = userIds[index]  
            val token = tokens[index]  
  
            try {  
                val reservationRequest =  
                    ReservationServiceDto.Request(  
                        userId = userId,  
                        concertId = concertId,  
                        scheduleId = scheduleId,  
                        seatIds = listOf(seatId),  
                    )  
                val result = reservationService.createReservations(token, reservationRequest)  
                if (result.isNotEmpty()) {  
                    successCount.incrementAndGet()  
                } else {  
                    failCount.incrementAndGet()  
                }  
            } catch (e: Exception) {  
                failCount.incrementAndGet()  
            } finally {  
                latch.countDown()  
            }  
        }  
    }  
    latch.await() // 모든 스레드가 작업을 마칠 때까지 대기  
  
    assertEquals(1, successCount.get(), "오직 한 명의 사용자만 예약에 성공해야 합니다")  
    assertEquals(999, failCount.get(), "999명의 사용자는 예약에 실패해야 합니다")  
  
    // 좌석 상태 확인  
    val reservedSeat = seatRepository.findById(seatId)!!  
    assertEquals(SeatStatus.UNAVAILABLE, reservedSeat.seatStatus, "예약된 좌석의 상태는 UNAVAILABLE이어야 합니다")  
}
```
- 1000명의 동시 예약 요청 중 단 한 명만 성공하는 것을 확인했다..
- 좌석 상태가 정확히 업데이트되는 것을 검증했다.

## 최종 결론

1. 동시성 제어의 효과적인 구현:
    - 분산 락과 비관적 락의 조합을 통해 동시성 제어의 목적을 달성했다.
    - 대규모 동시 요청 상황에서도 데이터 정합성을 유지할 수 있음을 확인했다.

2. 성능과 정확성의 균형:
    - Redis를 활용한 분산 락으로 빠른 응답 시간을 유지하면서도 정확한 동시성 제어를 구현했다.
    - 비관적 락을 2차 안전장치로 사용하여 데이터베이스 수준의 안전성을 보장하도록 했다.

