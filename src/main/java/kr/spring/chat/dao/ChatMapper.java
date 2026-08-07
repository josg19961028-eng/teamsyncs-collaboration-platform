package kr.spring.chat.dao;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import kr.spring.chat.vo.ChatChannelVO;
import kr.spring.chat.vo.ChatFileVO;
import kr.spring.chat.vo.ChatMentionVO;
import kr.spring.chat.vo.ChatMessageVO;
import kr.spring.chat.vo.ChatReadStatusVO;
import kr.spring.team.vo.TeamMemberVO;

@Mapper
public interface ChatMapper {
	//채팅방생성
	public void insertChannel(ChatChannelVO channelVO);
	//내 채팅방 목록 조회
	List<ChatChannelVO> selectChannelList(@Param("user_num") long user_num,@Param("team_num") long team_num);
	// 특정 채널의 메시지 내역 가져오기
	List<ChatMessageVO> selectMessageList(long channel_num);
		
	//메시지 저장
	public void insertMessage(ChatMessageVO messageVO);
	//파일 저장
	public void insertChatFile(ChatFileVO chatFileVO);
	
	//파일 읽어오기
	@Select("select file_num,message_num,channel_num,uploader_num,origin_name,save_name,file_path,file_size,file_type from chat_file where file_num=#{file_num}")
	public ChatFileVO selectFileByNum(long file_num);
	
	//채팅방 번호 가져오기
	@Select("select channel_num,channel_name,is_default from chat_channel where channel_num=#{channel_num}")
	public ChatChannelVO selectChannelByNum(long channel_num);
	
	//채널 삭제
	@Delete("delete from chat_channel where channel_num = #{channel_num} and is_default = 'N'")
	public void deleteChannel(long channel_num);
	//멘션(언급) 삭제
	@Delete("delete from chat_mention where message_num in(select message_num from chat_message where channel_num = #{channel_num})")
	public void deleteMentionByChannel(long channel_num);
	//읽음 처리 삭제
	@Delete("delete from chat_read_status where channel_num = #{channel_num}")
	public void deleteReadStatusByChannel(long channel_num);
	//채팅방 파일 삭제
	@Delete("delete from chat_file where channel_num = #{channel_num}")
	public void deleteFileByChannel(long channel_num);
	//채팅방 메시지 삭제
	@Delete("delete from chat_message where channel_num=#{channel_num}")
	public void deleteMessageByChannel(long channel_num);
	
	//처음 채널 입장 시
	@Insert("insert into chat_read_status(channel_num,user_num,last_read_message_num) values(#{channel_num},#{user_num},0)")
	public void insertReadStatus(ChatReadStatusVO readStatusVO);
	//읽음 상태 update
	@Update("update chat_read_status set last_read_message_num = #{last_read_message_num} where channel_num = #{channel_num} and user_num = #{user_num}")
	public void updateReadStatus(ChatReadStatusVO readStatusVO);
	//읽음 상태 존재 여부 확인
	@Select("select channel_num,user_num,last_read_message_num from chat_read_status where channel_num =#{channel_num} and user_num=#{user_num}")
	public ChatReadStatusVO selectReadStatus(ChatReadStatusVO readStatusVO);
	//채널의 마지막 메시지 조회
	@Select("select nvl(max(message_num),0) from chat_message where channel_num=#{channel_num}")
	public long selectLastMessageNum(long channel_num);
	
	//답장 후 부모글 가져오기
	@Select("select m.message_num,m.content,u.user_name as userName from chat_message m join users u on m.user_num = u.user_num where m.message_num = #{message_num}")
	public ChatMessageVO selectMessageByNum(long message_num);
	
	//언급하기
	@Insert("insert into chat_mention(mention_num,message_num,mentioned_member_num) values (seq_chat_mention.nextval,#{message_num},#{mentioned_member_num})")
	public void insertMention(ChatMentionVO chatMentionVO);
	
	//팀 멤버 가져오기
	@Select("select tm.user_num, u.user_name from team_member tm join users u on tm.user_num = u.user_num where tm.team_num = #{team_num} and tm.join_status = 1")
	public List<TeamMemberVO> selectTeamMembers(long team_num);
	
	//채널에 연결된 보관함 삭제
	@Delete("delete from file_folder where folder_name = (select channel_name from chat_channel where channel_num = #{channel_num}) and team_num = (select team_num from chat_channel where channel_num = #{channel_num})")
	public void deleteFolderByChannelName(long channel_num);
	
	void deleteChildFoldersByChannelName(long channel_num);
	void deleteFilesByChildFolders(long channel_num);

}
