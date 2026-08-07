package kr.spring.team.service;

import java.util.List;

import kr.spring.team.vo.TeamVO;

public interface TeamService {

    // 팀 생성
    void insertTeam(TeamVO team);

    // 팀 단건 조회
    TeamVO selectTeamByNum(long teamNum);

    // 팀명 중복 체크용 조회 (TM-001)
    TeamVO selectTeamByName(String teamName);

    // 유저가 만든 팀 목록
    List<TeamVO> selectTeamsByCreator(long userNum);

    // 팀 정보 수정
    void updateTeam(TeamVO team);

    // 팀 삭제 (soft delete)
    void deleteTeam(long teamNum);
    
    // 사이드바용 팀 헤더 조회
    java.util.Map<String, Object> selectTeamSidebar(long teamNum);
}