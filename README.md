# HamHaeVoca

TOEIC 등 영어 단어 학습을 위해 단어장을 직접 가져오고, 플래시카드·퀴즈·매칭 학습과 학습 기록을 관리하는 **Android 로컬 단어 암기 앱**입니다.

> 이 문서는 프로젝트 인수인계 및 AI 코딩 도구를 이용한 후속 개발을 위한 안내서입니다. `main` 브랜치의 공개 소스 중 주요 Activity, 파서, 데이터베이스와 Gradle 설정을 확인해 작성했습니다. 전체 리소스/테스트 파일 목록과 실제 기기 실행은 별도로 검증하지 않았습니다.

## 1. 개요

### 개발 목적

사용자가 보유한 영어 단어 데이터를 모바일 단어장으로 만들고, 여러 학습 모드에서 암기 여부와 정답·오답 이력을 누적 관리합니다. 별도 서버와 계정이 아닌 Android 기기 내 SQLite 데이터베이스를 중심으로 동작합니다.

### 기술 스택

| 영역 | 사용 기술 |
| --- | --- |
| 플랫폼 | Android (네이티브) |
| 구현 언어 | Java 8 소스/타깃 호환 |
| UI | Android Activity + XML 레이아웃, 기본 View/ListView |
| 데이터 저장 | SQLite (`SQLiteOpenHelper`) |
| 프로젝트/빌드 | Gradle, Android Gradle Plugin 7.3.0 |
| SDK 설정 | `compileSdk 32`, `targetSdk 32`, `minSdk 21` |
| 의존성 | AndroidX AppCompat 1.4.1, Material 1.5.0, ConstraintLayout 2.1.3 |
| 테스트 의존성 | JUnit 4.13.2, AndroidX Test, Espresso |

현재 확인한 코드에서 네트워크 기반 백엔드, 사용자 계정 또는 생성형 AI 기능은 확인되지 않았습니다. 여기서 말하는 **AI 활용 개발**은 AI 코딩 도구가 소스 코드를 이해하고 기능을 확장하는 것을 의미합니다.

## 2. 주요 기능

### 2.1 단어장 관리

- 단어장 목록과 전체/암기/미암기 단어 수 확인
- 단어장 생성, 이름 변경, 삭제
- 개별 단어 검색·추가·수정·삭제 및 암기 상태 관리
- 단어장별 학습 모드 진입, 설정 및 학습 이력 확인

**주의:** 단어장을 삭제하면 연결된 단어 및 퀴즈 기록도 외래 키의 `ON DELETE CASCADE` 정책에 따라 삭제됩니다.

### 2.2 단어 가져오기 및 갱신

- 텍스트 붙여넣기 또는 외부 파일 불러오기
- `.csv`, `.tsv`, 기타 구분자 텍스트 및 `.xlsx` 가져오기
- 탭/쉼표/세미콜론/파이프 구분자 선택 또는 자동 감지
- 단어·뜻·선택적 Day 열 처리
- 유효하지 않은 행과 중복 행을 미리보기에서 표시
- Day별 가져오기 범위 선택
- 기존 단어장에 데이터를 **병합 갱신**하여 학습 기록 보존

#### 가져오기 데이터 예시

첫 번째 행은 헤더로 간주해 건너뛰므로 아래처럼 작성합니다.

```csv
word,meaning,day
abandon,포기하다,1
accurate,정확한,1
benefit,이익,2
```

- 1열: 영단어(`term`), 2열: 뜻(`meaning`), 3열: Day(선택)
- 데이터의 첫 번째 비어 있지 않은 행은 현재 파서 구현상 **항상 건너뜁니다**. 헤더 없는 파일은 첫 단어가 사라질 수 있습니다.
- `.xlsx`는 시트 XML을 직접 읽으며 현재 구현에서는 첫 번째 시트를 사용합니다. 구형 `.xls`는 지원하지 않습니다.
- 텍스트 파일은 UTF-8로 해석하며, `ImportActivity`의 텍스트 파일 입력 제한은 10MB입니다. `.xlsx`는 해당 크기 제한 경로와 별도로 처리됩니다.
- 중복 판정은 가져오기 파서에서 단어·뜻·Day 조합을 기준으로 수행합니다.

### 2.3 플래시카드

- 단어/뜻을 카드 양면으로 학습
- 카드 순서, 앞면 방향, 묶음 크기 등의 옵션
- 암기·미암기 상태 표시 및 오답 횟수 반영
- 플래시카드 세션/설정의 일부 상태를 `SharedPreferences`로 관리

### 2.4 퀴즈

