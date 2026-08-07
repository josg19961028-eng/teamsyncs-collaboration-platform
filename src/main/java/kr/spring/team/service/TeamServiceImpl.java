package kr.spring.team.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.spring.team.dao.TeamMapper;
import kr.spring.team.vo.TeamVO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
public class TeamServiceImpl implements TeamService {

    @Autowired
    private TeamMapper teamMapper;

    @Override
    public void insertTeam(TeamVO team) {
        teamMapper.insertTeam(team);
    }

    @Override
    public TeamVO selectTeamByNum(long teamNum) {
        return teamMapper.selectTeamByNum(teamNum);
    }

    @Override
    public TeamVO selectTeamByName(String teamName) {
        return teamMapper.selectTeamByName(teamName);
    }

    @Override
    public List<TeamVO> selectTeamsByCreator(long userNum) {
        return teamMapper.selectTeamsByCreator(userNum);
    }

    @Override
    public void updateTeam(TeamVO team) {
        teamMapper.updateTeam(team);
    }

    @Override
    public void deleteTeam(long teamNum) {
        // TODO: TeamMemberMapper 완성 후 팀원 수 체크 로직 추가
        // int memberCount = teamMemberMapper.countMembers(teamNum);
        // if (memberCount > 1) throw new IllegalStateException("팀원이 있어 삭제 불가");
        teamMapper.deleteTeam(teamNum);
    }

    @Override
    public java.util.Map<String, Object> selectTeamSidebar(long teamNum) {
        return teamMapper.selectTeamSidebar(teamNum);
    }
}