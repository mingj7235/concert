# [콘서트 대기열 시스템의 인덱스 적용과 차후 적용할 MSA 관점에서의 설계]

# 1> 쿼리 분석 및 인덱스 필요성 평가

## 1.1. 인덱스가 필요해 보이는 쿼리

### 1.1.1 사용자 잔액 조회 쿼리
```kotlin
BalanceJpaRepository.findByUserId(userId: Long)
```
- 기능 : 사용자 ID 로 잔액을 조회한다.

- 실제 생성되는 SQL
    ```sql
    SELECT b.* FROM balance b WHERE b.user_id = ?
    ```

- 인덱스가 필요해 보이는 이유
  - 사용자별로 잔액 조회가 빈번하게 일어날 것으로 예상된다.
  - 결제 과정, 잔액 확인 등 다양한 비즈니스 로직에서 자주 사용될 가능성이 높다.
  - 사용자 수가 증가함에 따라 테이블 크기가 커질 것이므로, 인덱스 없이는 성능 저하가 예상된다.

- 인덱스 적용 : `user.id` 
  - Cardinality 고려:
    - `user_id` 컬럼은 높은 Cardinality를 가질 것으로 예상된다. 각 사용자는 고유한 ID를 가지므로, 이 컬럼의 값들은 매우 다양할 것이다.
    - 높은 Cardinality는 인덱스 효율성을 크게 향상시킨다. 데이터베이스는 인덱스를 통해 특정 사용자를 빠르게 찾을 수 있다.
    - 이는 B-트리 인덱스의 장점을 최대한 활용할 수 있게 해주어, 검색 성능을 크게 개선해준다.


- JPA 를 사용 한 방법
  ```kotlin
  @Entity
  @Table(name = "balance", indexes = @Index(name = "idx_user_id", columnList = "user_id"))
  public class Balance {
      ...
  }
  ```
  - DB 에 직접 인덱스를 적용하는 방법
    ```sql
    CREATE INDEX idx_user_id ON balance (user_id);
    ```

- 인덱스 적용으로 인한 기대효과 :
  - 잔액 조회 성능이 크게 향상될 것으로 기대된다.
  - 특히 대량의 데이터에서 특정 사용자의 잔액을 빠르게 조회할 수 있게 될 것으로 기대한다.


- 인덱스 적용 전과 후의 결과
  ```sql
  # 인덱스 적용 전
  +----+-------------+---------+------------+------+---------------+------+---------+------+--------+----------+-------------+
  | id | select_type | table   | partitions | type | possible_keys | key  | key_len | ref  | rows   | filtered | Extra       |
  +----+-------------+---------+------------+------+---------------+------+---------+------+--------+----------+-------------+
  |  1 | SIMPLE      | balance | NULL       | ALL  | NULL          | NULL | NULL    | NULL | 2848   |    10.00 | Using where |
  +----+-------------+---------+------------+------+---------------+------+---------+------+--------+----------+-------------+
  ```
  - 전체 테이블 스캔 (type: ALL) 을 수행한다.
  - 조회해야 할 rows 가 2848 개다. 

  ```sql
  # 인덱스 적용 후
  +----+-------------+---------+------------+------+---------------+-------------+---------+-------+------+----------+-------------+
  | id | select_type | table   | partitions | type | possible_keys | key         | key_len | ref   | rows | filtered | Extra       |
  +----+-------------+---------+------------+------+---------------+-------------+---------+-------+------+----------+-------------+
  |  1 | SIMPLE      | balance | NULL       | ref  | idx_user_id   | idx_user_id | 8       | const |    1 |   100.00 | Using index |
  +----+-------------+---------+------------+------+---------------+-------------+---------+-------+------+----------+-------------+
  ```

  - 인덱스를 사용 (type: ref) 하여 빠르게 결과를 찾을 수 있다.
  - 조회해야 할 rows 가 1개로 줄었다.

<br>

### 1.1.2 콘서트 스케줄 조회 쿼리

```kotlin
ConcertScheduleJpaRepository.findAllByConcertId(concertId: Long)
```
- 기능: 특정 콘서트의 모든 스케줄을 조회한다.
- 실제 생성되는 SQL:
    ```sql
    SELECT cs.* FROM concert_schedule cs WHERE cs.concert_id = ?
    ```

- 인덱스가 필요해 보이는 이유
  - 콘서트별 스케줄 조회가 자주 발생할 것으로 예상된다.
  - 사용자가 특정 콘서트의 일정을 조회하는 경우가 빈번할 것으로 예상한다.
  - 콘서트 수가 많아질수록 전체 스캔은 비효율적이므로, 인덱스가 콘서트 ID 로 필요하다고 판단했다.

- 인덱스 적용: `concert_id` 컬럼에 인덱스 적용
  - Cardinality 고려:
    - `concert_id` 컬럼은 중간에서 높은 Cardinality를 가질 것으로 예상된다. 각 콘서트는 여러 개의 스케줄을 가질 수 있지만, 콘서트 수에 비해 스케줄 수가 상대적으로 많지 않을 것이다.
    - 이 정도의 Cardinality는 인덱스의 효율성을 상당히 높여줄 수 있다. 특정 콘서트의 스케줄을 조회할 때 데이터베이스는 인덱스를 통해 해당 콘서트의 스케줄들을 빠르게 찾을 수 있다.


- JPA를 사용한 방법
    ```kotlin
    @Entity
    @Table(name = "concert_schedule", indexes = [@Index(name = "idx_concert_id", columnList = "concert_id")])
    class ConcertSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
        ...
    }
    ```

- DB에 직접 인덱스를 적용하는 방법:
    ```sql
    CREATE INDEX idx_concert_id ON concert_schedule (concert_id);
    ```

- 인덱스 적용으로 인한 기대효과
  - 특정 콘서트의 스케줄을 빠르게 조회할 수 있게 된다.
  - 콘서트 상세 페이지 로딩 시간이 크게 개선될 것으로 예상된다.


- 인덱스 적용 전과 후의 결과
  ```sql
  # 인덱스 적용 전
  
  +----+-------------+------------------+------------+------+---------------+------+---------+------+--------+----------+-------------+
  | id | select_type | table            | partitions | type | possible_keys | key  | key_len | ref  | rows   | filtered | Extra       |
  +----+-------------+------------------+------------+------+---------------+------+---------+------+--------+----------+-------------+
  |  1 | SIMPLE      | concert_schedule | NULL       | ALL  | NULL          | NULL | NULL    | NULL | 3741   |    10.00 | Using where |
  +----+-------------+------------------+------------+------+---------------+------+---------+------+--------+----------+-------------+
  ```
  - 전체 테이블 스캔 (type: ALL) 을 수행한다.
  - 조회해야 할 rows 가 3741 개다.

  ```sql
  # 인덱스 적용 후
  +----+-------------+------------------+------------+------+------------------+------------------+---------+-------+------+----------+-------------+
  | id | select_type | table            | partitions | type | possible_keys    | key              | key_len | ref   | rows | filtered | Extra       |
  +----+-------------+------------------+------------+------+------------------+------------------+---------+-------+------+----------+-------------+
  |  1 | SIMPLE      | concert_schedule | NULL       | ref  | idx_concert_id   | idx_concert_id   | 8       | const |   32 |   100.00 | Using index |
  +----+-------------+------------------+------------+------+------------------+------------------+---------+-------+------+----------+-------------+
  ```

  - 인덱스를 사용 (type: ref) 하여 빠르게 결과를 찾을 수 있다.
  - 인덱스를 적용 후 조회하는 rows 가 32개로 줄었다.

