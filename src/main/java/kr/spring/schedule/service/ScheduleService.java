package kr.spring.schedule.service;

import java.util.List;

import kr.spring.schedule.vo.ScheduleVO;

public interface ScheduleService {

    // 일정 단건 조회
    ScheduleVO selectSchedule(Long schedule_num);

    // 팀별 활성 일정 목록
    List<ScheduleVO> selectScheduleListByTeam(Long team_num);
}