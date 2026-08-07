package kr.spring.chat.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import kr.spring.chat.vo.ChatChannelVO;
import kr.spring.chat.vo.ChatFileVO;
import kr.spring.chat.vo.ChatMentionVO;
import kr.spring.chat.vo.ChatMessageVO;
import kr.spring.chat.vo.ChatReadStatusVO;
import kr.spring.team.vo.TeamMemberVO;

public interface ChatService {
	List<ChatChannelVO> selectChannelList(long user_num,long team_num);
	List<ChatMessageVO> selectMessageList(long channel_num);
	public void insertChannel(ChatChannelVO channelVO);
	public void insertMessage(ChatMessageVO messageVO);
	public void sendMessage(ChatMessageVO msgVO, ChatFileVO fileVO);
	public ChatFileVO selectFileByNum(long file_num);
	
	public void deleteChannel(long channel_num);
	public ChatChannelVO selectChannelByNum(long channel_num);
	
	public void insertReadStatus(ChatReadStatusVO readStatusVO);
	public void updateReadStatus(ChatReadStatusVO readStatusVO);
	public ChatReadStatusVO selectReadStatus(ChatReadStatusVO readStatusVO);
	public long selectLastMessageNum(long channel_num);
	public ChatMessageVO selectMessageByNum(long message_num);
	
	List<TeamMemberVO> selectTeamMembers(long team_num);
	void insertMention(ChatMentionVO mentionVO);
	
	public void deleteFolderByChannelName(long channel_num);
}
