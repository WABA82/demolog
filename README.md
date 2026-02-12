# Demolog - SNS 데모 프로젝트

![Java](https://img.shields.io/badge/Java-21-blue?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.10-green?style=flat-square)
![MySQL](https://img.shields.io/badge/MySQL-8.4-orange?style=flat-square)
![Redis](https://img.shields.io/badge/Redis-9.0.1-red?style=flat-square)

**Spring Boot 기반 SNS 데모 프로젝트**입니다.

게시물, 댓글, 게시물 좋아요, 알림 등 SNS의 핵심 기능을 중심으로, 특히 **Post 도메인 설계와 구현에 집중**하여 개발했습니다.

실제 서비스 운영 환경에서 발생할 수 있는 문제를 고려하여 **데이터 중복 생성 방지**, **동시 요청 방어**, **도메인 간 느슨한 결합을 위한 구조 설계** 등을 직접 고민하고 해결하는 것을 목표로 진행한 프로젝트입니다.

## 프로젝트 개요

**Demolog**는 DDD 기반 모놀리식 아키텍처 SNS 데모 프로젝트입니다.

### 주요 특징

- **AOP 기반 멱등성 인프라** - `@Idempotent` 어노테이션을 통한 자동 중복 요청 방지
- **스냅샷 기반 수정 이력 추적** - PostRevision을 통한 게시물 버전 관리 및 감사 추적
- **UNIQUE 제약 기반 동시성 제어** - DB 레벨 중복 방지로 안전한 동시성 처리
- **DDD 계층 분리 및 도메인 중심 설계** - 비즈니스 로직과 기술 구현의 명확한 분리

## 주요 기능

| 도메인 | 주요 기능 | 기술적 특징 |
|--------|-----------|-------------|
| **Auth** | 회원가입, 로그인/로그아웃 | 세션 기반, Spring Security, Redis |
| **Post** | 게시물 CRUD, 페이징 | 멱등성 적용, 권한 검증, UUID v7 |
| **PostComment** | 댓글 CRUD, 페이징 | 멱등성 적용, 게시물 소속성 검증 |
| **PostLike** | 좋아요 추가/취소, 개수 조회 | UNIQUE 제약, 동시성 제어 |
| **PostRevision** | 수정 이력 조회, 버전 복원 | 스냅샷 기반 감사 추적, 버전 관리 |
| **Idempotency** | 멱등성 인프라 | AOP, DB 저장, TTL 관리 |

## 구현 포인트

### 멱등성 패턴 (Idempotency Pattern)

반복된 동일 요청이 단 한 번의 실행 결과를 보장합니다.

**구현 방식:**
- `@Idempotent` 어노테이션 기반 AOP 인터셉터
- `Idempotency-Key` 헤더를 통한 요청 식별 및 추적
- DB의 Idempotency 테이블에 요청 상태와 응답 결과 저장
- 상태 전이: `PENDING` → `COMPLETED` (TTL: 24시간)
- 재시도 시 저장된 응답 즉시 반환 (비즈니스 로직 재실행 안 함)

**동작 흐름:**
```
1. 요청 들어옴 → Idempotency-Key 추출
2. 캐시 확인
   ├─ 완료됨 → 기존 응답 반환
   ├─ 진행중 → IN_PROGRESS 응답 반환
   └─ 없음 → 3단계로 진행
3. 비즈니스 로직 실행
4. 결과 저장 및 응답
```

**적용 예시:**
```java
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
@Idempotent
public ResponseEntity<ApiResponse<PostResponse>> createPost(
    @Valid @RequestBody CreatePostRequest request,
    @AuthenticationPrincipal CustomUserDetails userDetails
) {
    PostResponse response = postApplicationService.createPost(request, userDetails.getUserId());
    return ApiResponse.created(response);
}
```
**클라이언트 멱등 키 관리 전략:**

```
1. 클라이언트는 이벤트 발생 시점에 Idempotency-key(UUIDv7)를 생성 및 메모리에 저장
2. 요청 헤더 멱등 키 추가 및 API 호출
3. 네트워크 지연 등의 문제로 새로고침하게 되면 세션/로컬 스토리지에 'Idempotency-key' 저장
4. 응답이 오면 Idempotency-key 제거
```

**클라이언트 사용 예시:**
```bash
curl -X POST http://localhost:8080/api/posts \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{"title": "...", "content": "..."}'
```

### 스냅샷 기반 수정 이력 추적 (Post Revision History)

게시물 수정 이력을 완벽하게 추적하고 특정 버전으로 복원 가능합니다.

**구현 방식:**
- `Post` 수정 시 **수정 전의 전체 상태를 스냅샷으로 저장** (PostRevision)
- Post 엔티티는 현재 상태만 유지
- 각 수정마다 `revisionNumber` 증가
- 모든 수정자(`modifiedBy`), 수정 시각 기록
- 특정 버전으로 복원 가능 (복원 시에도 스냅샷 저장)

**작동 원리:**
```
1. Post 생성 후 수정 시도
   → 현재 Post 상태를 PostRevision으로 저장
   → Post 엔티티 업데이트

2. 버전 복원 시도
   → 현재 Post 상태를 먼저 PostRevision으로 저장
   → 복원할 PostRevision의 내용을 Post로 복원
```

**버전 관리 예시:**
```
Post ID: 123 (Current: 최신 상태)
├─ Revision 1 (초기 작성)
│  ├─ Title: "Spring Boot 첫 걸음"
│  ├─ Content: "..." (1000자)
│  ├─ revisionNumber: 1
│  └─ createdAt: 2025-02-01 10:00
├─ Revision 2 (첫 수정 전 상태 스냅샷)
│  ├─ Title: "Spring Boot 첫 걸음"
│  ├─ Content: "..." (1000자)
│  ├─ modifiedBy: user-001 (이 사람이 수정함)
│  ├─ revisionNumber: 2
│  └─ createdAt: 2025-02-01 14:30
└─ Revision 3 (두 번째 수정 전 상태 스냅샷)
   ├─ Title: "Spring Boot 완벽 가이드"
   ├─ Content: "..." (1500자)
   ├─ modifiedBy: user-002 (이 사람이 수정함)
   ├─ revisionNumber: 3
   └─ createdAt: 2025-02-02 09:15
```

### 동시성 제어 (UNIQUE 제약)

데이터베이스 레벨에서 중복을 방지하여 안전한 동시성 처리를 구현합니다.

**PostLike 예시:**
```java
@Entity
@Table(name = "POST_LIKE", uniqueConstraints = {
    @UniqueConstraint(name = "uk_post_like_post_user", columnNames = {"post_id", "user_id"})
})
public class PostLike {
    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    private UUID postId;
    private UUID userId;
}
```

**효과:**
- 동일한 사용자가 동일 게시물에 중복 좋아요 불가능
- 동시성 환경에서도 안전 (DB 레벨 제약)
- 중복 시도 → `DataIntegrityViolationException` → 클라이언트 친화적 예외 처리

**동작:**
```
스레드 A: user-001이 post-123에 좋아요 요청
스레드 B: user-001이 post-123에 좋아요 요청 (거의 동시에)
         ↓
        DB UNIQUE 제약 위반 감지
         ↓
    하나의 요청만 성공, 다른 하나는 예외 발생
```

### UUID v7 (Time-based UUID)

시간 순서를 보장하면서 분산 환경에 안전합니다.

**특징:**
- **시간순 정렬 가능** - 생성 순서대로 정렬됨 (데이터베이스 인덱스 효율 향상)
- **분산 환경 안전** - 중앙 ID 생성 서버 불필요
- **비순환성** - 중복 가능성 극히 낮음 (2^80 조합)

**성능 이점:**
```
UUID v4 (Random)          UUID v7 (Time-based)
└─ 랜덤 분포            └─ 시간순 분포
  └─ B+ 트리 성능 저하   └─ B+ 트리 성능 최적화
  └─ 페이지 분할 빈번    └─ 페이지 분할 최소화
```

### 세션 기반 인증

Spring Security + Redis를 활용한 엔터프라이즈급 세션 관리입니다.

**특징:**
- **Spring Security** - 표준 인증/인가 프레임워크
- **Redis 저장소** - 분산 환경 확장성
- **CSRF 보호** - 토큰 기반 CSRF 방어
- **동시 세션 제어** - 중복 로그인 방지 (옵션)

**보안 헤더:**
- `Secure` - HTTPS 전송만 허용
- `HttpOnly` - JavaScript 접근 차단
- `SameSite=Strict` - CSRF 공격 방지


## 아키텍처

### DDD 계층 구조

```
┌─────────────────────────────────────┐
│         Controller (HTTP)            │
│  - 요청/응답 처리, 검증              │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│   Application Service (Use Case)     │
│  - 비즈니스 로직 조율                 │
│  - Repository 호출                   │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│    Domain Service (Business Logic)   │
│  - 핵심 업무 규칙                     │
│  - Model 조작                        │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│    Model (Entity, Value Object)      │
│  - 비즈니스 도메인                    │
│  - 불변성, 일관성 보장                │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│  Repository (Data Access)            │
│  - 데이터 조회/저장                   │
│  - ORM 매핑                          │
└─────────────────────────────────────┘
```

### 패키지 구조

```
com.examples.demolog
├── global/                    # 전역 공통 요소
│   ├── config/               # Security, Session, AOP 설정
│   ├── response/             # 표준 ApiResponse 객체
│   ├── exception/            # 전역 예외 처리, ErrorCode
│   ├── security/             # CustomUserDetails, 보안 필터
│   └── utils/                # JsonUtil 등 유틸리티
│
└── domains/                   # 도메인 모듈 (도메인 주도 설계)
    ├── common/               # 도메인 공통 인프라
    │   └── idempotency/      # 멱등성 인프라
    │       ├── annotation/   # @Idempotent
    │       ├── aspect/       # IdempotencyAspect
    │       ├── model/        # Idempotency 엔티티
    │       ├── service/      # 멱등성 비즈니스 로직
    │       ├── repository/   # 멱등성 조회
    │       ├── dto/          # 응답 DTO
    │       └── exception/    # 멱등성 예외
    │
    ├── auth/                 # 인증 도메인
    │   ├── controller/       # 인증 API
    │   ├── service/          # 인증 비즈니스 로직
    │   ├── repository/       # 사용자 조회
    │   ├── model/            # User 엔티티
    │   ├── dto/              # 요청/응답 DTO
    │   └── exception/        # 인증 예외
    │
    ├── post/                 # 게시물 도메인
    │   ├── controller/       # 게시물 API
    │   ├── service/          # 게시물 비즈니스 로직
    │   ├── repository/       # 게시물 조회
    │   ├── model/            # Post 엔티티
    │   ├── dto/              # 요청/응답 DTO
    │   └── exception/        # 게시물 예외
    │
    ├── postcomment/          # 댓글 도메인
    │   └── ...               # (post와 동일 구조)
    │
    ├── postlike/             # 좋아요 도메인
    │   └── ...               # (post와 동일 구조)
    │
    └── postrevision/         # 게시물 수정 이력 도메인
        └── ...               # (post와 동일 구조)
```

**계층별 책임:**
| 계층 | 책임 | 참조 가능 |
|------|------|---------|
| **Controller** | HTTP 요청 처리, 입력값 검증 | Application Service only |
| **Application Service** | Use Case 조율, 트랜잭션 관리 | Repository, Domain Service |
| **Domain Service** | 비즈니스 규칙 구현 | Repository, Model |
| **Model** | 핵심 비즈니스 로직, 상태 관리 | Value Objects, Enums |
| **Repository** | 데이터 조회/저장 | ORM 프레임워크 |

---

## API 엔드포인트

### 인증 (Auth)
```
POST   /api/auth/signup          - 회원가입
POST   /api/auth/login           - 로그인
POST   /api/auth/logout          - 로그아웃
GET    /api/auth/me              - 현재 사용자 정보
```

### 게시물 (Post)
```
POST   /api/posts                - 게시물 생성 (멱등성 적용)
GET    /api/posts                - 게시물 목록 조회 (페이징)
GET    /api/posts/feed           - 인기 게시물 피드 조회 (좋아요 수 기반 정렬)
GET    /api/posts/{postId}       - 단일 게시물 조회
PUT    /api/posts/{postId}       - 게시물 수정
DELETE /api/posts/{postId}       - 게시물 삭제
```

### 댓글 (PostComment)
```
POST   /api/posts/{postId}/comments                - 댓글 생성 (멱등성 적용)
GET    /api/posts/{postId}/comments                - 댓글 목록 조회 (페이징)
GET    /api/posts/{postId}/comments/{commentId}    - 단일 댓글 조회
PUT    /api/posts/{postId}/comments/{commentId}    - 댓글 수정
DELETE /api/posts/{postId}/comments/{commentId}    - 댓글 삭제
```

### 좋아요 (PostLike)
```
POST   /api/posts/{postId}/likes       - 좋아요 추가 (멱등성 + 동시성 제어)
DELETE /api/posts/{postId}/likes       - 좋아요 취소
GET    /api/posts/{postId}/likes/count - 좋아요 개수 조회
GET    /api/posts/{postId}/likes       - 좋아요 목록 조회 (페이징)
GET    /api/me/likes                   - 내 좋아요 목록 조회 (페이징)
```

### 게시물 수정 이력 (PostRevision)
```
GET    /api/posts/{postId}/revisions              - 수정 이력 목록 조회
POST   /api/posts/{postId}/revisions/{revisionNumber}/restore - 특정 버전으로 복원
```

### Swagger UI
```
GET    /swagger-ui.html          - 상호작용 API 문서
GET    /v3/api-docs              - OpenAPI JSON 스펙
```

## 컨벤션 가이드 문서

자세한 아키텍처, 테스트 전략, 커밋 컨벤션은 `docs/` 폴더의 가이드를 참조하세요.

### [ARCHITECTURE.md](docs/ARCHITECTURE.md)
DDD 기반 아키텍처의 완벽한 가이드입니다.

**포함 내용:**
- 패키지 구조 및 도메인 조직
- 계층별 책임 및 설계 원칙
- Entity, Value Object, Repository 패턴
- DTO와 Exception 핸들링
- 네이밍 컨벤션 (클래스, 메서드)
- 각 계층의 구현 예시

### [TESTING.md](docs/TESTING.md)
테스트 피라미드와 계층별 테스트 전략입니다.

**포함 내용:**
- 테스트 피라미드 및 계층별 테스트 방법
- 단위 테스트 (Model, Domain Service, Application Service)
- 통합 테스트 (Repository, Controller)
- BDD 스타일 테스트 작성
- @Nested, @DisplayName 활용법
- 모든 계층의 구현 예시
- 테스트 실행 및 리포트 확인 방법

### [COMMITS.md](docs/COMMITS.md)
Conventional Commits 규약입니다.

**포함 내용:**
- 커밋 메시지 구조
- 커밋 타입 (feat, fix, docs, refactor, test, chore)
- Scope과 Breaking Changes
- 다양한 시나리오별 예시
- 버전 관리 및 Changelog 연동

## 라이선스

MIT License
