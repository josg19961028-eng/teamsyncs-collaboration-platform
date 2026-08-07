package kr.spring.kanban.vo;

import java.sql.Date;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kr.spring.team.vo.TeamMemberVO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class KanbanCardVO {
	private long card_num;
	private long team_num;
	private long writer_num;
	@NotBlank(message = "제목을 입력하세요.")
	private String title;
	private String content;
	@Size(max = 5, message = "태그는 5자 이내로 입력하세요.")
	private String tag;
	private String tag_color;
	private int kanban_status;
	@NotNull(message = "마감일을 입력하세요.")
	@FutureOrPresent(message = "마감일은 오늘 이후 날짜만 선택할 수 있습니다.")
	private Date deadline;
	private Date reg_date;
	private Date modify_date;
	private int status;
	
	private MultipartFile upload_file;
	private String filename;
	
	private KanbanAssignVO kanbanAssignVO;
	private List<Long> assignee_nums;

	//작성자 이름
	private String writer_name;
	//담당자 목록 표시용
	private List<KanbanAssignVO> assignList;
	//상세보기 시 수정삭제 버튼 유무
	private boolean editable;
	
	//상세보기에서 체크리스트 띄우기 위해
	private List<KanbanChecklistVO> checklistList;
	
	//체크리스트 갯수 및 진행률 확인용
	private int checklist_done_count;
	private int checklist_total_count;
	private int checklist_progress;
	
	private List<KanbanCommentVO> commentList;
	
	//담당자 지정용
	private List<TeamMemberVO> teamMemberList;
	
	//파일보관함 연동
	private Long file_num;
	private String origin_name;
	
}
