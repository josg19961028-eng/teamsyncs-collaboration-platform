package kr.spring.minutes.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import kr.spring.minutes.vo.MeetingMinutesAttendeeVO;
import kr.spring.minutes.vo.MeetingMinutesVO;

@Mapper
public interface MeetingMinutesMapper {

    void insertMeetingMinutes(MeetingMinutesVO meetingMinutesVO);

    MeetingMinutesVO selectMeetingMinutes(@Param("minutes_num") Long minutes_num);

    List<MeetingMinutesVO> selectMeetingMinutesListByTeam(@Param("team_num") Long team_num);

    MeetingMinutesVO selectMeetingMinutesBySchedule(@Param("schedule_num") Long schedule_num);

    List<MeetingMinutesVO> selectRecentMeetingMinutesList(@Param("team_num") Long team_num);

    void updateMeetingMinutes(MeetingMinutesVO meetingMinutesVO);

    void deleteMeetingMinutes(@Param("minutes_num") Long minutes_num);

    void insertMeetingMinutesAttendee(MeetingMinutesAttendeeVO attendeeVO);

    List<MeetingMinutesAttendeeVO> selectMeetingMinutesAttendeeList(@Param("minutes_num") Long minutes_num);

    void deleteMeetingMinutesAttendeeByMinutes(@Param("minutes_num") Long minutes_num);
}