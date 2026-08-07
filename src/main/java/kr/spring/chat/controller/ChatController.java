package kr.spring.chat.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import kr.spring.chat.service.ChatService;
import kr.spring.chat.vo.ChatChannelVO;
import kr.spring.chat.vo.ChatFileVO;
import kr.spring.users.vo.PrincipalDetails;
import kr.spring.util.FileUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/chat")
public class ChatController {
	
	private final ChatService chatService;
	
	public ChatController(ChatService chatService) {
		this.chatService = chatService;
	}

	@GetMapping("/list")
	public String chat(@AuthenticationPrincipal PrincipalDetails principal, Model model,HttpSession session) {
		
		//사용자의 회원번호 가져오기
		long user_num = principal.getUsersVO().getUser_num();
		long team_num = (Long)session.getAttribute("teamNum");
		
		List<ChatChannelVO> channelList = chatService.selectChannelList(user_num,team_num);
		// Controller에서 처리
		Map<Integer, List<ChatChannelVO>> groupedChannels =
		        channelList.stream()
		            .sorted(Comparator.comparing(ChatChannelVO::getChannel_name))
		            .collect(Collectors.groupingBy(
		                ChatChannelVO::getCategory,   // int → Integer로 자동 박싱
		                TreeMap::new,
		                Collectors.toList()
		            ));
		model.addAttribute("groupedChannels", groupedChannels);
		
		int teamRole = 1;
		model.addAttribute("teamRole",teamRole);
		model.addAttribute("currentMenu", "chat");
		
		return "thviews/chat/list";
	}
	
	@GetMapping("/download/{file_num}")
	public void downloadFile(@PathVariable("file_num") long file_num,
							 HttpServletResponse response,
							 HttpServletRequest request) throws IOException{
		//DB에서 파일 정보 조회
		ChatFileVO fileVO = chatService.selectFileByNum(file_num);
		
		if(fileVO == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		
		String filePath = request.getServletContext().getRealPath("/assets/upload") + "/" + fileVO.getSave_name();
		
		byte fileBytes[] = FileUtil.getBytes(filePath);
		
		if (fileBytes == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		
		//한글 파일명 깨짐 방지
		String encodedName = URLEncoder.encode(fileVO.getOrigin_name(),"UTF-8").replace("+","%20");
		response.setContentType("application/octet-stream");
	    response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedName);
	    response.setContentLength(fileBytes.length);
	    response.getOutputStream().write(fileBytes);
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}