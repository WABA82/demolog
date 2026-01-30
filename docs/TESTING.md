# Testing Guide

> DDD 기반 스프링부트 애플리케이션을 위한 테스트 전략 가이드입니다.

---

## 테스트 피라미드

```
        /\
       /  \        E2E 테스트 (선택)
      /----\
     /      \      통합 테스트 (Controller, Repository)
    /--------\
   /          \    단위 테스트 (Model, Service)
  --------------

```

- **단위 테스트**: 가장 많이 작성, 빠른 피드백
- **통합 테스트**: 컴포넌트 간 상호작용 검증
- **E2E 테스트**: 전체 흐름 검증 (선택적)

---

## 레이어별 테스트 전략

| **레이어** | **테스트 종류** | **어노테이션** | **DB** | **Spring Context** |
| --- | --- | --- | --- | --- |
| Model (Entity, VO) | 단위 테스트 | 없음 | X | X |
| Domain Service | 단위 테스트 | 없음 | X | X |
| Application Service | 단위 테스트 | `@ExtendWith(MockitoExtension.class)` | X | X |
| Repository | 슬라이스 테스트 | `@DataJpaTest` | H2 | 부분 |
| Controller | 슬라이스 테스트 | `@WebMvcTest` | X | 부분 |

---

## 1. Model 테스트 (Entity, Value Object) (반드시 테스트)

### 테스트 포인트

- 생성 규칙 (유효성 검증)
- 상태 변경 메서드
- 비즈니스 규칙 위반 시 예외
- 동등성 비교 (Value Object)

### 특징

- 순수 Java 단위 테스트
- Spring Context 불필요 → **가장 빠름**
- 비즈니스 규칙 검증에 집중

### Value Object 테스트 예시

```java
@DisplayName("Money 값 객체")
class MoneyTest {

    @Nested
    @DisplayName("생성")
    class Creation {

        @Test
        @DisplayName("양수 금액으로 생성할 수 있다")
        void createWithPositiveAmount() {
            Money money = Money.of(BigDecimal.valueOf(1000));

            assertThat(money.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        }

        @Test
        @DisplayName("음수 금액으로 생성하면 예외가 발생한다")
        void createWithNegativeAmountThrowsException() {
            assertThatThrownBy(() -> Money.of(BigDecimal.valueOf(-1000)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
```

### Entity 테스트 예시

```java
@DisplayName("Product 엔티티")
class ProductTest {

    @Nested
    @DisplayName("재고 관리")
    class StockManagement {

        @Test
        @DisplayName("재고를 추가할 수 있다")
        void addStock() {
            Product product = createProduct(100);

            product.addStock(50);

            assertThat(product.getStockQuantity()).isEqualTo(150);
        }

        @Test
        @DisplayName("재고보다 많은 수량을 차감하면 예외가 발생한다")
        void removeStockExceedingQuantityThrowsException() {
            Product product = createProduct(100);

            assertThatThrownBy(() -> product.removeStock(150))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("재고가 부족합니다");
        }
    }

    private Product createProduct(int stockQuantity) {
        return Product.create("테스트 상품", "설명", BigDecimal.valueOf(10000), stockQuantity);
    }
}
```

---

## 2. Domain Service 테스트

### 테스트 포인트

- 비즈니스 규칙 검증 (calculateTotalPrice, reserveStock 등)
- 도메인 규칙 위반 시 예외 발생
- 여러 Entity를 사용하는 복잡한 로직

### 특징

- 순수 Java 단위 테스트
- 의존성이 있어도 실제 객체 사용 가능 (가벼운 경우)

### 예시

