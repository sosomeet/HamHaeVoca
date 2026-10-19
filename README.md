# HamHaeVoca (함해보카)

Android Studio로 개발한 **로컬 기반 영어 단어 학습 앱**입니다. 단어장을 만들고 파일에서 단어를 가져오며, 낱말카드·객관식 시험·품사 시험·단어 맞추기를 통해 학습할 수 있습니다. 암기 여부와 정답/오답 기록은 기기에 저장됩니다.

> **기준 코드:** 공유된 `app.zip`과 예문 기능 수정본 `HamHaeVoca_example_update.zip`을 기준으로 작성했습니다. 예문 기능은 수정본을 실제 프로젝트에 적용한 경우에만 사용할 수 있습니다. 수정본의 실제 Android 기기 빌드 및 실행 검증은 아직 완료되지 않았습니다.

## 주요 기능

| 구분 | 기능 |
| --- | --- |
| 단어장 | 생성, 검색, 폴더 분류, 상세 보기 및 설정 |
| 가져오기 | CSV / TSV / XLSX 파일 불러오기, 미리보기, Day 선택, 중복·오류 행 검사 |
| 갱신 | 기존 단어장에 파일 데이터를 병합하고 학습 기록 유지하도록 설계 |
| 낱말카드 | 영어/뜻 앞면 선택, 순서·묶음 설정, 스와이프 학습, 학습 이어하기 |
| 시험 | 객관식 / 품사 문제, 문제 수·제한 시간·최소 오답 횟수 설정, 결과 확인 |
| 맞추기 | 단어-뜻 연결, 제한 시간 및 오답 횟수 필터, 결과 확인 |
| 학습 기록 | 암기 상태, 정답 횟수, 오답 횟수 및 시험 응답 기록 저장 |
| 예문 (수정본) | 정답 공개 시 하단 구분선 아래 예문 표시, 본문/예문 크기 전역 설정 |

## 기술 스택

- **Language:** Java 8
- **UI:** Android XML Layout / Android Views
- **Database:** SQLite (`SQLiteOpenHelper`)
- **Build:** Gradle, Android Gradle Plugin 기반
- **Android:** `minSdk 21`, `compileSdk 32`, `targetSdk 32` (제공된 `app/build.gradle` 기준)
- **Application ID:** `com.example.hamhaevoca`
- **주요 의존성:** AndroidX AppCompat, Material Components, ConstraintLayout

## 프로젝트 구조

```text
app/
├── build.gradle
└── src/main/
    ├── AndroidManifest.xml
    ├── java/com/example/hamhaevoca/
    │   ├── MainActivity.java             # 단어장 목록, 검색, 폴더
    │   ├── BookDetailActivity.java       # 단어장 상세, 단어 목록
    │   ├── BookSettingsActivity.java     # 단어장 설정
    │   ├── ImportActivity.java           # 파일 가져오기·갱신 UI
    │   ├── FlashcardActivity.java        # 낱말카드 학습
    │   ├── QuizSetupActivity.java        # 시험 설정
    │   ├── QuizActivity.java             # 객관식 시험
    │   ├── PosQuizActivity.java          # 품사 시험
    │   ├── QuizResultActivity.java       # 시험 결과
    │   ├── MatchingSetupActivity.java    # 맞추기 설정
    │   ├── MatchingActivity.java         # 맞추기 진행
    │   ├── MatchingResultActivity.java   # 맞추기 결과
    │   ├── data/
    │   │   └── DatabaseHelper.java       # SQLite 및 데이터 갱신
    │   ├── model/                        # 단어·시험·파싱 데이터 모델
    │   ├── parser/
    │   │   ├── DelimitedTextParser.java  # CSV/TSV 정규화
    │   │   └── XlsxParser.java           # XLSX 첫 시트 읽기
    │   └── util/                         # 시험 엔진 등 보조 로직
    └── res/
        ├── layout/                       # 화면 및 다이얼로그 XML
        ├── drawable/                     # UI 리소스
        ├── menu/
        └── values/
```

## 시작하기

1. Android Studio에서 프로젝트 루트 폴더를 엽니다. (`app/` 폴더만 받은 경우에는 상위 Gradle 프로젝트 파일도 필요합니다.)
2. Gradle Sync가 완료될 때까지 기다립니다.
3. Android 5.0 (API 21) 이상 에뮬레이터 또는 기기를 연결합니다.
4. `app` 실행 구성을 선택하고 Run을 실행합니다.

### 예문 기능 수정본 적용

기존 프로젝트를 백업한 후 `HamHaeVoca_example_update.zip`의 `src/main/...` 파일들을 기존 프로젝트 `app/src/main/...`의 같은 상대 경로에 덮어씁니다. **기존 데이터를 유지하려면 앱을 삭제하거나 앱 데이터 초기화를 하지 마세요.** 변경 후 Gradle 빌드와 실제 단말 동작 확인이 필요합니다.

## 단어장 파일 형식

첫 번째 시트를 읽는 XLSX 및 구분자 텍스트 파일을 지원합니다. 첫 행은 헤더로 사용하며, 다음과 같은 열 배치를 기준으로 합니다.

| 열 | 헤더 | 설명 | 필수 |
| --- | --- | --- | --- |
| A | `Vocabulary` | 영어 단어 또는 표현·문장 구조 | 예 |
| B | `Mean` | 한국어 뜻·해석 | 예 |
| C | `Day` | 학습 단위 | 아니요 |
| D | `Category` | 분류 정보(원본 파일 열) | 아니요 |
| E | `Example` | 낱말카드 정답 공개 시 표시할 예문 | 아니요, **예문 수정본에서 지원** |

