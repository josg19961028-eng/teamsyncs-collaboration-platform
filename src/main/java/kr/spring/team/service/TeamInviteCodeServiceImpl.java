package kr.spring.team.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.spring.team.dao.TeamInviteCodeMapper;
import kr.spring.team.vo.TeamInviteCodeVO;

@Service
@Transactional
public class TeamInviteCodeServiceImpl implements TeamInviteCodeService{

	@Autowired
	private TeamInviteCodeMapper teamInviteCodeMapper;
	
	@Override
	public TeamInviteCodeVO selectByTeamNum(long teamNum) {
		return teamInviteCodeMapper.selectByTeamNum(teamNum);
	}

	@Override
	public void insertInviteCode(TeamInviteCodeVO vo) {
		teamInviteCodeMapper.insertInviteCode(vo);
		
	}

	@Override
	public void updateInviteCode(TeamInviteCodeVO vo) {
		teamInviteCodeMapper.updateInviteCode(vo);
		
	}

	@Override
	public TeamInviteCodeVO selectByCode(String code) {
		return teamInviteCodeMapper.selectByCode(code);
	}

	@Override
	public void expireInviteCode(long inviteCodeNum) {
		teamInviteCodeMapper.expireInviteCode(inviteCodeNum);
		
	}

	@Override
	public void disableInviteCodeByTeam(long teamNum) {
		teamInviteCodeMapper.disableInviteCodeByTeam(teamNum);
		
	}

}
