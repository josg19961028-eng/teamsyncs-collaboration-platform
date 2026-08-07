package kr.spring.schedule.vo;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ScheduleAttendeeVO {

    private Long attendee_num;
    private Long schedule_num;
    private Long user_num;

    private Integer status;

    private Date response_date;
}