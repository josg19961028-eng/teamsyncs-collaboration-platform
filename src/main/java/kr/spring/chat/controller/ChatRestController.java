package kr.spring.chat.controller;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import kr.spring.chat.service.ChatService;
import kr.spring.chat.vo.ChatChannelVO;
import kr.spring.chat.vo.ChatFileVO;
import kr.spring.chat.vo.ChatMentionVO;
import kr.spring.chat.vo.ChatMessageVO;
import kr.spring.chat.vo.ChatReadStatusVO;
import kr.spring.storage.service.StorageService;
import kr.spring.storage.vo.FileFolderVO;
import kr.spring.team.vo.TeamMemberVO;
import kr.spring.users.vo.PrincipalDetails;
import kr.spring.util.FileUtil;

@RestController
@RequestMapping("/chat")
public class ChatRestController {
	
	private final ChatService chatService;
	private final SimpMessagingTemplate messagingTmeplate;
	private final StorageService storageService;
	
	public ChatRestController(ChatService chatService,SimpMessagingTemplate messagingTmeplate,StorageService storageService) {
		this.chatService = chatService;
		this.messagingTmeplate = messagingTmeplate;
		this.storageService = storageService;
		
	}
	
	@GetMapping("/messages/{channel_num}")
	public ResponseEntity<List<ChatMessageVO>> getMessage(@PathVariable("channel_num") long channel_num){
		List<ChatMessageVO> list = chatService.selectMessageList(channel_num);
		
		return ResponseEntity.ok(list);
	}
	
	@PostMapping("/channel")
	public ResponseEntity<ChatChannelVO> createChannelAsync(ChatChannelVO channelVO,@AuthenticationPrincipal PrincipalDetails principal,HttpSession session){
		long user_num = principal.getUsersVO().getUser_num();
		channelVO.setCreate_by(user_num);
		
		long team_num = (Long)session.getAttribute("teamNum");
		channelVO.setTeam_num(team_num);
		
		chatService.insertChannel(channelVO);
		
		//채널 생성 시 폴더 자동 생성
		FileFolderVO folderVO = new FileFolderVO();
		folderVO.setTeam_num(team_num);
		folderVO.setFolder_name(channelVO.getChannel_name());
		folderVO.setParent_folder_num(null);
		folderVO.setIs_chat_folder("Y");
		folderVO.setChannel_num(channelVO.getChannel_num());
		storageService.insertFolder(folderVO);
		
		return ResponseEntity.ok(channelVO);
	}
	
