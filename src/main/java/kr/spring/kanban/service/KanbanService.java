package kr.spring.kanban.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import kr.spring.kanban.vo.KanbanAssignVO;
import kr.spring.kanban.vo.KanbanCardVO;
import kr.spring.kanban.vo.KanbanChecklistVO;
import kr.spring.kanban.vo.KanbanCommentVO;
import kr.spring.team.vo.TeamMemberVO;

public interface KanbanService {
	//부모글
	public List<KanbanCardVO> selectKanbanCardList(Map<String, Object> map);
	public List<TeamMemberVO> selectTeamMember(long team_num);
	public void insertKanbanCard(KanbanCardVO card, HttpServletRequest request) throws IOException;
	public KanbanCardVO selectKanbanCard(@Param("card_num") long card_num, @Param("team_num") long team_num);
	public List<KanbanAssignVO> selectKanbanAssignList(long card_num);
	
	//칸반카드 변경가능여부 확인
	public boolean canChangeStatus(long card_num,long user_num, long team_num);
	public void updateKanbanStatus(KanbanCardVO card);
	//칸반카드 내용 수정 및 삭제
	public void updateKanbanCard(KanbanCardVO card, long user_num, HttpServletRequest request) throws IOException;
	//칸반카드 내용 수정을 위한 기존 담당자 삭제
	public void deleteKanbanCardAssign(long card_num);
	//칸반카드 삭제
	public void deleteKanbanCard(KanbanCardVO card);
	//칸반카드 태그 조회
	public List<String> selectKanbanTagList(Long team_num);
	
	// 체크리스트
	// 조회
	public List<KanbanChecklistVO> selectKanbanChecklistList(long card_num);
	// 추가
	public void insertKanbanChecklist(KanbanChecklistVO checklist);
	// 상태변경
	public void updateKanbanChecklistChecked(KanbanChecklistVO checklist);
	// 삭제
	public void deleteKanbanChecklist(KanbanChecklistVO checklist);

	//댓글
	public List<KanbanCommentVO> selectListComment(long card_num);
	public Integer selectRowCountComment(Map<String,Object> map);
	public void insertComment(KanbanCommentVO kanbanComment);
	public KanbanCommentVO selectComment(Long comment_num);
	public void updateComment(KanbanCommentVO kanbanComment);
	public void deleteComment(Long comment_num);
	
	//담당자 추가
	public void insertKanbanCardAssign(KanbanAssignVO assign);
	//담당자 삭제
	public void deleteKanbanAssign(KanbanAssignVO assign);
	
	
	//파일명조회
	public String selectKanbanFilename(@Param("card_num") long card_num,
								   @Param("team_num") long team_num);
	//파일업로드
	String uploadKanbanFile(MultipartFile file, long team_num, long user_num,  HttpServletRequest request) throws IOException;
	
	//폴더번호조회
	public Long selectKanbanFolderNum(@Param("team_num") long team_num);
}
