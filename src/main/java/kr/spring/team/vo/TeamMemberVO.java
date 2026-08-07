package kr.spring.team.vo;

import java.sql.Date;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class TeamMemberVO {

    private long team_member_num;  // PK
    private long team_num;         // FK - TEAM
    private long user_num;         // FK - USERS
    private int role;              // 1:팀원, 2:매니저, 3:팀장
    private int join_status;       // 1:소속중, 2:강퇴, 3:자진탈퇴
    private Date joined_at;
    private Date last_activity_at;
    private Date exited_at;

    // ===== USERS 조인 전용 (DB 컬럼 아님, 목록/상세 조회시에만 채워짐) =====
    private String user_name;
    private String email;
    private String intro;
    private String photo_name;

    // ===== 활동 통계 (칸반/채팅/보관함 모듈 완성 전까지 0 고정) =====
    private int card_cnt;   // 칸반카드 생성 수
    private int done_cnt;   // 칸반카드 완료 수
    private int file_cnt;   // 파일 업로드 횟수
    private int chat_cnt;   // 채팅 메시지 수

    // 역할 문자열 반환
    public String getRoleName() {
        switch (role) {
            case 3: return "LEADER";
            case 2: return "MANAGER";
            default: return "MEMBER";
        }
    }

    // 소속 중 여부
    public boolean isActive() {
        return join_status == 1;
    }
}