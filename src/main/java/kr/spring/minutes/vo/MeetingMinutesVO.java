package kr.spring.minutes.vo;

import java.util.Date;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class MeetingMinutesVO {

    private Long minutes_num;
    private Long team_num;
    private Long schedule_num;
    private Long writer_num;

    private String title;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private Date meeting_date;

    private String content;
    private String decision_content;
    private String action_item;

    private String pdf_path;

    private Integer status;
    private Date reg_date;
    private Date modify_date;

    // DB 컬럼 아님 - 작성 폼에서 선택한 참석자 user_num 목록
    private List<Long> attendeeNums;

    // DB 컬럼 아님 - 조인 조회용
    private String writer_name;
    private String schedule_title;
    private String team_name;

    // DB 컬럼 아님 - 상세 조회용 참석자 정보
    private List<MeetingMinutesAttendeeVO> attendeeInfoList;
}