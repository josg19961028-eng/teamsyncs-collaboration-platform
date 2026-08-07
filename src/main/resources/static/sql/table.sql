/* =====================================================================
TeamSync DB Schema (Oracle)

Final3차.xlsx 설계서 기준
작성 순서: RESET(DROP) -> CREATE TABLE -> CREATE SEQUENCE
PK 값은 컬럼 DEFAULT가 아니라 INSERT 시점에 시퀀스로 채움.
예) MyBatis 매퍼에서 로 SEQ_xxx.NEXTVAL 조회 후 주입
또는 INSERT INTO ... VALUES (SEQ_xxx.NEXTVAL, ...)
===================================================================== */
/* ---------------------------------------------------------------------
0. RESET (의존 역순: 자식 -> 부모)
※ 최초 실행 시 "존재하지 않는 테이블/시퀀스" 오류는 무시해도 됩니다.
--------------------------------------------------------------------- */
-- 테이블 삭제
DROP TABLE BOT_LOG CASCADE CONSTRAINTS;
DROP TABLE NOTIFICATION CASCADE CONSTRAINTS;
DROP TABLE NOTICE CASCADE CONSTRAINTS;
DROP TABLE MEETING_MINUTES_ATTENDEE CASCADE CONSTRAINTS;
DROP TABLE SCHEDULE_ATTENDEE CASCADE CONSTRAINTS;
DROP TABLE MEETING_MINUTES CASCADE CONSTRAINTS;
DROP TABLE SCHEDULE CASCADE CONSTRAINTS;
DROP TABLE CHAT_MENTION CASCADE CONSTRAINTS;
DROP TABLE CHAT_READ_STATUS CASCADE CONSTRAINTS;
DROP TABLE CHAT_FILE CASCADE CONSTRAINTS;
DROP TABLE FILE_FOLDER CASCADE CONSTRAINTS;
DROP TABLE CHAT_MESSAGE CASCADE CONSTRAINTS;
DROP TABLE CHAT_CHANNEL CASCADE CONSTRAINTS;
DROP TABLE KANBAN_COMMENT CASCADE CONSTRAINTS;
DROP TABLE KANBAN_CHECKLIST CASCADE CONSTRAINTS;
DROP TABLE KANBAN_ASSIGN CASCADE CONSTRAINTS;
DROP TABLE KANBAN_CARD CASCADE CONSTRAINTS;
DROP TABLE PERSONAL_TODO CASCADE CONSTRAINTS;
DROP TABLE TODO_CATEGORY CASCADE CONSTRAINTS;
DROP TABLE TEAM_INVITE_CODE CASCADE CONSTRAINTS;
DROP TABLE EMAIL_INVITATION CASCADE CONSTRAINTS;
DROP TABLE TEAM_MEMBER CASCADE CONSTRAINTS;
DROP TABLE TEAM CASCADE CONSTRAINTS;
DROP TABLE USERS CASCADE CONSTRAINTS;

-- 시퀀스 삭제
DROP SEQUENCE SEQ_USERS;
DROP SEQUENCE SEQ_TEAM;
DROP SEQUENCE SEQ_TEAM_MEMBER;
DROP SEQUENCE SEQ_EMAIL_INVITATION;
DROP SEQUENCE SEQ_TEAM_INVITE_CODE;
DROP SEQUENCE SEQ_TODO_CATEGORY;
DROP SEQUENCE SEQ_PERSONAL_TODO;
DROP SEQUENCE SEQ_KANBAN_CARD;
DROP SEQUENCE SEQ_KANBAN_ASSIGN;
DROP SEQUENCE SEQ_KANBAN_CHECKLIST;
DROP SEQUENCE SEQ_KANBAN_COMMENT;
DROP SEQUENCE SEQ_CHAT_CHANNEL;
DROP SEQUENCE SEQ_CHAT_MESSAGE;
DROP SEQUENCE SEQ_FILE_FOLDER;
DROP SEQUENCE SEQ_CHAT_FILE;
DROP SEQUENCE SEQ_CHAT_MENTION;
DROP SEQUENCE SEQ_SCHEDULE;
DROP SEQUENCE SEQ_MEETING_MINUTES;
DROP SEQUENCE SEQ_SCHEDULE_ATTENDEE;
DROP SEQUENCE SEQ_MEETING_MINUTES_ATTENDEE;
DROP SEQUENCE SEQ_NOTIFICATION;
DROP SEQUENCE SEQ_NOTICE;
DROP SEQUENCE SEQ_BOT_LOG;