<br>

### 1.1.3 예약별 결제 정보 조회 쿼리
```kotlin
PaymentJpaRepository.findByReservationId(reservationId: Long)
```

- 기능: 예약 ID로 해당 예약의 결제 정보를 조회한다.

- 실제 생성되는 SQL:
  ```sql
  SELECT p.* FROM payment p WHERE p.reservation_id = ?
  ```

- 인덱스가 필요해 보이는 이유:
  - 예약별 결제 정보 조회가 자주 발생할 것으로 예상된다.
  - 예약 확인, 결제 상태 확인 등에서 빈번하게 사용될 수 있다.
  - 예약 수가 증가함에 따라 전체 스캔은 비효율적이다.


- 인덱스 적용: `reservation_id` 컬럼에 인덱스 적용
  - Cardinality 고려:
    - `reservation_id` 컬럼은 매우 높은 Cardinality를 가질 것으로 예상된다. 각 예약은 고유한 ID를 가지므로, 이 컬럼의 값들은 매우 다양할 것이다.
    - 높은 Cardinality는 인덱스 효율성을 극대화한다. 데이터베이스는 인덱스를 통해 특정 예약의 결제 정보를 매우 빠르게 찾을 수 있다.
    - 이는 B-트리 인덱스의 장점을 최대한 활용할 수 있게 해주어, 검색 성능을 크게 개선할 것이다.


- JPA를 사용한 방법:
    ```kotlin
    @Entity
    @Table(name = "payment", indexes = [@Index(name = "idx_reservation_id", columnList = "reservation_id")])
    class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
    ...
    }
    ```


- DB에 직접 인덱스를 적용하는 방법:
  ```sql
  CREATE INDEX idx_reservation_id ON payment (reservation_id);
  ```

- 인덱스 적용으로 인한 기대효과:
  - 특정 예약의 결제 정보를 빠르게 조회할 수 있게 된다.
  - 예약 확인 페이지 로딩 시간이 개선될 것으로 예상된다.


- 인덱스 적용 전과 후의 결과

  ```sql
  # 인덱스 적용 전
  
  +----+-------------+---------+------------+------+---------------+------+---------+------+--------+----------+-------------+
  | id | select_type | table   | partitions | type | possible_keys | key  | key_len | ref  | rows   | filtered | Extra       |
  +----+-------------+---------+------------+------+---------------+------+---------+------+--------+----------+-------------+
  |  1 | SIMPLE      | payment | NULL       | ALL  | NULL          | NULL | NULL    | NULL | 2873   |    10.00 | Using where |
  +----+-------------+---------+------------+------+---------------+------+---------+------+--------+----------+-------------+
  
  ```
  - 전체 테이블 스캔 (type: ALL) 을 수행한다.
  - 조회해야 할 rows 가 2873 개다.

  ```sql
  # 인덱스 적용 후
  +----+-------------+---------+------------+------+----------------------+----------------------+---------+-------+------+----------+-------------+
  | id | select_type | table   | partitions | type | possible_keys        | key                  | key_len | ref   | rows | filtered | Extra       |
  +----+-------------+---------+------------+------+----------------------+----------------------+---------+-------+------+----------+-------------+
  |  1 | SIMPLE      | payment | NULL       | ref  | idx_reservation_id   | idx_reservation_id   | 8       | const |    1 |   100.00 | Using index |
  +----+-------------+---------+------------+------+----------------------+----------------------+---------+-------+------+----------+-------------+
  ```
  - 인덱스를 사용 (type: ref) 하여 빠르게 결과를 찾을 수 있다.
  - 인덱스를 적용 후 조회하는 rows 가 1개로 줄었다.

<br>

### 1.1.4 만료된 예약 조회 쿼리

```kotlin
ReservationJpaRepository.findExpiredReservations(reservationStatus: ReservationStatus, expirationTime: LocalDateTime)
```

