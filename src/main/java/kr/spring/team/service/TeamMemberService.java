package kr.spring.team.service;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import kr.spring.team.vo.TeamMemberVO;
import kr.spring.team.vo.TeamVO;

public interface TeamMemberService {
	// 팀원 추가
	public void insertTeamMember(TeamMemberVO teamMember);

	// 특정 팀에서 특정 유저 조회 (역할 체크용)
	public TeamMemberVO selectMemberByTeamAndUser(@Param("teamNum") long teamNum,
			@Param("userNum") long userNum);

	// 팀원 목록 조회 (소속중인 팀원만)
	public List<TeamMemberVO> selectMembersByTeamNum(long teamNum);

	// 팀원 수 조회 (삭제 조건 체크용)
	public int countMembers(long teamNum);

	// 내가 속한 팀 목록 (TEAM 조인)
	public List<TeamVO> selectTeamsByUserNum(long userNum);

	// 역할 변경 (팀원↔매니저, 팀장 위임 시 공통 사용)
	public void updateRole(TeamMemberVO teamMember);

	// 강퇴 (join_status = 2)
	public void kickMember(@Param("teamNum") long teamNum,
			@Param("userNum") long userNum);

	// 자진탈퇴 (join_status = 3)
	public void exitMember(@Param("teamNum") long teamNum,
			@Param("userNum") long userNum);

	public TeamMemberVO selectMemberByTeamAndUserAnyStatus(@Param("teamNum") long teamNum, @Param("userNum") long userNum);

	public void reactivateMember (@Param("teamNum") long teamNum, @Param("userNum") long userNum);

	// 이메일 초대 자동완성 (현재 팀에 소속중인 사람 제외)
	public List<String> searchEmailsForInvite(@Param("teamNum") long teamNum,
			@Param("keyword") String keyword);
	
	// 팀장 조회
	public TeamMemberVO selectLeaderByTeamNum(long teamNum);
	
}
