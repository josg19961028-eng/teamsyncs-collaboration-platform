package kr.spring.users.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
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
public class CustomAccessDeniedHandler implements AccessDeniedHandler{

	//파일의 최대 업로드 사이트
	@Value("${spring.servlet.multipart.max-file-size}")
	private String max_file_size;

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException, ServletException {
		//{}는 SLF4J(Simple Logging Facade for Java)에서
		//제공하는 플레이스홀더
		//문자열 결합보다 성능이 더 좋음
		//예)
		//문자열 결합 : log.error("오류 발생 : " + accessDeniedException.toString());
		//플레이스홀더 이용 : log.error("오류 발생 : {}",accessDeniedException.toString());

		log.debug("<<예외 발생 페이지>> : {}", request.getRequestURI());
		log.error("<<예외 발생>> : {}", accessDeniedException.toString());

		//Ajax 요청 여부 체크
		if (isAjaxRequest(request)) {
			handleAjaxErrorResponse(request, response, accessDeniedException);
			return;
		}

		FlashMap flashMap = new FlashMap();
		FlashMapManager flashMapManager = new SessionFlashMapManager();
		
		//파일 업로드 오류 체크
		String contentType = request.getContentType();
		String contentLength = request.getHeader("Content-Length");
		long maxSize = Long.parseLong(max_file_size.substring(0, max_file_size.indexOf("MB")))*1024*1024;
		if (contentType != null && contentType.startsWith("multipart/form-data")) {
			long size = Long.parseLong(contentLength);
			log.debug("<<파일 업로드 오류>> 파일 사이즈 : {}, 최대 업로드 파일 사이즈 : {}", size, maxSize);
			if(size > maxSize) {
				flashMap.put("max_size", max_file_size);
				flashMap.put("size", String.format("%.2f",(double)size/(1024*1024))+"MB");
				flashMapManager.saveOutputFlashMap(flashMap, request, response);
				response.sendRedirect(request.getContextPath()+"/fileSizeLimit");
				return;
			}
		}
		
		if (accessDeniedException instanceof InvalidCsrfTokenException | 
				accessDeniedException instanceof MissingCsrfTokenException) {
			if(request.getRequestURI().equals("/member/logout")) {
				response.sendRedirect("/main/home");
				return;
			}
			
			flashMap.put("accessMsg", "CSRF TOKEN 미사용 또는 오류");
			flashMapManager.saveOutputFlashMap(flashMap, request, response);
			response.sendRedirect(request.getContextPath()+"/accessDenied");
			return;
		} 

		response.sendRedirect(request.getContextPath()+"/accessDenied");
	}
	// Ajax 요청 여부 확인
	private boolean isAjaxRequest(HttpServletRequest request) {
		//Ajax 요청 판별 -> X-Requested-With: XMLHttpRequest 헤더 체크
		String requestedWith = request.getHeader("X-Requested-With");
		return "XMLHttpRequest".equalsIgnoreCase(requestedWith);
	}

	// Ajax 에러 응답 (401/403 구분)
	/*
	 * 응답 예시
	 * 401 Unauthorized (비로그인 상태)
	 * {
	 * 	 "result":"error",
	 * 	 "message":"로그인이 필요합니다."
	 * }
	 * 403 Fobidden (로그인 했지만 권한 없음)
	 * {
	 *   "result":"error",
	 *   "message":"권한이 없습니다."
	 * }
	 */
	private void handleAjaxErrorResponse(HttpServletRequest request, HttpServletResponse response, Exception ex) throws IOException {
		boolean isAuthenticated = request.getUserPrincipal() != null; // 인증 여부 확인
		
		response.setContentType("application/json;charset=UTF-8");
		
		String errorMessage;
		if(ex.toString().contains("null")) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			errorMessage = "{\"result\": \"error\",\"message\": \"CSRF 토큰 오류\"}";
		}else if (isAuthenticated) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403 : 로그인은 했지만, 권한이 부족할 때
			errorMessage = "{\"result\": \"error\",\"message\": \"권한이 없습니다.\"}";
		} else {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 : 로그인 자체가 필요할 때
			errorMessage = "{\"result\": \"error\",\"message\": \"로그인 후 사용하세요(접속이 제한됨)\"}";
		}

		response.getWriter().write(errorMessage);
	}

}
