# 백엔드 동시성 이슈 해결 가이드

## 현재 문제

`createOrGetRoom` API에서 동시에 요청이 들어올 때 `Optional.orElseGet`의 Check-Then-Act 패턴으로 인해 채팅방이 중복 생성됨.

---

## 해결 방안

### 1️⃣ 데이터베이스 UNIQUE 제약 조건 (필수)

**문제를 근본적으로 해결하는 가장 중요한 단계입니다.**

#### SQL 마이그레이션

```sql
-- Flyway 또는 Liquibase 마이그레이션 파일
-- V003__add_unique_constraint_chat_room.sql

ALTER TABLE chat_room
ADD CONSTRAINT uk_chat_room_buyer_seller_product
UNIQUE (buyer_id, seller_id, product_id);

-- 인덱스 추가 (검색 성능 향상)
CREATE INDEX idx_chat_room_lookup
ON chat_room(buyer_id, seller_id, product_id);
```

#### Hibernate/JPA 엔티티

```java
@Entity
@Table(name = "chat_room",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_chat_room_buyer_seller_product",
            columnNames = {"buyer_id", "seller_id", "product_id"}
        )
    }
)
public class ChatRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    // ... 나머지 필드
}
```

---

### 2️⃣ Repository 개선 - Pessimistic Lock 추가

**동시 접근 시 하나의 트랜잭션만 실행되도록 보장**

```java
@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    /**
     * 조회 시점에 행 잠금(WRITE LOCK)을 획득하여
     * 다른 트랜잭션의 쓰기 작업을 차단합니다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ChatRoom c " +
           "WHERE c.buyerId = :buyerId " +
           "AND c.sellerId = :sellerId " +
           "AND c.productId = :productId")
    Optional<ChatRoom> findExistingRoomWithLock(
        @Param("buyerId") Long buyerId,
        @Param("sellerId") Long sellerId,
        @Param("productId") Long productId
    );

    // 락 없는 일반 조회 (읽기용)
    @Query("SELECT c FROM ChatRoom c " +
           "WHERE c.buyerId = :buyerId " +
           "AND c.sellerId = :sellerId " +
           "AND c.productId = :productId")
    Optional<ChatRoom> findExistingRoom(
        @Param("buyerId") Long buyerId,
        @Param("sellerId") Long sellerId,
        @Param("productId") Long productId
    );
}
```

---

### 3️⃣ Service 레이어 개선

**트랜잭션 격리 수준 설정 및 예외 처리**

```java
@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMapper chatRoomMapper;

    /**
     * 채팅방 생성 또는 조회
     * SERIALIZABLE 격리 수준으로 동시성 이슈 완벽 차단
     */
    @Transactional(
        isolation = Isolation.SERIALIZABLE,
        rollbackFor = Exception.class
    )
    public ChatRoomResponse createOrGetRoom(Long buyerId, Long productId, Long sellerId) {
        // 1. Pessimistic Lock을 통해 행 잠금
        var existingRoom = chatRoomRepository.findExistingRoomWithLock(
            buyerId,
            sellerId,
            productId
        );

        // 2. 이미 존재하면 반환
        if (existingRoom.isPresent()) {
            return chatRoomMapper.toResponse(existingRoom.get());
        }

        // 3. 존재하지 않으면 새로 생성
        ChatRoom newRoom = ChatRoom.builder()
            .buyerId(buyerId)
            .sellerId(sellerId)
            .productId(productId)
            .createdAt(LocalDateTime.now())
            .build();

        ChatRoom saved = chatRoomRepository.save(newRoom);
        return chatRoomMapper.toResponse(saved);
    }

    /**
     * 동시 요청 시 중복 생성 방지 (대체 방안)
     * DataIntegrityViolationException을 통해 중복 감지
     */
    @Transactional(
        isolation = Isolation.READ_COMMITTED,
        rollbackFor = Exception.class
    )
    public ChatRoomResponse createOrGetRoomWithFallback(
        Long buyerId, Long productId, Long sellerId
    ) {
        try {
            // 락 없이 조회 시도
            var existing = chatRoomRepository.findExistingRoom(
                buyerId,
                sellerId,
                productId
            );

            if (existing.isPresent()) {
                return chatRoomMapper.toResponse(existing.get());
            }

            // 새로 생성 시도
            ChatRoom newRoom = ChatRoom.builder()
                .buyerId(buyerId)
                .sellerId(sellerId)
                .productId(productId)
                .createdAt(LocalDateTime.now())
                .build();

            ChatRoom saved = chatRoomRepository.save(newRoom);
            return chatRoomMapper.toResponse(saved);

        } catch (DataIntegrityViolationException e) {
            // UNIQUE 제약 조건 위반 발생 시, 다른 트랜잭션이 생성한 것으로 간주
            // 재조회하여 반환
            var existing = chatRoomRepository.findExistingRoom(
                buyerId,
                sellerId,
                productId
            );

            if (existing.isPresent()) {
                return chatRoomMapper.toResponse(existing.get());
            }

            // 예상 불가능한 상황 발생
            throw new ChatRoomCreationException(
                "Failed to create or retrieve chat room after UNIQUE constraint violation",
                e
            );
        }
    }
}
```

---

### 4️⃣ 예외 처리 개선

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
        DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(
                "DUPLICATE_CHAT_ROOM",
                "This chat room already exists"
            ));
    }

    @ExceptionHandler(ChatRoomCreationException.class)
    public ResponseEntity<ErrorResponse> handleChatRoomCreation(
        ChatRoomCreationException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse(
                "CHAT_ROOM_CREATION_ERROR",
                e.getMessage()
            ));
    }
}

public class ChatRoomCreationException extends RuntimeException {
    public ChatRoomCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

---

## 권장 설정 조합

| 환경              | 권장 방식                               | 설명            |
| ----------------- | --------------------------------------- | --------------- |
| **개발/테스트**   | UNIQUE 제약 + findExistingRoom          | 간단하고 충분함 |
| **소규모 서비스** | UNIQUE 제약 + Pessimistic Lock          | 안정적          |
| **대규모 서비스** | UNIQUE 제약 + Optimistic Lock (version) | 높은 동시성     |

---

## Optimistic Lock 대체 방안 (고성능 환경)

```java
@Entity
@Table(name = "chat_room",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_chat_room_buyer_seller_product",
            columnNames = {"buyer_id", "seller_id", "product_id"}
        )
    }
)
public class ChatRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 낙관적 잠금용 버전 필드
    @Version
    private Long version;

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    // ... 나머지 필드
}

// Service에서는 동일한 방식으로 사용
// @Transactional(isolation = Isolation.READ_COMMITTED)로 충분
```

---

## 마이그레이션 체크리스트

- [ ] UNIQUE 제약 조건 추가 (DB 마이그레이션)
- [ ] Repository에 `findExistingRoomWithLock` 메서드 추가
- [ ] Service 메서드 `@Transactional` 설정 검토
- [ ] 예외 처리 개선
- [ ] 단위 테스트 작성 (동시성 테스트 포함)
- [ ] 통합 테스트 실행
- [ ] 스테이징 환경에서 부하 테스트

---

## 성능 고려사항

| 방식             | 처리량     | 동시성     | 복잡도 |
| ---------------- | ---------- | ---------- | ------ |
| UNIQUE 제약      | ⭐⭐⭐⭐   | ⭐⭐       | ⭐     |
| Pessimistic Lock | ⭐⭐⭐     | ⭐⭐       | ⭐⭐   |
| Optimistic Lock  | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |

**권장:** UNIQUE 제약 + Pessimistic Lock (안정성과 성능의 균형)
