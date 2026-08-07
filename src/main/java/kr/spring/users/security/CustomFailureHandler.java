package kr.spring.users.security;

import java.io.IOException;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.servlet.support.SessionFlashMapManager;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
//로그인 실패 시 처리를 담당하는 클래스. 사용자가 인증(로그인)을 시도했지만 실패했을 때, 사용자를 어떤 URL로 리다이렉트할지 지정하거나 추가적인 로직을 실행
public class CustomFailureHandler extends SimpleUrlAuthenticationFailureHandler{

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException exception) throws IOException, ServletException {
		log.error("[Spring Security Login Check 2] CustomFailureHandler : " + exception.toString());
		
		//String error = "";
		//if(exception instanceof UsernameNotFoundException | exception instanceof BadCredentialsException){
		//	error = "error";
		//}
		
		//페이지가 리다이렉트되어서 아래와 같이 셋팅했을 경우 request에서 데이터를 읽을 수 있게 처리할 수 있음.
		//따라서 파라미터로 데이터를 넘길 필요가 없음
		FlashMap flashMap = new FlashMap();
        flashMap.put("error", "error");
        FlashMapManager flashMapManager = new SessionFlashMapManager();
        flashMapManager.saveOutputFlashMap(flashMap, request, response);
		
		setDefaultFailureUrl("/member/login");
		
		super.onAuthenticationFailure(request, response, exception);
	}

}
