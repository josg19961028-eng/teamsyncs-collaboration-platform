package kr.spring.users.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import kr.spring.users.vo.UsersVO;

@Mapper
public interface GoogleUserMapper {

    UsersVO selectByGoogleId(String googleId);

    UsersVO selectByEmail(String email);

    UsersVO selectByUserNum(long userNum);

    void insertGoogleUser(UsersVO user);

    int updateGoogleAdditionalInfo(UsersVO user);

    int linkGoogleAccount(@Param("userNum") long userNum,
                          @Param("googleId") String googleId);

    int unlinkGoogleAccount(long userNum);

    int countByPhoneExceptUser(@Param("phone") String phone,
                               @Param("userNum") long userNum);
}
