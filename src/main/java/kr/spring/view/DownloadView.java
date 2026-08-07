package kr.spring.view;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.servlet.view.AbstractView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class DownloadView extends AbstractView{

	@Override
	protected void renderMergedOutputModel(Map<String, Object> model,
			HttpServletRequest request, HttpServletResponse response)
					throws Exception {
		//다운로드하는 파일의 경로 정보가 저장된 File 객체 반환
		File file = (File) model.get("downloadFile");
		
		//컨텐트 타입 지정
		response.setContentType("application/download; charset=utf-8");
		//컨텐트의 용량 지정
		response.setContentLength((int) file.length());
		//파일명 구하기
		String filename = new String(file.getName().getBytes("utf-8"),"iso-8859-1");
		
		//HTTP 응답 메시지 헤더 셋팅
		response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\";");
		response.setHeader("Content-Transfer-Encoding", "binary");
		//파일 쓰기
		OutputStream out = response.getOutputStream();
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);//파일 읽기
			//읽은 정보를 쓰기 정보로 변환
			FileCopyUtils.copy(fis, out);
		} finally {
			if (fis != null)try {fis.close();} catch (IOException ex) {}
		}
		out.flush();//파일 전송
	}
}





