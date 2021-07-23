package io.github.unresolved.liveupload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class Config {

    private String sourceDirectory;
    private String destinationPath;
    private String upyunOperator;
    private String upyunPassword;
    private String upyunBucket;
    private int checkInterval;

}
