package kr.spring.schedule.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.spring.schedule.dao.ScheduleMapper;
import kr.spring.schedule.vo.ScheduleVO;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleServiceImpl implements ScheduleService {

    private final ScheduleMapper scheduleMapper;

    @Override
    public ScheduleVO selectSchedule(Long schedule_num) {
        return scheduleMapper.selectSchedule(schedule_num);
    }

    @Override
    public List<ScheduleVO> selectScheduleListByTeam(Long team_num) {
        return scheduleMapper.selectScheduleListByTeam(team_num);
    }
}