package kr.spring.team.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.spring.team.dao.EmailInvitationMapper;
import kr.spring.team.vo.EmailInvitationVO;

@Service
@Transactional
public class EmailInvitationServiceImpl implements EmailInvitationService{

	@Autowired
	private EmailInvitationMapper emailInvitationMapper;
	
	@Override
	public void insertInvitation(EmailInvitationVO vo) {
		emailInvitationMapper.insertInvitation(vo);
		
	}

	@Override
	public EmailInvitationVO selectByTeamAndEmail(long teamNum, String email) {
		return emailInvitationMapper.selectByTeamAndEmail(teamNum, email);
	}

	@Override
	public EmailInvitationVO selectByNum(long InvitationNum) {
		return emailInvitationMapper.selectByNum(InvitationNum);
	}

	@Override
	public void updateToPending(long invitationNum) {
		emailInvitationMapper.updateToPending(invitationNum);
		
	}

	@Override
	public void respondInvitation(long invitationNum, int status) {
		emailInvitationMapper.respondInvitation(invitationNum, status);
		
	}

	@Override
	public void updateStatusOnly(long invitationNum, int status) {
		emailInvitationMapper.updateStatusOnly(invitationNum, status);
		
	}

	@Override
	public void cancelPendingByTeam(long teamNum) {
		emailInvitationMapper.cancelPendingByTeam(teamNum);
		
	}

}
