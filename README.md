# 함해보카 (HamHaeVoca)

Android에서 개인 단어장을 만들고, Excel 파일을 가져와 낱말카드·시험·맞추기로 반복 학습하는 어휘 학습 앱입니다.

> **문서 기준:** 2026-10-28까지 제공된 `src.zip` 및 세 차례 Verb 관련 변경 패치. GitHub에 반영되었거나 Android 전체 빌드·실기기 테스트가 완료되었다는 의미는 아닙니다.

## 1. 프로젝트 정보

| 항목 | 내용 |
|---|---|
| 앱 표시 이름 | **함해보카** (기기 언어와 관계없이 고정) |
| 내부 프로젝트 이름 | HamHaeVoca |
| 저장소 | <https://github.com/sosomeet/HamHaeVoca> |
| 패키지 | `com.example.hamhaevoca` |
| 언어 / 화면 | Java / Android XML |
| 로컬 저장소 | SQLite (`SQLiteOpenHelper`) |
| Min / Target SDK | 21 / 32 (인수인계 명세 기준; Gradle 설정은 별도 확인 필요) |
| 빌드 도구 | Gradle / Android Studio |

## 2. 주요 기능

- **단어장 관리:** 생성, 가져오기, 갱신, 검색, 이름순·역순·최근 수정순 정렬, 폴더별 분류.
- **다중 선택:** 단어장 여러 개 선택 후 폴더 이동 또는 삭제; 삭제 전 확인.
- **단어 관리:** 단어·뜻·Day·Category 및 지원 형식의 예문을 관리; 암기/미암기 상태와 오답 횟수 확인.
- **학습:** 낱말카드, 객관식·품사 시험, 맞추기, 시험 결과와 오답 기록.
- **학습 조건:** 단어 수, 시간, 최소 오답 횟수 등 해당 화면의 설정 사용.
- **언어:** 한국어, 영어, 프랑스어, 스페인어, 베트남어, 일본어, 중국어(간체)의 앱 UI 지원을 목표로 함. **단어장 데이터는 자동 번역하지 않음.**

### UI 원칙

기존 HamHaeVoca의 카드·색상·레이아웃을 유지합니다. 삼성 노트는 주로 검색 노출, 폴더, 사이드 메뉴의 **조작 방식**만 참고합니다. 사용자가 지정하지 않은 아이콘과 장식을 추가하지 않습니다. 메인 화면의 추가 버튼과 선택된 단어장 카드 테두리는 민트색을 사용하며, 선택 테두리는 한 겹입니다.

## 3. 화면 IA (Information Architecture)

IA ID는 개발 문서·이슈 보고용 식별자이며 Android의 실제 클래스 이름을 변경하지 않습니다.

| IA ID | 화면 또는 상태 | 구현 클래스 |
|---|---|---|
| HOME-01 | 메인 / 단어장 목록 | `MainActivity` |
| HOME-02 | 사이드 메뉴 | `MainActivity` |
| HOME-03 | 폴더 관리 | `MainActivity` |
| HOME-04 | 단어장 다중 선택 | `MainActivity` |
| BOOK-01 | 단어장 상세 | `BookDetailActivity` |
| BOOK-02 | 단어장 설정 | `BookSettingsActivity` |
| BOOK-03 | 단어 추가·수정 | `BookDetailActivity` |
| IMPORT-01 | 단어장 생성·가져오기 | `ImportActivity` |
| IMPORT-02 | 단어장 갱신 | `ImportActivity` |
| STUDY-01 | 낱말카드 | `FlashcardActivity` |
| STUDY-02 | 낱말카드 설정 | `FlashcardActivity` |
| QUIZ-01 | 시험 설정 | `QuizSetupActivity` |
| QUIZ-02 | 객관식 시험 | `QuizActivity` |
| QUIZ-03 | 품사 시험 | `PosQuizActivity` |
| QUIZ-04 | 시험 결과 | `QuizResultActivity` |
| MATCH-01 | 맞추기 설정 | `MatchingSetupActivity` |
| MATCH-02 | 맞추기 진행 | `MatchingActivity` |
| MATCH-03 | 맞추기 결과 | `MatchingResultActivity` |

