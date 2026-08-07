package kr.spring.team.dao;

import org.apache.ibatis.annotations.Mapper;

import kr.spring.team.vo.TeamInviteCodeVO;

@Mapper
public interface TeamInviteCodeMapper {
	//팀의 초대코드 row 조회(UK 설정으로 있으면 status 상태값 상관 없이 1건)
	public TeamInviteCodeVO selectByTeamNum(long teamNum);
	
	//신규 발급 (해당 팀에 row가 아예 없을 때)
	public void insertInviteCode(TeamInviteCodeVO vo);
	
	//재발급 (기존 row 덮어쓰기 - TEAM_NUM UK 제약 때문에 UPDATE로 작업)
	public void updateInviteCode(TeamInviteCodeVO vo);
	
	//초대코드값 조회 (초대코드를 통한 참여 시 사용)
	public TeamInviteCodeVO selectByCode(String code);
	
	//만료 처리(lazy - 조회 시점에 기한 지났으면 상태 전환)
	public void expireInviteCode(long inviteCodeNum);
	
	//팀 삭제 시 무조건 비활성화
	public void disableInviteCodeByTeam(long teamNum);
}
