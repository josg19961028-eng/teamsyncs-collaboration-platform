package kr.spring.storage.service;

import java.io.IOException;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import kr.spring.chat.vo.ChatFileVO;
import kr.spring.storage.vo.FileFolderVO;

public interface StorageService {
	public void insertFolder(FileFolderVO folderVO);
	public Long selectFolderNumByChannelNum(long channel_num);
	
	public List<FileFolderVO> selectFolderTree(long tema_num);
	public List<ChatFileVO> selectFileListByFolder(long folder_num);
	public List<ChatFileVO> selectFileListByTeam(long team_num);
	
	public void deleteFolder(long folder_num, HttpServletRequest request);
	public void deleteFile(long file_num, HttpServletRequest request);
	public void uploadFile(MultipartFile[] files, long folder_num, long team_num, long user_num, HttpServletRequest request) throws IOException;
	public FileFolderVO selectFolderByNum(long folder_num);
}
