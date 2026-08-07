package kr.spring.util;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileUtil {
	//업로드 상대 경로 
	private static final String UPLOAD_PATH = "/assets/upload";

	//파일 업로드 처리
	public static String createFile(HttpServletRequest request,MultipartFile file) throws IllegalStateException, IOException{
		//컨텍스트 루트상의 절대 경로 구하기
		String path = request.getServletContext().getRealPath(UPLOAD_PATH);
		String filename = null;
		if(file!=null && !file.isEmpty()) {
			//파일명이 중복되지 않도록 파일명 변경
			//원래 파일명을 보존하지 않을 경우
			filename = UUID.randomUUID()+file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
			//_ 이후에 원래 파일명을 보존할 경우
			//filename = UUID.randomUUID()+"_"+file.getOriginalFilename();
			file.transferTo(new File(path+"/"+filename));
		}
		return filename;
	}
	//파일 삭제
	public static void removeFile(HttpServletRequest request, String filename) {
		if(filename!=null) {
			//컨텍스트 루트상의 절대 경로 구하기
			String path = request.getServletContext().getRealPath(UPLOAD_PATH);
			File file = new File(path+"/"+filename);
			if(file.exists()) file.delete();
		}
	}
	public static String createThumbnail(HttpServletRequest request, String uploadedFile,int thumbnailWidth, int thumbnailHeight){
		String path = request.getServletContext().getRealPath(UPLOAD_PATH);
		String thumbnailFile = "s" + uploadedFile;
		int index = uploadedFile.lastIndexOf(".");
		if(index !=-1){//썸네일의 확장자는 jpg로 변경
			thumbnailFile = "s_" + uploadedFile.substring(0,index) + ".jpg";
		}

		FileInputStream fs = null; 
		try { 
			fs = new FileInputStream(path+"/"+uploadedFile);
			BufferedImage im = ImageIO.read(fs);

			int width;
			int height;

			if(thumbnailHeight == 0){//높이를 0으로 지정했을 경우 넓이를 

				int radio = im.getWidth() / thumbnailWidth;//축소할 비율을 구함

				width = thumbnailWidth;
				height = im.getHeight() / radio;
			}else{
				width = thumbnailWidth;
				height = thumbnailHeight;
			}

			BufferedImage thumb = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			Graphics2D 	g2 = thumb.createGraphics();

			g2.drawImage(im.getScaledInstance(width, height, Image.SCALE_SMOOTH), 0, 0, width, height, null);
			ImageIO.write(thumb, "jpg", new File(path,thumbnailFile));
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			if(fs!=null)try {fs.close();} catch (IOException e) {}
		}
		return thumbnailFile;
	}
		
	//지정한 경로의 파일을 읽어들여 byte 배열로 변환
	public static byte[] getBytes(String path) {
		FileInputStream fis = null;
		byte[] readbyte = null;
		try {
			fis = new FileInputStream(path);
			readbyte = new byte[fis.available()]; 
			fis.read(readbyte);
		} catch (Exception e) {
			log.error("<<파일 변환 오류>> : {}",e.toString());
		}finally {
			if(fis!=null)try {fis.close();}catch(IOException e) {}
		}
		return readbyte;
	}
}