/* ---------------------------------------------------------------------
1. USERS (회원)
--------------------------------------------------------------------- */
CREATE TABLE USERS (
    user_num    NUMBER          NOT NULL,
    email       VARCHAR2(100)   NOT NULL,
    passwd      VARCHAR2(255),
    user_name   VARCHAR2(50)    NOT NULL,
    phone       VARCHAR2(20),
    birth       DATE,
    intro       VARCHAR2(1000),
    photo       BLOB,
    photo_name  VARCHAR2(2000),
    auth        VARCHAR2(50)    DEFAULT 'USER_MEMBER' NOT NULL,
    login_type  NUMBER(1)       DEFAULT 1 NOT NULL,
    google_id   VARCHAR2(255),
    status      NUMBER(1)       DEFAULT 1 NOT NULL,
    withdraw_date DATE,
    reg_date    DATE            DEFAULT SYSDATE NOT NULL,
    modify_date DATE,
    CONSTRAINT PK_USERS          PRIMARY KEY (user_num),
    CONSTRAINT UK_USERS_EMAIL    UNIQUE (email),
    CONSTRAINT UK_USERS_GOOGLEID UNIQUE (google_id),
    CONSTRAINT CK_USERS_LOGINTYPE CHECK (login_type IN (1,2,3)),
    CONSTRAINT CK_USERS_STATUS    CHECK (status IN (1,2,3))
);
COMMENT ON TABLE  USERS           IS '회원 정보';
COMMENT ON COLUMN USERS.user_num  IS '회원 번호 (PK)';
COMMENT ON COLUMN USERS.email     IS '로그인 이메일 (UK)';
COMMENT ON COLUMN USERS.passwd    IS '일반 로그인용 암호화 비밀번호';
COMMENT ON COLUMN USERS.user_name IS '서비스에서 사용할 닉네임';
COMMENT ON COLUMN USERS.auth      IS 'USER_MEMBER, USER_ADMIN';
COMMENT ON COLUMN USERS.login_type IS '일반(1), 구글(2), 구글연동(3)';
COMMENT ON COLUMN USERS.google_id IS '구글 계정 고유 ID (UK)';
COMMENT ON COLUMN USERS.status    IS '정상(1), 정지(2), 탈퇴(3)';

/* ---------------------------------------------------------------------
2. TEAM (팀)
--------------------------------------------------------------------- */
CREATE TABLE TEAM (
    team_num        NUMBER          NOT NULL,
    team_name       VARCHAR2(100)   NOT NULL,
    description     VARCHAR2(500),
    team_photo      BLOB,
    team_photo_name VARCHAR2(2000),
    color           VARCHAR2(20)    NOT NULL,
    status          NUMBER(1)       DEFAULT 1 NOT NULL,
    creator_num     NUMBER          NOT NULL,
    created_at      DATE            DEFAULT SYSDATE NOT NULL,
    updated_at      DATE,
    deleted_at      DATE,
    CONSTRAINT PK_TEAM        PRIMARY KEY (team_num),
    CONSTRAINT UK_TEAM_NAME   UNIQUE (team_name),
    CONSTRAINT FK_TEAM_CREATOR FOREIGN KEY (creator_num) REFERENCES USERS (user_num),
    CONSTRAINT CK_TEAM_STATUS  CHECK (status IN (1,2,3))
);
COMMENT ON TABLE  TEAM             IS '팀(그룹) 정보';
COMMENT ON COLUMN TEAM.team_num    IS '팀 고유 식별자 (PK)';
COMMENT ON COLUMN TEAM.team_name   IS '팀명 (UK)';
COMMENT ON COLUMN TEAM.color       IS '팀 색상 (HEX 코드)';
COMMENT ON COLUMN TEAM.status      IS '정상(1), 비활성(2), 삭제(3)';
COMMENT ON COLUMN TEAM.creator_num IS '팀 생성자 (USERS.user_num 참조)';
COMMENT ON COLUMN TEAM.deleted_at  IS '팀 삭제일 (소프트 삭제)';

/* ---------------------------------------------------------------------
3. TEAM_MEMBER (팀 멤버)
--------------------------------------------------------------------- */
CREATE TABLE TEAM_MEMBER (
    team_member_num NUMBER      NOT NULL,
    team_num        NUMBER      NOT NULL,
    user_num        NUMBER      NOT NULL,
    role            NUMBER(1)   DEFAULT 1 NOT NULL,
    join_status     NUMBER(1)   DEFAULT 1 NOT NULL,
    joined_at       DATE        DEFAULT SYSDATE NOT NULL,
    last_activity_at DATE,
    exited_at       DATE,
    CONSTRAINT PK_TEAM_MEMBER     PRIMARY KEY (team_member_num),
    CONSTRAINT FK_TEAMMEMBER_TEAM FOREIGN KEY (team_num)  REFERENCES TEAM  (team_num),
    CONSTRAINT FK_TEAMMEMBER_USER FOREIGN KEY (user_num)  REFERENCES USERS (user_num),
    CONSTRAINT UK_TEAMMEMBER      UNIQUE (team_num, user_num),
    CONSTRAINT CK_TEAMMEMBER_ROLE CHECK (role        IN (1,2,3)),
    CONSTRAINT CK_TEAMMEMBER_JOIN CHECK (join_status IN (1,2,3))
);
COMMENT ON TABLE  TEAM_MEMBER             IS '팀에 속한 회원 정보';
COMMENT ON COLUMN TEAM_MEMBER.role        IS '팀원(1), 매니저(2), 팀장(3)';
COMMENT ON COLUMN TEAM_MEMBER.join_status IS '소속중(1), 강퇴(2), 자진탈퇴(3)';

