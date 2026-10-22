# HamHaeVoca (함해보카)

Android용 개인 단어장 학습 앱입니다. Excel/텍스트 파일에서 단어장을 만들고, 낱말카드·시험·맞추기 기능으로 단어를 반복 학습할 수 있습니다.

> **개발 상태:** 기능 확장 및 UI 안정화 중입니다. 아래 기능은 프로젝트 소스와 최근 개발 작업을 기준으로 정리했으며, 전체 Gradle 빌드 및 모든 기기에서의 동작이 검증되었다는 의미는 아닙니다.

## 주요 기능

- **단어장 관리:** 단어장 생성, 상세 보기, 수정·갱신, 검색, 정렬
- **폴더 관리:** 폴더 생성·이름 변경·삭제, 단어장 폴더 이동, 폴더별 단어장 개수 표시
- **단어장 선택:** 다중 선택 작업과 선택된 카드의 민트색 테두리 표시
- **파일 가져오기:** CSV, TSV, XLSX 형식에서 단어장 생성 및 갱신
- **선택적 예문:** `Example` 열이 있으면 단어에 연결해 저장하고 학습·미리보기 화면에서 활용; 없는 파일도 지원
- **낱말카드:** 앞·뒷면 학습, 암기 상태 관리, 정답 확인 후 예문 표시, 전역 글자 크기 설정
- **시험:** 객관식 시험, 품사 시험, 시험 결과 및 오답 기록
- **맞추기:** 단어 맞추기 학습, 결과 확인
- **학습 기록:** 암기 상태, 정답·오답 횟수 및 시험 이력의 SQLite 저장
- **다국어 UI:** 한국어, 영어, 프랑스어, 스페인어, 베트남어, 일본어, 중국어 간체

> UI 언어 변경은 앱 자체의 메뉴·버튼·메시지에 적용됩니다. 사용자가 가져온 단어(`Vocabulary`), 뜻(`Mean`), 예문(`Example`)은 자동 번역하지 않습니다.

## 지원 단어장 형식

첫 행에 열 이름을 두는 형식을 기준으로 합니다.

**기본 형식 (4열)**

| Vocabulary | Mean | Day | Category |
| --- | --- | --- | --- |
| apple | 사과 | 1 | noun |

**예문 포함 형식 (5열)**

| Vocabulary | Mean | Day | Category | Example |
| --- | --- | --- | --- | --- |
| although | 비록 ~일지라도 | 1 | conjunction | Although it rained, we went out. |

- `Example`은 선택 항목이며, 예문이 없으면 예문 영역을 숨깁니다.
- 가져오기 미리보기의 번호는 헤더를 제외한 **첫 단어 1번**부터 순서대로 표시합니다.
- 기존 단어장을 갱신할 때는 학습 기록 보존을 목표로 설계되어 있습니다. 데이터 매칭과 보존 결과는 실제 테스트로 확인하세요.

## 화면과 사용 흐름

1. 메인 화면에서 단어장을 추가하거나, 검색·정렬·폴더 메뉴로 단어장을 찾습니다.
2. 단어장을 열면 단어 목록과 암기·오답 관련 상태를 확인할 수 있습니다.
3. `낱말카드`, `시험`, `맞추기` 중 학습 방식을 선택합니다.
4. 학습 결과와 오답 기록을 확인하고 반복 학습합니다.

UI는 기존 HamHaeVoca 디자인을 유지하는 방향으로 개선하고 있습니다. 단어장 선택 상태를 민트색 테두리로 구분하고, 메인 단어장 목록과 단어장 내부 단어 목록에 일관된 좌우 여백을 적용하는 작업이 진행되었습니다.

## 개발 환경

| 항목 | 내용 |
| --- | --- |
| 언어 | Java |
| UI | Android XML Layout |
| 데이터베이스 | SQLite (`SQLiteOpenHelper`) |
| 최소 Android SDK | API 21 |
| Target SDK | API 32 |
| 빌드 | Gradle / Android Studio |
| 패키지 | `com.example.hamhaevoca` |

