package kr.spring.calendar.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import kr.spring.calendar.vo.CalendarEventVO;
import kr.spring.schedule.vo.ScheduleVO;

@Mapper
public interface CalendarMapper {

    // 팀 캘린더 일정 조회
    List<CalendarEventVO> selectScheduleEventList(
            @Param("team_num") Long team_num
    );

    // 홈 캘린더용, 내가 속한 팀 전체 일정 조회
    List<CalendarEventVO> selectMyScheduleEventList(
            @Param("user_num") Long user_num
    );

    // 기간 조건 일정 조회
    List<CalendarEventVO> selectScheduleEventListByPeriod(
            @Param("team_num") Long team_num,
            @Param("start_date") String start_date,
            @Param("end_date") String end_date
    );

    // 기간 조건 칸반 일정 조회
    List<CalendarEventVO> selectKanbanEventList(
            @Param("team_num") Long team_num,
            @Param("user_num") Long user_num,
            @Param("start_date") String start_date,
            @Param("end_date") String end_date
    );

    // 일정 등록 권한 개수 조회
    int countScheduleWritePermission(
            @Param("team_num") Long team_num,
            @Param("user_num") Long user_num
    );

    // 일정 등록
    int insertSchedule(ScheduleVO scheduleVO);
    
    int updateSchedule(ScheduleVO scheduleVO);

    int deleteSchedule(
            @Param("schedule_num") Long schedule_num,
            @Param("user_num") Long user_num
    );
}
