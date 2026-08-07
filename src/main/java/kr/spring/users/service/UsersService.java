package kr.spring.users.service;

import java.sql.Date;
import java.util.List;

import kr.spring.users.vo.UsersVO;

public interface UsersService {

	public UsersVO selectByUserNum(long userNum);

	public UsersVO selectByEmail(String email);

	public boolean isEmailDuplicated(String email);

	public boolean isPhoneDuplicated(String phone);

	public boolean isPhoneDuplicatedExceptUser(String phone, long userNum);

	public int updateMyProfile(UsersVO userVO);

	public List<UsersVO> findEmailsByUserInfo(String realName, String phone, Date birth);

	public void insertUser(UsersVO userVO);

	public int updatePassword(String email, String passwd);
}
