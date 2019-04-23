package io.strimzi.util;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.kafka.connect.errors.ConnectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class FTPConnection implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(FTPConnection.class);

    private static final FTPClient client = new FTPClient();
    private String url;
    private String user;
    private String pass;
    private int attempts;
    private long backoff;
    private String filepath;

    public FTPConnection(String url, String user, String pass, int attempts, long backoff, String filepath) {
        this.url = url;
        this.user = user;
        this.pass = pass;
        this.attempts = attempts;
        this.backoff = backoff;
        this.filepath = filepath;
    }

    public synchronized void connect() {
        try {
            if (!client.isConnected()) {
                attemptConnect();
            }
        } catch (IOException ioe) {
            throw new ConnectException(ioe);
        }
    }

    public BufferedReader getFileReader() {
        if (!filePathExists()) {
            ConnectException ce = new ConnectException("File does not exist on remote server");
            log.warn("File does not exist at path {} ", filepath, ce);
            throw ce;
        }

        InputStream inputStream;
        try {
            inputStream = client.retrieveFileStream(filepath);
        } catch (IOException e) {
            throw new ConnectException(String.format("Failed to retrieve input stream at %s", filepath));
        }

        return new BufferedReader(new InputStreamReader(inputStream));
    }

    private boolean filePathExists() {
        boolean fileExists = false;
        try {
            if (client.getDataConnectionMode() != FTPClient.PASSIVE_LOCAL_DATA_CONNECTION_MODE) {
                log.info("FTPClient entering PASSIVE_LOCAL_DATA_CONNECTION_MODE");
                client.enterLocalPassiveMode();
            }
            FTPFile file = client.mlistFile(filepath);
            if (file != null && file.isValid()) {
                log.info("File located at {}", file.getName());
                fileExists = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fileExists;
    }

    public boolean notConnected() {
        if (!client.isConnected()) {
            return true;
        }
        int reply = client.getReplyCode();
        return !FTPReply.isPositiveCompletion(reply);
    }

    private void attemptConnect() throws IOException {
        int att = 0;
        while (att < attempts) {
            try {
                log.info("Starting connection attempt #{} to {}", att + 1, url);
                client.connect(url);
                log.info("Logging in with {} {}", user, pass);
                client.login(user, pass);
                if (notConnected()) {
                    close();
                    throw new IOException("FTP Reply invalid");
                } else if (!filePathExists()) {
                    close();
                    throw new FileNotFoundException("File does not exist on remote server");
                } else {
                    log.info("Connection successful");
                    break;
                }
            } catch (FileNotFoundException fnfe) {
                log.warn("File does not exist on remote server ", fnfe);
                throw fnfe;
            } catch (IOException ioe) {
                att++;
                log.warn("Unable to connect to FTP server on {}, with user {}, pass {}, ", url, user, pass, ioe);
                if (att < attempts) {
                    log.info("Retrying in {}ms", backoff);
                    try {
                        Thread.sleep(backoff);
                    } catch (InterruptedException ex) {
                        // continue
                    }
                } else {
                    throw ioe;
                }
            }
        }
    }

    @Override
    public synchronized void close() {
        try {
            if (client.isConnected()) {
                client.logout();
                client.disconnect();
            }
        } catch (IOException e) {
            log.info("Disconnect failed with exception {}", e.getLocalizedMessage());
        }
    }
}