- 퀴즈 조건 설정 후 문제 풀이 및 결과 확인
- 일반 퀴즈와 품사 관련 퀴즈 화면(`PosQuizActivity`) 제공
- 회차별 정답·오답·시간 초과 수 및 응답 기록 저장
- 선택 답변과 정답, 소요 시간 등 문제별 상세 이력 유지

### 2.5 매칭 학습

- 매칭 조건 설정, 문제 진행, 결과 화면 제공
- 매칭 관련 구현 진입점: `MatchingSetupActivity`, `MatchingActivity`, `MatchingResultActivity`

## 3. 프로젝트 구조

주요 소스 코드와 Android Manifest를 기준으로 확인한 구조입니다. 아래는 **핵심 파일 중심의 구조**이며 전체 저장소 파일을 빠짐없이 열거한 것은 아닙니다.

```text
HamHaeVoca/
├── app/
│   ├── build.gradle                 # Android 앱 빌드 및 의존성
│   └── src/main/
│       ├── AndroidManifest.xml      # Activity 등록, 런처 지정
│       ├── java/com/example/hamhaevoca/
│       │   ├── MainActivity.java             # 단어장 목록/관리
│       │   ├── ImportActivity.java           # 파일·텍스트 가져오기/갱신
│       │   ├── BookDetailActivity.java       # 단어 목록/검색/편집
│       │   ├── BookSettingsActivity.java     # 단어장 설정
│       │   ├── FlashcardActivity.java        # 플래시카드 학습
│       │   ├── QuizSetupActivity.java        # 퀴즈 조건 설정
│       │   ├── QuizActivity.java             # 퀴즈 진행
│       │   ├── PosQuizActivity.java          # 품사 퀴즈
│       │   ├── QuizResultActivity.java       # 퀴즈 결과
│       │   ├── MatchingSetupActivity.java    # 매칭 조건 설정
│       │   ├── MatchingActivity.java         # 매칭 학습
│       │   ├── MatchingResultActivity.java   # 매칭 결과
│       │   ├── data/
│       │   │   └── DatabaseHelper.java       # SQLite 스키마 및 CRUD/학습 기록
│       │   ├── parser/
│       │   │   ├── DelimitedTextParser.java  # CSV/TSV 등 구분자 파싱
│       │   │   └── XlsxParser.java           # XLSX 첫 시트 파싱
│       │   └── model/                        # VocabItem, BookSummary,
│       │                                      # ParseResult, ParsedRow,
│       │                                      # MergeResult, QuizAnswerData 등
│       └── res/                              # 화면 레이아웃 및 Android 리소스
├── gradle/wrapper/                  # Gradle Wrapper
├── build.gradle                     # 최상위 Android Gradle Plugin 정의
├── settings.gradle                  # 프로젝트명 및 :app 모듈 등록
├── gradle.properties
├── gradlew
└── gradlew.bat
```

### 코드 계층의 역할

| 계층 | 관련 파일 | 책임 |
| --- | --- | --- |
| 화면/상호작용 | `*Activity.java` | 사용자 입력, 화면 전환, UI 출력 |
| 데이터 입력 | `parser/*` | 외부 데이터 파싱·정규화·유효성 검증 |
| 도메인 모델 | `model/*` | 단어, 단어장, 파싱 및 퀴즈 데이터 전달 |
| 영속성 | `data/DatabaseHelper.java` | SQLite 저장, 조회, 갱신, 통계, 학습 이력 |
| UI 리소스 | `res/*` | 레이아웃, 문자열, 스타일, 이미지 등 |

이 프로젝트는 현재 Activity에서 데이터베이스 헬퍼를 직접 호출하는 구조입니다. MVVM/Repository 계층이 이미 존재한다고 가정하지 마세요.

## 4. 데이터 구조와 흐름

### 4.1 SQLite 스키마

`DatabaseHelper`의 데이터베이스 파일명은 `hamhaevoca.db`, 스키마 버전은 `1`입니다.

| 테이블 | 핵심 필드 | 용도 |
| --- | --- | --- |
| `books` | `id`, `name`, `source_type`, `created_at`, `updated_at` | 단어장 |
| `words` | `id`, `book_id`, `term`, `meaning`, `sort_order`, `memorized`, `correct_count`, `incorrect_count` | 개별 단어·암기 통계 |
| `quiz_attempts` | `id`, `book_id`, `mode`, `time_limit`, `total_count`, `correct_count`, `incorrect_count`, `timed_out_count` | 퀴즈 회차 요약 |
| `quiz_answers` | `id`, `attempt_id`, `item_id`, `direction`, `term_snapshot`, `meaning_snapshot`, `prompt`, `selected_answer`, `correct_answer`, `is_correct`, `timed_out`, `response_time_ms` | 문항별 결과 스냅샷 |