	@PostMapping("/send")
	public ResponseEntity<?> sendMessage(@RequestParam(value="content",required=false) String content,
										 @RequestParam(value="file",required=false) MultipartFile file,
										 @RequestParam("channel_num") long channel_num,
										 @AuthenticationPrincipal PrincipalDetails principal,
										 @RequestParam(value="parent_message", required=false) Long parent_message,
										 HttpSession session,
										 HttpServletRequest request) throws IOException{
		long user_num = principal.getUsersVO().getUser_num();
		String userName = principal.getUsersVO().getUser_name();
		long team_num = (Long)session.getAttribute("teamNum");
		//메시지VO 셋팅
		ChatMessageVO msgVO = new ChatMessageVO();
		msgVO.setChannel_num(channel_num);
		msgVO.setUser_num(user_num);
		msgVO.setContent(content);
		msgVO.setUserName(userName);
		
		//파일VO 세팅
		ChatFileVO fileVO = null;
		if(file != null && !file.isEmpty()) {
			String saveName = FileUtil.createFile(request, file);
			
			Long folderNum = storageService.selectFolderNumByChannelNum(channel_num);
			
			fileVO = new ChatFileVO();
			fileVO.setTeam_num(team_num);
			fileVO.setChannel_num(msgVO.getChannel_num());
			fileVO.setUploader_num(user_num);
			fileVO.setOrigin_name(file.getOriginalFilename());
			fileVO.setSave_name(saveName);
			fileVO.setFile_path("/assets/upload/"+saveName);
			fileVO.setFile_size(file.getSize());
			fileVO.setFile_source("CHAT");
			
			if(folderNum != null) {
				fileVO.setFolder_num(folderNum);
			}
			
			String mimeType = file.getContentType();
			fileVO.setFile_type(mimeType != null && mimeType.startsWith("image/") ? "IMAGE" : "FILE");
			
			msgVO.setOrigin_name(file.getOriginalFilename());
		    msgVO.setFile_size(file.getSize());
		    msgVO.setFile_type(fileVO.getFile_type());
		}
		
		if (parent_message != null && parent_message > 0) {
		    ChatMessageVO parentMsg = chatService.selectMessageByNum(parent_message);
		    msgVO.setParent_message(parent_message);
		    if (parentMsg != null) {
		        msgVO.setParentContent(parentMsg.getContent());
		        msgVO.setParentUserName(parentMsg.getUserName());
		    }
		}
		
		chatService.sendMessage(msgVO, fileVO);
		if (fileVO != null) {
		    msgVO.setFile_num(fileVO.getFile_num());
		}
		
		if(content != null && content.contains("@")) {
			List<TeamMemberVO> members = chatService.selectTeamMembers(team_num);
			for(TeamMemberVO member : members) {
				if(content.contains("@" + member.getUser_name())) {
					ChatMentionVO mentionVO = new ChatMentionVO();
					mentionVO.setMessage_num(msgVO.getMessage_num());
					mentionVO.setMentioned_member_num(member.getUser_num());
					chatService.insertMention(mentionVO);
				}
			}
		}
		
		messagingTmeplate.convertAndSend("/sub/chat/room/"+msgVO.getChannel_num(),msgVO);
		
		return ResponseEntity.ok().build();
	}
	
	//채팅방 삭제
	@DeleteMapping("/channel/{channel_num}")
	public ResponseEntity<?> deleteChannel(@PathVariable("channel_num") long channel_num,
										   @AuthenticationPrincipal PrincipalDetails principal,
										   HttpSession session){
		int teamRole = 1;
		if(teamRole != 1 && teamRole != 2) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("권한이 없습니다.");
		}
		
		//기본 채널 삭제 방지
		//is_default 확인 로직 변경
		ChatChannelVO channel = chatService.selectChannelByNum(channel_num);
		if(channel == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("채널을 찾을 수 없습니다.");
		}
		
		if("Y".equals(channel.getIs_default())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("기본 채널은 삭제할 수 없습니다.");
		}
		
		chatService.deleteChannel(channel_num);
		
		return ResponseEntity.ok().build();
	}
	
	@PostMapping("/read/{channel_num}")
	public ResponseEntity<?> updateReadStatus(@PathVariable("channel_num") long channel_num,@AuthenticationPrincipal PrincipalDetails principal){
		long user_num = principal.getUsersVO().getUser_num();
		
		//최신 메시지 번호 조회
		long lastMessageNum = chatService.selectLastMessageNum(channel_num);
		
		ChatReadStatusVO readStatusVO = new ChatReadStatusVO();
		readStatusVO.setChannel_num(channel_num);
		readStatusVO.setUser_num(user_num);
		readStatusVO.setLast_read_message_num(lastMessageNum);
		
		ChatReadStatusVO existing = chatService.selectReadStatus(readStatusVO);
		if(existing == null) {
			chatService.insertReadStatus(readStatusVO);
		}else {
			chatService.updateReadStatus(readStatusVO);
		}
		
		return ResponseEntity.ok().build();
	}
	
	@GetMapping("/members/{team_num}")
	public ResponseEntity<List<TeamMemberVO>> getTeamMembers(@PathVariable("team_num") long tema_num){
		List<TeamMemberVO> members = chatService.selectTeamMembers(tema_num);
		return ResponseEntity.ok(members);
	}

	
}
