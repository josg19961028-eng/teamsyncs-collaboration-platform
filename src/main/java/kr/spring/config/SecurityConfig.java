package kr.spring.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;

import kr.spring.users.security.CustomAccessDeniedHandler;
import kr.spring.users.security.CustomFailureHandler;
import kr.spring.users.security.CustomOAuth2FailureHandler;
import kr.spring.users.security.CustomOAuth2UserService;
import kr.spring.users.security.UserSecurityService;
import lombok.extern.slf4j.Slf4j;
 
@Slf4j
//이 클래스가 Spring 설정 파일이라는 의미
@Configuration
//모든 요청 URL이 스프링 시큐리티의 제어를 받도록 만드는 애너테이션,스프링 시큐리티를 활성화하는 역할
//내부적으로 SecurityFilterChain 클래스가 동작하여 모든 요청 URL에 이 클래스가 필터로 적용되어 URL별로 특별한 설정 가능
@EnableWebSecurity
//Controller 메서드 레벨에서 권한을 체크할 수 있도록 설정. @PreAuthorize 사용시 추가
@EnableMethodSecurity
public class SecurityConfig{
	//쿠키에 사용되는 값을 암호화하기 위한 키(key)값
	@Value("${dataconfig.rememberme-key}")
	private String rememberme_key;
	
	//DB연동을 위한 DataSource 지정
	@Autowired
	@Qualifier("dataSource")
	private DataSource dataSource;

	//로그인 시 사용자 정보를 조회하고, 이를 기반으로 인증(Authentication)을 수행하는 데 사용
	//로그인 시 DB에서 회원 정보를 가져올 때 사용
	@Autowired
	private UserSecurityService userSecurityService;
	
	//인증(로그인)에 성공한 후, 리다이렉트할 URL을 지정하거나 처리 로직을 직접 작성할 때 사용
	@Autowired
	private AuthenticationSuccessHandler authenticationSuccessHandler;
	//로그인 실패 시 처리를 담당하는 클래스. 사용자가 인증(로그인)을 시도했지만 실패했을 때, 사용자를 어떤 URL로 리다이렉트할지 지정하거나 추가적인 로직을 실행
	@Autowired
	private CustomFailureHandler authenticationFailureHandler;
	// 권한이 없을 때 처리하는 클래스
	@Autowired
	private CustomAccessDeniedHandler customAccessDeniedHandler;

	@Autowired
	private CustomOAuth2UserService customOAuth2UserService;

	@Autowired
	private CustomOAuth2FailureHandler customOAuth2FailureHandler;

	@Bean
	//보안 설정의 핵심 역할. HTTP 요청에 대해 어떤 보안 규칙을 적용할 것인지 설정하는 메서드
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {	
		return http
				// URL 접근 권한 설정
				.authorizeHttpRequests(authorize-> authorize
						//롤 설정을 먼저 하고 permitAll()를 호출해야 정상적으로 롤이 지정됨
						// /admin/**: ROLE_ADMIN 권한 필요 지정
						.requestMatchers("/admin/**").hasAuthority("ROLE_ADMIN")
						//기타 경로: 누구나 접근 가능
						.requestMatchers(
							    "/assets/**",
							    "/",
							    "/main/**",
							    "/member/**",
							    "/team/**",
							    "/calendar/**",
							    "/kanban/**",
							    "/minutes/**",
							    "/chat/**",
							    "/storage/**",
							    "/notice/**",
							    "/member/**",
							    "/dev/**",
							    "/users/**",
							    "/notification/**",
							    "/bot/**",
							    "/ws-chat/**",
							    "/chat/send"
						).permitAll()
						// 위 조건 외에는 인증 필요
						//인증되지 않은 요청은 로그인 페이지로 리다이렉트됨
						.anyRequest().authenticated() 
						)
				// 일반 로그인 설정
				.formLogin(login -> login 
						// 사용자 정의 로그인 페이지 주소
						.loginPage("/member/login")
						// 로그인 성공 시 처리
						.successHandler(authenticationSuccessHandler)
						// 로그인 실패 시 처리
						.failureHandler(authenticationFailureHandler)
						 // 로그인 폼의 아이디 input email
						.usernameParameter("email")
						// 로그인 폼의 비밀번호 input name
						.passwordParameter("passwd"))
				.oauth2Login(oauth -> oauth
						.loginPage("/member/login")
						.userInfoEndpoint(userInfo -> userInfo
								.userService(customOAuth2UserService))
						.successHandler(authenticationSuccessHandler)
						.failureHandler(customOAuth2FailureHandler))
				// 로그아웃 설정
				.logout(logout -> logout 
						// 로그아웃 요청 URL
						.logoutUrl("/member/logout")
						 // 로그아웃 후 이동 페이지
						.logoutSuccessUrl("/")
						 // 세션 제거
						.invalidateHttpSession(true)
						 // 쿠키 삭제
						.deleteCookies("remember-me","JSESSIONID"))
				// 예외 처리 설정
				.exceptionHandling(error -> error
						.authenticationEntryPoint((req,res,e) -> { 
						
						// 로그인 안 한 사용자가 접근
				        if (e instanceof InsufficientAuthenticationException) {
				            res.sendRedirect("/member/login");
				            return;
				        }

				        // 세션 만료
				        if (e instanceof SessionAuthenticationException) {
				        	 res.sendRedirect("/member/login");
				            return;
				        }
						
				        //이외의 오류
						req.getRequestDispatcher("/main/resultError").forward(req, res);
						
						}
					)
					//권한 없는 접근 발생 시 실행
				    .accessDeniedHandler(customAccessDeniedHandler)
				)

				// 자동 로그인(Remember-Me) 설정
				/*
				.rememberMe(me -> me
						.key(rememberme_key) //쿠키에 사용되는 값을 암호화하기 위한 키(key)값
						.tokenRepository(persistentTokenRepository()) //토큰은 데이터베이스에 저장
						 // 자동 로그인 유지 시간
                        // 60초 * 60분 * 24시간 * 7일
                        // = 7일
						.tokenValiditySeconds(60*60*24*7)
						 // 사용자 정보 조회 서비스
						.userDetailsService(userSecurityService))
				*/
				.build();	
	}
	
	// 비밀번호 암호화 객체 생성
	@Bean
	public PasswordEncoder passwordEncoder() {
		 // BCrypt 방식 암호화 사용
		return new BCryptPasswordEncoder();
	}
	
	/*
	 * 자동로그인 사용시 자동으로 생성되는 persistent_logins 테이블 컬럼 설명
	 * series :  사용자의 로그인 세션을 식별하는 고유한 값
	 * username: 로그인한 사용자의 ID
	 * token: 사용자의 브라우저에 저장되는 토큰 값(쿠키에 저장되는 암호화된 토큰 값)
	 *        이 토큰을 통해 시스템은 사용자를 인증
	 *        매번 로그인이 유지될 때마다 갱신
	 *        토큰이 일치하지 않으면 Remember-Me 세션이 무효화
	 * last_used: 토큰이 마지막으로 사용된 시각. 토큰의 유효 기간을 관리하는 데 사용
	 */
	
	/*
	@Bean
	public PersistentTokenRepository persistentTokenRepository() {
		// JDBC 기반 토큰 저장 객체 생성
		JdbcTokenRepositoryImpl repo = new JdbcTokenRepositoryImpl();
		// DB 연결 설정
		repo.setDataSource(dataSource);
		return repo;
	}
	*/
}
