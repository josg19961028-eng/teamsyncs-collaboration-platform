package kr.spring.schedule.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import kr.spring.schedule.vo.ScheduleAttendeeVO;
import kr.spring.schedule.vo.ScheduleVO;

@Mapper
public interface ScheduleMapper {

    void insertSchedule(ScheduleVO scheduleVO);

    ScheduleVO selectSchedule(@Param("schedule_num") Long schedule_num);

    List<ScheduleVO> selectScheduleListByTeam(@Param("team_num") Long team_num);

    void updateSchedule(ScheduleVO scheduleVO);

    void deleteSchedule(@Param("schedule_num") Long schedule_num);

    void insertScheduleAttendee(ScheduleAttendeeVO attendeeVO);

    List<ScheduleAttendeeVO> selectScheduleAttendeeList(@Param("schedule_num") Long schedule_num);

    void deleteScheduleAttendeeBySchedule(@Param("schedule_num") Long schedule_num);

    void updateScheduleAttendeeStatus(@Param("schedule_num") Long schedule_num,
                                      @Param("user_num") Long user_num,
                                      @Param("status") Integer status);
}