/* ---------------------------------------------------------------------
4. EMAIL_INVITATION (이메일 초대)
--------------------------------------------------------------------- */
CREATE TABLE EMAIL_INVITATION (
    invitation_num  NUMBER          NOT NULL,
    team_num        NUMBER          NOT NULL,
    inviter_num     NUMBER          NOT NULL,
    invitee_email   VARCHAR2(100)   NOT NULL,
    status          NUMBER(1)       DEFAULT 1 NOT NULL,
    sent_at         DATE            DEFAULT SYSDATE NOT NULL,
    responsed_at    DATE,
    expired_at      DATE            NOT NULL,
    CONSTRAINT PK_EMAIL_INVITATION  PRIMARY KEY (invitation_num),
    CONSTRAINT FK_EMAILINV_TEAM     FOREIGN KEY (team_num)    REFERENCES TEAM  (team_num),
    CONSTRAINT FK_EMAILINV_INVITER  FOREIGN KEY (inviter_num) REFERENCES USERS (user_num),
    CONSTRAINT CK_EMAILINV_STATUS   CHECK (status IN (1,2,3,4,5))
);
COMMENT ON TABLE  EMAIL_INVITATION        IS '이메일 초대 정보';
COMMENT ON COLUMN EMAIL_INVITATION.status IS '대기(1), 수락(2), 거절(3), 만료(4), 취소(5)';

/* ---------------------------------------------------------------------
5. TEAM_INVITE_CODE (초대 코드)
--------------------------------------------------------------------- */
CREATE TABLE TEAM_INVITE_CODE (
    invite_code_num NUMBER          NOT NULL,
    team_num        NUMBER          NOT NULL,
    code            VARCHAR2(36)    NOT NULL,
    status          NUMBER(1)       DEFAULT 1 NOT NULL,
    issued_at       DATE            DEFAULT SYSDATE NOT NULL,
    expired_at      DATE            NOT NULL,
    issuer_num      NUMBER          NOT NULL,
    CONSTRAINT PK_TEAM_INVITE_CODE  PRIMARY KEY (invite_code_num),
    CONSTRAINT UK_INVITECODE_TEAM   UNIQUE (team_num),
    CONSTRAINT FK_INVITECODE_TEAM   FOREIGN KEY (team_num)   REFERENCES TEAM  (team_num),
    CONSTRAINT FK_INVITECODE_ISSUER FOREIGN KEY (issuer_num) REFERENCES USERS (user_num),
    CONSTRAINT CK_INVITECODE_STATUS CHECK (status IN (1,2,3))
);
COMMENT ON TABLE  TEAM_INVITE_CODE        IS '팀 초대코드 관리';
COMMENT ON COLUMN TEAM_INVITE_CODE.code   IS 'UUID 기반 초대 코드 값';
COMMENT ON COLUMN TEAM_INVITE_CODE.status IS '사용가능(1), 만료(2), 비활성화(3)';

/* ---------------------------------------------------------------------
6. TODO_CATEGORY (개인 할 일 카테고리)
--------------------------------------------------------------------- */
CREATE TABLE TODO_CATEGORY (
    todo_category_num NUMBER          NOT NULL,
    user_num          NUMBER          NOT NULL,
    category_name     VARCHAR2(50)    NOT NULL,
    color             VARCHAR2(20),
    CONSTRAINT PK_TODO_CATEGORY          PRIMARY KEY (todo_category_num),
    CONSTRAINT FK_TODOCATEGORY_USER      FOREIGN KEY (user_num) REFERENCES USERS (user_num),
    CONSTRAINT UK_TODO_CATEGORY_USER_NAME UNIQUE (user_num, category_name)
);
COMMENT ON TABLE TODO_CATEGORY IS '개인 할 일 카테고리';

/* ---------------------------------------------------------------------
7. PERSONAL_TODO (개인 할 일)
--------------------------------------------------------------------- */
CREATE TABLE PERSONAL_TODO (
    todo_num          NUMBER          NOT NULL,
    user_num          NUMBER          NOT NULL,
    todo_category_num NUMBER,
    title             VARCHAR2(200)   NOT NULL,
    content           VARCHAR2(1000),
    deadline          DATE,
    priority          NUMBER(1)       DEFAULT 2 NOT NULL,
    complete          NUMBER(1)       DEFAULT 1 NOT NULL,
    CONSTRAINT PK_PERSONAL_TODO  PRIMARY KEY (todo_num),
    CONSTRAINT FK_PTODO_USER     FOREIGN KEY (user_num)          REFERENCES USERS         (user_num),
    CONSTRAINT FK_PTODO_CATEGORY FOREIGN KEY (todo_category_num) REFERENCES TODO_CATEGORY (todo_category_num) ON DELETE SET NULL,
    CONSTRAINT CK_PTODO_PRIORITY CHECK (priority IN (1,2,3)),
    CONSTRAINT CK_PTODO_COMPLETE CHECK (complete IN (1,2))
);
COMMENT ON TABLE  PERSONAL_TODO          IS '개인 할 일';
COMMENT ON COLUMN PERSONAL_TODO.priority IS '낮음(1), 보통(2), 높음(3)';
COMMENT ON COLUMN PERSONAL_TODO.complete IS '미완료(1), 완료(2)';