```text
books (1) ─── (N) words
   │
   └──── (N) quiz_attempts (1) ─── (N) quiz_answers
```

`quiz_answers.item_id`는 단어 식별자를 보관하지만, 현재 생성 SQL에서는 `words`에 대한 외래 키 제약이 없습니다. 문제 당시 단어/뜻을 스냅샷으로 보관하므로 단어를 수정하더라도 과거 퀴즈 내용을 일부 재현할 수 있습니다.

### 4.2 단어 가져오기 흐름

```text
파일 선택 또는 텍스트 붙여넣기
    ↓
ImportActivity
    ├─ 구분자 텍스트 → DelimitedTextParser
    └─ .xlsx → XlsxParser → DelimitedTextParser.normalize()
    ↓
ParseResult / ParsedRow
    ↓
유효성 검사 + 중복 검사 + Day 필터 + 미리보기
    ↓
새 단어장: DatabaseHelper.createBookWithWords()
기존 단어장: DatabaseHelper.mergeBookWords()
    ↓
SQLite: books / words
```

### 4.3 단어장 갱신 규칙 (중요)

`mergeBookWords()`는 트랜잭션 내에서 다음 순서로 처리합니다.

1. 공백·대소문자를 정규화한 **단어 + 뜻**이 일치하면 기존 항목을 재사용합니다.
2. 완전 일치가 없어도 정규화된 **단어가 양쪽에서 유일**하면 기존 단어의 뜻을 갱신할 수 있습니다.
3. 매칭되지 않으면 새 단어를 추가합니다.
4. 파일에서 누락된 기존 단어는 **삭제하지 않고 뒤로 배치**합니다.
5. 기존 항목의 `memorized`, `correct_count`, `incorrect_count`는 유지합니다.

후속 개발 시 이 규칙을 임의로 삭제·변경하면 사용자 학습 데이터가 유실될 수 있습니다.

### 4.4 학습 결과 저장

```text
학습 화면
    ↓
사용자 답변/암기 상태
    ↓
DatabaseHelper
    ├─ words.memorized 변경
    ├─ words.correct_count / incorrect_count 누적
    └─ 퀴즈의 경우 quiz_attempts + quiz_answers 저장
    ↓
단어장 통계 / 결과 화면
```

## 5. 실행 및 개발 환경

1. Android Studio에서 저장소 루트를 엽니다.
2. Gradle Sync를 실행합니다.
3. 프로젝트 설정에 맞는 Android SDK Platform 32 및 필요한 빌드 도구를 설치합니다.
4. Android 에뮬레이터 또는 Android 5.0(API 21) 이상 기기에서 `app` 모듈을 실행합니다.

명령줄 빌드:

```bash
# macOS / Linux
./gradlew assembleDebug

# Windows
.\gradlew.bat assembleDebug
```

테스트 명령 예시:

```bash
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
```

> 테스트 라이브러리 의존성은 확인했지만 테스트 케이스의 범위나 빌드 성공 여부는 확인하지 못했습니다. Gradle/JDK 호환성은 실제 환경에서 검증해야 합니다.

## 6. AI를 이용한 후속 개발 가이드

### 개발 전에 읽어야 할 파일

| 변경할 기능 | 우선 확인할 코드 |
| --- | --- |
| 단어장 목록/삭제 | `MainActivity`, `DatabaseHelper` |
| 단어 가져오기 | `ImportActivity`, `DelimitedTextParser`, `XlsxParser`, `ParseResult` |
| 기존 단어장 갱신 | `DatabaseHelper.mergeBookWords()`, `ImportActivity` |
| 단어 검색·편집 | `BookDetailActivity`, `VocabItem`, `DatabaseHelper` |
| 플래시카드 | `FlashcardActivity`, `DatabaseHelper` |
| 퀴즈 | `QuizSetupActivity`, `QuizActivity`, `PosQuizActivity`, `QuizResultActivity`, `QuizAnswerData` |
| 매칭 | `MatchingSetupActivity`, `MatchingActivity`, `MatchingResultActivity` |
| 저장 구조 변경 | `DatabaseHelper.onCreate()`, `onUpgrade()`, 관련 모델 |
| 화면 디자인 | 대상 Activity와 대응하는 `res/layout` 및 스타일 리소스 |

### AI 코딩 도구에 지시할 프로젝트 규칙

