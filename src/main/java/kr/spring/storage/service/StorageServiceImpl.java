package kr.spring.storage.service;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import kr.spring.chat.dao.ChatMapper;
import kr.spring.chat.vo.ChatFileVO;
import kr.spring.storage.dao.StorageMapper;
import kr.spring.storage.vo.FileFolderVO;
import kr.spring.util.FileUtil;

@Service
@Transactional
public class StorageServiceImpl implements StorageService {
	
	private final StorageMapper storageMapper;
	
	public StorageServiceImpl(StorageMapper storageMapper) {
		this.storageMapper = storageMapper;
	}

	@Override
	public void insertFolder(FileFolderVO folderVO) {
		storageMapper.insertFolder(folderVO);
	}
	
	@Override
	public Long selectFolderNumByChannelNum(long channel_num) {
		return storageMapper.selectFolderNumByChannelNum(channel_num);
	}

	@Override
	public List<FileFolderVO> selectFolderTree(long tema_num) {
		return storageMapper.selectFolderTree(tema_num);
	}

	@Override
	public List<ChatFileVO> selectFileListByFolder(long folder_num) {
		return storageMapper.selectFileListByFolder(folder_num);
	}

	@Override
	public List<ChatFileVO> selectFileListByTeam(long team_num) {
		return storageMapper.selectFileListByTeam(team_num);
	}

	@Override
	public void deleteFolder(long folder_num, HttpServletRequest request) {
		//하위 폴더 재귀 삭제
		List<FileFolderVO> children = storageMapper.selectChildFolders(folder_num);
		for (FileFolderVO child : children) {
			deleteFolder(child.getFolder_num(),request);
		}
		
		//폴더 안 파일 서버에서 삭제
		List<ChatFileVO> files = storageMapper.selectFileListByFolder(folder_num);
		for(ChatFileVO file: files) {
			FileUtil.removeFile(request, file.getSave_name());
		}
		
		//폴더 안 파일 DB삭제
		storageMapper.deleteFilesByFolder(folder_num);
		
		//폴더 DB 삭제
		storageMapper.deleteFolder(folder_num);
	}

	@Override
	public void deleteFile(long file_num, HttpServletRequest request) {
		ChatFileVO file = storageMapper.selectFileByNum(file_num);
		if(file != null) {
			FileUtil.removeFile(request, file.getSave_name());
			storageMapper.deleteFile(file_num);
		}
	}

	@Override
	public void uploadFile(MultipartFile[] files, long folder_num, long team_num, long user_num,
			HttpServletRequest request) throws IOException {
		for(MultipartFile file : files) {
			if (file.isEmpty()) continue;
			
			String saveName = FileUtil.createFile(request, file);
			
			ChatFileVO fileVO = new ChatFileVO();
			fileVO.setFolder_num(folder_num);
	        fileVO.setTeam_num(team_num);
	        fileVO.setUploader_num(user_num);
	        fileVO.setOrigin_name(file.getOriginalFilename());
	        fileVO.setSave_name(saveName);
	        fileVO.setFile_path("/assets/upload/" + saveName);
	        fileVO.setFile_size(file.getSize());
	        fileVO.setFile_source("DRIVE");
	        
	        String mimeType = file.getContentType();
	        fileVO.setFile_type(mimeType != null && mimeType.startsWith("image/") ? "IMAGE" : "FILE");

	        storageMapper.insertFile(fileVO);
		}
	}

	@Override
	public FileFolderVO selectFolderByNum(long folder_num) {
		return storageMapper.selectFolderByNum(folder_num);
	}

}