- 기능: 특정 상태의 만료된 예약들을 조회한다.
- 실제 생성되는 SQL:
  ```sql
  SELECT r.* FROM reservation r WHERE r.reservation_status = ? AND r.created_at < ?
  ````

- 인덱스가 필요해 보이는 이유:
  - 만료된 예약을 정기적으로 조회하는 스케쥴러 작업에서 사용된다.
  - 예약 상태와 생성 시간을 함께 고려해야 하므로, 두가지를 조합한 복합 인덱스가 효과적일 것으로 판단했다.
  - 대량의 예약 데이터에서 조건에 맞는 레코드를 빠르게 찾아야 한다.

- 인덱스 적용: `reservation_status` 와 `created_at` 컬럼에 복합 인덱스 적용
  - Cardinality 고려:
    - `reservation_status` 컬럼은 낮은 Cardinality를 가질 것으로 예상된다 (예: '대기중', '완료', '취소' 등 몇 가지 상태만 존재).
    - `created_at` 컬럼은 높은 Cardinality를 가진다 (각 예약마다 고유한 생성 시간을 가짐).
    - 복합 인덱스를 사용함으로써, 낮은 Cardinality의 `reservation_status`로 1차 필터링을 하고, 높은 Cardinality의 `created_at`으로 2차 필터링을 수행하여 효율적인 검색이 가능하도록 한다.
    - 이러한 조합은 인덱스의 선택성(Selectivity)을 높여, 쿼리 성능을 최적화한다.

- JPA를 사용한 방법:
  ```kotlin
  @Entity
  @Table(name = "reservation", indexes = [@Index(name = "idx_status_created_at", columnList = "reservation_status, created_at")])
  class Reservation {
      // 필드 및 메서드
  }
  
  ```

- DB에 직접 인덱스를 적용하는 방법:
  ```sql
  CREATE INDEX idx_status_created_at ON reservation (reservation_status, created_at);
  ```

- 인덱스 적용으로 인한 기대효과
  - 만료된 예약을 빠르게 조회할 수 있게 된다.
  - 스케쥴러 작업의 성능이 크게 개선될 것으로 예상된다.

- 인덱스 적용 전과 후의 결과

  ```sql
  # 인덱스 적용 전
  
  +----+-------------+-------------+------------+------+---------------+------+---------+------+--------+----------+-------------+
  | id | select_type | table       | partitions | type | possible_keys | key  | key_len | ref  | rows   | filtered | Extra       |
  +----+-------------+-------------+------------+------+---------------+------+---------+------+--------+----------+-------------+
  |  1 | SIMPLE      | reservation | NULL       | ALL  | NULL          | NULL | NULL    | NULL | 1732   |     5.00 | Using where |
  +----+-------------+-------------+------------+------+---------------+------+---------+------+--------+----------+-------------+
  
  ```
  - 전체 테이블 스캔 (type: ALL) 을 수행한다.
  - 조회해야 할 rows 가 1732 개다.

  ```sql
  # 인덱스 적용 후
  +----+-------------+-------------+------------+-------+------------------------+------------------------+---------+------+-------+----------+-------------+
  | id | select_type | table       | partitions | type  | possible_keys          | key                    | key_len | ref  | rows  | filtered | Extra       |
  +----+-------------+-------------+------------+-------+------------------------+------------------------+---------+------+-------+----------+-------------+
  |  1 | SIMPLE      | reservation | NULL       | range | idx_status_created_at  | idx_status_created_at  | 9       | NULL | 28    |   100.00 | Using where |
  +----+-------------+-------------+------------+-------+------------------------+------------------------+---------+------+-------+----------+-------------+
  ```

  - 인덱스를 사용 (type: ref) 하여 빠르게 결과를 찾을 수 있다.
  - 인덱스를 적용 후 조회하는 rows 가 28개로 줄었다.

<br>
  
### 1.1.5 콘서트 스케줄별 좌석 조회 쿼리
  ```kotlin
  SeatJpaRepository.findAllByScheduleId(scheduleId: Long)
  ```

- 기능: 특정 콘서트 스케줄의 모든 좌석을 조회한다.

- 실제 생성되는 SQL
  ```sql
  SELECT s.* FROM seat s WHERE s.concert_schedule_id = ?
  ```

- 인덱스가 필요해 보이는 이유:
  - 스케줄별 좌석 조회가 빈번할 것으로 예상된다.
  - 사용자가 특정 공연의 좌석 현황을 조회할 때 자주 사용될 것으로 예상한다.
  - 대규모 공연장의 경우 좌석 수가 많아 전체 스캔은 비효율적이다.


- 인덱스 적용: `concert_schedule_id` 컬럼에 인덱스 적용
  - Cardinality 고려:
    - `concert_schedule_id` 컬럼은 높은 Cardinality를 가질 것으로 예상된다. 각 콘서트 스케줄은 여러 개의 좌석을 가지지만, 스케줄의 수에 비해 좌석의 수가 훨씬 많을 것이다.
    - 이러한 높은 Cardinality는 인덱스의 효율성을 크게 향상시킨다. 데이터베이스는 인덱스를 통해 특정 스케줄의 좌석들을 매우 빠르게 찾을 수 있다.
    - B-트리 인덱스의 특성을 고려할 때, 이 정도의 Cardinality에서는 검색 성능이 대폭 개선될 것으로 예상된다.

- JPA를 사용한 방법:
  ```kotlin
  @Entity
  @Table(name = "seat", indexes = [@Index(name = "idx_concert_schedule_id", columnList = "concert_schedule_id")])
  class Seat {
      // 필드 및 메서드
  }
  ```

- DB에 직접 인덱스를 적용하는 방법
  ```sql
  CREATE INDEX idx_concert_schedule_id ON seat (concert_schedule_id);
  ```

- 인덱스 적용으로 인한 기대효과:
  - 특정 콘서트 스케줄의 좌석을 빠르게 조회할 수 있게 된다.
  - 좌석 선택 페이지의 로딩 시간이 크게 개선될 것으로 예상된다.


- 인덱스 적용 전과 후의 결과

  ```sql
  # 인덱스 적용 전
  
  +----+-------------+-------+------------+------+---------------+------+---------+------+--------+----------+-------------+
  | id | select_type | table | partitions | type | possible_keys | key  | key_len | ref  | rows   | filtered | Extra       |
  +----+-------------+-------+------------+------+---------------+------+---------+------+--------+----------+-------------+
  |  1 | SIMPLE      | seat  | NULL       | ALL  | NULL          | NULL | NULL    | NULL | 1983   |    10.00 | Using where |
  +----+-------------+-------+------------+------+---------------+------+---------+------+--------+----------+-------------+
  
  ```
  - 전체 테이블 스캔 (type: ALL) 을 수행한다.
  - 조회해야 할 rows 가 1732 개다.

  ```sql
  # 인덱스 적용 후
  +----+-------------+-------+------------+------+---------------------------+---------------------------+---------+-------+------+----------+-------------+
  | id | select_type | table | partitions | type | possible_keys             | key                       | key_len | ref   | rows | filtered | Extra       |
  +----+-------------+-------+------------+------+---------------------------+---------------------------+---------+-------+------+----------+-------------+
  |  1 | SIMPLE      | seat  | NULL       | ref  | idx_concert_schedule_id   | idx_concert_schedule_id   | 8       | const | 3    |   100.00 | Using index |
  +----+-------------+-------+------------+------+---------------------------+---------------------------+---------+-------+------+----------+-------------+
  ```

  - 인덱스를 사용 (type: ref) 하여 빠르게 결과를 찾을 수 있다.
  - 인덱스를 적용 후 조회하는 rows 가 3개로 줄었다.

<br>
  

### 1.1.6 기대되는 전체적인 성능 향상 

성능 향상 예상 수치:
- 인덱스 적용 전: O(n) - 전체 테이블 스캔
- 인덱스 적용 후: O(log n) - B-트리 인덱스 검색
- 예상 개선율: 약 85-95%의 쿼리 시간 감소 예상
  - (예: 100만 건 예약 데이터 기준, 인덱스 없이 1초 -> 인덱스 적용 후 50ms 이하)



<br>


## 1.2. 인덱스가 필요하지 않아 보이는 쿼리

위의 내용과는 다르게, 인덱스가 따로 필요하지 않은 쿼리들에 대해서도 분석해보았다.


### 1.2.1 기본 CRUD 작업 (ConcertJpaRepository, UserJpaRepository)

- 대상 쿼리
  - `ConcertJpaRepository` 의 기본 CRUD 작업
  - `UserJpaRepository` 의 기본 CRUD 작업

- 인덱스가 필요하지 않은 이유
  1. 기본키 (PK) 자동 인덱싱: `JPA`에서 `@Id`로 지정된 필드는 자동으로 기본키가 되며, 대부분의 데이터베이스에서 기본키에는 자동으로 인덱스가 생성되기 때문에 불필요하다.
  2. `JpaRepository` 최적화: Spring Data JPA의 JpaRepository 인터페이스는 이미 기본적인 CRUD 작업에 대해 최적화되어 있기 때문에 기본 작업에 있어서는 인덱스가 불필요하다.
  3. 단일 레코드 접근: findById(), save(), delete() 등의 메서드는 주로 단일 레코드에 대한 작업을 수행하므로, 추가 인덱스 없이도 효율적으로 동작한다.


### 1.2.2 ReservationJpaRepository.updateAllStatus

```kotlin
ReservationJpaRepository.updateAllStatus(reservationIds: List<Long>, reservationStatus: ReservationStatus)
```

- 생성되는 SQL:
  ```sql
  UPDATE Reservation r SET r.reservationStatus = :reservationStatus WHERE r.id IN :reservationIds
  ```

- 인덱스가 필요하지 않은 이유:
  1. 기본키 (PK) 사용: 이 쿼리는 이미 기본키(id)를 사용하여 레코드를 특정하고 있다.
  2. IN 절의 효율성: 데이터베이스 옵티마이저는 IN 절을 효율적으로 처리하도록 최적화되어 있다.
  3. 대량 업데이트 특성: 이 쿼리는 여러 레코드를 한 번에 업데이트하는 작업으로, 개별 레코드 접근 시의 인덱스 이점이 크지 않다.

  
### 1.2.3 SeatJpaRepository.updateAllStatus
```kotlin
SeatJpaRepository.updateAllStatus(seatIds: List<Long>, status: SeatStatus)
```

- 생성되는 SQL:
  ```sql
  UPDATE Seat seat SET seat.seatStatus = :status WHERE seat.id IN :seatIds
  ```

- 인덱스가 필요하지 않은 이유:
  1. 기본키 (PK) 사용: `ReservationJpaRepository.updateAllStatus`와 마찬가지로, 이 쿼리도 기본키(id)를 사용하고 있다.
  2. IN 절의 효율성: 데이터베이스는 IN 절을 효율적으로 처리하도록 최적화되어 있다
  3. 대량 업데이트 작업: 여러 좌석의 상태를 한 번에 업데이트하는 작업으로, 개별 인덱스의 이점이 제한적이다.


## 2. 결론

본 분석을 통해 콘서트 대기열 시스템의 주요 쿼리들에 대한 인덱스 적용 방안을 검토했다.
주요 결론은 다음과 같다.

1. 성능 개선:
  - 분석된 5개의 주요 쿼리에 대해 적절한 인덱스를 적용함으로써, 예상 쿼리 실행 시간을 85-95% 감소시킬 수 있을 것으로 기대한다.
  - 특히 사용자 잔액 조회, 콘서트 스케줄 조회, 좌석 조회 등 빈번하게 사용되는 쿼리의 성능이 크게 향상될 것으로 예상된다.

2. Cardinality 고려:
  - 대부분의 제안된 인덱스는 높은 Cardinality를 가진 컬럼에 적용되어, 인덱스의 효율성을 극대화할 수 있다.
  - 만료된 예약 조회 쿼리의 경우, 복합 인덱스를 통해 낮은 Cardinality와 높은 Cardinality 컬럼을 조합하여 최적의 성능을 도출할 수 있다.

3. 시스템 영향:
  - 인덱스 적용으로 인한 약간의 쓰기 성능 저하가 있을 수 있으나, 읽기 작업의 대폭적인 성능 향상으로 상쇄될 것으로 예상된다.
  - 읽기 작업이 대부분의 쿼리 호출의 비중을 차지하므로 전체적인 시스템 응답 시간과 사용자 경험이 크게 개선될 것으로 기대된다.

이러한 인덱스 최적화 전략을 통해 콘서트 대기열 시스템의 전반적인 성능과 확장성이 크게 개선될 것으로 예상된다.
사용자들은 더 빠른 응답 시간과 안정적인 서비스를 경험할 수 있을 것으로 기대된다.


<br>

# 2> MSA 관점에서의 트랜잭션 관리와 Saga 패턴 적용에 대한 설계 보고서 

## 1. 서론
- 본 보고서는 콘서트 티켓 예약 시스템의 마이크로서비스 아키텍처(MSA) 전환을 가정하고, 그 관점에서 트랜잭션 관리 방안과 Saga 패턴의 적용에 대해 다룬다.
- 본 보고서는 '`PaymentService`' 의 결제 기능을 중점으로 설계에 대한 분석을 다루는 것을 목적으로 한다.
- 현재 시스템의 분석을 바탕으로 MSA 환경에서의 효과적인 트랜잭션 관리 전략에 대해 공부하고 정리한 것을 바탕으로 한다.


## 2. 현재 시스템 분석
### 2.1 아키텍처 개요
- 현재 '`PaymentService`' 시스템은 모놀리식 구조로, 다음과 같은 주요 컴포넌트로 구성되어 있다.

#### PaymentService
- 결제 처리의 핵심 로직을 담당하는 서비스
- 사용자 인증, 예약 확인, 결제 처리, 대기열 관리, 콘서트 상태 업데이트 등 전반적인 결제 프로세스를 조율한다. (Facade 의 역할)

#### UserManager
- 사용자 정보 관리 담당
- 사용자 인증, 계정 잔액 확인 등의 기능을 수행한다.

#### ReservationManager
- 예약 정보 관리 담당
- 예약 생성, 조회, 상태 업데이트 등의 기능 수행한다.

#### PaymentManager
- 실제 결제 처리 및 결제 내역 관리 담당
- 결제 실행, 결제 내역 저장, 결제 취소 등의 기능을 수행한다.

#### QueueManager
- 대기열 관리 담당
- Redis를 사용하여 대기열 토큰 관리, 대기열 상태 업데이트 등의 기능을 수행한다.

#### ConcertManager
- 콘서트 정보 관리 담당
- 콘서트 조회, 좌석 상태 확인, 콘서트 상태 업데이트 등의 기능 수행한다. 

#### ConcertCacheManager
- 콘서트 정보의 캐시 관리 담당
- 콘서트 정보 캐싱, 캐시 무효화 등의 기능 수행한다.


<br>

### 2.2 현재 트랜잭션 관리 현황 

- 현재 시스템의 핵심 트랜잭션은 `PaymentService` 의 `executePayment` 메서드에서 관리되고 있다.
- 이 메서드는 `@Transactional` 어노테이션을 통해 하나의 큰 트랜잭션으로 처리되고 있으며, 다음과 같은 주요 작업들을 포함한다

```kotlin
@Transactional
fun executePayment(token: String, userId: Long, reservationIds: List<Long>): List<PaymentServiceDto.Result> {
// 1. 사용자 및 예약 정보 조회
// 2. 결제 실행 및 결제 내역 저장
// 3. 예약 상태 업데이트
// 4. 대기열 토큰 처리
// 5. 콘서트 상태 업데이트
    ...
}
```
- 이러한 구조는 데이터의 일관성을 유지하는 데 도움이 될 수 있지만, 여러 가지 문제점을 내포하고 있다. 아래에서 더 자세하게 알아보자.

<br>

### 2.3 현재 구조의 문제점

#### 1) 긴 트랜잭션으로 인한 성능 저하 가능성
- 하나의 트랜잭션 내에서 여러 복잡한 작업이 수행되므로, 트랜잭션의 지속 시간이 길어질 수 있다.
- 긴 트랜잭션은 데이터베이스 연결을 오랫동안 점유하게 되어, 전체 시스템의 처리량을 저하시킬 수 있다.
- 동시에 여러 결제 요청이 들어올 경우, 트랜잭션 간 경합이 발생하여 성능 저하가 심화될 수 있다.


#### 2) 여러 서비스 간의 강한 결합
- 하나의 트랜잭션 내에서 여러 서비스(User, Reservation, Payment, Queue, Concert)가 밀접하게 연관되어 있다.
- 이러한 강한 결합은 개별 서비스의 독립적인 변경이나 확장을 어렵게 만든다.
- 한 서비스의 변경이 다른 서비스에 영향을 미칠 가능성이 높아, 시스템 유지보수의 복잡성이 증가한다.


#### 3) Redis 작업 포함으로 인한 분산 트랜잭션 문제
- Redis를 사용한 대기열 처리가 동일한 트랜잭션 내에 포함되어 있어, 분산 트랜잭션 문제가 발생할 수 있다.
- 관계형 데이터베이스와 Redis 간의 트랜잭션 일관성을 보장하기 어려워, 데이터 불일치가 발생할 가능성이 있다.
- 네트워크 지연이나 Redis 서버 장애 시, 전체 트랜잭션이 실패할 위험이 있다.


#### 4) 단일 실패 지점(Single Point of Failure) 존재
- 모든 주요 로직이 하나의 서비스에 집중되어 있어, 이 서비스에 문제가 발생하면 전체 결제 시스템이 마비될 수 있다.
- 부분적인 기능 장애가 전체 시스템의 장애로 확대될 가능성이 높다.

#### 5) 개별 서비스의 독립적 확장 어려움
- 모든 기능이 하나의 서비스에 통합되어 있어, 특정 기능만을 선택적으로 확장하기 어렵다.
- 시스템의 일부분에 부하가 집중되더라도, 전체 시스템을 스케일아웃해야 하는 비효율성이 존재한다.
- 각 기능별로 다른 확장 전략을 적용하기 어려워, 리소스 활용의 최적화가 제한된다.

이러한 문제점들은 시스템의 확장성, 유연성, 그리고 장애 대응 능력을 제한하며, 향후 서비스의 성장과 변화에 대응하는 데 어려움을 줄 수 있다. 

따라서 이를 개선하기 위한 MSA 기반의 새로운 설계가 필요하다.

<br>

## 3. MSA 로의 전환
- 현재의 모놀리식 구조에서 MSA 로의 전환은 시스템의 확장성, 유연성, 그리고 장애 대응 능력을 크게 향상시킬 수 있다. 
- 이 섹션에서는 MSA로의 전환 전략을 상세히 설명한다.

### 3.1 서비스 분리
현재 시스템을 다음과 같은 마이크로서비스로 분리한다.

#### 1) Payment Service
- 책임: 결제 처리, 결제 내역 관리
- 주요 기능:
  - 결제 실행 및 검증
  - 결제 내역 저장 및 조회
  - 결제 취소 처리

#### 2) User Service
- 책임: 사용자 정보 관리, 인증 및 권한 처리
- 주요 기능:
  - 사용자 프로필 관리
  - 사용자 인증 및 권한 확인
  - 계정 잔액 관리
  
#### 3) Reservation Service
- 책임: 예약 관리
- 주요 기능:
  - 예약 생성 및 조회
  - 예약 상태 업데이트
  - 예약 취소 처리

#### 4) Queue Service
- 책임: 대기열 관리
- 주요 기능:
  - 대기열 토큰 생성 및 관리
  - 대기열 상태 업데이트
  - 대기열 우선순위 처리

#### 5) Concert Service
- 책임: 콘서트 및 좌석 정보 관리
- 주요 기능:
  - 콘서트 정보 관리
  - 콘서트 스케쥴 관리
  - 좌석 상태 관리
  - 콘서트 상태 업데이트

<br>

### 3.2 트랜잭션 분리
각 서비스별로 트랜잭션을 분리하여 관리함으로써, 전체 시스템의 결합도를 낮추고 개별 서비스의 자율성을 높인다.

#### 1) Payment Transaction
- 범위: 결제 실행 및 결제 내역 저장
  ```kotlin
  @Transactional
  fun executePayment(userId: Long, amount: BigDecimal): Payment {
  // 결제 로직 실행
  // 결제 내역 저장
  }
  ```

#### 2) Reservation Transaction
- 범위: 예약 상태 업데이트
  ```kotlin
  @Transactional
  fun updateReservationStatus(reservationId: Long, status: ReservationStatus) {
  // 예약 상태 업데이트 로직
  }
  ```

#### 3) Concert Transaction
- 범위: 콘서트 상태 업데이트
  ```kotlin
  @Transactional
  fun updateConcertStatus(concertId: Long, status: ConcertStatus) {
      // 콘서트 상태 업데이트 로직
  }
  ```

이러한 트랜잭션 분리를 통해 각 서비스는 자체적인 데이터 일관성을 유지하면서, 전체 시스템의 유연성과 확장성을 향상시킬 수 있다.

<br>

### 3.3 이벤트 기반 아키텍처 도입
- 서비스 간 통신을 위해 이벤트 기반 아키텍처를 도입하도록 설계한다.
- 이를 통해 서비스 간 결합도를 낮추고, 비동기적인 처리를 가능하게 한다.
- Spring의 `ApplicationEventPublisher`와 `@TransactionalEventListener`를 활용한다. 

#### 3.3.1 이벤트 정의

```kotlin
// 예시 
data class PaymentCompletedEvent(val paymentId: Long, val reservationId: Long)
data class ReservationUpdatedEvent(val reservationId: Long, val status: ReservationStatus)
...
```

#### 3.3.2 이벤트 발행
- Spring의 `ApplicationEventPublisher`를 사용하여 이벤트 발행
```kotlin
@Service
class PaymentService(private val eventPublisher: ApplicationEventPublisher) {
    @Transactional
    fun processPayment(paymentDetails: PaymentDetails) {
        // 결제 처리 로직
        val payment = executePayment(paymentDetails)
        eventPublisher.publishEvent(PaymentCompletedEvent(payment.id, payment.reservationId))
    }
}
```

#### 3.3.3 이벤트 구독
- `@TransactionalEventListener`를 사용하여 이벤트 처리
```kotlin
@Service
class ReservationService {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handlePaymentCompleted(event: PaymentCompletedEvent) {
        // 예약 상태 업데이트 로직
        updateReservationStatus(event.reservationId, ReservationStatus.PAID)
    }
}
```

#### 3.3.4 이벤트 처리의 이점
- 비동기 처리: 서비스 간 즉각적인 응답이 필요 없는 경우 비동기적으로 처리
- 느슨한 결합: 서비스 간 직접적인 의존성 제거
- 확장성: 새로운 기능 추가 시 기존 서비스 수정 없이 새로운 EventListener 추가 가능

이벤트 기반 아키텍처의 도입을 통해 시스템의 확장성과 유연성을 크게 향상시킬 수 있으며, 각 서비스의 자율성을 보장하면서도 전체 시스템의 일관성을 유지할 수 있을 것이라 기대한다.

<br>

## 4. MSA 에서 트랜잭션 관리하기 - Saga 패턴 적용

### 4.1 Saga 패턴 개요
- Saga 패턴은 마이크로서비스 아키텍처에서 분산 트랜잭션을 관리하기 위한 효과적인 방법이다. 이 패턴의 핵심 개념은 다음과 같다.

  - 로컬 트랜잭션 시퀀스: 하나의 큰 트랜잭션을 여러 개의 작은 로컬 트랜잭션으로 분할한다.
  - 보상 트랜잭션: 각 단계에서 실패가 발생할 경우, 이전 단계들의 변경사항을 취소하는 보상 트랜잭션을 실행한다.
  - 이벤트 기반 통신: 서비스 간 통신은 이벤트를 통해 이루어진다.


- Saga 패턴을 통해 다음과 같은 이점을 얻을 수 있을 것이라 기대한다.

  - 서비스 간 결합도 감소
  - 개별 서비스의 자율성 증가
  - 시스템 전체의 확장성 및 유연성 향상
  - 장애 상황에서의 복원력 증대

<br>

### 4.2 Saga 패턴 구현

#### 4.2.1 이벤트 정의
- 각 이벤트는 특정 비즈니스 프로세스의 단계를 나타내며, 서비스 간 통신의 기반이 된다.

```kotlin
data class PaymentInitiatedEvent(val userId: Long, val reservationIds: List<Long>)
data class PaymentCompletedEvent(val paymentId: Long, val reservationIds: List<Long>)
data class PaymentFailedEvent(val reservationIds: List<Long>)
data class ReservationsCompletedEvent(val reservationIds: List<Long>)
data class QueueCompletedEvent(val token: String)
data class ConcertStatusUpdatedEvent(val concertId: Long)
data class SagaCompletedEvent(val reservationIds: List<Long>)
data class SagaFailedEvent(val step: SagaStep, val reservationIds: List<Long>)

