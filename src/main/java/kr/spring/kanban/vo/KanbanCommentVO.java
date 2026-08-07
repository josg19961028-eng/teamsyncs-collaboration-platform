package kr.spring.kanban.vo;

import java.sql.Date;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class KanbanCommentVO {
	private long comment_num;
	private long card_num;
	private long user_num;
	@NotBlank
	private String content;
	private Date reg_date;
	private Date modify_date;
	private int status;
	
	private String user_name;
}