예시:

```csv
Vocabulary,Mean,Day,Category,Example
although,비록 ~이지만,1,양보·대조,"Although it was raining, we went out."
because,~ 때문에,1,원인·결과,"I stayed home because I was tired."
```

- **기존 Unit4 형식:** `Vocabulary`, `Mean`, `Day`, `Category` 열을 유지하면 됩니다.
- **새 Conj 형식:** 같은 열에 `Example` 열을 추가합니다. 예문이 없는 행은 기존처럼 표시합니다.
- `Example` 열은 **헤더 이름을 기준으로 감지**합니다. 헤더는 `Example` 또는 `예문`을 사용하세요.
- 현재 가져오기/학습에서 `Category` 값의 독립적인 영속 저장 또는 분류 활용은 보장하지 않습니다. 실제 단어장 폴더와는 별개입니다.
- 첫 시트와 헤더 처리 방식에 맞게 파일을 준비하고, 불러오기 미리보기에서 유효한 행 수를 확인하세요.

## 낱말카드와 예문

기본 카드 동작은 유지하고, 예문 데이터가 있는 단어에 한해서 **정답을 공개한 뒤** 회색의 얇은 구분선 아래 예문을 표시하도록 수정했습니다.

```text
[ 정답 확인 전 ]             [ 정답 확인 후 ]
┌──────────────────┐       ┌──────────────────┐
│                  │       │                  │
│       단어       │  →    │        뜻        │
│                  │       │                  │
│                  │       │ ──────────────── │
│                  │       │       예문       │
└──────────────────┘       └──────────────────┘
```

- 앞면이 **영어인지 한국어인지와 관계없이** 예문 표시 조건은 같습니다.
- 본문 단어·뜻의 중앙 위치가 하단 예문 표시 때문에 바뀌지 않도록 별도 영역으로 구성합니다.
- 예문이 비어 있으면 구분선과 예문 영역을 숨깁니다.
- 낱말카드 설정에서 **본문 크기(12–60sp)**, **예문 크기(10–36sp)**를 설정합니다.
- 크기 설정은 `SharedPreferences`에 저장되어 **모든 단어장에 공통 적용**됩니다.

## 데이터베이스

`data/DatabaseHelper.java`에서 SQLite를 관리합니다.

| 테이블 | 역할 |
| --- | --- |
| `folders` | 단어장 폴더 |
| `books` | 단어장 메타데이터 |
| `words` | 단어, 뜻, 정렬, 암기 상태, 정답·오답 횟수, 예문(수정본) |
| `quiz_attempts` | 시험 시도 결과 |
| `quiz_answers` | 개별 시험 답변 |

- 기존 DB 버전: **2**
- 예문 수정본 DB 버전: **3** (`words.example` 열 추가)
- 마이그레이션에서 기존 학습 기록을 유지하도록 설계되어 있습니다. 실제 데이터가 있는 단말에서 업그레이드 전 백업을 권장합니다.

## 단어장 갱신 — 현재 확인 필요

갱신은 `ImportActivity.java`의 업데이트 모드에서 `DatabaseHelper.mergeBookWords()`를 호출하여 수행하도록 구현되어 있습니다. 기본 의도는 기존 단어에 대한 학습 기록을 유지하며 가져온 데이터를 병합하는 것입니다.

**현재 단어장 갱신이 정상 동작하지 않는다는 사용자 보고가 있으며, 정확한 재현 조건과 원인은 아직 확정되지 않았습니다.** 따라서 안정화된 기능으로 표시하지 않습니다.

현재 구현에서 확인할 점:

1. 갱신은 전체 교체가 아니라 **병합**이므로 새 파일에서 삭제된 단어가 기존 DB에 남을 수 있습니다.
2. 예문 수정본의 `mergeBookWords()`에는 가져온 `example` 값으로 기존 값을 갱신하는 로직이 있어, 예문 열이 없는 파일을 병합할 때 기존 예문이 빈 값으로 대체될 수 있습니다.
3. 갱신 후 UI에 결과가 표시되지 않는 경우 데이터 저장 문제와 화면 새로고침 문제를 분리해서 확인해야 합니다.

관련 파일: `ImportActivity.java`, `data/DatabaseHelper.java`, `parser/DelimitedTextParser.java`, `parser/XlsxParser.java`, `BookDetailActivity.java`.

## 개발 시 유의사항

- 기존 학습 기록(`memorized`, `correct_count`, `incorrect_count`, 시험 기록)을 보존하세요.
- DB 스키마를 변경하면 버전을 올리고 `onUpgrade()`에 비파괴적 마이그레이션을 추가하세요.
- 예문이 없는 기존 파일과 새 예문 파일을 **동시에 지원**하세요.
- 카드 앞면 언어, 시험·맞추기 로직, 기존 조작을 변경하지 마세요.
- 폰트 설정을 단어장별 설정으로 분리하지 않고 전체에 적용하세요.
- 테스트 권장 항목: 새 단어장 불러오기, 기존 단어장 갱신, 중복 행, 예문 없음/있음, 영·한 앞면 전환, 화면 길이·폰트 크기, DB 2→3 업그레이드, 학습 기록 유지.

## 프로젝트 상태

이 README는 제공받은 소스와 예문 기능 변경 내용을 설명하는 문서입니다. **프로젝트를 실제 단말에서 빌드·실행하여 모든 기능을 검증했다는 의미는 아닙니다.** 특히 단어장 갱신은 추후 원인 분석 및 수정이 필요한 상태입니다.