**BOOK-01 세부 구역:** A 상단 제목·뒤로가기, B 암기 통계, C 학습 버튼, D 검색, E 오답 필터, F 단어 개수, G 단어 목록. BOOK-01-G의 카드 여백은 상단 헤더의 여백과 별개로 관리합니다(최근 작업 기준 20dp).

## 4. 지원 단어장 / Excel 데이터 형식

CSV, TSV, XLSX 입력을 지원하도록 설계되어 있습니다. **가져오기 미리보기의 단어 인덱스는 Excel 행 번호가 아닌 1부터 시작하는 단어 순번**입니다.

### Unit4 — 일반 단어장

| 열 | 필드 |
|---|---|
| A | Vocabulary |
| B | Mean |
| C | Day |
| D | Category |

### Conj — 예문형 단어장

| 열 | 필드 |
|---|---|
| A | Vocabulary |
| B | Mean |
| C | Day |
| D | Category |
| E | Example (선택) |

예문이 있는 단어는 낱말카드에서 정답을 확인한 후 얇은 구분선 아래에 예문을 표시합니다. 예문이 없으면 기존 카드 기능은 유지합니다.

### Verb — 동사 3단 변화형 (신규)

`CCI_Voca_Verb.xlsx` 형식에 대응합니다.

| 열 | 필드 | 예시 |
|---|---|---|
| A | 현재형 (Present) | begin |
| B | 과거형 (Past) | began |
| C | 과거분사 (Past Participle) | begun |
| D | 뜻 (Meaning) | 시작하다 |
| E | Day | Day 값 |
| F | Category | 분류 값 |

동사형 데이터를 위한 `past`, `pastParticiple` 필드가 모델/DB에 추가되었으며, 해당 값이 있는 단어에서 Verb 카드 흐름을 사용합니다. **Verb 형식의 Example 열은 현재 제공된 Excel 구조에 포함되어 있지 않습니다.**

## 5. 낱말카드 동작

| 카드 유형 | 터치/Flip 단계 |
|---|---|
| Unit4 | 단어 ↔ 뜻 (기존 방식) |
| Conj | 단어 ↔ 뜻, 정답 확인 후 예문 표시 |
| Verb | **현재형 → 과거형 → 과거분사 → 뜻** |

Verb 카드의 경우 **총 3회의 Flip 후 뜻**이 표시됩니다.

```text
begin        (첫 화면, 예문 영역: 1)
  ↓ Flip 1
began        (예문 영역: 2)
  ↓ Flip 2
begun        (예문 영역: 3)
  ↓ Flip 3
시작하다       (뜻, 단계 번호 없음)
```

**Verb 단계 번호는 단어 본문이나 시험 문제에 붙지 않고 낱말카드의 예문 영역에만 표시**합니다. 기존 Excel에 Example 값이 없더라도 `1`, `2`, `3`이 표시됩니다. 예문 텍스트가 있을 경우 `1. 예문`처럼 표시하도록 처리한 코드입니다. 단어·뜻·예문의 글자 크기 설정은 전역 설정으로 저장합니다.

## 6. 시험과 맞추기

시험 설정은 기존 출제 방식 선택을 2×2 배열로 표시하며, `품사 태그 복수 선택` 대신 `품사 선택`이라는 용어를 사용합니다. 시험 설정의 뒤로가기 UI는 다른 학습 설정 화면과 통일합니다.

Verb 단어 시험은 **현재형 외에 과거형·과거분사형도 문제에 사용할 수 있게** 확장되었습니다. 단, 최근 수정에 따라 시험 문제에 표시되던 `[2 · past]` 등의 **형태 인덱스**와 `Meaning Hint` / `English word hint` 문구는 삭제합니다. 이는 낱말카드 예문 영역의 `1·2·3` 표시와 다른 요구사항입니다.

