package com.sync;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;
import org.apache.commons.net.ftp.FTPReply;

public class Synchronizer {

	private final static String LOCAL_DIRECTORY = "";
	private final static String REMOTE_DIRECTORY = "users";

	public List<File> listLocalFiles() {
		File localFile = new File(LOCAL_DIRECTORY);
		File[] files = null;
		if (localFile != null && localFile.exists() && localFile.isDirectory()) {
			files = localFile.listFiles();
			if (files != null)
				return Arrays.asList(files);
		}
		return null;
	}

	public FTPClient getFtpClient(String pathName) {

		FTPClient ftp = null;
		try {

			ftp = new FTPClient();
			InetAddress inetAddress = InetAddress
					.getByName("mdserta.com");
			ftp.connect(inetAddress);
			if (!ftp.login("screensaver", "mds3rt@")) {
				ftp.logout();
			}
			int reply = ftp.getReplyCode();

			if (!FTPReply.isPositiveCompletion(reply)) {
				ftp.disconnect();
			}

			ftp.setFileTransferMode(FTP.BINARY_FILE_TYPE);
			ftp.enterLocalPassiveMode();
			return ftp;

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return null;
	}

	public void disConnectFtp(FTPClient ftpClient) {
		try {

		} finally {
			if (ftpClient != null) {
				if (ftpClient.isConnected()) {
					try {
						ftpClient.logout();
					} catch (IOException e) {
						e.printStackTrace();
					}
					try {
						ftpClient.disconnect();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	public Map<String,File> getFileMap(List<File> latestFileLists){
		Map<String, File> listFileMap = new HashMap<String, File>();
		Iterator<File> iterator = latestFileLists.iterator();
		while(iterator.hasNext()){
			File tempFile = iterator.next();
			StringBuilder fileNameBuilder = new StringBuilder();
			generateFileNameFileMapped(tempFile, fileNameBuilder, listFileMap);
		}
		return listFileMap;
	}
	public void generateFileNameFileMapped(File file,StringBuilder str, Map<String, File> map){
		if(file.isDirectory()){
			str.append(file.getName());
			File[] files = file.listFiles();
			if(files != null && files.length > 0){
				
				for (File tempFile : files){
					generateFileNameFileMapped(tempFile, str, map);
				}
			}else{
				map.put(str.toString(), file);
			}
		}else if(file.isFile()){
			map.put(str.toString()+file.getName(), file);
		}
	}
	
	public List<File> filesNeedsToUpdate(Map<String,File> currentMap,Map<String,File> updatedMap){
		List<File> fileList = new ArrayList<File>();
		for(String key : updatedMap.keySet()){
			if(currentMap.get(key) == null){
				fileList.add(updatedMap.get(key));
			}
		}
		return fileList;
	}
	public static void main(String[] arg) throws IOException{
		File file1 = new File ("E:\\logs");
		File file = new File ("E:\\logs1");
		Map<String, FTPFile> adminMap = new HashMap<String, FTPFile>();
		Map<String, FTPFile> userMap = new HashMap<String, FTPFile>();
		Synchronizer sc= new Synchronizer();
		FTPClient client = sc.getFtpClient("");
		FTPFile[] files = client.listFiles("", new FTPFileFilter() {
			
			@Override
			public boolean accept(FTPFile arg0) {
				if(arg0.getName().equals(".") || arg0.getName().equals("..") || arg0.getName().equals("users"))
					return false;
				else
					return true;
			}
		});
		for (FTPFile ftpFile : files){
			adminMap.put(ftpFile.getName(), ftpFile);
		}
		client.changeWorkingDirectory("users");
		FTPFile[] files1 = client.listFiles("", new FTPFileFilter() {
			
			@Override
			public boolean accept(FTPFile arg0) {
				if(arg0.getName().equals(".") || arg0.getName().equals(".."))
					return false;
				else
					return true;
			}
		});
		for (FTPFile ftpFile : files1){
			userMap.put(ftpFile.getName(), ftpFile);
		}
		client.cdup();
		String tempDirectory = "Temp-"+System.nanoTime();
		File tempDir = new File(tempDirectory);
		tempDir.mkdir();
		for(String key : adminMap.keySet()){
			if(userMap.get(key) == null){
				File uploadTempFile = new File(tempDirectory+File.separator+adminMap.get(key).getName());
				FileOutputStream fos = new FileOutputStream(uploadTempFile);
				client.retrieveFile(adminMap.get(key).getName(), fos);
				fos.close();
				System.out.println(uploadTempFile.getAbsolutePath() + "  is Downloaded..");
			}
		}
		client.changeWorkingDirectory("users");
		if(tempDir.exists()){
			File[] fileUpload = tempDir.listFiles();
			for(File ChunkUpload : fileUpload){
				if(client.storeFile(ChunkUpload.getName(), new FileInputStream(ChunkUpload)))
					System.out.println(ChunkUpload.getName() + "  is Uploaded..");
			}
		}
		
		sc.disConnectFtp(client);
		/*Map<String,File> map = sc.getFileMap(Arrays.asList(file1.listFiles()));
		List<File> files = sc.filesNeedsToUpdate(sc.getFileMap(Arrays.asList(file.listFiles())), sc.getFileMap(Arrays.asList(file1.listFiles())));
		Iterator<File> iterator = files.iterator();
		while(iterator.hasNext()){
			try{
				
				sc.copyFileUsingFileStreams(iterator.next());
			}catch(IOException ioe){
				ioe.printStackTrace();
			}
		}*/
	}

	private void copyFileUsingFileStreams(File source) throws IOException {
		
		File dest = new File("E://sagar"+File.separator+source.getName());
		InputStream input = null;
		OutputStream output = null;
		try {
			input = new FileInputStream(source);
			output = new FileOutputStream(dest);
			byte[] buf = new byte[1024];
			int bytesRead;
			while ((bytesRead = input.read(buf)) > 0) {
				output.write(buf, 0, bytesRead);
			}
			System.out.println(source.getName() + " copied successfully.");
		} finally {
			input.close();
			output.close();
		}
	}
}
