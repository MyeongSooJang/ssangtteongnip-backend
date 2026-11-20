# ERD

운영 DB(PostgreSQL) 테이블 구성. 엔티티가 바뀌면 이 문서도 같은 커밋에서 고친다 (`.claude/hooks/erd-guard.py`가 확인한다).

```mermaid
erDiagram
    users ||--o| profiles : "user_id (1:1)"
    users ||--o{ user_files : "user_id"
    users ||..o{ applications : "user_id (FK 없음)"
    users ||..o{ study_logs : "user_id (FK 없음)"
    companies ||--o{ applications : "company_id"
    applications ||--o{ schedules : "application_id"
    applications ||--o{ interviews : "application_id"
    applications ||--o| reviews : "application_id (1:1)"
    interviews ||--o{ interview_questions : "interview_id"
    reviews ||--o{ review_questions : "review_id"

    users {
        bigint id PK
        varchar email UK
        varchar name
        varchar password_hash
        timestamp created_at
        timestamp updated_at
    }
    profiles {
        bigint id PK
        bigint user_id FK,UK
        varchar phone
        date birth_date
        varchar address
        varchar github_url
        varchar blog_url
        varchar portfolio_url
        varchar education
        varchar career
        date job_search_start_date
        varchar desired_job
        varchar introduction
        timestamp created_at
        timestamp updated_at
    }
    user_files {
        bigint id PK
        bigint user_id FK
        varchar type "FileType: RESUME, PORTFOLIO, CERTIFICATE, ETC"
        varchar file_name
        bigint size_bytes
        varchar object_key "S3 키"
        varchar content_type
        timestamp created_at "업로드 시각, 목록 정렬 기준"
    }
    companies {
        bigint id PK
        varchar name UK
        varchar industry
        varchar website
        timestamp created_at
        timestamp updated_at
    }
    company_catalog {
        bigint id PK
        varchar corp_code UK "DART 고유번호 8자리"
        varchar name "법인명"
        varchar eng_name "영문 법인명"
        varchar search_name "검색용 법인명(법인 형태·공백·기호 제거, 소문자)"
        varchar search_eng_name "검색용 영문명"
        boolean listed "상장 여부"
    }
    applications {
        bigint id PK
        bigint user_id "FK 없음"
        bigint company_id FK
        varchar position
        varchar status "JobApplicationStatus"
        date applied_date
        varchar job_site "JobSite, 선택"
        timestamp document_passed_at "서류 통과(면접 일정 최초 등록) 시점, 서류 탈락이면 null"
        date deadline "공고 마감일, 선택. 비어 있으면 자동 불합격 대상이 아니다"
        varchar resume_view "ResumeView, 기본 NOT_VIEWED"
        timestamp auto_rejected_at "마감일 + 1달 경과로 자동 불합격된 시점, 아니면 null"
        timestamp created_at
        timestamp updated_at
    }
    schedules {
        bigint id PK
        bigint application_id FK
        varchar title
        timestamp scheduled_at
        varchar type "ScheduleType"
        varchar address
        varchar note
        timestamp created_at
    }
    interviews {
        bigint id PK
        bigint application_id FK
        int round
        timestamp interview_date
        varchar location
        text note
        int score
        timestamp created_at
    }
    interview_questions {
        bigint id PK
        bigint interview_id FK
        varchar question
        text answer
        varchar memo
    }
    reviews {
        bigint id PK
        bigint application_id FK,UK
        text content
        int rating "1~5"
        int difficulty "1~5, 선택"
        varchar atmosphere "InterviewAtmosphere, 선택"
        varchar route "ApplyRoute, 선택"
        timestamp created_at
        timestamp updated_at
    }
    review_questions {
        bigint review_id PK,FK
        int question_order PK
        text question
    }
    study_logs {
        bigint id PK
        bigint user_id "FK 없음, idx(user_id, study_date)"
        date study_date
        varchar title
        text content
        int study_minutes
        timestamp created_at
        timestamp updated_at
    }
```

## 참고

- `applications.user_id`, `study_logs.user_id`는 값 컬럼이고 DB FK가 없다. 소유자 검증은 서비스에서 한다.
- `profiles`는 `PersonalInfo`(@Embeddable)가 같은 테이블에 펼쳐진 구조다.
- `review_questions`는 `Review.questions`(@ElementCollection)의 컬렉션 테이블이고, `question_order`는 @OrderColumn이다.
- `company_catalog`는 DART 기업 목록을 담은 회사 검색용 카탈로그다. `companies`와 FK 없이 분리되어 있고, 회사 이름이 같으면 같은 회사로 본다.
- 마이그레이션 도구가 없고 `ddl-auto: update`는 컬럼 추가만 하므로, 변경 사항은 운영 DB에 DDL로 직접 반영해야 할 수 있다.
- `user_files.user_id`는 유니크가 아니다. 사용자당 최대 개수는 `UserFile.MAX_FILES_PER_USER`이고, 업로드 시 사용자 행을 잠근 뒤 개수를 확인해 지킨다.
- `applications.resume_view`는 `not null default 'NOT_VIEWED'`로 추가되어 기존 행이 미열람으로 채워진다.
- 2026-11-10 코드 패키지를 `com.jobtracker`에서 `com.ssangtteongnip`으로 바꿨다. 엔티티 11개가 `package`와 `import` 줄만 바뀌었고 테이블, 컬럼, 관계는 그대로다.
