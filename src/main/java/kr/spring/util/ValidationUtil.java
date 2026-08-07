package kr.spring.util;

import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ValidationUtil {
	public static void printErrorFields(BindingResult result) {
		log.debug("<<=====유효성 체크 에러 발생 : 필드 목록 시작=====>>");
		for(FieldError f : result.getFieldErrors()){	
			log.debug("<<에러 필드>> : " + f.getField());
		}
		log.debug("<<=====유효성 체크 에러 발생 : 필드 목록 끝=====>>");
	}
}