```java
@DisplayName("StockService 도메인 서비스")
class StockServiceTest {

    private StockService stockService;

    @BeforeEach
    void setUp() {
        stockService = new StockService();
    }

    @Nested
    @DisplayName("재고 예약")
    class ReserveStock {

        @Test
        @DisplayName("충분한 재고가 있으면 예약할 수 있다")
        void reserveStockSuccessfully() {
            Product product = createProduct(100);

            stockService.reserveStock(product, 30);

            assertThat(product.getStockQuantity()).isEqualTo(70);
        }

        @Test
        @DisplayName("재고가 부족하면 예외가 발생한다")
        void reserveStockWithInsufficientStock() {
            Product product = createProduct(10);

            assertThatThrownBy(() -> stockService.reserveStock(product, 20))
                    .isInstanceOf(ProductOutOfStockException.class);
        }
    }
}
```

---

## 3. Application Service 테스트 (선택적으로 테스트)

### 테스트 포인트

- Repository는 Mock 처리
- 중요한 비즈니스 플로우만 테스트
- 트랜잭션 동작 검증

### 특징

- Mockito를 사용한 단위 테스트
- Repository, Domain Service를 Mock 처리
- 유즈케이스 흐름 검증

### 예시

```java
@DisplayName("ProductApplicationService")
@ExtendWith(MockitoExtension.class)
class ProductApplicationServiceTest {

    @InjectMocks
    private ProductApplicationService productApplicationService;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockService stockService;

    @Test
    @DisplayName("상품을 생성할 수 있다")
    void createProduct() {
        // given
        CreateProductRequest request = new CreateProductRequest(
                "새 상품", "설명", BigDecimal.valueOf(10000), 100
        );
        Product savedProduct = Product.create(
                request.name(), request.description(), request.price(), request.stockQuantity()
        );
        given(productRepository.save(any(Product.class))).willReturn(savedProduct);

        // when
        ProductResponse response = productApplicationService.createProduct(request);

        // then
        assertThat(response.name()).isEqualTo("새 상품");
        then(productRepository).should().save(any(Product.class));
    }

    @Test
    @DisplayName("존재하지 않는 상품을 조회하면 예외가 발생한다")
    void getProductNotFound() {
        // given
        Long productId = 999L;
        given(productRepository.findById(productId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productApplicationService.getProduct(productId))
                .isInstanceOf(ProductNotFoundException.class);
    }
}
```

### BDDMockito 패턴

```java
// Stubbing
given(repository.findById(id)).willReturn(Optional.of(entity));

// Verification
then(repository).should().save(any(Entity.class));
then(service).should(never()).doSomething();
```

---

## 4. Repository 테스트

### 테스트 포인트

- CRUD 동작 확인
- 커스텀 쿼리 메서드 검증
- 연관관계 매핑 확인

### 특징

- `@DataJpaTest` 사용 → JPA 관련 빈만 로드
- H2 인메모리 데이터베이스 사용
- 트랜잭션 자동 롤백

### 예시

```java
@DisplayName("ProductRepository 통합 테스트")
@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    @DisplayName("상품을 저장하고 조회할 수 있다")
    void saveAndFind() {
        // given
        Product product = Product.create("테스트 상품", "설명", BigDecimal.valueOf(10000), 100);

        // when
        Product savedProduct = productRepository.save(product);
        Product foundProduct = productRepository.findById(savedProduct.getId()).orElse(null);

        // then
        assertThat(foundProduct).isNotNull();
        assertThat(foundProduct.getName()).isEqualTo("테스트 상품");
    }

    @Test
    @DisplayName("상태별로 상품을 조회할 수 있다")
    void findByStatus() {
        // given
        Product availableProduct = Product.create("판매중 상품", "설명", BigDecimal.valueOf(10000), 100);
        Product discontinuedProduct = Product.create("판매중지 상품", "설명", BigDecimal.valueOf(20000), 50);
        discontinuedProduct.discontinue();

        productRepository.save(availableProduct);
        productRepository.save(discontinuedProduct);

        // when
        List<Product> availableProducts = productRepository.findByStatus(ProductStatus.AVAILABLE);

        // then
        assertThat(availableProducts).hasSize(1);
        assertThat(availableProducts.get(0).getName()).isEqualTo("판매중 상품");
    }
}
```

---

## 5. Controller 테스트

### 테스트 포인트

