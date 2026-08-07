package kr.spring.kanban.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import kr.spring.chat.vo.ChatFileVO;
import kr.spring.kanban.dao.KanbanMapper;
import kr.spring.kanban.vo.KanbanAssignVO;
import kr.spring.kanban.vo.KanbanCardVO;
import kr.spring.kanban.vo.KanbanChecklistVO;
import kr.spring.kanban.vo.KanbanCommentVO;
import kr.spring.storage.dao.StorageMapper;
import kr.spring.team.vo.TeamMemberVO;
import kr.spring.util.FileUtil;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class KanbanServiceImpl implements KanbanService{

	@Autowired
    private KanbanMapper kanbanMapper;
	@Autowired
	private StorageMapper storageMapper;
	
	/*==============================
	 * 칸반카드
	 * ===========================*/
	@Override
	public List<KanbanCardVO> selectKanbanCardList(Map<String, Object> map) {
	    List<KanbanCardVO> cardList = kanbanMapper.selectKanbanCardList(map);

	    for (KanbanCardVO card : cardList) {
	        // 담당자 목록
	        List<KanbanAssignVO> assignList =
	                kanbanMapper.selectKanbanAssignList(card.getCard_num());
	        card.setAssignList(assignList);

	        // 체크리스트 목록
	        List<KanbanChecklistVO> checklistList =
	                kanbanMapper.selectKanbanChecklistList(card.getCard_num());
	        card.setChecklistList(checklistList);

	        int totalCount = checklistList.size();
	        int doneCount = 0;

	        for (KanbanChecklistVO checklist : checklistList) {
	            if (checklist.getChecked() == 2) {
	                doneCount++;
	            }
	        }

	        card.setChecklist_total_count(totalCount);
	        card.setChecklist_done_count(doneCount);

	        int progress = 0;
	        if (totalCount > 0) {
	            progress = (doneCount * 100) / totalCount;
	        }

	        card.setChecklist_progress(progress);
	    }

	    return cardList;
	}

	@Override
	public List<TeamMemberVO> selectTeamMember(long team_num) {
		return kanbanMapper.selectTeamMember(team_num);
	}
	
	@Override
	public void insertKanbanCard(KanbanCardVO card, HttpServletRequest request) throws IOException {
		// 첨부파일 저장 및 chat_file 등록
	    MultipartFile file = card.getUpload_file();

	    if (file != null && !file.isEmpty()) {
	        String saveName = uploadKanbanFile(
	                file,
	                card.getTeam_num(),
	                card.getWriter_num(),
	                request
	        );

	        card.setFilename(saveName);
	    }
		
		//카드 등록
		kanbanMapper.insertKanbanCard(card);
		
		//담당자 등록
		List<Long> assigneeList = card.getAssignee_nums();
		if (assigneeList != null && !assigneeList.isEmpty()) {
            for (Long user_num : assigneeList) {
                KanbanAssignVO assignVO = new KanbanAssignVO();
                assignVO.setCard_num(card.getCard_num());
                assignVO.setTeam_num(card.getTeam_num());
                assignVO.setUser_num(user_num);
                
                kanbanMapper.insertKanbanCardAssign(assignVO);
            }
        }
	}
	
	
	
	@Override
	public KanbanCardVO selectKanbanCard(long card_num, long team_num) {
	    KanbanCardVO card = kanbanMapper.selectKanbanCard(card_num, team_num);

	    if (card != null) {
	    	//카드 담당자 목록 조회
	        List<KanbanAssignVO> assignList = kanbanMapper.selectKanbanAssignList(card_num);
	        card.setAssignList(assignList);

	        //담당자 번호 목록 생성
	        List<Long> assignee_nums = new ArrayList<>();
	        for (KanbanAssignVO assign : assignList) {
	            assignee_nums.add(assign.getUser_num());
	        }
	        card.setAssignee_nums(assignee_nums);

	        //체크리스트 목록 조회
	        List<KanbanChecklistVO> checklistList = kanbanMapper.selectKanbanChecklistList(card_num);
	        card.setChecklistList(checklistList);
	        //전체 체크리스트 개수
	        card.setChecklist_total_count(checklistList.size());
	        //완료 체크리스트 개수
	        int done_count = 0;
	        for(KanbanChecklistVO checklist : checklistList) {
	        	if(checklist.getChecked()==2) {
	        		done_count++;
	        	}
	        }
	        card.setChecklist_done_count(done_count);
	        
	        //체크리스트 진행률
	        int checklist_progress = 0;
	        if(checklistList.size()>0) {
	        	checklist_progress = (done_count * 100) / checklistList.size();
	        }
	        card.setChecklist_progress(checklist_progress);
	        
			// 댓글 목록 조회
			List<KanbanCommentVO> commentList = kanbanMapper.selectListComment(card_num);
			card.setCommentList(commentList);
	        
	    }

	    return card;
	}
	
	@Override
	public List<KanbanAssignVO> selectKanbanAssignList(long card_num) {
		return kanbanMapper.selectKanbanAssignList(card_num);
	}
	
	@Override
	public boolean canChangeStatus(long card_num, long user_num, long team_num) {
	    KanbanCardVO card = kanbanMapper.selectKanbanCard(card_num, team_num);
	    if (card == null) {
	        return false;
	    }
	    if (card.getWriter_num() == user_num) {
	        return true;
	    }
	    KanbanAssignVO assign = new KanbanAssignVO();
	    assign.setCard_num(card_num);
	    assign.setUser_num(user_num);
	    int count = kanbanMapper.countKanbanCardAssignee(assign);
	    return count > 0;
	}

	@Override
	public void updateKanbanStatus(KanbanCardVO card) {
	    kanbanMapper.updateKanbanStatus(card);
	}
	
	@Override
	public void updateKanbanCard(KanbanCardVO card, long user_num, HttpServletRequest request) throws IOException {
		 // 새 첨부파일이 있으면 저장 및 chat_file 등록
	    MultipartFile file = card.getUpload_file();

	    if (file != null && !file.isEmpty()) {
	        String saveName = uploadKanbanFile(
	                file,
	                card.getTeam_num(),
	                user_num,
	                request
	        );

	        card.setFilename(saveName);
	    }
	    //카드수정
	    kanbanMapper.updateKanbanCard(card);
	    
	    //기존담당자삭제
		kanbanMapper.deleteKanbanCardAssign(card.getCard_num());
		
		List<Long> assigneeList = card.getAssignee_nums();
		if(assigneeList != null && !assigneeList.isEmpty()) {
			for(Long assignee_num : assigneeList) {
                KanbanAssignVO assignVO = new KanbanAssignVO();
                assignVO.setCard_num(card.getCard_num());
                assignVO.setTeam_num(card.getTeam_num());
                assignVO.setUser_num(assignee_num);
                
                kanbanMapper.insertKanbanCardAssign(assignVO);
			}
		}
	}
	
	//칸반카드 담당자 삭제
	@Override
	public void deleteKanbanCardAssign(long card_num) {
		kanbanMapper.deleteKanbanCardAssign(card_num);
	}
	//칸반카드 삭제
	@Override
	public void deleteKanbanCard(KanbanCardVO card) {
		kanbanMapper.deleteKanbanCard(card);
	}
	
	//칸반카드 태그 조회
	@Override
	public List<String> selectKanbanTagList(Long team_num) {
	    return kanbanMapper.selectKanbanTagList(team_num);
	}
	
	/*==============================
	 * 체크리스트
	 * ===========================*/
	@Override
	public List<KanbanChecklistVO> selectKanbanChecklistList(long card_num) {
		return kanbanMapper.selectKanbanChecklistList(card_num);
	}
	@Override
	public void insertKanbanChecklist(KanbanChecklistVO checklist) {
		kanbanMapper.insertKanbanChecklist(checklist);
	}
	@Override
	public void updateKanbanChecklistChecked(KanbanChecklistVO checklist) {
		kanbanMapper.updateKanbanChecklistChecked(checklist);
	}
	@Override
	public void deleteKanbanChecklist(KanbanChecklistVO checklist) {
		kanbanMapper.deleteKanbanChecklist(checklist);
	}
	
	
	/*==============================
	 * 댓글
	 * ===========================*/
	@Override
	public List<KanbanCommentVO> selectListComment(long card_num) {
		return kanbanMapper.selectListComment(card_num);
	}
	@Override
	public Integer selectRowCountComment(Map<String, Object> map) {
		return kanbanMapper.selectRowCountComment(map);
	}
	@Override
	public void insertComment(KanbanCommentVO kanbanComment) {
		kanbanMapper.insertComment(kanbanComment);
	}
	@Override
	public KanbanCommentVO selectComment(Long comment_num) {
		return kanbanMapper.selectComment(comment_num);
	}
	@Override
	public void updateComment(KanbanCommentVO kanbanComment) {
		kanbanMapper.updateComment(kanbanComment);
	}
	@Override
	public void deleteComment(Long comment_num) {
		kanbanMapper.deleteComment(comment_num);
	}
	@Override
	public void insertKanbanCardAssign(KanbanAssignVO assign) {
		kanbanMapper.insertKanbanCardAssign(assign);
	}
	@Override
	public void deleteKanbanAssign(KanbanAssignVO assign) {
		kanbanMapper.deleteKanbanAssign(assign);
	}

	@Override
	public String selectKanbanFilename(long card_num, long team_num) {
		return kanbanMapper.selectKanbanFilename(card_num, team_num);
	}
	
	//파일업로드
	@Override
	public String uploadKanbanFile(
	        MultipartFile file,
	        long team_num,
	        long user_num,
	        HttpServletRequest request
	) throws IOException {

	    if (file == null || file.isEmpty()) {
	        return null;
	    }
	    // 칸반 폴더 고정
	    Long folder_num =
	    		kanbanMapper.selectKanbanFolderNum(team_num);
	    
	    if (folder_num == null) {
	    	throw new IllegalStateException(
	    			"해당 팀에 '칸반보드' 폴더가 없습니다."
	    			);
	    }

	    String saveName = FileUtil.createFile(request, file);

	    ChatFileVO fileVO = new ChatFileVO();


	    fileVO.setFolder_num(folder_num);
	    fileVO.setTeam_num(team_num);
	    fileVO.setUploader_num(user_num);
	    fileVO.setOrigin_name(file.getOriginalFilename());
	    fileVO.setSave_name(saveName);
	    fileVO.setFile_path("/assets/upload/" + saveName);
	    fileVO.setFile_size(file.getSize());
	    fileVO.setFile_source("KANBAN");

	    String mimeType = file.getContentType();

	    fileVO.setFile_type(
	            mimeType != null && mimeType.startsWith("image/")
	                    ? "IMAGE"
	                    : "FILE"
	    );

	    storageMapper.insertFile(fileVO);

	    return saveName;
	}





	@Override
	public Long selectKanbanFolderNum(long team_num) {
		return kanbanMapper.selectKanbanFolderNum(team_num);
	}

}

