package kr.spring.users.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import kr.spring.users.service.UsersService;
import kr.spring.users.vo.PrincipalDetails;
import kr.spring.users.vo.UserStatus;
import kr.spring.users.vo.UsersVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
// 로그인 시 사용자 정보를 조회하고, 이를 기반으로 인증(Authentication)을 수행하는 데 사용
public class UserSecurityService implements UserDetailsService {

    private final UsersService usersService;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("[Login Check 1 - UserSecurityService] 로그인 이메일 :" + email);
        UsersVO users = usersService.selectByEmail(email);

        // 탈퇴 회원이거나 존재하지 않는 회원이면 로그인 차단
        if (users == null || users.getStatus() == UserStatus.WITHDRAWN.getValue()) {
            log.debug("[Login Check 1] 로그인 이메일이 없거나 탈퇴회원");
            throw new UsernameNotFoundException("UserNotFound");
        }

        // 정지 회원은 일단 로그인은 허용하고, 성공 핸들러(CustomSuccessHandler)에서 차단 처리
        return new PrincipalDetails(users);
    }
}