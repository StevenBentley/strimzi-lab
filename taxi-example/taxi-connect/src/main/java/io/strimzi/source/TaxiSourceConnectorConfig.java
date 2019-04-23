package io.strimzi.source;

import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.Type;
import org.apache.kafka.common.config.ConfigDef.Width;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class TaxiSourceConnectorConfig extends AbstractConfig {
    private static final Logger log = LoggerFactory.getLogger(TaxiSourceConnectorConfig.class);

    public static final String FTP_URL_CONFIG = "connect.ftp.address";
    private static final String FTP_URL_DOC = "FTP connection URL host:port.";
    private static final String FTP_URL_DISPLAY = "FTP URL host:port";

    public static final String FTP_USER_CONFIG = "connect.ftp.user";
    private static final String FTP_USER_DOC = "FTP connection user.";
    private static final String FTP_USER_DISPLAY = "FTP User";

    public static final String FTP_PASSWORD_CONFIG = "connect.ftp.password";
    private static final String FTP_PASSWORD_DOC = "FTP connection password.";
    private static final String FTP_PASSWORD_DISPLAY = "FTP Password";

    public static final String FTP_ATTEMPTS_CONFIG = "connect.ftp.attempts";
    private static final String FTP_ATTEMPTS_DOC
            = "Maximum number of attempts to retrieve a valid FTP connection.";
    private static final String FTP_ATTEMPTS_DISPLAY = "FTP connection attempts";
    public static final int FTP_ATTEMPTS_DEFAULT = 3;

    public static final String FTP_BACKOFF_CONFIG = "connect.ftp.backoff.ms";
    private static final String FTP_BACKOFF_DOC
            = "Backoff time in milliseconds between connection attempts.";
    private static final String FTP_BACKOFF_DISPLAY
            = "FTP connection backoff in milliseconds";
    public static final long FTP_BACKOFF_DEFAULT = 10000L;

    public static final String FILEPATH_CONFIG = "connect.ftp.filepath";
    private static final String FILEPATH_DOC = "Path to file on remote FTP server.";
    private static final String FILEPATH_DISPLAY = "FTP file path";

    public static final String TOPIC_CONFIG = "connect.ftp.topic";
    private static final String TOPIC_DOC = "KafkaTopic to output file to.";
    private static final String TOPIC_DISPLAY = "Kafka topic";

    public static final String FTP_GROUP = "FTP";

    public static final ConfigDef CONFIG_DEF = baseConfigDef();

    private static ConfigDef baseConfigDef() {
        ConfigDef config = new ConfigDef();
        addFtpConfig(config);
        return config;
    }

    private static void addFtpConfig(ConfigDef config) {
        int order = 0;
        config.define(
                FTP_URL_CONFIG,
                Type.STRING,
                null,
                Importance.HIGH,
                FTP_URL_DOC,
                FTP_GROUP,
                ++order,
                Width.LONG,
                FTP_URL_DISPLAY
        ).define(
                FTP_USER_CONFIG,
                Type.STRING,
                null,
                Importance.HIGH,
                FTP_USER_DOC,
                FTP_GROUP,
                ++order,
                Width.LONG,
                FTP_USER_DISPLAY
        ).define(
                FTP_PASSWORD_CONFIG,
                Type.STRING,
                null,
                Importance.HIGH,
                FTP_PASSWORD_DOC,
                FTP_GROUP,
                ++order,
                Width.LONG,
                FTP_PASSWORD_DISPLAY
        ).define(
                FTP_ATTEMPTS_CONFIG,
                Type.INT,
                FTP_ATTEMPTS_DEFAULT,
                Importance.LOW,
                FTP_ATTEMPTS_DOC,
                FTP_GROUP,
                ++order,
                Width.SHORT,
                FTP_ATTEMPTS_DISPLAY
        ).define(
                FTP_BACKOFF_CONFIG,
                Type.LONG,
                FTP_BACKOFF_DEFAULT,
                Importance.LOW,
                FTP_BACKOFF_DOC,
                FTP_GROUP,
                ++order,
                Width.SHORT,
                FTP_BACKOFF_DISPLAY
        ).define(
                FILEPATH_CONFIG,
                Type.STRING,
                null,
                Importance.HIGH,
                FILEPATH_DOC,
                FTP_GROUP,
                ++order,
                Width.LONG,
                FILEPATH_DISPLAY
        ).define(
                TOPIC_CONFIG,
                Type.STRING,
                null,
                Importance.HIGH,
                TOPIC_DOC,
                FTP_GROUP,
                ++order,
                Width.LONG,
                TOPIC_DISPLAY
        );
    }

    public TaxiSourceConnectorConfig(Map<String, ?> props) {
        super(CONFIG_DEF, props);
    }
}