- HTTP 상태 코드
- 요청 유효성 검증 (`@Valid`)
- 응답 JSON 구조
- 예외 처리 (GlobalExceptionHandler)

### 특징

- `@WebMvcTest` 사용 → Web Layer만 로드
- MockMvc로 HTTP 요청/응답 테스트
- Application Service를 Mock 처리

### 예시

```java
@DisplayName("ProductController 통합 테스트")
@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductApplicationService productApplicationService;

    @Test
    @DisplayName("상품을 생성할 수 있다")
    void createProduct() throws Exception {
        // given
        CreateProductRequest request = new CreateProductRequest(
                "새 상품", "상품 설명", BigDecimal.valueOf(10000), 100
        );
        ProductResponse response = createProductResponse(1L, "새 상품", BigDecimal.valueOf(10000), 100);
        given(productApplicationService.createProduct(any())).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("새 상품"));
    }

    @Test
    @DisplayName("상품명이 없으면 400 에러가 발생한다")
    void createProductWithoutName() throws Exception {
        // given
        CreateProductRequest request = new CreateProductRequest(
                "", "설명", BigDecimal.valueOf(10000), 100
        );

        // when & then
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("존재하지 않는 상품을 조회하면 404 에러가 발생한다")
    void getProductNotFound() throws Exception {
        // given
        Long productId = 999L;
        given(productApplicationService.getProduct(productId))
                .willThrow(new ProductNotFoundException(productId));

        // when & then
        mockMvc.perform(get("/api/products/{productId}", productId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("P001"));
    }
}
```

---

## 테스트 작성 패턴

### 1. given-when-then (BDD 스타일)

```java
@Test
void testMethod() {
    // given - 테스트 준비
    Product product = createProduct(100);

    // when - 테스트 실행
    product.addStock(50);

    // then - 결과 검증
    assertThat(product.getStockQuantity()).isEqualTo(150);
}
```

### 2. @Nested로 테스트 그룹화

```java
@DisplayName("Product 엔티티")
class ProductTest {

    @Nested
    @DisplayName("생성")
    class Creation { ... }

    @Nested
    @DisplayName("재고 관리")
    class StockManagement { ... }

    @Nested
    @DisplayName("상태 변경")
    class StatusChange { ... }
}
```

### 3. @DisplayName으로 한글 테스트명

```java
@Test
@DisplayName("재고보다 많은 수량을 차감하면 예외가 발생한다")
void removeStockExceedingQuantityThrowsException() { ... }
```

### 4. 팩토리 메서드로 테스트 데이터 생성

```java
private Product createProduct(int stockQuantity) {
    return Product.create("테스트 상품", "설명", BigDecimal.valueOf(10000), stockQuantity);
}

private ProductResponse createProductResponse(Long id, String name, BigDecimal price, Integer stockQuantity) {
    return new ProductResponse(id, name, "설명", price, stockQuantity, ProductStatus.AVAILABLE, true, LocalDateTime.now(), null);
}
```

---

## 테스트 실행 명령어

```bash
# 전체 테스트 실행
./gradlew test

# 특정 테스트 클래스 실행
./gradlew test --tests "*.ProductTest"

# 특정 테스트 메서드 실행
./gradlew test --tests "*.ProductTest.createProduct"

# Nested 클래스 테스트 실행
./gradlew test --tests "*.ProductTest\$Creation"

# 테스트 리포트 확인
open build/reports/tests/test/index.html
```

---

## 테스트 의존성

```groovy
dependencies {
    // 테스트 기본
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'

    // H2 인메모리 DB (Repository 테스트용)
    testRuntimeOnly 'com.h2database:h2'
}
```

### spring-boot-starter-test 포함 라이브러리

- JUnit 5
- AssertJ
- Mockito
- JSONPath
- Spring Test / Spring Boot Test

---

## 테스트 설정 파일

### src/test/resources/application.properties

```properties
spring.application.name=spring-boot-monolithic-starter

# H2 Database for Testing
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=false
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
```