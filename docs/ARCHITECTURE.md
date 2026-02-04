# CONVENTION Guide

> DDD 기반 스프링부트 애플리케이션을 위한 컨벤션 전략 가이드입니다.

---

## 📂 Package Structure

> 도메인 중심 구조 + 계층 책임 분리
>

```
com.example.app
│
├── global/                         # 전역 공통 요소
│   ├── config/                     # 전역 설정 (JPA, Security, Web, Kafka 등)
│   ├── response/                   # API 응답 표준 (ApiResponse, ErrorResponse)
│   ├── exception/                  # 전역 예외 처리 (GlobalExceptionHandler, ErrorCode)
│   └── util/                       # 유틸리티 클래스
│
└── domains/                        # 도메인별 모듈
    ├── common/                     # 도메인 공통 인프라
    │   ├── outbox/                 # Transaction Outbox 패턴 (선택)
    │   └── saga/                   # Saga 공통 인프라 (선택)
    │
    └── {domain-name}/              # 예: order, product, customer
        ├── controller/             # REST API 컨트롤러
        ├── service/
        │   ├── application/        # Application Service (유즈케이스 조율)
        │   └── domain/             # Domain Service (도메인 규칙)
        ├── repository/             # Repository 인터페이스
        ├── model/                  # Entity, Value Object, Enum
        ├── dto/
        │   ├── request/            # 요청 DTO
        │   └── response/           # 응답 DTO
        ├── exception/              # 도메인 예외
        ├── event/                  # 도메인 이벤트 (선택)
        ├── saga/                   # Saga 구현 (선택)
        └── kafka/                  # Kafka Producer/Consumer (선택)

```

---

## 🧱 Layer Responsibilities

### Controller

- HTTP 요청/응답 처리
- 요청 검증(@Valid)
- **Application Service만 호출**
- 비즈니스 로직 ❌
- **사용자 정보 필요 시 `@AuthenticationPrincipal` 우선 사용**
  - Controller에서 사용자 정보를 주입받아 Application Service에 전달
  - SecurityContextHolder 직접 사용 금지
  - 예: `@AuthenticationPrincipal CustomUserDetails userDetails`

```java
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductApplicationService productApplicationService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody CreateProductRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails  // 사용자 정보 주입
    ) {
        ProductResponse response = productApplicationService.createProduct(request, userDetails.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(
            @PathVariable Long productId
    ) {
        ProductResponse response = productApplicationService.getProduct(productId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
```

### Application Service

- 유즈케이스 흐름 관리
- 트랜잭션 경계 관리
- 여러 Repository / Domain Service 조합
- **다른 도메인의 Application Service 호출 ❌**
- **사용자 정보 필요 시 매개변수로 수신**
  - SecurityContextHolder 직접 사용 금지
  - Controller에서 `@AuthenticationPrincipal`로 주입받은 사용자 정보를 매개변수로 전달받음
  - 예: `public PostResponse createPost(CreatePostRequest request, UUID userId)`
- **@Transactional 규칙**
  - 클래스 레벨: `@Transactional(readOnly = true)` (기본값)
  - 메서드 레벨: 쓰기 메서드만 `@Transactional` 적용 (override)
  - 이유: 읽기 성능 최적화 + 의도치 않은 쓰기 방지

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductApplicationService {

    private final ProductRepository productRepository;
    private final StockService stockService;

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        Product product = Product.create(
                request.name(),
                request.description(),
                request.price(),
                request.stockQuantity()
        );
        Product savedProduct = productRepository.save(product);
        return ProductResponse.from(savedProduct);
    }

    public ProductResponse getProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        return ProductResponse.from(product);
    }

    @Transactional
    public void removeStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        stockService.reserveStock(product, quantity);
    }
}
```

### Domain Service

- 하나의 Entity에 넣기 애매한 비즈니스 규칙을 담당
- Stateless (상태와 관련된 값을 맴버 변수로 가질 수 없음) ❌
- **자기 도메인만 사용**
- 트랜잭션 관리 ❌
- **@Transactional 사용 금지** ❌
  - Application Service에서 트랜잭션 경계 관리
  - Domain Service는 비즈니스 규칙만 담당

```java
@Service
public class StockService {

