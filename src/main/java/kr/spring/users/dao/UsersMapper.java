package kr.spring.users.dao;

import java.sql.Date;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import kr.spring.users.vo.UsersVO;

@Mapper
public interface UsersMapper {

	public UsersVO selectByUserNum(long userNum);

	public UsersVO selectByEmail(String email);

	public int countByEmail(String email);
	
	//이미 사용중인 전화번호인지 확인
	public int countByPhone(String phone); 
	 //마이페이지 수정시 내번호는 제외하고 다른 사람이 쓰는 번호인지 확인
	public int countByPhoneExceptUser(@Param("phone") String phone,
									  @Param("userNum") long userNum);
//	마이페이지에서 수정 가능한 값만 업데이트
//	user_name, phone, intro만 바꿀 예정
	public int updateMyProfile(UsersVO userVO);
	
	public List<UsersVO> findEmailsByUserInfo(@Param("realName") String realName,
	                                          @Param("phone") String phone,
	                                          @Param("birth") Date birth);

	public void insertUser(UsersVO userVO);

	public int updatePassword(@Param("email") String email, @Param("passwd") String passwd);
}
