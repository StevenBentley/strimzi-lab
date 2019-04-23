package io.strimzi.source;

import io.strimzi.util.FTPConnection;
import io.strimzi.util.Version;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class TaxiSourceTask extends SourceTask {
    private static final Logger log = LoggerFactory.getLogger(TaxiSourceTask.class);

    private TaxiSourceConnectorConfig config;
    private FTPConnection connection;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private static int streamOffset = 0;
    private static BufferedReader stream;

    @Override
    public void start(Map<String, String> properties) {
        log.info("Starting Taxi source task");

        try {
            config = new TaxiSourceConnectorConfig(properties);
        } catch (ConfigException e) {
            throw new ConnectException("Unable to start TaxiSourceConnector due to configuration error",
                    e);
        }

        String ftpUrl = config.getString(TaxiSourceConnectorConfig.FTP_URL_CONFIG);
        String ftpUsr = config.getString(TaxiSourceConnectorConfig.FTP_USER_CONFIG);
        String ftpPass = config.getString(TaxiSourceConnectorConfig.FTP_PASSWORD_CONFIG);
        int ftpAttempts = config.getInt(
                TaxiSourceConnectorConfig.FTP_ATTEMPTS_CONFIG
        );
        long ftpRetryBackoff = config.getLong(
                TaxiSourceConnectorConfig.FTP_BACKOFF_CONFIG
        );
        String filepath = config.getString(TaxiSourceConnectorConfig.FILEPATH_CONFIG);

        connection = new FTPConnection(
                ftpUrl,
                ftpUsr,
                ftpPass,
                ftpAttempts,
                ftpRetryBackoff,
                filepath);

        while(connection.notConnected()) {
            try {
                Thread.sleep(10000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        stream = connection.getFileReader();

        running.set(true);
        log.info("Taxi source task started");
    }

    @Override
    public List<SourceRecord> poll() throws InterruptedException {

        while (running.get()) {
            try {
                if (stream != null && stream.ready()) {
                    String line = stream.readLine();
                    SourceRecord record = new SourceRecord(
                            Collections.singletonMap("", null),
                            Collections.singletonMap("position", streamOffset),
                            config.getString(TaxiSourceConnectorConfig.TOPIC_CONFIG),
                            Schema.STRING_SCHEMA,
                            line);
                    streamOffset++;
                    return Collections.singletonList(record);
                } else {
                    Thread.sleep(10000L);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        closeConnection();
        return null;
    }

    private void closeConnection() {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                // stream failed to close
            }
        }
        if (connection != null ) {
            connection.close();
            connection = null;
        }
    }

    @Override
    public void stop() {
        log.info("Stopping taxi source task");
        running.set(false);
    }

    @Override
    public String version() {
        return Version.getVersion();
    }

}
