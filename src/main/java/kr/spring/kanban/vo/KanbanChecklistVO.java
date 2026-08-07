package kr.spring.kanban.vo;

import java.sql.Date;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class KanbanChecklistVO {
	private long checklist_num;
	private long card_num;
	@NotBlank(message = "체크리스트 내용을 입력하세요.")
	private String content;
	private int checked;
	private Date reg_date;
}
