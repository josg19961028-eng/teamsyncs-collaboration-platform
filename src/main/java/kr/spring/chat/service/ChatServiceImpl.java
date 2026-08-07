package kr.spring.chat.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import kr.spring.chat.dao.ChatMapper;
import kr.spring.chat.vo.ChatChannelVO;
import kr.spring.chat.vo.ChatFileVO;
import kr.spring.chat.vo.ChatMentionVO;
import kr.spring.chat.vo.ChatMessageVO;
import kr.spring.chat.vo.ChatReadStatusVO;
import kr.spring.team.vo.TeamMemberVO;

@Service
@Transactional
public class ChatServiceImpl implements ChatService {
	
	private final ChatMapper chatMapper;
	
	public ChatServiceImpl(ChatMapper chatMapper) {
		this.chatMapper = chatMapper;
	}

	@Override
	public List<ChatChannelVO> selectChannelList(long user_num,long team_num) {
		return chatMapper.selectChannelList(user_num,team_num);
	}

	@Override
	public List<ChatMessageVO> selectMessageList(long channel_num) {
		return chatMapper.selectMessageList(channel_num);
	}

	@Override
	public void insertChannel(ChatChannelVO channelVO) {
		chatMapper.insertChannel(channelVO);
	}

	@Override
	public void insertMessage(ChatMessageVO messageVO) {
		chatMapper.insertMessage(messageVO);
	}

	@Override
	public void sendMessage(ChatMessageVO msgVO, ChatFileVO fileVO) {
		//메시지 저장
		chatMapper.insertMessage(msgVO);
		
		//파일이 있을 때만 저장
		if (fileVO != null) {
			fileVO.setMessage_num(msgVO.getMessage_num());
			chatMapper.insertChatFile(fileVO);
		}
	}

	@Override
	public ChatFileVO selectFileByNum(long file_num) {
		return chatMapper.selectFileByNum(file_num);
	}

	@Override
	public void deleteChannel(long channel_num) {
		chatMapper.deleteMentionByChannel(channel_num);
		chatMapper.deleteReadStatusByChannel(channel_num);
		chatMapper.deleteFileByChannel(channel_num);
		chatMapper.deleteFilesByChildFolders(channel_num);
		chatMapper.deleteMessageByChannel(channel_num);
		chatMapper.deleteChildFoldersByChannelName(channel_num);
		chatMapper.deleteChannel(channel_num);
	}

	@Override
	public ChatChannelVO selectChannelByNum(long channel_num) {
		return chatMapper.selectChannelByNum(channel_num);
	}

	@Override
	public void insertReadStatus(ChatReadStatusVO readStatusVO) {
		chatMapper.insertReadStatus(readStatusVO);
	}

	@Override
	public void updateReadStatus(ChatReadStatusVO readStatusVO) {
		chatMapper.updateReadStatus(readStatusVO);
	}

	@Override
	public ChatReadStatusVO selectReadStatus(ChatReadStatusVO readStatusVO) {
		return chatMapper.selectReadStatus(readStatusVO);
	}

	@Override
	public long selectLastMessageNum(long channel_num) {
		return chatMapper.selectLastMessageNum(channel_num);
	}

	@Override
	public ChatMessageVO selectMessageByNum(long message_num) {
		return chatMapper.selectMessageByNum(message_num);
	}

	@Override
	public List<TeamMemberVO> selectTeamMembers(long team_num) {
		return chatMapper.selectTeamMembers(team_num);
	}

	@Override
	public void insertMention(ChatMentionVO mentionVO) {
		chatMapper.insertMention(mentionVO);
	}

	@Override
	public void deleteFolderByChannelName(long channel_num) {
		chatMapper.deleteFolderByChannelName(channel_num);
	}
	
}
