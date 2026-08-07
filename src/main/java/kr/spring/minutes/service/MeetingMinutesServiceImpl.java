package kr.spring.minutes.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.spring.minutes.dao.MeetingMinutesMapper;
import kr.spring.minutes.vo.MeetingMinutesAttendeeVO;
import kr.spring.minutes.vo.MeetingMinutesVO;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MeetingMinutesServiceImpl
        implements MeetingMinutesService {

    private final MeetingMinutesMapper meetingMinutesMapper;

    // 회의록 등록
    @Override
    @Transactional
    public void insertMeetingMinutes(
            MeetingMinutesVO meetingMinutesVO) {

        // MEETING_MINUTES 등록
        meetingMinutesMapper.insertMeetingMinutes(
                meetingMinutesVO);

        // 선택한 참석자 등록
        insertAttendeeList(
                meetingMinutesVO.getMinutes_num(),
                meetingMinutesVO.getAttendeeNums());
    }

    // 회의록 상세
    @Override
    public MeetingMinutesVO selectMeetingMinutes(
            Long minutes_num) {

        return meetingMinutesMapper
                .selectMeetingMinutes(minutes_num);
    }

    // 팀별 회의록 목록
    @Override
    public List<MeetingMinutesVO>
        selectMeetingMinutesListByTeam(Long team_num) {

        return meetingMinutesMapper
                .selectMeetingMinutesListByTeam(team_num);
    }

    // 연결 일정으로 회의록 조회
    @Override
    public MeetingMinutesVO selectMeetingMinutesBySchedule(
            Long schedule_num) {

        return meetingMinutesMapper
                .selectMeetingMinutesBySchedule(schedule_num);
    }

    // 최근 회의록 목록
    @Override
    public List<MeetingMinutesVO>
        selectRecentMeetingMinutesList(Long team_num) {

        return meetingMinutesMapper
                .selectRecentMeetingMinutesList(team_num);
    }

    // 회의록 수정
    @Override
    @Transactional
    public void updateMeetingMinutes(
            MeetingMinutesVO meetingMinutesVO) {

        // 기본 회의록 내용 수정
        meetingMinutesMapper.updateMeetingMinutes(
                meetingMinutesVO);

        // 기존 참석자 전체 삭제
        meetingMinutesMapper
                .deleteMeetingMinutesAttendeeByMinutes(
                        meetingMinutesVO.getMinutes_num());

        // 수정 화면에서 선택된 참석자 다시 등록
        insertAttendeeList(
                meetingMinutesVO.getMinutes_num(),
                meetingMinutesVO.getAttendeeNums());
    }

    // 회의록 논리 삭제
    @Override
    @Transactional
    public void deleteMeetingMinutes(Long minutes_num) {

        meetingMinutesMapper
                .deleteMeetingMinutes(minutes_num);
    }

    // 참석자 한 명 등록
    @Override
    @Transactional
    public void insertMeetingMinutesAttendee(
            MeetingMinutesAttendeeVO attendeeVO) {

        meetingMinutesMapper
                .insertMeetingMinutesAttendee(attendeeVO);
    }

    // 참석자 목록
    @Override
    public List<MeetingMinutesAttendeeVO>
        selectMeetingMinutesAttendeeList(Long minutes_num) {

        return meetingMinutesMapper
                .selectMeetingMinutesAttendeeList(minutes_num);
    }

    // 회의록에 연결된 참석자 전체 삭제
    @Override
    @Transactional
    public void deleteMeetingMinutesAttendeeByMinutes(
            Long minutes_num) {

        meetingMinutesMapper
                .deleteMeetingMinutesAttendeeByMinutes(
                        minutes_num);
    }

    // 선택한 참석자를 반복 등록하는 내부 메서드
    private void insertAttendeeList(
            Long minutes_num,
            List<Long> attendeeNums) {

        if (attendeeNums == null ||
                attendeeNums.isEmpty()) {
            return;
        }

        for (Long user_num : attendeeNums) {

            MeetingMinutesAttendeeVO attendeeVO =
                    new MeetingMinutesAttendeeVO();

            attendeeVO.setMinutes_num(minutes_num);
            attendeeVO.setUser_num(user_num);

            meetingMinutesMapper
                    .insertMeetingMinutesAttendee(attendeeVO);
        }
    }
}