enum class SagaStep {
    PAYMENT, RESERVATION, QUEUE, CONCERT
}
```

- 각 이벤트의 목적과 의미:
  - PaymentInitiatedEvent: 결제 프로세스의 시작을 알린다. 사용자 ID와 예약 ID 목록을 포함하여 결제 서비스에 필요한 정보를 전달하도록 한다.
  - PaymentCompletedEvent: 결제가 성공적으로 완료되었음을 알린다. 생성된 결제 ID와 관련 예약 ID 목록을 포함한다.
  - PaymentFailedEvent: 결제 실패 시 발생하며, 실패한 예약 ID 목록을 포함한다. 이를 통해 다른 서비스들이 적절한 보상 트랜잭션을 실행할 수 있다.
  - ReservationsCompletedEvent: 예약 상태가 성공적으로 업데이트되었음을 알린다. 이는 결제 완료 후 예약 상태를 '결제 완료'로 변경한 후 발생한다.
  - QueueCompletedEvent: 대기열 처리가 완료되었음을 알린다. 처리된 대기열의 토큰을 포함하여 다음 단계(콘서트 상태 업데이트)로 진행할 수 있게 한다.
  - ConcertStatusUpdatedEvent: 콘서트 상태가 업데이트되었음을 알린다. 이는 전체 Saga 프로세스의 마지막 단계를 나타낸다.
  - SagaCompletedEvent: 전체 Saga 프로세스가 성공적으로 완료되었음을 알린다.
  - SagaFailedEvent: Saga 프로세스 중 어느 단계에서 실패가 발생했는지를 알린다. 실패한 단계(SagaStep)와 관련 예약 ID 목록을 포함하여 적절한 보상 트랜잭션을 실행할 수 있도록 한다.

이러한 이벤트 구조를 통해 각 서비스는 자신의 역할을 수행하고 그 결과를 다른 서비스에 알릴 수 있으며, 실패 시 적절한 보상 조치를 취할 수 있다.

<br>

#### 4.2.2 서비스 구현
- 각 서비스는 특정 도메인의 로직을 처리하고, 관련 이벤트를 발행 및 구독하도록 한다.

**PaymentService**
```kotlin
@Service
class PaymentService(
    private val eventPublisher: ApplicationEventPublisher,
    private val paymentManager: PaymentManager,
) {
    @Transactional
    fun processPayment(userId: Long, reservationIds: List<Long>) {
        try {
            // 결제 로직 실행
            val payment = paymentManager.executePayment(userId, reservationIds)
            eventPublisher.publishEvent(PaymentCompletedEvent(payment.id, reservationIds))
        } catch (e: Exception) {
            eventPublisher.publishEvent(PaymentFailedEvent(reservationIds))
            throw e
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    fun handlePaymentFailed(event: PaymentFailedEvent) {
        // 결제 실패 처리 로직
    }
}
```
- `processPayment` 메소드는 실제 결제 처리를 수행하고, 성공 또는 실패에 따라 적절한 이벤트를 발행하도록 한다.
- 트랜잭션 내에서 결제가 수행되며, 실패 시 예외를 발생시켜 트랜잭션이 롤백되도록 한다.
- `handlePaymentFailed` 메소드는 결제 실패 시 추가적인 처리(예: 로깅, 알림 등)를 수행할 수 있도록 한다.

**ReservationService**
```kotlin
@Service
class ReservationService(
    private val eventPublisher: ApplicationEventPublisher,
    private val reservationManager: ReservationManager,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handlePaymentCompleted(event: PaymentCompletedEvent) {
        completeReservations(event.reservationIds)
        eventPublisher.publishEvent(ReservationsCompletedEvent(event.reservationIds))
    }

    @Transactional
    fun completeReservations(reservationIds: List<Long>) {
        // 예약 상태 업데이트 로직
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    fun handleReservationFailed(event: SagaFailedEvent) {
        if (event.step == SagaStep.RESERVATION) {
            // 예약 실패 처리 로직
        }
    }
}
```
- `handlePaymentCompleted` 메소드는 결제 완료 이벤트를 수신하여 예약 상태를 업데이트하고, 예약 완료 이벤트를 발행한다.
- `completeReservations` 메소드는 실제 예약 상태 업데이트 로직을 수행한다.
- `handleReservationFailed` 메소드는 예약 단계에서 실패가 발생했을 때의 처리 로직을 구현한다.

**QueueService**
```kotlin
@Service
class QueueService(private val eventPublisher: ApplicationEventPublisher) {
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleReservationsCompleted(event: ReservationsCompletedEvent) {
        completeQueue(event.reservationIds)
        eventPublisher.publishEvent(QueueCompletedEvent(getTokenFromReservations(event.reservationIds)))
    }

    private fun completeQueue(reservationIds: List<Long>) {
        // Redis 대기열 처리 로직
    }
}
```
- `@Async` 어노테이션을 사용하여 비동기적으로 대기열 처리를 수행한다.
- 예약 완료 이벤트를 수신하여 대기열 처리를 수행하고, 처리 완료 후 대기열 완료 이벤트를 발행한다.
- Redis를 사용한 대기열 처리 로직은 `completeQueue` 메소드에서 구현된다.

**ConcertService**
```kotlin
@Service
class ConcertService(private val eventPublisher: ApplicationEventPublisher) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleQueueCompleted(event: QueueCompletedEvent) {
        updateConcertStatus(event.token)
        eventPublisher.publishEvent(ConcertStatusUpdatedEvent(getConcertIdFromToken(event.token)))
    }

    @Transactional
    fun updateConcertStatus(token: String) {
        // 콘서트 상태 업데이트 로직
    }
}
```
- 대기열 완료 이벤트를 수신하여 콘서트 상태를 업데이트한다.
- 상태 업데이트 후 콘서트 상태 업데이트 완료 이벤트를 발행한다.

**참고**
- 위의 서비스 로직들은 가안이다.
- `eventPublisher` 는 각각의 도메인에 맞게끔 세부적으로 추후 구현할 예정이다. 

<br>

#### 4.2.3 PaymentSagaOrchestrator 구현

- `PaymentSagaOrchestrator`는 전체 Saga 프로세스를 조율하는 중요한 역할을 한다.

```kotlin
@Service
class PaymentSagaOrchestrator(private val eventPublisher: ApplicationEventPublisher) {
  fun startSaga(userId: Long, reservationIds: List<Long>) {
    eventPublisher.publishEvent(PaymentInitiatedEvent(userId, reservationIds))
  }

  @EventListener
  fun onSagaCompleted(event: ConcertStatusUpdatedEvent) {
    // Saga 완료 처리 로직
  }

  @EventListener
  fun onSagaFailed(event: SagaFailedEvent) {
    when (event.step) {
      SagaStep.PAYMENT -> {
        // 결제 실패 시, 추가 보상 트랜잭션 불필요
      }
      SagaStep.RESERVATION -> {
        // 예약 실패 시, 결제 취소
        eventPublisher.publishEvent(PaymentFailedEvent(event.reservationIds))
      }
      SagaStep.QUEUE -> {
        // 대기열 처리 실패 시, 예약 및 결제 취소
        eventPublisher.publishEvent(SagaFailedEvent(SagaStep.RESERVATION, event.reservationIds))
      }
      SagaStep.CONCERT -> {
        // 콘서트 상태 업데이트 실패 시, 대기열, 예약, 결제 취소
        eventPublisher.publishEvent(SagaFailedEvent(SagaStep.QUEUE, event.reservationIds))
      }
    }
  }
}
```
- `PaymentSagaOrchestrator` 의 주요 흐름
  - Saga 시작: startSaga 메소드를 통해 전체 프로세스를 시작한다. 결제 시작 이벤트를 발행하여 첫 단계를 트리거한다.

  - Saga 완료 처리: onSagaCompleted 메소드는 콘서트 상태 업데이트가 완료되면 호출되어 전체 Saga 프로세스의 성공적인 완료를 처리하도록 한다.

  - 실패 처리 및 보상 트랜잭션: onSagaFailed 메소드는 각 단계에서 발생할 수 있는 실패를 처리한다. 실패 지점에 따라 적절한 보상 트랜잭션을 시작한다.

    - 결제 실패: 추가 조치 불필요
    - 예약 실패: 결제 취소 이벤트 발행
    - 대기열 처리 실패: 예약 취소 이벤트 발행 (이는 연쇄적으로 결제 취소로 이어짐)
    - 콘서트 상태 업데이트 실패: 대기열 처리 취소 이벤트 발행 (이는 연쇄적으로 예약 및 결제 취소로 이어짐)


#### 4.2.4 Saga 패턴으로 설계한 전체 프로세스의 흐름 정리  

1. 사용자가 결제를 시작하면 PaymentSagaOrchestrator.startSaga가 호출된다.
2. PaymentService가 결제를 처리하고 결과 이벤트를 발행한다.
3. 결제 성공 시, ReservationService가 예약 상태를 업데이트한다.
4. 예약 업데이트 성공 시, QueueService가 대기열을 처리한다.
5. 대기열 처리 성공 시, ConcertService가 콘서트 상태를 업데이트한다.
6. 각 단계에서 실패가 발생하면 PaymentSagaOrchestrator.onSagaFailed가 호출되어 적절한 보상 트랜잭션을 시작한다.
7. 모든 단계가 성공적으로 완료되면 PaymentSagaOrchestrator.onSagaCompleted가 호출되어 최종 처리를 수행한다.

- 이 구조를 통해 `PaymentSagaOrchestrator` 전체 프로세스를 조율하면서도, 각 서비스의 자율성을 해치지 않을 것으로 기대된다.
- 또한, 각 단계에서 발생할 수 있는 실패에 대해 적절히 대응하여 시스템의 일관성을 유지할 것으로 기대된다. 
- - 이벤트 기반 통신을 사용함으로써 서비스 간 결합도를 낮추고, 시스템의 확장성과 유연성을 높일 수 있을 것으로 기대된다.

<br>

### 4.3 보상 트랜잭션
- 각 단계에서 실패 시 이전 단계들의 작업을 취소하는 보상 트랜잭션은 다음과 같이 구현될 수 있다.

#### 1) Payment 보상
- 결제 취소 및 환불 처리
```kotlin
@Service
class PaymentService(private val paymentRepository: PaymentRepository) {
    ...
    @Transactional
    fun compensatePayment(paymentId: Long) {
        val payment = paymentRepository.findById(paymentId).orElseThrow()
        payment.status = PaymentStatus.CANCELLED
        payment.refundedAt = LocalDateTime.now()
        paymentRepository.save(payment)
        // 실제 결제 취소 API 호출 (외부 결제 시스템이 있다면..!)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    fun handlePaymentFailed(event: PaymentFailedEvent) {
        event.reservationIds.forEach { reservationId ->
            val payment = paymentRepository.findByReservationId(reservationId)
            payment?.let { compensatePayment(it.id) }
        }
    }
    ...
}
```

#### 2) Reservation 보상
- 예약 상태를 취소로 변경
```kotlin
@Service
class ReservationService(private val reservationRepository: ReservationRepository) {
    ...
    @Transactional
    fun compensateReservation(reservationId: Long) {
        val reservation = reservationRepository.findById(reservationId).orElseThrow()
        reservation.status = ReservationStatus.CANCELLED
        reservationRepository.save(reservation)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    fun handleReservationFailed(event: SagaFailedEvent) {
        if (event.step == SagaStep.RESERVATION) {
            event.reservationIds.forEach { reservationId ->
                compensateReservation(reservationId)
            }
        }
    }
    ...
}
```

#### 3) Queue 보상
- 대기열 토큰을 다시 황성 상태로 원복
```kotlin
@Service
class QueueService(private val redisTemplate: RedisTemplate<String, String>) {
    ...
    @Transactional
    fun compensateQueue(token: String) {
        // Redis에서 토큰 상태를 원복
        redisTemplate.opsForValue().set("queue:$token", QueueStatus.PROCESSING.name)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    fun handleQueueFailed(event: SagaFailedEvent) {
        if (event.step == SagaStep.QUEUE) {
            val token = getTokenFromReservations(event.reservationIds)
            compensateQueue(token)
        }
    }
}
```

#### Concert 보상
- 콘서트 상태 원복
```kotlin
@Service
class ConcertService(private val concertRepository: ConcertRepository) {
    ...
    @Transactional
    fun compensateConcertStatus(concertId: Long) {
        val concert = concertRepository.findById(concertId).orElseThrow()
        // 이전 상태로 롤백 (AVAILABLE로 변경)
        concert.status = ConcertStatus.AVAILABLE
        concertRepository.save(concert)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    fun handleConcertUpdateFailed(event: SagaFailedEvent) {
        if (event.step == SagaStep.CONCERT) {
            val concertId = getConcertIdFromReservations(event.reservationIds)
            compensateConcertStatus(concertId)
        }
    }
}
```

- 각 보상 트랜잭션은 해당 도메인 서비스에 위치하며, `@TransactionalEventListener`를 사용하여 실패 이벤트를 감지하고 처리한다.
- 이렇게 함으로써 각 서비스는 자신의 도메인에 대한 책임을 유지하면서도 전체 Saga 프로세스의 일관성을 보장할 수 있도록 한다.

<br>

## 5. 트랜잭션 관리 개선 사항에 대한 분석

### 5.1 `@TransactionalEventListener` 활용
- `@TransactionalEventListener`는 Spring 에서 제공하는 강력한 기능으로, 트랜잭션의 특정 단계에서 이벤트를 처리할 수 있게 해준다. 
- 이를 통해 얻을 수 있는 이점은 다음과 같다

#### 5.1.1. 트랜잭션 완료 후 이벤트 처리로 데이터 일관성 보장:
- 의미: 트랜잭션이 성공적으로 커밋된 후에만 이벤트가 처리되므로, 데이터베이스의 상태와 이벤트 처리 로직이 일관성을 유지하도록 한다.
```kotlin
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
fun handlePaymentCompleted(event: PaymentCompletedEvent) {
    // 이 메소드는 결제 트랜잭션이 성공적으로 커밋된 후에만 실행된다.
    // 따라서 결제 데이터의 일관성이 보장된 상태에서 후속 처리를 할 수 있다.
}
```

#### 5.1.2. 실패 시 자동 롤백으로 시스템 안정성 향상:
- 의미: 트랜잭션 실패 시 이벤트 리스너가 실행되지 않거나, `AFTER_ROLLBACK` 단계에서 실행되어 적절한 보상 로직을 수행할 수 있다.
```kotlin
@TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
fun handlePaymentFailed(event: PaymentFailedEvent) {
    // 결제 트랜잭션이 실패하고 롤백된 후에 실행된다
    // 여기서 추가적인 실패 처리 로직을 구현할 수 있다.
}
```
#### 5.1.3 서비스 간 결합도 감소로 유지보수성 개선:
- 의미: 이벤트 기반 통신을 사용함으로써 서비스 간 직접적인 의존성을 제거하고, 느슨한 결합을 달성한다.

```kotlin
// PaymentService
@Transactional
fun processPayment(paymentDetails: PaymentDetails) {
    // 결제 처리 로직
    eventPublisher.publishEvent(PaymentCompletedEvent(paymentId, reservationId))
}

// ReservationService
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
fun handlePaymentCompleted(event: PaymentCompletedEvent) {
    // 예약 상태 업데이트 로직
}
```
- 이 구조에서 `PaymentService`는 `ReservationService`의 존재를 몰라도 되며, 단순히 이벤트만 발행한다. 이는 서비스 간 결합도를 크게 낮춘다.

<br> 

### 5.2 비동기 처리
- `@Async` 어노테이션은 Spring의 비동기 실행 기능을 활용하여 메소드를 비동기적으로 실행할 수 있게 해준다. 
- 특히 Redis 작업과 같은 외부 시스템 연동 작업에서 유용하게 적용할 수 있다고 생각한다.

#### 5.2.1. @Async의 역할:
- 메소드를 별도의 스레드에서 비동기적으로 실행하도록 해준다.
- 메소드 호출자는 즉시 반환되며, 실제 작업은 백그라운드에서 진행되도록 한다.


#### 5.2.2. 성능 향상 방식:
- 동기적 처리에서 발생할 수 있는 블로킹을 방지한다.
- 여러 작업을 병렬로 처리할 수 있어 전체 처리 시간을 단축시킨다.

#### 5.2.3. 분산 트랜잭션 문제 해결:
- 외부 시스템과의 작업을 메인 트랜잭션에서 분리하여 처리함으로써, 분산 트랜잭션의 복잡성을 줄이는 것을 기대할 수 있다.
- 실패 시 보상 트랜잭션을 통해 일관성을 유지할 수 있다.

```kotlin
@Service
class QueueService(private val redisTemplate: RedisTemplate<String, String>) {
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleReservationsCompleted(event: ReservationsCompletedEvent) {
        // Redis 작업 수행
        val token = getTokenFromReservations(event.reservationIds)
        redisTemplate.opsForValue().set("queue:$token", QueueStatus.COMPLETED.name)
        
        // 작업 완료 후 다음 단계 이벤트 발행
        eventPublisher.publishEvent(QueueCompletedEvent(token))
    }
}
```
- 이 구조에서 Redis 작업은 메인 트랜잭션과 별도의 스레드에서 비동기적으로 처리된다. 
- 이로 인해 메인 트랜잭션의 지연 시간이 줄어들고, Redis 작업의 실패가 메인 트랜잭션에 직접적인 영향을 미치지 않는다. 
- 또한, 작업 완료 후 다음 단계의 이벤트를 발행함으로써 전체 Saga 프로세스의 흐름을 유지하도록 한다.
- 이러한 접근 방식은 시스템의 응답성을 향상시키고, 각 서비스의 독립성을 강화하며, 전체적인 시스템의 확장성과 유연성을 개선할 것으로 기대된다.

<br>

## 6. 결론

- 본 보고서에서는 콘서트 티켓 예약 시스템의 MSA 전환 과정에서 트랜잭션 관리 방안과 Saga 패턴의 적용에 대해 공부한 내용을 바탕으로 정리와 분석을 진행하고 설계하였다. 
- 주요 결론은 다음과 같다:

### MSA 전환의 필요성:
- 현재의 모놀리식 구조는 확장성, 유연성, 그리고 장애 격리에 한계가 있다. 
- MSA로의 전환을 통해 이러한 문제점들을 해결하고, 시스템의 전반적인 성능과 유지보수성을 향상시킬 수 있다고 기대한다.

### Saga 패턴의 효과성:
- 분산 환경에서의 트랜잭션 관리를 위해 Saga 패턴을 도입함으로써, 서비스 간 데이터 일관성을 유지하면서도 각 서비스의 자율성을 보장할 수 있다. 
- 이는 시스템의 확장성과 유연성을 크게 향상시킨다.

### 이벤트 기반 아키텍처의 이점:
- Spring의 이벤트 시스템을 활용한 이벤트 기반 아키텍처는 서비스 간 결합도를 낮추고, 시스템의 유연성을 높이는 데 효과적이라고 생각한다. 
- 특히 `@TransactionalEventListener`의 활용은 트랜잭션 관리와 이벤트 처리의 통합을 가능하게 한다.

### 비동기 처리의 중요성:
- `@Async` 어노테이션을 활용한 비동기 처리는 시스템의 응답성을 향상시키고, 외부 시스템과의 연동 시 발생할 수 있는 문제를 효과적으로 관리할 수 있게 해준다.

### 보상 트랜잭션의 구현:
- 각 서비스에 대한 보상 트랜잭션을 구현함으로써, 분산 환경에서의 데이터 일관성을 유지하고 시스템의 신뢰성을 보장할 수 있도록 한다.

### 마무리

- 결론적으로, 제안된 MSA 기반의 트랜잭션 관리 및 Saga 패턴 적용은 콘서트 티켓 예약 시스템의 확장성, 유연성, 그리고 장애 대응 능력을 크게 향상시킬 것으로 기대된다.



