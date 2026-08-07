package kr.spring.storage.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import kr.spring.chat.vo.ChatFileVO;
import kr.spring.storage.service.StorageService;
import kr.spring.storage.vo.FileFolderVO;
import kr.spring.users.vo.PrincipalDetails;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/storage")
public class StorageController {

	private final StorageService storageService;

	public StorageController(StorageService storageService) {
		this.storageService = storageService;
	}

	@GetMapping("/list")
	public String list(@AuthenticationPrincipal PrincipalDetails principal, Model model,HttpSession session) {
		long team_num = (Long)session.getAttribute("teamNum");

		//폴더 트리 조회
		List<FileFolderVO> folderTree = storageService.selectFolderTree(team_num);

		//팀 전체 파일 목록 조회(초기 화면)
		List<ChatFileVO> fileList = storageService.selectFileListByTeam(team_num);

		model.addAttribute("folderTree",folderTree);
		model.addAttribute("fileList",fileList);
		model.addAttribute("currentMenu","storage");

		return "thviews/storage/list";
	}

	// 폴더 클릭 시 해당 폴더 파일 목록 조회
	@GetMapping("/files/{folder_num}")
	@ResponseBody
	public ResponseEntity<List<ChatFileVO>> getFilesByFolder(@PathVariable("folder_num") long folder_num){
		List<ChatFileVO> fileList = storageService.selectFileListByFolder(folder_num);
		return ResponseEntity.ok(fileList);
	}



	// 폴더 생성
	@PostMapping("/folder")
	@ResponseBody
	public ResponseEntity<?> createFolder(FileFolderVO folderVO) {
		storageService.insertFolder(folderVO);
		return ResponseEntity.ok(folderVO);
	}

	// 폴더 삭제
	@DeleteMapping("/folder/{folder_num}")
	@ResponseBody
	public ResponseEntity<?> deleteFolder(@PathVariable("folder_num") long folder_num,
			HttpServletRequest request) {
		 // 채팅방 연동 폴더면 삭제 불가
	    FileFolderVO folder = storageService.selectFolderByNum(folder_num);
	    if (folder == null) {
	        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("폴더를 찾을 수 없습니다.");
	    }
	    if ("Y".equals(folder.getIs_chat_folder())) {
	        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
	                             .body("채팅방 연동 폴더는 삭제할 수 없습니다.\n채팅방을 삭제하면 함께 삭제됩니다.");
	    }
		storageService.deleteFolder(folder_num, request);
		return ResponseEntity.ok().build();
	}

	// 파일 삭제
	@DeleteMapping("/file/{file_num}")
	@ResponseBody
	public ResponseEntity<?> deleteFile(@PathVariable("file_num") long file_num,
			HttpServletRequest request) {
		storageService.deleteFile(file_num, request);
		return ResponseEntity.ok().build();
	}

	// 파일 업로드
	@PostMapping("/upload")
	@ResponseBody
	public ResponseEntity<?> uploadFile(
			@RequestParam("files") MultipartFile[] files,
			@RequestParam("folder_num") long folder_num,
			@RequestParam("team_num") long team_num,
			@AuthenticationPrincipal PrincipalDetails principal,
			HttpServletRequest request) throws IOException {

		long user_num = principal.getUsersVO().getUser_num();
		storageService.uploadFile(files, folder_num, team_num, user_num, request);
		return ResponseEntity.ok().build();
	}

	// 전체 파일 조회
	@GetMapping("/files/all")
	@ResponseBody
	public ResponseEntity<List<ChatFileVO>> getAllFiles(HttpSession session) {
		long team_num = (Long) session.getAttribute("teamNum");
		List<ChatFileVO> fileList = storageService.selectFileListByTeam(team_num);
		return ResponseEntity.ok(fileList);
	}









}