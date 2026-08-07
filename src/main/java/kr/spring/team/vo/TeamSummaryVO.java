package kr.spring.team.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 사용자 홈 화면의 "내 팀" 목록 표시용 (DB 테이블과 무관, 화면 조립용 DTO)
 */
@Getter
@Setter
@ToString
public class TeamSummaryVO {
    private TeamVO team;
    private int role;        // 1:MEMBER, 2:MANAGER, 3:LEADER (로그인 사용자의 이 팀에서의 역할)
    private int memberCount; // 현재 소속 팀원 수

    public String getRoleName() {
        switch (role) {
            case 3: return "LEADER";
            case 2: return "MANAGER";
            default: return "MEMBER";
        }
    }
}