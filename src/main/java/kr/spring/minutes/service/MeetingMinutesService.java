package kr.spring.minutes.service;

import java.util.List;

import kr.spring.minutes.vo.MeetingMinutesAttendeeVO;
import kr.spring.minutes.vo.MeetingMinutesVO;

public interface MeetingMinutesService {

    void insertMeetingMinutes(MeetingMinutesVO meetingMinutesVO);

    MeetingMinutesVO selectMeetingMinutes(Long minutes_num);

    List<MeetingMinutesVO> selectMeetingMinutesListByTeam(Long team_num);

    MeetingMinutesVO selectMeetingMinutesBySchedule(Long schedule_num);

    List<MeetingMinutesVO> selectRecentMeetingMinutesList(Long team_num);

    void updateMeetingMinutes(MeetingMinutesVO meetingMinutesVO);

    void deleteMeetingMinutes(Long minutes_num);

    void insertMeetingMinutesAttendee(
            MeetingMinutesAttendeeVO attendeeVO);

    List<MeetingMinutesAttendeeVO>
        selectMeetingMinutesAttendeeList(Long minutes_num);

    void deleteMeetingMinutesAttendeeByMinutes(
            Long minutes_num);
}