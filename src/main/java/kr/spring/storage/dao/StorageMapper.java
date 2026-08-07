package kr.spring.storage.dao;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import kr.spring.chat.vo.ChatFileVO;
import kr.spring.storage.vo.FileFolderVO;

@Mapper
public interface StorageMapper {
	//폴더 insert (채널 생성 시 자동 생성 + 직접 생성)
	@Insert("insert into file_folder(folder_num,team_num,folder_name,parent_folder_num,create_date,is_chat_folder,channel_num) values(seq_file_folder.nextval,#{team_num},#{folder_name},#{parent_folder_num,jdbcType=NUMERIC},sysdate,NVL(#{is_chat_folder,jdbcType=CHAR},'N'),#{channel_num,jdbcType=NUMERIC})")
	public void insertFolder(FileFolderVO folderVO);
	
	//채널 번호로 폴더 번호 조회
	@Select("select folder_num from file_folder where channel_num = #{channel_num} and is_chat_folder='Y'")
	public Long selectFolderNumByChannelNum(long channel_num);
	
	//팀의 폴더 트리 전체 조회
	public List<FileFolderVO> selectFolderTree(long team_num);
	//폴더 안 파일 목록 조회
	public List<ChatFileVO> selectFileListByFolder(long team_num);
	//팀 전체 파일 목록 조회(전체보기)
	public List<ChatFileVO> selectFileListByTeam(long team_num);
	
	//폴더 삭제
	@Delete("delete from file_folder where folder_num = #{folder_num}")
	public void deleteFolder(long folder_num);
	
	//폴더 안 파일 삭제(폴더 삭제 시 먼저 실행)
	@Delete("delete from chat_file where folder_num = #{folder_num}")
	public void deleteFilesByFolder(long folder_num);
	
	//파일 단건 삭제
	@Delete("delete from chat_file where file_num = #{file_num}")
	public void deleteFile(long file_num);
	
	//파일 경로 조회(서버 파일 삭제용)
	@Select("select file_num,save_name,file_path from chat_file where file_num = #{file_num}")
	public ChatFileVO selectFileByNum(long file_num);
	
	//하위 폴더 조회 (폴더 삭제 시 하위 폴더도 삭제)
	@Select("select folder_num from file_folder where parent_folder_num = #{folder_num}")
	public List<FileFolderVO> selectChildFolders(long folder_num);
	
	//파일 추가
	public void insertFile(ChatFileVO chatFileVO);
	
	@Select("select folder_num,team_num,folder_name,parent_folder_num,is_chat_folder from file_folder where folder_num = #{folder_num}")
	public FileFolderVO selectFolderByNum(long folder_num);
	
	
}