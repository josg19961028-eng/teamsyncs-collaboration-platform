package kr.spring.team.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.spring.team.dao.TeamMemberMapper;
import kr.spring.team.vo.TeamMemberVO;
import kr.spring.team.vo.TeamVO;


@Service
@Transactional
public class TeamMemberServiceImpl implements TeamMemberService{
	
	
	@Autowired
	private TeamMemberMapper teamMemberMapper;
	
	@Override
	public void insertTeamMember(TeamMemberVO teamMember) {
		teamMemberMapper.insertTeamMember(teamMember);
		
	}

	@Override
	public TeamMemberVO selectMemberByTeamAndUser(long teamNum, long userNum) {
		return teamMemberMapper.selectMemberByTeamAndUser(teamNum, userNum);
	}

	@Override
	public List<TeamMemberVO> selectMembersByTeamNum(long teamNum) {
		return teamMemberMapper.selectMembersByTeamNum(teamNum);
	}

	@Override
	public int countMembers(long teamNum) {
		return teamMemberMapper.countMembers(teamNum);
	}

	@Override
	public List<TeamVO> selectTeamsByUserNum(long userNum) {
		return teamMemberMapper.selectTeamsByUserNum(userNum);
	}

	@Override
	public void updateRole(TeamMemberVO teamMember) {
		teamMemberMapper.updateRole(teamMember);
		
	}

	@Override
	public void kickMember(long teamNum, long userNum) {
		teamMemberMapper.kickMember(teamNum, userNum);
		
	}

	@Override
	public void exitMember(long teamNum, long userNum) {
		teamMemberMapper.exitMember(teamNum, userNum);
		
	}

	@Override
	public TeamMemberVO selectMemberByTeamAndUserAnyStatus(long teamNum, long userNum) {
		return teamMemberMapper.selectMemberByTeamAndUserAnyStatus(teamNum, userNum);
	}

	@Override
	public void reactivateMember(long teamNum, long userNum) {
		teamMemberMapper.reactivateMember(teamNum, userNum);
		
	}

	@Override
	public List<String> searchEmailsForInvite(long teamNum, String keyword) {
		return teamMemberMapper.searchEmailsForInvite(teamNum, keyword);
	}

	@Override
	public TeamMemberVO selectLeaderByTeamNum(long teamNum) {
		return teamMemberMapper.selectLeaderByTeamNum(teamNum);
	}
	

}
