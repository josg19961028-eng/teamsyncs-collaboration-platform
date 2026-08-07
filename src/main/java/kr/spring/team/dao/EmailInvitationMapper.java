package kr.spring.team.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import kr.spring.team.vo.EmailInvitationVO;

@Mapper
public interface EmailInvitationMapper {
	//신규 초대 등록
	public void insertInvitation(EmailInvitationVO vo);
	
	//동일 팀 + 동일 이메일의 최근 초대 1건 조회(재초대 판단용)
	public EmailInvitationVO selectByTeamAndEmail(@Param("teamNum") long teamNum, @Param("email") String email);
	
	//초대번호로 단건 조회(수락/거절 처리용)
	EmailInvitationVO selectByNum(long InvitationNum);
	
	//기존 REJECTED/EXPIRED/CANCELED row 재사용 - > PENDING으로 갱신(재초대)
	public void updateToPending(long invitationNum);
	
	//수락/거절 처리 (응답일 갱신)
	public void respondInvitation(@Param("invitationNum") long invitationNum, @Param("status") int status);
	
	//만료 lazy 처리(응답일 갱신은 하지 않음)
	public void updateStatusOnly(@Param("invitationNum") long invitationNum, @Param("status") int status);
	
	//팀 삭제 시 PENDING 초대 일괄 취소 (팀이 삭제되면 이메일초대로 들어가면 안됨)
	public void cancelPendingByTeam(long teamNum);
}
