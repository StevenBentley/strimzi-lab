package io.strimzi;

import io.strimzi.source.TaxiSourceConnectorConfig;
import io.strimzi.source.TaxiSourceTask;
import io.strimzi.util.FTPConnection;
import io.strimzi.util.Version;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.source.SourceConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TaxiSourceConnector extends SourceConnector {
    private static final Logger log = LoggerFactory.getLogger(TaxiSourceConnector.class);
    private TaxiSourceConnectorConfig config;
    private FTPConnection connection;

    @Override
    public void start(Map<String, String> properties) {
        log.info("Starting Taxi source connector");

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

        connection.connect();
    }

    @Override
    public Class<? extends Task> taskClass() {
        return TaxiSourceTask.class;
    }

    @Override
    public List<Map<String, String>> taskConfigs(int maxTasks) {
        List<Map<String, String>> taskConfigs = new ArrayList<>(1);
        taskConfigs.add(config.originalsStrings());
        log.info("Task configs {}", taskConfigs);
        return taskConfigs;
    }

    @Override
    public void stop() {
        log.info("Stopping taxi source connector");
        closeConnection();
    }

    private void closeConnection() {
        if (connection != null ) {
            connection.close();
            connection = null;
        }
    }

    @Override
    public ConfigDef config() {
        return TaxiSourceConnectorConfig.CONFIG_DEF;
    }

    @Override
    public String version() {
        return Version.getVersion();
    }
}
