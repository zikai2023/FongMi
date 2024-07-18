package com.fongmi.android.tv.bean;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPSClient;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.*;
import java.nio.charset.StandardCharsets;



public class FtpManager {
    private String server;
    private String path;
    private int port;
    private String username;
    private String password;
    private boolean useFTPS;
    public boolean isServerReachable = false;

    public FtpManager(String server, String path, int port, String username, String password, boolean useFTPS) {
        this.server = server;
        this.path = path;
        this.port = port;
        this.username = username;
        this.password = password;
        this.useFTPS = useFTPS;
        this.isServerReachable = !this.server.trim().isEmpty() || this.server != null || !this.path.trim().equalsIgnoreCase("");
    }

    public FtpManager(String ftpUrl, String username, String password) {
        try {
            this.username = username;
            this.password = password;
            parseUrl(ftpUrl);
            this.isServerReachable = !this.server.trim().isEmpty() || this.server != null || !this.path.trim().equalsIgnoreCase("");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private void parseUrl(String ftpUrl) throws URISyntaxException {
        URI uri = new URI(ftpUrl);
        this.server = uri.getHost();
        this.path= uri.getPath();
        this.useFTPS = uri.getScheme().equalsIgnoreCase("ftps");
        this.port = (uri.getPort() == -1) ? useFTPS ? 990 : 21 : uri.getPort();

        if (this.username.trim().isEmpty() || this.password.trim().isEmpty() || this.username.trim().equalsIgnoreCase("") || this.password.trim().equalsIgnoreCase(""))
        {
            if (uri.getUserInfo() != null) {
                String[] userInfo = uri.getUserInfo().split(":");
                this.username = userInfo[0];
                this.password = (userInfo.length > 1) ? userInfo[1] : "";
            } else {
                this.username = "anonymous";
                this.password = "";
            }
        }
    }

    private FTPClient connectToFTP() throws IOException {
        if (!isServerReachable) {
            throw new IOException("Server is not reachable.");
        }

        FTPClient ftpClient = useFTPS ? new FTPSClient() : new FTPClient();
        ftpClient.connect(server, port);
        
        if (username != null && !username.isEmpty()) {
            ftpClient.login(username, password);
        } else {
            ftpClient.login("anonymous", "");
        }

        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
        ftpClient.enterLocalPassiveMode();
        return ftpClient;
    }

    public String downloadJsonFileAsString(String remoteFilePath) throws IOException {
        remoteFilePath = remoteFilePath==null? this.path : remoteFilePath;

        FTPClient ftpClient =  connectToFTP();
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            boolean success = ftpClient.retrieveFile(remoteFilePath, outputStream);

            if (!success) {
                return null;
                //throw new IOException("Failed to download the file: " + remoteFilePath);
            }
            return outputStream.toString(StandardCharsets.UTF_8.name());
        } finally {
            if (ftpClient != null && ftpClient.isConnected()) {
                try {
                    ftpClient.disconnect();
                } catch (IOException e) {
                    // Log the exception
                }
            }
        }
    }

    public void uploadJsonString(String jsonString, String remoteFilePath) throws IOException {
       remoteFilePath = remoteFilePath==null? this.path : remoteFilePath;

        FTPClient ftpClient = connectToFTP();
        try {
            createRemoteDirectories(ftpClient, remoteFilePath);

            // Convert the JSON string to an InputStream
            InputStream inputStream = new ByteArrayInputStream(jsonString.getBytes(StandardCharsets.UTF_8));

            boolean done = ftpClient.storeFile(remoteFilePath, inputStream);
            inputStream.close();
            if (!done) {
                throw new IOException("Failed to upload the file.");
            }
        } finally {
            ftpClient.disconnect();
        }
    }

    private void createRemoteDirectories(FTPClient ftpClient, String remoteFilePath) throws IOException {
        String[] pathElements = remoteFilePath.split("/");
        String currentPath = "";

        for (int i = 0; i < pathElements.length - 1; i++) {
            if (!pathElements[i].isEmpty()) {
                currentPath += "/" + pathElements[i];
                boolean dirExists = ftpClient.changeWorkingDirectory(currentPath);
                if (!dirExists) {
                    boolean created = ftpClient.makeDirectory(currentPath);
                    if (!created) {
                        throw new IOException("Unable to create remote directory: " + currentPath);
                    }
                }
            }
        }
    }
}