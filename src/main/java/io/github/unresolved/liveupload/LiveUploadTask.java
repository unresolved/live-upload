package io.github.unresolved.liveupload;

import com.upyun.RestManager;
import com.upyun.UpException;
import com.upyun.UpYunUtils;
import com.upyun.RestManager.PARAMS;
import lombok.extern.log4j.Log4j;
import okhttp3.Response;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Log4j
public class LiveUploadTask implements Runnable {

    private static final String PATH_SEPARATOR = "/";
    private static final String DIR_ITER_EOF = "g2gCZAAEbmV4dGQAA2VvZg";

    private final Config config;
    private final RestManager manager;

    public LiveUploadTask(Config config) {
        this.config = config;
        manager = new RestManager(config.getUpyunBucket(), config.getUpyunOperator(), config.getUpyunPassword());
    }

    @Override
    public void run() {
        while (true) {
            log.info("Start planning task...");
            try {
                doTask();
            } catch (Exception e) {
                log.error("Error while executing task!", e);
            }
            log.info("End of task execution...");
            // schedule at next time
            try {
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.SECOND, config.getCheckInterval());
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                log.info("Next execution time: " + format.format(calendar.getTime()));
                Thread.sleep(TimeUnit.SECONDS.toMillis(config.getCheckInterval()));
            } catch (InterruptedException e) {
                log.error("Thread interrupted!", e);
            }
        }
    }

    private void doTask() {
        List<String> remoteFileList = getRemoteFileList();
        assert remoteFileList != null;
        File srcDir = new File(config.getSourceDirectory());

        List<String> uploadFileList = new ArrayList<>();
        for (String localFile : Objects.requireNonNull(srcDir.list())) {
            if (!remoteFileList.contains(localFile)) {
                uploadFileList.add(localFile);
            }
        }

        for (int i = 0; i < uploadFileList.size(); i++) {
            String uploadFile = uploadFileList.get(i);

            log.info(String.format("[%d of %d] Trying to upload file %s...", i + 1, uploadFileList.size(), uploadFile));
            File file = new File(srcDir, uploadFile);
            log.info("Local file: " + file.getPath());

            Map<String, String> params = new HashMap<>();
            // Content-MD5 & content-md5 is different!
            params.put(PARAMS.CONTENT_MD5.getValue(), UpYunUtils.md5(file, 1024));
            try {
                Response response = manager.writeFile(config.getDestinationPath() + PATH_SEPARATOR + uploadFile, file, params);
                if (!response.isSuccessful()) {
                    log.error("Failed to upload file " + uploadFile + ": "
                            + Objects.requireNonNull(response.body()).string());
                } else {
                    log.info("Upload " + uploadFile + " success.");
                }
            } catch (IOException | UpException e) {
                log.error("Error while uploading file!", e);
            }
        }

    }

    private List<String> getRemoteFileList() {
        try {
            String url = config.getDestinationPath() + PATH_SEPARATOR;
            List<String> fileList = new ArrayList<>();

            String iter = null;
            do {
                Map<String, String> params = new HashMap<>();
                params.put(PARAMS.ACCEPT.getValue(), "application/json");
                if (iter != null) {

                    params.put(PARAMS.X_LIST_ITER.getValue(), iter);
                }
                Response response = manager.readDirIter(url, params);
                if (response.code() != 200) {
                    return null;
                }

                String responseString = Objects.requireNonNull(response.body()).string();
//                log.info("response.body().string() = " + responseString);

                JSONObject responseJson = new JSONObject(responseString);
                iter = responseJson.getString("iter");

                responseJson.getJSONArray("files").forEach(fileObject -> {
                    JSONObject fileJson = (JSONObject) fileObject;

//                    log.info("fileJson = " + fileJson);

                    if (!fileJson.getString("type").equals("folder")) {
                        fileList.add(fileJson.getString("name"));
                    }
                });
            } while (!iter.equals(DIR_ITER_EOF));

            return fileList;
        } catch (IOException | UpException e) {
            log.error("Error while getting file list from remote!", e);
        }

        return null;
    }

}
