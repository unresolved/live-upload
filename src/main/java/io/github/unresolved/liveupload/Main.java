package io.github.unresolved.liveupload;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lombok.extern.log4j.Log4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.Properties;

@Log4j
public class Main {

    private Config config;

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }

    public void run(String[] args) throws IOException {
        OptionParser parser = new OptionParser();
        parser.accepts("config", "The path of the LiveUpload configuration file.")
                .withRequiredArg().required().ofType(String.class);
        OptionSet options = parser.parse(args);

        byte[] bannerData = Objects.requireNonNull(ClassLoader.getSystemClassLoader().getResourceAsStream("banner.txt")).readAllBytes();
        log.info(new String(bannerData));

        String configPath = String.valueOf(options.valueOf("config"));
        log.info("Target config file: " + configPath);
        // copy default configuration file if it is not exists
        copyDefaultConfig(configPath);
        // load configuration file from specified file
        loadConfig(configPath);
        // validate configuration file
        validateConfig();

        // start upload task now
        LiveUploadTask task = new LiveUploadTask(config);
        task.run();

    }

    private void copyDefaultConfig(String file) {
        File configFile = new File(file);
        if (!configFile.exists()) {
            log.info("Specified config file does not exists, It will be auto created...");
            try {
                // save default configuration file to target file if target file is not exits
                Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("default-config.properties"))
                        .transferTo(new FileOutputStream(file));
                log.info("NOTICE: You need to manual config this file, then start this program again!");
                System.exit(0);
            } catch (IOException e) {
                log.error("Failed to copy default config file: " + e.getMessage());
                System.exit(1);
            }
        }
    }

    private void loadConfig(String file) {
        try {
            Properties properties = new Properties();
            properties.load(new FileInputStream(file));
            config = new Config();

            String sourceDirectory = properties.getProperty("source-directory");
            log.info("Source directory = \"" + sourceDirectory + "\"");
            config.setSourceDirectory(sourceDirectory);

            String destinationPath = properties.getProperty("destination-path");
            log.info("Destination path = \"" + destinationPath + "\"");
            config.setDestinationPath(destinationPath);

            String upyunBucket = properties.getProperty("upyun-bucket");
            log.info("Upyun Bucket = \"" + upyunBucket + "\"");
            config.setUpyunBucket(upyunBucket);

            String upyunOperator = properties.getProperty("upyun-operator");
            log.info("Upyun Operator = \"" + upyunOperator + "\"");
            config.setUpyunOperator(upyunOperator);

            String upyunPassword = properties.getProperty("upyun-password");
            log.info("Upyun Password = \"" + "*".repeat(upyunPassword.length()) + "\"");
            config.setUpyunPassword(upyunPassword);

            int checkInterval = Integer.parseInt(properties.getProperty("check-interval"));
            log.info("Check Interval = " + checkInterval);
            config.setCheckInterval(checkInterval);
        } catch (Exception e) {
            log.error("An error occurred while loading the configuration: " + e.getMessage(), e);
        }
    }

    private void validateConfig() {

        // check source directory
        File sourceDirectory = new File(config.getSourceDirectory());
        if (!sourceDirectory.exists() || !sourceDirectory.isDirectory()) {
            log.error("Invalid source directory!");
            System.exit(1);
        }

        // check destination path
        String destinationPath = config.getDestinationPath();
        if (!destinationPath.startsWith("/") // prefix must begin with "/"
                || (!destinationPath.equals("/")
                && destinationPath.endsWith("/"))   // suffix must not end with "/"
        ) {
            log.error("Destination path must not be empty and start with \"/\" but not end with \"/\" (Except for the root directory)!");
            System.exit(1);
        }

        // check upyun bucket
        String upyunBucket = config.getUpyunBucket();
        if (upyunBucket.equals("")) {
            log.error("Upyun bucket can not be empty!");
            System.exit(1);
        }

        // check upyun operator
        String uoyunOperator = config.getUpyunOperator();
        if (uoyunOperator.equals("")) {
            log.error("Upyun operator can not be empty!");
            System.exit(1);
        }

        // check upyun password
        String upyunPassword = config.getUpyunPassword();
        if (upyunPassword.equals("")) {
            log.error("Upyun password can not be empty!");
            System.exit(1);
        }

        // check the check interval
        int checkInterval = config.getCheckInterval();
        if (checkInterval < 0) {
            log.error("Check interval cannot be less than 0!");
            System.exit(1);
        }

    }

}
