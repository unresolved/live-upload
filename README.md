# live-upload

A upyun synchronization tool

## Usage

- Prepare a jre version at least 11
- Invoke `java -jar live-upload.jar --config config.properties`
- Edit `config.properties` file in current directory
- Re invoke `java -jar live-upload.jar --config config.properties`
- Enjoy!

## Configuration file

> When specified configuration file not exits, LiveUpload will auto create it, You should edit it after program run completed.

```properties

# the source directory
# e.g. D:\backups\demo
source-directory=<SOURCE DIRECTORY>

# the destination path
# e.g. /uploads
destination-path=<DESTINATION PATH>

# the upyun bucket
upyun-bucket=<YOUR UPYUN BUCKET>

# the upyun operator
upyun-operator=<YOUR UPYUN OPERATOR>

# the upyun password
upyun-password=<YOUR UPYUN PASSWORD>

# schedule each 5 minutes do this task
check-interval=300


```