맞추기 설정 안내 문구는 `사용할 단어 수와 주어지는 시간을 선택하세요`로 통일합니다. 기존 맞추기 로직을 Verb의 세 형태별 개별 문제로 확장했다는 뜻은 아닙니다.

## 7. 데이터 보존 및 마이그레이션

- 기존 단어장, 단어, 암기 상태, 정답/오답 횟수, 시험 기록 및 폴더 데이터를 보존하는 것이 최우선입니다.
- Verb 지원 작업에서 `DatabaseHelper`의 **DB 버전이 4**로 올라갔습니다. 기존 설치에서 업데이트할 때 데이터 이동 경로와 필드 기본값을 확인해야 합니다.
- 단어장을 갱신할 때 학습 기록이 무조건 초기화되지 않도록 동일 단어 판별 및 갱신 정책을 검토해야 합니다.
- 데이터 백업 없이 앱 데이터를 삭제하거나, DB를 삭제하고 재설치하여 마이그레이션을 대신하지 마세요.

## 8. 다국어 UI

| 언어 | 리소스 디렉터리 |
|---|---|
| 한국어 | `res/values` |
| 영어 | `res/values-en` |
| 프랑스어 | `res/values-fr` |
| 스페인어 | `res/values-es` |
| 베트남어 | `res/values-vi` |
| 일본어 | `res/values-ja` |
| 중국어(간체) | `res/values-zh` |

버튼, 대화상자, 오류 메시지 및 **긴 안내 문구**를 포함한 UI 전체를 번역 대상으로 봅니다. 새로운 문자열을 추가할 때 7개 언어의 키 일치 여부를 확인해야 합니다. 앱 런처 표시 이름은 **`함해보카`로 고정**하며 내부 화면의 언어 전환과 분리합니다.

## 9. 주요 소스 구조

```text
app/
└── src/
    ├── main/
    │   ├── AndroidManifest.xml
    │   ├── java/com/example/hamhaevoca/
    │   │   ├── MainActivity.java
    │   │   ├── BookDetailActivity.java
    │   │   ├── BookSettingsActivity.java
    │   │   ├── ImportActivity.java
    │   │   ├── FlashcardActivity.java
    │   │   ├── QuizSetupActivity.java
    │   │   ├── QuizActivity.java
    │   │   ├── PosQuizActivity.java
    │   │   ├── QuizResultActivity.java
    │   │   ├── MatchingSetupActivity.java
    │   │   ├── MatchingActivity.java
    │   │   ├── MatchingResultActivity.java
    │   │   ├── data/DatabaseHelper.java
    │   │   ├── model/
    │   │   ├── parser/
    │   │   └── util/QuizEngine.java
    │   └── res/
    │       ├── layout/
    │       ├── drawable/
    │       ├── values/
    │       └── values-{en,fr,es,vi,ja,zh}/
    ├── test/
    └── androidTest/
```

위 트리는 핵심 파일 중심의 요약입니다. 실제 최신 ZIP에는 추가 클래스/리소스가 있습니다.

## 10. 빌드 및 실행

1. Android Studio에서 **프로젝트 루트**(`settings.gradle` 등이 있는 폴더)를 엽니다.
2. Gradle 동기화를 진행합니다. Android SDK 및 프로젝트 Gradle 플러그인에 맞는 JDK를 사용합니다.
3. 에뮬레이터 또는 기기(API 21 이상)를 선택합니다.
4. `app` 실행 구성을 선택하여 빌드 및 실행합니다.
5. 기존 데이터를 가진 기기에서는 먼저 백업 후 DB 마이그레이션과 업데이트 경로를 테스트합니다.

### 전달 패치 적용 시

제공되는 코드 패치 ZIP의 최상위 폴더는 `src/`입니다. 그 안의 **변경된 파일만** 기존 `app/src/`에 덮어씁니다. 과거 ZIP을 무작정 다시 덮어쓰면 이후 개선 사항을 되돌릴 수 있습니다. **README는 코드 패치의 `src/`가 아니라 저장소 루트의 `README.md`**에 위치합니다.

