import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;
import org.apache.commons.net.ftp.FTPReply;

public class SynchronizeOverFTP {

	public FTPClient getFtpClient(String userName,String pwd, String host) {

		FTPClient ftp = null;
		try {

			ftp = new FTPClient();
			InetAddress inetAddress = InetAddress
					.getByName(host);
			ftp.connect(inetAddress);
			if (!ftp.login(userName, pwd)) {
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

	public static void main(String[] arg) throws IOException{
		
		
		String clientPath = System.getenv("SS_HOME");
		if(clientPath == null){
			System.out.println("please set SS_HOME system variable.");
			System.exit(0);
		}else
			new SynchronizeOverFTP().startSchedule(clientPath);
		

	}
	
	private void startSchedule(final String clientPath){
		Timer timer = new Timer();
		TimerTask tt = new TimerTask(){
			public void run(){
				
				try
				{
					startJob(clientPath);
				} catch (IOException e)
				{
					System.out.println("Error occured , reason :"+ e.getMessage());
					System.exit(0);
				}
			}
		};
		timer.schedule(tt,10,20*3600);
	}
	
	private void startJob(String clientPath) throws IOException{
		
		if(clientPath == null){
			System.out.println("Please system variable in your machine with named SS_HOME");
			System.exit(0);
		}
		Map<String, FTPFile> adminMap = new HashMap<String, FTPFile>();
		Map<String, File> userMap = new HashMap<String, File>();
		SynchronizeOverFTP sc= new SynchronizeOverFTP();
		FTPClient adminClient = sc.getFtpClient("screensaver","mds3rt@","mdserta.com");
		File userClient = new File(clientPath);

		FTPFile[] files = adminClient.listFiles("", new FTPFileFilter() {
			
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
		File[] files1 = userClient.listFiles(new FileFilter() {
			
			public boolean accept(File arg0) {
				if(arg0.getName().equals(".") || arg0.getName().equals(".."))
					return false;
				else
					return true;
			}
		});
		for (File ftpFile : files1){
			userMap.put(ftpFile.getName(), ftpFile);
		}
		
		int fileCounter = 0;
		System.out.println("Checking Updates. Please Wait ...");
		for(String key : adminMap.keySet()){
			if(userMap.get(key) == null){
				System.out.println("Updating File :"+adminMap.get(key).getName()+"....." );

				File uploadTempFile = new File(userClient.getAbsolutePath()+File.separator+adminMap.get(key).getName());
				FileOutputStream fos = new FileOutputStream(uploadTempFile);
				adminClient.retrieveFile(adminMap.get(key).getName(), fos);
				fos.close();
				fileCounter++;
			}
		}
		if(fileCounter > 0){
			System.out.println(fileCounter +" file(s) is updated.");
			
		}else{
			System.out.println("User is already Updated. No updates available.");
		}
		sc.disConnectFtp(adminClient);
	}
}