> SDK 설정은 사용 중인 `app/build.gradle`에서 최종 확인하세요. 이 문서의 값은 인수인계된 프로젝트 설정을 기준으로 합니다.

## 프로젝트 구조

```text
app/src/main/
├── AndroidManifest.xml
├── java/com/example/hamhaevoca/
│   ├── MainActivity.java
│   ├── BookDetailActivity.java
│   ├── BookSettingsActivity.java
│   ├── ImportActivity.java
│   ├── FlashcardActivity.java
│   ├── QuizSetupActivity.java
│   ├── QuizActivity.java
│   ├── PosQuizActivity.java
│   ├── QuizResultActivity.java
│   ├── MatchingSetupActivity.java
│   ├── MatchingActivity.java
│   ├── MatchingResultActivity.java
│   ├── Lang.java
│   ├── LocalizedActivity.java
│   ├── data/         # SQLite DB 및 데이터 처리
│   ├── model/        # 단어·시험 데이터 모델
│   ├── parser/       # XLSX 및 구분자 기반 텍스트 파서
│   └── util/         # 학습 로직 등 유틸리티
└── res/
    ├── layout/
    ├── drawable/
    ├── values/
    ├── values-en/
    ├── values-fr/
    ├── values-es/
    ├── values-vi/
    ├── values-ja/
    └── values-zh/
```

## 실행 방법

1. Android Studio에서 저장소를 Clone하거나 프로젝트 폴더를 엽니다.
2. 설치된 Android SDK와 프로젝트의 Gradle 설정을 확인합니다.
3. Gradle 동기화 후 앱을 빌드합니다.
4. 에뮬레이터 또는 Android 기기에서 실행합니다.
5. CSV/TSV/XLSX 파일로 단어장을 가져와 학습 기능을 시험합니다.

## 데이터 관리와 호환성

- 단어장, 폴더, 단어 및 학습 기록은 SQLite에 저장됩니다.
- 기존 데이터를 보존하려면 앱의 데이터를 삭제하거나 무조건 재설치하기보다 업데이트 설치 및 DB 마이그레이션을 검증하세요.
- 소스의 `DatabaseHelper`는 `example` 열과 폴더 관계를 포함하며 기존 DB 업그레이드 코드를 제공합니다.
- 대량 가져오기, 기존 단어장 갱신, 사용자 학습 기록 보존은 배포 전 별도 검증이 필요합니다.

## 현재 검증 및 개선 과제

아래 항목은 **실기기에서 전체 검증 완료로 간주하지 않습니다.**

- [ ] Android Studio에서 전체 Gradle 빌드 성공 확인
- [ ] 신규 단어장 생성 및 기존 단어장 갱신
- [ ] XLSX 파일 선택기와 파일 파싱 동작
- [ ] 업데이트 설치 후 기존 학습 데이터 보존
- [ ] 낱말카드 예문 표시 및 글자 크기 설정 유지
- [ ] 폴더 이동·정렬·단어장 다중 선택
- [ ] 처음 선택한 단어장의 민트색 단일 테두리 표시
- [ ] 상단 UI와 단어 목록의 좌우 여백 및 텍스트 줄바꿈
- [ ] 7개 언어 전환 및 모든 화면 번역·잘림 확인
- [ ] 시험·품사 시험·맞추기 기능 회귀 테스트

## 개발 및 기여 시 유의사항

- 기존 UI 스타일과 사용자 데이터를 유지하면서 필요한 부분만 변경합니다.
- 사용자가 요청하지 않은 아이콘을 새로 추가하지 않습니다.
- 새로운 UI 문자열은 모든 지원 언어의 리소스 키를 함께 관리합니다.
- 단어장 가져오기/갱신 시 암기 상태 및 정답·오답 횟수 보존을 고려합니다.
- UI 변경 후에는 관련 학습 화면의 회귀 테스트를 수행합니다.
- 정적 검사, Gradle 빌드, 실기기 검증 결과는 서로 구분해 기록합니다.

## Repository

https://github.com/sosomeet/HamHaeVoca