/* ---------------------------------------------------------------------
8. KANBAN_CARD (칸반 카드)
--------------------------------------------------------------------- */
CREATE TABLE KANBAN_CARD (
    card_num        NUMBER          NOT NULL,
    team_num        NUMBER          NOT NULL,
    writer_num      NUMBER          NOT NULL,
    title           VARCHAR2(200)   NOT NULL,
    content         VARCHAR2(2000),
    tag             VARCHAR2(24),
    kanban_status   NUMBER(1)       DEFAULT 1 NOT NULL,
    deadline        DATE            NOT NULL,
    reg_date        DATE            DEFAULT SYSDATE NOT NULL,
    modify_date     DATE,
    status          NUMBER(1)       DEFAULT 1 NOT NULL,
    CONSTRAINT PK_KANBAN_CARD      PRIMARY KEY (card_num),
    CONSTRAINT FK_KCARD_TEAM       FOREIGN KEY (team_num)   REFERENCES TEAM  (team_num),
    CONSTRAINT FK_KCARD_WRITER     FOREIGN KEY (writer_num) REFERENCES USERS (user_num),
    CONSTRAINT CK_KCARD_KSTATUS    CHECK (kanban_status IN (1,2,3,4)),
    CONSTRAINT CK_KCARD_STATUS     CHECK (status        IN (1,2))
);
COMMENT ON TABLE  KANBAN_CARD               IS '칸반 카드';
COMMENT ON COLUMN KANBAN_CARD.kanban_status IS 'TODO(1), DOING(2), REVIEW(3), DONE(4)';
COMMENT ON COLUMN KANBAN_CARD.status        IS 'ACTIVE(1), DELETED(2)';

/* ---------------------------------------------------------------------
9. KANBAN_ASSIGN (칸반 담당자)
--------------------------------------------------------------------- */
CREATE TABLE KANBAN_ASSIGN (
    assignee_num    NUMBER  NOT NULL,
    team_num        NUMBER  NOT NULL,
    card_num        NUMBER  NOT NULL,
    user_num        NUMBER  NOT NULL,
    CONSTRAINT PK_KANBAN_ASSIGN PRIMARY KEY (assignee_num),
    CONSTRAINT FK_KASSIGN_TEAM  FOREIGN KEY (team_num) REFERENCES TEAM         (team_num),
    CONSTRAINT FK_KASSIGN_CARD  FOREIGN KEY (card_num) REFERENCES KANBAN_CARD  (card_num),
    CONSTRAINT FK_KASSIGN_USER  FOREIGN KEY (user_num) REFERENCES USERS        (user_num),
    CONSTRAINT UK_KASSIGN       UNIQUE (card_num, user_num)
);
COMMENT ON TABLE KANBAN_ASSIGN IS '칸반 담당자';

/* ---------------------------------------------------------------------
10. KANBAN_CHECKLIST (칸반 체크리스트)
--------------------------------------------------------------------- */
CREATE TABLE KANBAN_CHECKLIST (
    checklist_num   NUMBER          NOT NULL,
    card_num        NUMBER          NOT NULL,
    content         VARCHAR2(500)   NOT NULL,
    checked         NUMBER(1)       DEFAULT 1 NOT NULL,
    reg_date        DATE            DEFAULT SYSDATE NOT NULL,
    CONSTRAINT PK_KANBAN_CHECKLIST  PRIMARY KEY (checklist_num),
    CONSTRAINT FK_KCHECK_CARD       FOREIGN KEY (card_num) REFERENCES KANBAN_CARD (card_num),
    CONSTRAINT CK_KCHECK_CHECKED    CHECK (checked IN (1,2))
);
COMMENT ON TABLE  KANBAN_CHECKLIST         IS '칸반 체크리스트';
COMMENT ON COLUMN KANBAN_CHECKLIST.checked IS '미완료(1), 완료(2)';

/* ---------------------------------------------------------------------
11. KANBAN_COMMENT (칸반 댓글)
--------------------------------------------------------------------- */
CREATE TABLE KANBAN_COMMENT (
    comment_num     NUMBER          NOT NULL,
    card_num        NUMBER          NOT NULL,
    user_num        NUMBER          NOT NULL,
    content         VARCHAR2(1000)  NOT NULL,
    reg_date        DATE            DEFAULT SYSDATE NOT NULL,
    modify_date     DATE,
    status          NUMBER(1)       DEFAULT 1 NOT NULL,
    CONSTRAINT PK_KANBAN_COMMENT    PRIMARY KEY (comment_num),
    CONSTRAINT FK_KCOMMENT_CARD     FOREIGN KEY (card_num) REFERENCES KANBAN_CARD (card_num),
    CONSTRAINT FK_KCOMMENT_USER     FOREIGN KEY (user_num) REFERENCES USERS       (user_num),
    CONSTRAINT CK_KCOMMENT_STATUS   CHECK (status IN (1,2))
);
COMMENT ON TABLE  KANBAN_COMMENT        IS '칸반 댓글';
COMMENT ON COLUMN KANBAN_COMMENT.status IS 'ACTIVE(1), DELETED(2)';

