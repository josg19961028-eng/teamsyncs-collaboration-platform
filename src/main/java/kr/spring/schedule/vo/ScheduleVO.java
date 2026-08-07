package kr.spring.schedule.vo;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ScheduleVO {

    private Long schedule_num;
    private Long team_num;
    private Long user_num;

    private String title;
    private String content;
    private String category;
    private String color;

    private String start_date;
    private String end_date;

    private Integer all_day;
    private Integer status;

    private Date reg_date;
    private Date modify_date;
}