    public void reserveStock(Product product, int quantity) {
        if (!product.isAvailable()) {
            throw new ProductOutOfStockException(product.getId(), quantity, 0);
        }
        if (product.getStockQuantity() < quantity) {
            throw new ProductOutOfStockException(
                    product.getId(),
                    quantity,
                    product.getStockQuantity()
            );
        }
        product.removeStock(quantity);
    }

    public void releaseStock(Product product, int quantity) {
        product.addStock(quantity);
    }

    public boolean hasEnoughStock(Product product, int quantity) {
        return product.isAvailable() && product.getStockQuantity() >= quantity;
    }
}
```

### Model (Entity / Value Object)

- 핵심 비즈니스 규칙 보유
- Entity
    - 기본 생성자는 `protected`
    - 객체 생성은 정적 팩토리 메서드 사용
    - Getter만 제공, Setter 금지
    - 비즈니스 의미를 가진 메서드로 상태 변경
    - `@NoArgsConstructor(access = AccessLevel.PROTECTED)`
- Value Object
    - 정적 팩토리 메서드로 생성
    - 생성 시 검증 수행
    - 불변 객체 (final 필드)
    - `equals/hashCode` 오버라이드
    - `@Embeddable` 어노테이션 사용

**Entity 예시**

```java
@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "price", nullable = false))
    private Money price;

    @Column(nullable = false)
    private Integer stockQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status;

    // 정적 팩토리 메서드
    public static Product create(String name, String description, BigDecimal price, Integer stockQuantity) {
        Product product = new Product();
        product.name = name;
        product.price = Money.of(price);
        product.stockQuantity = stockQuantity;
        product.status = ProductStatus.AVAILABLE;
        return product;
    }

    // 비즈니스 메서드
    public void addStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("추가할 재고 수량은 0보다 커야 합니다.");
        }
        this.stockQuantity += quantity;
    }

    public void removeStock(int quantity) {
        int restStock = this.stockQuantity - quantity;
        if (restStock < 0) {
            throw new IllegalStateException("재고가 부족합니다. 현재 재고: " + this.stockQuantity);
        }
        this.stockQuantity = restStock;
    }

    public void discontinue() {
        this.status = ProductStatus.DISCONTINUED;
    }

    public boolean isAvailable() {
        return this.status == ProductStatus.AVAILABLE && this.stockQuantity > 0;
    }
}
```

**Value Object 예시**

```java
@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Money {

    private BigDecimal amount;

    private Money(BigDecimal amount) {
        this.amount = amount;
    }

    // 정적 팩토리 메서드 + 검증
    public static Money of(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("금액은 0 이상이어야 합니다.");
        }
        return new Money(amount);
    }

    public static Money zero() {
        return new Money(BigDecimal.ZERO);
    }

    // 불변 연산
    public Money add(Money other) {
        return new Money(this.amount.add(other.amount));
    }

    public Money multiply(int quantity) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(quantity)));
    }
}
```

### Repository

- JPA 기반 데이터 접근 인터페이스

```java
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByStatus(ProductStatus status);

    List<Product> findByNameContaining(String name);
}
```

### DTO

- API 전송 전용 객체
- Request / Response 분리
- Response는 `from()` 팩토리 메서드 사용

**Request DTO 예시**

```java
public record CreateProductRequest(
        @NotBlank(message = "상품명은 필수입니다.")
        String name,

        String description,

        @NotNull(message = "가격은 필수입니다.")
        @Min(value = 0, message = "가격은 0 이상이어야 합니다.")
        BigDecimal price,

        @NotNull(message = "재고 수량은 필수입니다.")
        @Min(value = 0, message = "재고 수량은 0 이상이어야 합니다.")
        Integer stockQuantity
) {
}
```

**Response DTO 예시**

```java
public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Integer stockQuantity,
        ProductStatus status,
        boolean available
) {
    // from() 팩토리 메서드
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice().getAmount(),
                product.getStockQuantity(),
                product.getStatus(),
                product.isAvailable()
        );
    }
}
```

### Exception

- 도메인별 비즈니스 예외 정의

**ErrorCode 예시**

```java
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "서버 오류가 발생했습니다."),

    // Product
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "상품을 찾을 수 없습니다."),
    PRODUCT_OUT_OF_STOCK(HttpStatus.BAD_REQUEST, "P002", "상품 재고가 부족합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
```

**도메인 예외 예시**

```java
public class ProductNotFoundException extends BusinessException {

    public ProductNotFoundException() {
        super(ErrorCode.PRODUCT_NOT_FOUND);
    }

    public ProductNotFoundException(Long productId) {
        super(ErrorCode.PRODUCT_NOT_FOUND, "상품을 찾을 수 없습니다. ID: " + productId);
    }
}
```

**GlobalExceptionHandler 예시**

```java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        log.error("BusinessException: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        ErrorResponse response = ErrorResponse.of(
                errorCode.getStatus().value(),
                errorCode.getCode(),
                e.getMessage()
        );
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    @ExceptionHandler(BindException.class)
    protected ResponseEntity<ErrorResponse> handleBindException(BindException e) {
        log.error("BindException: {}", e.getMessage());
        ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;
        ErrorResponse response = ErrorResponse.of(
                errorCode.getStatus().value(),
                errorCode.getCode(),
                errorCode.getMessage()
        );
        e.getBindingResult().getFieldErrors()
                .forEach(error -> response.addFieldError(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }
}
```

---

## 🔗 규칙 및 원칙

### 의존성 규칙

**계층 간 참조 규칙**

| 계층 | 참조 가능 | 참조 금지 |
| --- | --- | --- |
| **Controller** | Application Service only | Domain Service, Repository, Model |
| **Application Service** | 자기 도메인의 Repository, Domain Service<br/>다른 도메인의 Repository | 다른 도메인의 Application Service |
| **Domain Service** | 자기 도메인의 Repository, Model | 다른 도메인의 Domain Service, Model |
| **Model** | Value Object, Enum | Repository, Service |

**의존성 흐름**

```
Controller
    ↓
Application Service ← → Repository, Domain Service (같은 도메인)
                  ↓
             Domain Service (다른 도메인의 Repository 직접 참조 가능)
                  ↓
                Model (Value Object, Enum)
```

**핵심 규칙:**
- Controller는 Application Service만 호출
- Application Service는 자기/다른 도메인의 Repository 참조 가능
- Domain Service는 자기 도메인만 조작 (다른 도메인의 Model 참조 ❌)
- Model은 순수 비즈니스 로직만 담당 (계층 의존 ❌)

---

### 보안 규칙

**사용자 정보 관리 원칙:**

- **사용자 정보는 Controller에서만 수신**: `@AuthenticationPrincipal CustomUserDetails` 사용
- **Application Service는 SecurityContextHolder 직접 접근 금지** ❌
- **사용자 정보는 메서드 매개변수로만 전달**: Controller → Application Service
  - 예: `public PostResponse createPost(CreatePostRequest request, UUID userId)`
- **각 계층은 보안 컨텍스트에 독립적**: 테스트 용이성과 계층 분리 원칙 준수

**Controller 예시:**
```java
@PostMapping
public ResponseEntity<ApiResponse<PostResponse>> createPost(
        @Valid @RequestBody CreatePostRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
) {
    // 사용자 정보를 매개변수로 전달
    PostResponse response = postApplicationService.createPost(request, userDetails.getUserId());
    return ApiResponse.created(response);
}
```

---

### 컨트롤러 메서드 파라미터 순서 규칙

**파라미터 배치 원칙: 식별 → 데이터 → 인증 → 메타데이터**

| 순서 | 파라미터 종류 | 어노테이션 | 설명 | 예시 |
| --- | --- | --- | --- | --- |
| 1 | 경로 변수 | `@PathVariable` | 자원을 식별하는 필수 값 (가장 앞) | `UUID postId` |
| 2 | 본문 데이터 | `@RequestBody` | API가 처리할 핵심 데이터 객체 | `CreatePostRequest request` |
| 3 | 쿼리 파라미터 | `@RequestParam` | 필터링, 정렬 등 옵션 값 | `String keyword`, `int page` |
| 4 | 인증/인가 정보 | `@AuthenticationPrincipal` | 현재 사용자 정보 | `CustomUserDetails userDetails` |
| 5 | 페이징 정보 | `Pageable` | 페이징 관련 메타데이터 | `Pageable pageable` |
| 6 | 시스템 파라미터 | - | HttpServletRequest, Locale 등 (가장 마지막) | `HttpServletRequest request` |

**잘못된 예시 ❌:**
```java
@PostMapping
public ResponseEntity<ApiResponse<PostResponse>> createPost(
        @AuthenticationPrincipal CustomUserDetails userDetails,  // ❌ 너무 앞에 배치
        @PathVariable UUID postId,                                // ❌ 식별자가 뒤에
        @Valid @RequestBody CreatePostRequest request             // ❌ 데이터가 마지막
) {
    // ...
}
```

**올바른 예시 ✅:**
```java
@PostMapping("/{postId}/comments")
public ResponseEntity<ApiResponse<PostCommentResponse>> createPostComment(
        @PathVariable UUID postId,                                // ✅ 1. 경로 변수 (식별)
        @Valid @RequestBody CreatePostCommentRequest request,     // ✅ 2. 본문 데이터
        @AuthenticationPrincipal CustomUserDetails userDetails    // ✅ 3. 인증 정보
) {
    PostCommentResponse response = postCommentApplicationService
            .createPostComment(postId, request, userDetails.getUserId());
    return ApiResponse.created(response);
}

@GetMapping("/{postId}/comments")
public ResponseEntity<ApiResponse<Page<PostCommentResponse>>> getPostComments(
        @PathVariable UUID postId,                                // ✅ 1. 경로 변수
        @RequestParam(required = false) String keyword,            // ✅ 2. 쿼리 파라미터
        @PageableDefault(size = 20, sort = "createdAt")
        Pageable pageable                                         // ✅ 3. 페이징 정보
) {
    Page<PostCommentResponse> response = postCommentApplicationService
            .getPostComments(postId, keyword, pageable);
    return ApiResponse.ok(response);
}
```

**규칙의 이점:**
- 메서드 시그니처 일관성: 팀 내 코드 스타일 통일
- 직관성: 가장 중요한 정보(식별자)가 먼저 나타남
- 가독성: 논리적 순서로 파라미터 배치
- 유지보수성: 새로운 팀원도 쉽게 이해 가능

---

### 트랜잭션 규칙

**@Transactional 사용 원칙:**
- **Application Service만 트랜잭션 관리** ✅
- **Domain Service: 트랜잭션 관리 금지** ❌
- **Repository / Model / Controller: 트랜잭션 관리 금지** ❌

**적용 패턴:**
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)  // 클래스 레벨: 기본값
public class PostApplicationService {

    private final PostRepository postRepository;
    private final PostCommentApplicationService postCommentApplicationService;

    @Transactional  // 메서드 레벨: 쓰기 메서드만 명시 (readOnly override)
    public PostResponse createPost(CreatePostRequest request, UUID userId) {
        Post post = Post.create(request.title(), request.content(), userId);
        return PostResponse.from(postRepository.save(post));
    }

    // 읽기 메서드는 클래스 레벨의 readOnly = true 상속
    public PostResponse getPost(UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostException(PostErrorCode.NOT_FOUND));
        return PostResponse.from(post);
    }
}
```

**규칙의 이점:**
- 읽기 메서드 성능 최적화 (readOnly = true)
- 의도치 않은 데이터 변경 방지 (읽기 메서드는 쓰기 차단)
- 계층별 책임 명확화: Application Service만 트랜잭션 경계 관리
- 테스트 용이성: 트랜잭션 로직 한곳에 집중화

---

## 🏷 Naming Convention

### 클래스명

| 구분 | 규칙 | 예시 |
| --- | --- | --- |
| Controller | `{Domain}Controller` | OrderController |
| Application Service | `{Domain}ApplicationService` | OrderApplicationService |
| Domain Service | `{비즈니스개념}Service` | OrderPricingService, StockService |
| Entity | 명사 | Order, Product |
| Value Object | 명사 | Money, Address |
| Repository | `{Entity}Repository` | OrderRepository |
| Request DTO | `{동사}{Domain}Request` | CreateOrderRequest |
| Response DTO | `{Domain}Response` | OrderResponse |
| Exception | `{Domain}{이유}Exception` | OrderNotFoundException |

### 메서드명

| 구분 | 규칙 | 예시 |
| --- | --- | --- |
| Application Service | 유즈케이스 동사 | createOrder, cancelOrder |
| Domain Service | 도메인 규칙 동사 | calculateTotalPrice, reserveStock |
| Entity | 비즈니스 동작 동사 | cancel, confirm, ship |
| Repository | find, save, delete | findByCustomerId, save |