/* ---------------------------------------------------------------------
12. CHAT_CHANNEL (채팅 채널)
--------------------------------------------------------------------- */
CREATE TABLE CHAT_CHANNEL (
    channel_num     NUMBER(7)       NOT NULL,
    team_num        NUMBER(7)       NOT NULL,
    channel_name    VARCHAR2(255)   NOT NULL,
    is_default      CHAR(1)         DEFAULT 'N',
    create_date     DATE            DEFAULT SYSDATE NOT NULL,
    create_by       NUMBER(7)       NOT NULL,
    CONSTRAINT PK_CHAT_CHANNEL      PRIMARY KEY (channel_num),
    CONSTRAINT FK_CHANNEL_TEAM      FOREIGN KEY (team_num)  REFERENCES TEAM  (team_num),
    CONSTRAINT FK_CHANNEL_CREATEBY  FOREIGN KEY (create_by) REFERENCES USERS (user_num),
    CONSTRAINT CK_CHANNEL_DEFAULT   CHECK (is_default IN ('Y','N'))
);
COMMENT ON TABLE  CHAT_CHANNEL            IS '채팅 채널';
COMMENT ON COLUMN CHAT_CHANNEL.is_default IS '기본 채널 여부 (Y/N)';
COMMENT ON COLUMN CHAT_CHANNEL.create_by  IS '채팅방 생성자 (USERS.user_num)';

/* ---------------------------------------------------------------------
13. CHAT_MESSAGE (채팅 메시지)
--------------------------------------------------------------------- */
CREATE TABLE CHAT_MESSAGE (
    message_num     NUMBER(7)   NOT NULL,
    channel_num     NUMBER(7)   NOT NULL,
    user_num        NUMBER(7)   NOT NULL,
    content         VARCHAR2(4000),
    parent_message  NUMBER(7),
    send_date       DATE        DEFAULT SYSDATE NOT NULL,
    CONSTRAINT PK_CHAT_MESSAGE  PRIMARY KEY (message_num),
    CONSTRAINT FK_MSG_CHANNEL   FOREIGN KEY (channel_num)    REFERENCES CHAT_CHANNEL (channel_num),
    CONSTRAINT FK_MSG_USER      FOREIGN KEY (user_num)       REFERENCES USERS        (user_num),
    CONSTRAINT FK_MSG_PARENT    FOREIGN KEY (parent_message) REFERENCES CHAT_MESSAGE (message_num)
);
COMMENT ON TABLE  CHAT_MESSAGE               IS '채팅 메시지';
COMMENT ON COLUMN CHAT_MESSAGE.parent_message IS '원본 메시지 ID (답글/인용용 자가참조)';

/* ---------------------------------------------------------------------
14. FILE_FOLDER (폴더)
--------------------------------------------------------------------- */
CREATE TABLE FILE_FOLDER (
    folder_num          NUMBER(7)       NOT NULL,
    team_num            NUMBER(7)       NOT NULL,
    folder_name         VARCHAR2(100)   NOT NULL,
    parent_folder_num   NUMBER,
    create_date         DATE            DEFAULT SYSDATE,
    CONSTRAINT PK_FILE_FOLDER   PRIMARY KEY (folder_num),
    CONSTRAINT FK_FOLDER_TEAM   FOREIGN KEY (team_num)          REFERENCES TEAM        (team_num),
    CONSTRAINT FK_FOLDER_PARENT FOREIGN KEY (parent_folder_num) REFERENCES FILE_FOLDER (folder_num)
);
COMMENT ON TABLE  FILE_FOLDER                   IS '폴더';
COMMENT ON COLUMN FILE_FOLDER.parent_folder_num IS '상위 폴더 ID (자가참조, NULL 허용)';