## 11. 최근 변경 내역

| 시기 | 영역 | 주요 변경 |
|---|---|---|
| 2026-10 | HOME / BOOK | 폴더·선택 UI, 암기/미암기 용어, 목록 여백, 다중 삭제 개선 |
| 2026-10 | IMPORT | 직접 입력 UI·자동 구분자 메뉴 제거, 가져오기/갱신 개선 이력 |
| 2026-10 | QUIZ / MATCH | 시험 설정 2×2, 뒤로가기 정렬, 안내 문구 수정 |
| 2026-10-28 | Verb 1차 | Verb Excel 파싱, 동사형 저장 필드, 4단계 카드, DB 버전 4 |
| 2026-10-28 | Verb 2차 | 3단 동사 수정 UI, 과거·과거분사 시험 출제, 런처 이름 고정 |
| 2026-10-28 | Verb 3차 | 낱말카드 예문 영역 1·2·3 표시, 시험 형태 인덱스 및 Meaning Hint 제거 |

**적용 순서 참고:** `HamHaeVoca_Verb_Flashcard_src.zip` → `HamHaeVoca_Verb_Enhancements_src.zip` → `HamHaeVoca_Verb_Index_Quiz_Cleanup_src.zip`. 순서가 중요한 것은 이전 ZIP에 없는 파일이 후속 패치에 의존할 수 있기 때문입니다. 실제 기준 소스가 완전히 통합되었는지는 별도로 확인해야 합니다.

## 12. 검증 현황 및 알려진 위험

| 항목 | 상태 |
|---|---|
| Verb 필드/카드/시험 로직의 소스 존재 | 확인 |
| DB 버전 4 명시 | 확인 |
| 런처 이름 `함해보카` 설정 | 최신 패치의 Manifest에서 확인 |
| 낱말카드 예문 영역의 1·2·3 표시 코드 | 최신 패치에서 확인 |
| 시험 인덱스 및 Hint 제거 로직 | 최신 패치 대상; 실제 화면 검증 필요 |
| 전체 Android Gradle 빌드 | **미검증** |
| 실기기 테스트 및 데이터 마이그레이션 | **미검증** |
| GitHub 기본 브랜치 반영 | **미확인** |

우선 검증할 시나리오:

1. 기존 설치 앱을 업데이트해 기존 단어·암기·오답 기록이 유지되는지 확인.
2. Unit4 / Conj / Verb의 CSV·TSV·XLSX 불러오기와 갱신 확인(특히 XLSX 파일 선택기의 활성화 상태).
3. Verb 낱말카드의 `현재형 → 과거형 → 과거분사 → 뜻`, 단계 번호 `1·2·3` 및 슬라이드·뒤집기 동작 확인.
4. Verb 단어 편집에서 세 형태가 모두 보이고 저장되는지 확인.
5. 시험에서 과거형·과거분사가 출제되면서 인덱스와 Meaning Hint가 다시 나타나지 않는지 확인.
6. 7개 언어 및 휴대폰 언어를 바꾸어도 런처 앱 이름이 `함해보카`인지 확인.
7. 첫 선택 카드의 민트색 단일 테두리, 폴더별 숫자, 상단 헤더와 단어 목록의 독립 여백 확인.

## 13. 개발 시 변경 보고 형식

앞으로 수정 요청에는 **IA ID, 원인, 변경된 전체 파일, 기존 기능에 미치는 영향, 데이터베이스 영향, 정적 검사 결과, Android 빌드 결과, 실기기 검증 여부**를 명확히 보고합니다. 코드 존재만으로 기능이 실기기에서 정상 작동한다고 단정하지 않습니다.

---

문서의 기능 설명은 *요구 명세*, *패치에서 확인한 구현*, *미검증 항목*을 구별하기 위해 작성되었습니다.
