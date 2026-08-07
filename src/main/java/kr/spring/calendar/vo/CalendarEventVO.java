package kr.spring.calendar.vo;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CalendarEventVO {

	private String id;
	private String title;
	private String start;
	private String end;
	private String color;
	private Boolean allDay;

	private String type;
	private Long schedule_num;
	private Long card_num;
	private Long team_num;
	private String category;
	private String content;
	private Integer kanban_status;

    // 추가 정보
    // 예: type=SCHEDULE, schedule_num=1, category=회의
    private Map<String, Object> extendedProps;
}