/* ---------------------------------------------------------------------
15. CHAT_FILE (채팅 파일)
--------------------------------------------------------------------- */
CREATE TABLE CHAT_FILE (
    file_num        NUMBER(7)       NOT NULL,
    folder_num      NUMBER(7),
    message_num     NUMBER(7)       NOT NULL,
    channel_num     NUMBER(7)       NOT NULL,
    origin_name     VARCHAR2(300)   NOT NULL,
    save_name       VARCHAR2(300)   NOT NULL,
    file_size       NUMBER(19)      NOT NULL,
    file_type       VARCHAR2(50)    NOT NULL,
    uploader_num    NUMBER(7)       NOT NULL,
    team_num        NUMBER(7)       NOT NULL,
    upload_date     DATE            DEFAULT SYSDATE,
    file_source     VARCHAR2(100),
    source_ref_id   NUMBER(7),
    CONSTRAINT PK_CHAT_FILE         PRIMARY KEY (file_num),
    CONSTRAINT FK_CFILE_FOLDER      FOREIGN KEY (folder_num)  REFERENCES FILE_FOLDER  (folder_num),
    CONSTRAINT FK_CFILE_MESSAGE     FOREIGN KEY (message_num) REFERENCES CHAT_MESSAGE (message_num) ON DELETE CASCADE,
    CONSTRAINT FK_CFILE_CHANNEL     FOREIGN KEY (channel_num) REFERENCES CHAT_CHANNEL (channel_num) ON DELETE CASCADE,
    CONSTRAINT FK_CFILE_UPLOADER    FOREIGN KEY (uploader_num) REFERENCES USERS       (user_num),
    CONSTRAINT FK_CFILE_TEAM        FOREIGN KEY (team_num)    REFERENCES TEAM         (team_num),
    CONSTRAINT CK_CFILE_TYPE        CHECK (file_type IN ('IMAGE','FILE'))
);
COMMENT ON TABLE  CHAT_FILE           IS '채팅 파일';
COMMENT ON COLUMN CHAT_FILE.save_name IS '서버 저장용 고유 파일명 (UUID)';
COMMENT ON COLUMN CHAT_FILE.file_type IS 'IMAGE 또는 FILE';
COMMENT ON COLUMN CHAT_FILE.file_source IS '파일 출처 (CHAT, MINUTES, DRIVE 등)';

/* ---------------------------------------------------------------------
16. CHAT_READ_STATUS (채널별 읽음 확인) - 복합 PK
--------------------------------------------------------------------- */
CREATE TABLE CHAT_READ_STATUS (
    channel_num             NUMBER(7)   NOT NULL,
    user_num                NUMBER(7)   NOT NULL,
    last_read_message_num   NUMBER(7)   DEFAULT 0,
    CONSTRAINT PK_CHAT_READ_STATUS  PRIMARY KEY (channel_num, user_num),
    CONSTRAINT FK_READ_CHANNEL      FOREIGN KEY (channel_num) REFERENCES CHAT_CHANNEL (channel_num),
    CONSTRAINT FK_READ_USER         FOREIGN KEY (user_num)    REFERENCES USERS        (user_num)
);
COMMENT ON TABLE  CHAT_READ_STATUS                       IS '채널별 읽음 확인';
COMMENT ON COLUMN CHAT_READ_STATUS.last_read_message_num IS '마지막으로 읽은 메시지 ID';

/* ---------------------------------------------------------------------
17. CHAT_MENTION (멘션)
--------------------------------------------------------------------- */
CREATE TABLE CHAT_MENTION (
    mention_num             NUMBER(7)   NOT NULL,
    message_num             NUMBER(7)   NOT NULL,
    mentioned_member_num    NUMBER(7)   NOT NULL,
    is_notified             CHAR(1)     DEFAULT 'Y' NOT NULL,
    CONSTRAINT PK_CHAT_MENTION      PRIMARY KEY (mention_num),
    CONSTRAINT FK_MENTION_MESSAGE   FOREIGN KEY (message_num)          REFERENCES CHAT_MESSAGE (message_num),
    CONSTRAINT FK_MENTION_MEMBER    FOREIGN KEY (mentioned_member_num) REFERENCES USERS        (user_num),
    CONSTRAINT CK_MENTION_NOTIFIED  CHECK (is_notified IN ('Y','N'))
);
COMMENT ON TABLE  CHAT_MENTION              IS '멘션';
COMMENT ON COLUMN CHAT_MENTION.is_notified  IS '알림 발송 여부 (Y/N)';

/* ---------------------------------------------------------------------
18. SCHEDULE (캘린더/일정)
--------------------------------------------------------------------- */
CREATE TABLE SCHEDULE (
    schedule_num    NUMBER          NOT NULL,
    team_num        NUMBER          NOT NULL,
    user_num        NUMBER          NOT NULL,
    title           VARCHAR2(200)   NOT NULL,
    content         VARCHAR2(2000),
    category        VARCHAR2(50),
    color           VARCHAR2(20),
    start_date      VARCHAR2(30)    NOT NULL,
    end_date        VARCHAR2(30)    NOT NULL,
    all_day         NUMBER(1)       DEFAULT 2 NOT NULL,
    status          NUMBER(1)       DEFAULT 1 NOT NULL,
    reg_date        DATE            DEFAULT SYSDATE NOT NULL,
    modify_date     DATE,
    CONSTRAINT PK_SCHEDULE          PRIMARY KEY (schedule_num),
    CONSTRAINT FK_SCHEDULE_TEAM     FOREIGN KEY (team_num) REFERENCES TEAM  (team_num),
    CONSTRAINT FK_SCHEDULE_USER     FOREIGN KEY (user_num) REFERENCES USERS (user_num),
    CONSTRAINT CK_SCHEDULE_ALLDAY   CHECK (all_day IN (1,2)),
    CONSTRAINT CK_SCHEDULE_STATUS   CHECK (status  IN (1,2))
);
COMMENT ON TABLE  SCHEDULE          IS '일정';
COMMENT ON COLUMN SCHEDULE.user_num IS '작성자 (USERS.user_num)';
COMMENT ON COLUMN SCHEDULE.all_day  IS '종일(1), 시간 지정(2)';
COMMENT ON COLUMN SCHEDULE.status   IS 'ACTIVE(1), DELETED(2)';

