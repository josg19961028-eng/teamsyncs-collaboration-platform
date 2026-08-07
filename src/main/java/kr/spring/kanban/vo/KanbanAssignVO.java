package kr.spring.kanban.vo;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class KanbanAssignVO {
	private long assignee_num;
	private long team_num;
	private long card_num;
	private long user_num;
	
	private String user_name;
}
