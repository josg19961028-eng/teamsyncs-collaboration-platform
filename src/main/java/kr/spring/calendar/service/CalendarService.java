package kr.spring.calendar.service;

import java.util.List;

import kr.spring.calendar.vo.CalendarEventVO;
import kr.spring.schedule.vo.ScheduleVO;

public interface CalendarService {

    List<CalendarEventVO> selectScheduleEventList(Long team_num);

    List<CalendarEventVO> selectMyScheduleEventList(Long user_num);

    List<CalendarEventVO> selectScheduleEventListByPeriod(
            Long team_num,
            String start_date,
            String end_date
    );

    List<CalendarEventVO> selectTeamCalendarEventList(
            Long team_num,
            Long user_num,
            String start_date,
            String end_date
    );

    int countScheduleWritePermission(
            Long team_num,
            Long user_num
    );

    int insertSchedule(ScheduleVO scheduleVO);
    
    int updateSchedule(ScheduleVO scheduleVO);

    int deleteSchedule(Long schedule_num, Long user_num);
}