/* ---------------------------------------------------------------------
19. MEETING_MINUTES (회의록)
--------------------------------------------------------------------- */
CREATE TABLE MEETING_MINUTES (
    minutes_num     NUMBER          NOT NULL,
    team_num        NUMBER          NOT NULL,
    schedule_num    NUMBER,
    writer_num      NUMBER          NOT NULL,
    title           VARCHAR2(200)   NOT NULL,
    meeting_date    DATE            NOT NULL,
    content         CLOB            NOT NULL,
    pdf_path        VARCHAR2(255),
    status          NUMBER(1)       DEFAULT 1 NOT NULL,
    reg_date        DATE            DEFAULT SYSDATE NOT NULL,
    modify_date     DATE,
    CONSTRAINT PK_MEETING_MINUTES   PRIMARY KEY (minutes_num),
    CONSTRAINT FK_MINUTES_TEAM      FOREIGN KEY (team_num)    REFERENCES TEAM     (team_num),
    CONSTRAINT FK_MINUTES_SCHEDULE  FOREIGN KEY (schedule_num) REFERENCES SCHEDULE (schedule_num),
    CONSTRAINT FK_MINUTES_WRITER    FOREIGN KEY (writer_num)  REFERENCES USERS    (user_num),
    CONSTRAINT CK_MINUTES_STATUS    CHECK (status IN (1,2))
);
COMMENT ON TABLE  MEETING_MINUTES        IS '회의록';
COMMENT ON COLUMN MEETING_MINUTES.status IS 'ACTIVE(1), DELETED(2)';

/* ---------------------------------------------------------------------
20. SCHEDULE_ATTENDEE (일정 참여자)
--------------------------------------------------------------------- */
CREATE TABLE SCHEDULE_ATTENDEE (
    attendee_num    NUMBER      NOT NULL,
    schedule_num    NUMBER      NOT NULL,
    user_num        NUMBER      NOT NULL,
    status          NUMBER(1)   NOT NULL,
    response_date   DATE,
    CONSTRAINT PK_SCHEDULE_ATTENDEE     PRIMARY KEY (attendee_num),
    CONSTRAINT FK_SATTENDEE_SCHEDULE    FOREIGN KEY (schedule_num) REFERENCES SCHEDULE (schedule_num),
    CONSTRAINT FK_SATTENDEE_USER        FOREIGN KEY (user_num)     REFERENCES USERS    (user_num),
    CONSTRAINT UK_SATTENDEE             UNIQUE (schedule_num, user_num),
    CONSTRAINT CK_SATTENDEE_STATUS      CHECK (status IN (1,2,3))
);
COMMENT ON TABLE  SCHEDULE_ATTENDEE        IS '일정 참여자';
COMMENT ON COLUMN SCHEDULE_ATTENDEE.status IS 'PENDING(1), ACCEPTED(2), DECLINED(3)';

/* ---------------------------------------------------------------------
21. MEETING_MINUTES_ATTENDEE (회의록 참석자)
--------------------------------------------------------------------- */
CREATE TABLE MEETING_MINUTES_ATTENDEE (
    minutes_attendee_num    NUMBER  NOT NULL,
    minutes_num             NUMBER  NOT NULL,
    user_num                NUMBER  NOT NULL,
    CONSTRAINT PK_MINUTES_ATTENDEE  PRIMARY KEY (minutes_attendee_num),
    CONSTRAINT FK_MATTENDEE_MINUTES FOREIGN KEY (minutes_num) REFERENCES MEETING_MINUTES (minutes_num),
    CONSTRAINT FK_MATTENDEE_USER    FOREIGN KEY (user_num)    REFERENCES USERS           (user_num),
    CONSTRAINT UK_MATTENDEE         UNIQUE (minutes_num, user_num)
);
COMMENT ON TABLE MEETING_MINUTES_ATTENDEE IS '회의록 참석자';

