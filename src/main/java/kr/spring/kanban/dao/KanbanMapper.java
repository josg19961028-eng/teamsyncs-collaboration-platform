package kr.spring.kanban.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import kr.spring.kanban.vo.KanbanAssignVO;
import kr.spring.kanban.vo.KanbanCardVO;
import kr.spring.kanban.vo.KanbanChecklistVO;
import kr.spring.kanban.vo.KanbanCommentVO;
import kr.spring.team.vo.TeamMemberVO;

@Mapper
public interface KanbanMapper {
	public KanbanCardVO selectKanbanCard(@Param("card_num") long card_num, @Param("team_num") long team_num);
	//칸반카드 조회
	public List<KanbanCardVO> selectKanbanCardList(Map<String, Object> map);
	//팀원리스트 조회
	public List<TeamMemberVO> selectTeamMember(Long team_num); 
	//카드추가
	public void insertKanbanCard(KanbanCardVO card);
	//카드별 담당자 다중 매핑 및 담당자 추가
	public void insertKanbanCardAssign(KanbanAssignVO assign);
	//카드 담당자 삭제
	@Delete("DELETE FROM kanban_assign WHERE card_num=#{card_num} AND user_num=#{user_num}")
	public void deleteKanbanAssign(KanbanAssignVO assign);
	//카드 담당자 조회
	public List<KanbanAssignVO> selectKanbanAssignList(Long card_num);
	//칸반카드 상태변경
	public void updateKanbanStatus(KanbanCardVO card);
	//칸반카드 담당자인지 확인용
	@Select("SELECT COUNT(*) FROM kanban_assign WHERE card_num=#{card_num} AND user_num=#{user_num}")
	public int countKanbanCardAssignee(KanbanAssignVO assign);
	//칸반카드 내용 수정 및 삭제
	public void updateKanbanCard(KanbanCardVO card);
	//칸반카드 내용 수정을 위한 기존 담당자 삭제
	@Delete("DELETE FROM kanban_assign WHERE card_num=#{card_num}")
	public void deleteKanbanCardAssign(Long card_num);
	//칸반카드 삭제
	@Update("UPDATE kanban_card SET status=2, modify_date=SYSDATE WHERE card_num= #{card_num} AND team_num=#{team_num}")
	public void deleteKanbanCard(KanbanCardVO card);
	//칸반카드 태그 조회
	public List<String> selectKanbanTagList(Long team_num);
	
	/*==============================
	 * 체크리스트
	 * ===========================*/
	//조회
	@Select("SELECT * FROM kanban_checklist WHERE card_num=#{card_num} ORDER BY checklist_num ASC")
	public List<KanbanChecklistVO> selectKanbanChecklistList(Long card_num);
	//추가
	@Insert("INSERT INTO kanban_checklist(checklist_num, card_num, content) VALUES(SEQ_KANBAN_CHECKLIST.nextval, #{card_num}, #{content})")
	public void insertKanbanChecklist(KanbanChecklistVO checklist);
	//상태변경
	@Update("UPDATE kanban_checklist SET checked = #{checked} WHERE checklist_num=#{checklist_num} AND card_num=#{card_num}")
	public void updateKanbanChecklistChecked(KanbanChecklistVO checklist);
	//삭제
	@Delete("DELETE FROM kanban_checklist WHERE checklist_num = #{checklist_num} AND card_num = #{card_num}")
	public void deleteKanbanChecklist(KanbanChecklistVO checklist);
	
	
	/*==============================
	 * 댓글
	 * ===========================*/
	public List<KanbanCommentVO> selectListComment(Long card_num);
	@Select("SELECT COUNT(*) FROM kanban_comment WHERE card_num=#{card_num} AND status=1")
	public Integer selectRowCountComment(Map<String,Object> map);
	@Insert("INSERT INTO kanban_comment(comment_num, card_num, user_num, content)"
			+ "VALUES(seq_kanban_comment.nextval, #{card_num}, #{user_num}, #{content})")
	public void insertComment(KanbanCommentVO kanbanComment);
	@Select("SELECT * FROM kanban_comment WHERE comment_num=#{comment_num} AND status=1")
	public KanbanCommentVO selectComment(Long comment_num);
	@Update("UPDATE kanban_comment SET content=#{content}, modify_date=SYSDATE WHERE comment_num=#{comment_num}")
	public void updateComment(KanbanCommentVO kanbanComment);
	@Update("UPDATE kanban_comment SET status=2, modify_date=SYSDATE WHERE comment_num=#{comment_num}")
	public void deleteComment(Long comment_num);
	/*==============================
	 * 파일업로드
	 * ===========================*/
	//파일명 조회
	@Select("SELECT filename FROM kanban_card WHERE card_num = #{card_num} AND team_num = #{team_num} AND status != 2")
	public String selectKanbanFilename(@Param("card_num") long card_num,
		        					   @Param("team_num") long team_num);
	
	//보관함에 칸반보드 폴더번호 조회
	@Select("SELECT folder_num FROM file_folder  WHERE team_num = #{team_num} AND folder_name = '칸반보드'")
	public Long selectKanbanFolderNum(@Param("team_num") long team_num);
	
	
	
	
}