- 기존 Java + Activity + XML + SQLite 구조를 먼저 파악하고, 요청하지 않은 전체 프레임워크 전환은 하지 않습니다.
- UI 변경 시 Java 코드의 `R.id` 참조와 XML 레이아웃을 함께 확인합니다.
- Activity 간 `Intent` extra 키(예: `book_id`, `update_book_id`)를 호환되게 유지합니다.
- 저장 모델 변경 시 DB 버전 상승과 `onUpgrade()` 마이그레이션을 구현합니다. DB를 삭제하고 재생성하는 방식으로 사용자 데이터를 잃지 않습니다.
- 단어장 갱신 시 기존 암기 상태·정오답 횟수·퀴즈 기록을 보존합니다.
- 퀴즈 정오답 처리 시 현재 단어 통계와 회차별 스냅샷의 역할을 구분합니다.
- 파서 변경 시 헤더 처리, UTF-8, 구분자 따옴표 이스케이프, Day 값, 중복 처리, 빈 칸 등 경계 조건을 테스트합니다.
- 기존 화면의 기능을 제거하거나 저장 데이터 의미를 변경해야 한다면 먼저 변경 이유와 호환성 영향 범위를 설명합니다.
- 구현 완료 후 최소한 관련 모듈 빌드, 주요 화면 동작, 데이터 보존 시나리오를 검증합니다.

### 기능 추가에 권장하는 절차

1. 관련 Activity → model → parser/database → XML 리소스의 의존 관계를 확인합니다.
2. 기존 사용자 데이터에 영향을 주는지 검토합니다.
3. 변경 범위를 좁혀 구현하고, 필요하면 별도의 서비스/저장소 계층을 점진적으로 도입합니다.
4. 정상 입력·빈 입력·중복·재실행·기존 데이터 업그레이드 등을 테스트합니다.
5. 변경된 기능과 데이터 규칙을 README에 동기화합니다.

### AI 개발 요청 프롬프트 예시

```text
이 저장소는 Java/Android Activity/XML/SQLite 기반 단어 학습 앱 HamHaeVoca입니다.
먼저 README.md와 변경 대상 Activity, model, parser, DatabaseHelper를 확인하세요.

목표: [추가 또는 수정하려는 기능]

요구사항:
1. 현재 구현 흐름과 수정이 필요한 파일을 먼저 정리하세요.
2. 기존 단어장 및 학습 기록의 하위 호환성을 유지하세요.
3. DB 스키마가 바뀌면 버전과 마이그레이션 코드를 함께 수정하세요.
4. UI 및 관련 리소스 수정 범위를 제시하고 구현하세요.
5. 관련 테스트, 빌드 확인 항목, 예상되는 회귀 위험을 보고하세요.
6. 확인하지 않은 동작은 구현된 기능이라고 추정하지 마세요.
```

## 7. 알려진 제약 및 개선 시 검토할 사항

- **헤더 없는 데이터:** 첫 비어 있지 않은 행을 무조건 건너뛰는 현재 파서 정책 확인 필요.
- **XLSX 처리:** 첫 시트만 읽고 `.xls`는 지원하지 않음. 대형 파일 및 다양한 Excel 서식 호환성 테스트 필요.
- **데이터 마이그레이션:** 현재 `DB_VERSION = 1`, `onUpgrade()`에 마이그레이션 로직이 없음.
- **UI와 비즈니스 로직 결합:** Activity에 화면 처리와 일부 흐름 제어 로직이 함께 있어 기능 확대 시 테스트 가능한 계층 분리 고려.
- **대용량 입력:** 미리보기에서 각 행을 View로 생성하므로 대량 단어 처리 시 스크롤·메모리 성능 검토 필요.
- **테스트 범위:** 현재 포함된 테스트의 실제 내용과 실행 결과는 별도 확인 필요.
- **오프라인 백업:** 로컬 DB 중심 구조이므로 기기 변경/앱 삭제 시 데이터 보관·복구 방안을 별도 설계할 필요가 있음.

## 8. 참고

- 저장소: https://github.com/sosomeet/HamHaeVoca
- Android 앱 설정: `app/build.gradle`
- 진입점: `app/src/main/AndroidManifest.xml`의 `MainActivity`
- 데이터 스키마 및 병합 정책: `app/src/main/java/com/example/hamhaevoca/data/DatabaseHelper.java`

이 문서는 프로젝트의 현재 동작을 우선 설명합니다. **향후 구현 계획을 현재 기능으로 혼동하지 않도록** 신규 기능 추가 후 이 문서도 갱신해 주세요.