/* ---------------------------------------------------------------------
22. NOTIFICATION (알림)
--------------------------------------------------------------------- */
CREATE TABLE NOTIFICATION (
    noti_num        NUMBER          NOT NULL,
    sender_num      NUMBER          NOT NULL,
    receiver_num    NUMBER          NOT NULL,
    team_num        NUMBER,
    noti_type       NUMBER(1)       NOT NULL,
    title           VARCHAR2(200)   NOT NULL,
    content         VARCHAR2(1000),
    reg_date        DATE            DEFAULT SYSDATE NOT NULL,
    read_status     NUMBER(1)       DEFAULT 1 NOT NULL,
    link            VARCHAR2(2000),
    CONSTRAINT PK_NOTIFICATION      PRIMARY KEY (noti_num),
    CONSTRAINT FK_NOTI_SENDER       FOREIGN KEY (sender_num)   REFERENCES USERS (user_num),
    CONSTRAINT FK_NOTI_RECEIVER     FOREIGN KEY (receiver_num) REFERENCES USERS (user_num),
    CONSTRAINT FK_NOTI_TEAM         FOREIGN KEY (team_num)     REFERENCES TEAM  (team_num),
    CONSTRAINT CK_NOTI_TYPE         CHECK (noti_type    IN (1,2,3,4,5)),
    CONSTRAINT CK_NOTI_READ         CHECK (read_status  IN (1,2))
);
COMMENT ON TABLE  NOTIFICATION              IS '알림';
COMMENT ON COLUMN NOTIFICATION.noti_type    IS '일정(1), 채팅(2), 칸반(3), 팀(4), 공지(5)';
COMMENT ON COLUMN NOTIFICATION.read_status  IS '안읽음(1), 읽음(2)';

/* ---------------------------------------------------------------------
23. NOTICE (공지사항)
--------------------------------------------------------------------- */
CREATE TABLE NOTICE (
    notice_num  NUMBER          NOT NULL,
    team_num    NUMBER          NOT NULL,
    user_num    NUMBER          NOT NULL,
    title       VARCHAR2(200)   NOT NULL,
    content     CLOB            NOT NULL,
    is_fixed    VARCHAR2(1)     DEFAULT 'N' NOT NULL,
    reg_date    DATE            DEFAULT SYSDATE NOT NULL,
    view_count  NUMBER          DEFAULT 0 NOT NULL,
    CONSTRAINT PK_NOTICE        PRIMARY KEY (notice_num),
    CONSTRAINT FK_NOTICE_TEAM   FOREIGN KEY (team_num) REFERENCES TEAM  (team_num),
    CONSTRAINT FK_NOTICE_USER   FOREIGN KEY (user_num) REFERENCES USERS (user_num),
    CONSTRAINT CK_NOTICE_FIXED  CHECK (is_fixed IN ('Y','N'))
);
COMMENT ON TABLE  NOTICE            IS '팀 공지사항';
COMMENT ON COLUMN NOTICE.notice_num IS '공지사항 번호 (PK)';
COMMENT ON COLUMN NOTICE.is_fixed   IS '고정 공지 여부 (Y/N)';
COMMENT ON COLUMN NOTICE.view_count IS '조회수';

CREATE INDEX IDX_NOTICE_TEAM ON NOTICE(TEAM_NUM, REG_DATE);

/* ---------------------------------------------------------------------
24. BOT_LOG (챗봇 로그)
사용자 탈퇴 시 hard delete -> user_num ON DELETE CASCADE
--------------------------------------------------------------------- */
CREATE TABLE BOT_LOG (
    bot_num     NUMBER  NOT NULL,
    user_num    NUMBER  NOT NULL,
    team_num    NUMBER  NOT NULL,
    message     CLOB    NOT NULL,
    sender      NUMBER(1)       NOT NULL,
    reg_date    DATE            DEFAULT SYSDATE NOT NULL,
    CONSTRAINT PK_BOT_LOG       PRIMARY KEY (bot_num),
    CONSTRAINT FK_BOTLOG_USER   FOREIGN KEY (user_num) REFERENCES USERS (user_num) ON DELETE CASCADE,
    CONSTRAINT FK_BOTLOG_TEAM   FOREIGN KEY (team_num) REFERENCES TEAM  (team_num),
    CONSTRAINT CK_BOTLOG_SENDER CHECK (sender IN (1,2))
);
COMMENT ON TABLE  BOT_LOG        IS '챗봇 로그';
COMMENT ON COLUMN BOT_LOG.sender IS '사용자(1), 챗봇(2)';

/* ---------------------------------------------------------------------
25. SEQUENCES (PK 채번용)
--------------------------------------------------------------------- */
CREATE SEQUENCE SEQ_USERS                    START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE SEQ_TEAM                     START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE SEQ_TEAM_MEMBER              START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE SEQ_EMAIL_INVITATION         START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE SEQ_TEAM_INVITE_CODE         START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE SEQ_TODO_CATEGORY            START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE SEQ_PERSONAL_TODO            START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE SEQ_KANBAN_CARD              START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE SEQ_KANBAN_ASSIGN            START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE SEQ_KANBAN_CHECKLIST         START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE SEQ_KANBAN_COMMENT           START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE SEQ_CHAT_CHANNEL             START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE SEQ_CHAT_MESSAGE             START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE SEQ_FILE_FOLDER              START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE SEQ_CHAT_FILE                START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE SEQ_CHAT_MENTION             START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE SEQ_SCHEDULE                 START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE SEQ_MEETING_MINUTES          START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE SEQ_SCHEDULE_ATTENDEE        START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE SEQ_MEETING_MINUTES_ATTENDEE START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE SEQ_NOTIFICATION             START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE SEQ_NOTICE                   START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE SEQ_BOT_LOG                  START WITH 1 INCREMENT BY 1 NOCACHE;

COMMIT;