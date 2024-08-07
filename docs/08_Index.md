# [콘서트 대기열 시스템의 인덱스 적용에 대한 분석 보고서]

## 1> 쿼리 분석 및 인덱스 필요성 평가

### 1.1. 인덱스가 필요해 보이는 쿼리

#### 1.1.1 사용자 잔액 조회 쿼리
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

#### 1.1.2 콘서트 스케줄 조회 쿼리

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

#### 1.1.3 예약별 결제 정보 조회 쿼리
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

#### 1.1.4 만료된 예약 조회 쿼리

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
  
#### 1.1.5 콘서트 스케줄별 좌석 조회 쿼리
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
  

#### 기대되는 전체적인 성능 향상 

성능 향상 예상 수치:
- 인덱스 적용 전: O(n) - 전체 테이블 스캔
- 인덱스 적용 후: O(log n) - B-트리 인덱스 검색
- 예상 개선율: 약 85-95%의 쿼리 시간 감소 예상
  - (예: 100만 건 예약 데이터 기준, 인덱스 없이 1초 -> 인덱스 적용 후 50ms 이하)



<br>


### 1.2. 인덱스가 필요하지 않아 보이는 쿼리

위의 내용과는 다르게, 인덱스가 따로 필요하지 않은 쿼리들에 대해서도 분석해보았다.


#### 1.2.1 기본 CRUD 작업 (ConcertJpaRepository, UserJpaRepository)

- 대상 쿼리
  - `ConcertJpaRepository` 의 기본 CRUD 작업
  - `UserJpaRepository` 의 기본 CRUD 작업

- 인덱스가 필요하지 않은 이유
  1. 기본키 (PK) 자동 인덱싱: `JPA`에서 `@Id`로 지정된 필드는 자동으로 기본키가 되며, 대부분의 데이터베이스에서 기본키에는 자동으로 인덱스가 생성되기 때문에 불필요하다.
  2. `JpaRepository` 최적화: Spring Data JPA의 JpaRepository 인터페이스는 이미 기본적인 CRUD 작업에 대해 최적화되어 있기 때문에 기본 작업에 있어서는 인덱스가 불필요하다.
  3. 단일 레코드 접근: findById(), save(), delete() 등의 메서드는 주로 단일 레코드에 대한 작업을 수행하므로, 추가 인덱스 없이도 효율적으로 동작한다.


#### 1.2.2 ReservationJpaRepository.updateAllStatus

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

  
#### 1.2.3 SeatJpaRepository.updateAllStatus
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


## 2> 결론

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


