package kr.spring.calendar.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.spring.calendar.dao.CalendarMapper;
import kr.spring.calendar.vo.CalendarEventVO;
import kr.spring.schedule.vo.ScheduleVO;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CalendarServiceImpl implements CalendarService {

    private final CalendarMapper calendarMapper;

    // 팀 캘린더 일정 조회
    @Override
    public List<CalendarEventVO> selectScheduleEventList(Long team_num) {
        return calendarMapper.selectScheduleEventList(team_num);
    }

    // 내가 속한 모든 팀 일정 조회
    @Override
    public List<CalendarEventVO> selectMyScheduleEventList(Long user_num) {
        return calendarMapper.selectMyScheduleEventList(user_num);
    }

    // 기간 조건 일정 조회
    @Override
    public List<CalendarEventVO> selectScheduleEventListByPeriod(
            Long team_num,
            String start_date,
            String end_date) {

        return calendarMapper.selectScheduleEventListByPeriod(
                team_num,
                start_date,
                end_date
        );
    }

    // 팀 일정과 칸반 마감 일정 통합 조회
    @Override
    public List<CalendarEventVO> selectTeamCalendarEventList(
            Long team_num,
            Long user_num,
            String start_date,
            String end_date) {

        List<CalendarEventVO> events = new ArrayList<>(
                calendarMapper.selectScheduleEventListByPeriod(
                        team_num,
                        start_date,
                        end_date
                )
        );

        events.addAll(
                calendarMapper.selectKanbanEventList(
                        team_num,
                        user_num,
                        start_date,
                        end_date
                )
        );

        return events;
    }

    // 팀장 또는 매니저인지 확인
    @Override
    public int countScheduleWritePermission(
            Long team_num,
            Long user_num) {

        return calendarMapper.countScheduleWritePermission(
                team_num,
                user_num
        );
    }

    // 일정 등록
    @Transactional
    @Override
    public int insertSchedule(ScheduleVO scheduleVO) {
        return calendarMapper.insertSchedule(scheduleVO);
    }
    
    @Transactional
    @Override
    public int updateSchedule(ScheduleVO scheduleVO) {
        return calendarMapper.updateSchedule(scheduleVO);
    }

    @Transactional
    @Override
    public int deleteSchedule(Long schedule_num, Long user_num) {
        return calendarMapper.deleteSchedule(schedule_num, user_num);
    }

}
