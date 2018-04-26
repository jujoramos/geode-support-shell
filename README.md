[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0) 

# GEODE Support Shell
1. [Overview](#overview)
2. [Building From Source](#building)
3. [Configuration Properties](#configuring)
4. [Execution of Commands](#commands)


## <a name="overview"></a>Overview

[Apache Geode](http://geode.apache.org/), also known as  [Pivotal GemFire](https://network.pivotal.io/products/pivotal-gemfire), 
is a data management platform that provides real-time, consistent access to data-intensive 
applications throughout widely distributed cloud architectures.

The [Visual Statistics Display (VSD)](https://network.pivotal.io/products/pivotal-gemfire) is an 
utility that reads GemFire/Geofe statistics and produces graphical displays for further analysis.

A Geode/GemFire cluster can be composed of a few or a lot of members, each one having its own configuration 
files and also generating its own set of logs and statistics for troubleshooting and analysis.
When dealing with large distributed systems running for a considerable period of time, however, the 
effort spent just by arranging the files required to analyze an incident can be extremely time 
consuming, and so does the general health asses executed on each file to check whether the 
configuration is the same for all members, whether there are JVM pauses making members 
unresponsive, whether the memory is correctly sized to a fixed value for each heap space; the list 
goes on and on...

A big percentage of these repetitive tasks could (and should) be automated, improving the 
productivity of the analysis itself, and also reducing the amount of human mistakes inherent to 
executing the same thing over and over again. The goal of this project is to build such a tool. 


## <a name="building"></a>Building From Source

All platforms require a Java installation with JDK 1.8 or more recent version. The JAVA\_HOME environment variable can be set as below:

| Platform | Command |
| :---: | --- |
|  Unix    | ``export JAVA_HOME=/usr/java/jdk1.8.0_121``            |
|  OSX     | ``export JAVA_HOME=`/usr/libexec/java_home -v 1.8``    |
|  Windows | ``set JAVA_HOME="C:\Program Files\Java\jdk1.8.0_121"`` |

Clone the current repository in your local environment and, within the directory containing the 
source code, run gradle build:
```
$ ./gradlew build
```

Once the build completes, the project files will be installed at `build/libs`. Verify the 
installation by invoking the `java -jar` command:
```
    $ java -jar build/libs/geode-support-shell-X.Y.Z.jar
```

The output should be as follows, and the available commands can be executed from the shell prompt.
```

  /$$$$$$                            /$$                  /$$$$$$                                                      /$$
 /$$__  $$                          | $$                 /$$__  $$                                                    | $$
| $$  \__/  /$$$$$$   /$$$$$$   /$$$$$$$  /$$$$$$       | $$  \__/ /$$   /$$  /$$$$$$   /$$$$$$   /$$$$$$   /$$$$$$  /$$$$$$
| $$ /$$$$ /$$__  $$ /$$__  $$ /$$__  $$ /$$__  $$      |  $$$$$$ | $$  | $$ /$$__  $$ /$$__  $$ /$$__  $$ /$$__  $$|_  $$_/
| $$|_  $$| $$$$$$$$| $$  \ $$| $$  | $$| $$$$$$$$       \____  $$| $$  | $$| $$  \ $$| $$  \ $$| $$  \ $$| $$  \__/  | $$
| $$  \ $$| $$_____/| $$  | $$| $$  | $$| $$_____/       /$$  \ $$| $$  | $$| $$  | $$| $$  | $$| $$  | $$| $$        | $$ /$$
|  $$$$$$/|  $$$$$$$|  $$$$$$/|  $$$$$$$|  $$$$$$$      |  $$$$$$/|  $$$$$$/| $$$$$$$/| $$$$$$$/|  $$$$$$/| $$        |  $$$$/
 \______/  \_______/ \______/  \_______/ \_______/       \______/  \______/ | $$____/ | $$____/  \______/ |__/         \___/
                                                                            | $$      | $$
                                                                            | $$      | $$
                                                                            |__/      |__/

geode-support-shell>
```


## <a name="configuring"></a>Configuration Properties

The application is built on top of [Spring-Boot](https://projects.spring.io/spring-boot/), so all 
configuration properties can be easily changed during startup using the following syntax:
```
$ java -jar geode-support-shell-X.Y.Z.jar --property.name=property.value
```

Below is a list of the configurable properties.

| Property |  Default Value |Description |
| :---: | :--- | :--- |
| logging.file | geode-support-shell.log | Location of a file where logs will be written |
| app.vsd.home | --- | Path to the folder where the Visual Statistics Display Tool (VSD) is installed.
| app.history.file | .geode-support-shell.history | Location of a file where history of commands executed will be saved |


## <a name="commands"></a>Execution of Commands

All commands included are self descriptive, more information about specific commands can be 
obtained with the assistance of the `help` command. The list below contains a detailed  description 
about each command, its arguments and what it should be used for.

### help

Displays all available commands. When using with a command name as an argument, displays help for 
the specified command and its arguments.

##### Syntax:
```
geode-support-shell>help clear

NAME
	clear - Clear the shell screen.

SYNOPSYS
	clear 
```

##### Parameters:

| Name | Description |
| :--- | :--- |
|  --C, --command | *Optional*. The command to obtain help for. |


### show statistics metadata

Displays general information about the statistics file specified, or about the full set of statistics 
files contained within the directory specified. When a folder is specified as the `--path` argument, 
the command will recursively iterate and parse all files matching the extension `.gfs` or `.gz`. The 
command aims to provide an overview of the statistics files, allowing the user to quickly asses the 
version and time frame covered by the file.

The result includes one or two tables, depending on whether the parsing of the different statistics 
files fails or succeeds.

The _Results_ table includes a list of the statistics files for which the parsing succeeded, along 
with the _File Name_ (relative to the original path), the _Product Version_ on which the member that 
created the statistics file was running, the underlying _Operating System_ installed on the host the 
member that created the statistics file was running, the original _Time Zone_ on which the file was 
created and the interval of time covered by the statistics file (_Start Time_ and _Finish Time_). 
It's worth noting that both the _Start Time_ and _Finish Time_ are shown using the original _Time 
Zone_ from the statistics file, unless a different _Time Zone_ has been specified when executing the 
command, in which case both timestamps are converted to the specified _Time Zone_. It should also be 
mentioned that the _Finish  Time_, unlike the _Start Time_ which is written in the file by the 
GemFire/Geode member directly, it's measured by retrieving the last available sample for a well 
known and always existing statistic (`VMStats.cpus`). 

The _Errors_ table includes a list of the statistics files for which the parsing failed, along with 
the _File Name_ (relative to the original path) and the _Error Description_.

##### Syntax:
```
$ geode-support-shell>show statistics metadata --path ./samples
+---------------------+---------------+----------------+---------------+-----------------------+------------------------+
|File Name            |Product Version|Operating System|Time Zone      |Start Time             |Finish Time             |
+---------------------+---------------+----------------+---------------+-----------------------+------------------------+
|/cluster1-locator.gz |GemFire 9.3.0  |Mac OS X 10.13.3|Europe/Dublin  |Mar 22, 2018 2:06:09 PM|Mar 22, 2018 3:17:05 PM |
+---------------------+---------------+----------------+---------------+-----------------------+------------------------+
|/cluster2-locator.gz |GemFire 8.2.8  |Mac OS X 10.13.3|America/Chicago|Mar 22, 2018 9:06:24 AM|Mar 22, 2018 10:17:04 AM|
+---------------------+---------------+----------------+---------------+-----------------------+------------------------+
|/sampleClient.gfs    |GemFire 9.1.0  |Mac OS X 10.13.3|Europe/Dublin  |Mar 22, 2018 2:06:51 PM|Mar 22, 2018 3:07:08 PM |
+---------------------+---------------+----------------+---------------+-----------------------+------------------------+


$ geode-support-shell>show statistics metadata --path ./samples --timeZone "America/Buenos_Aires"
+---------------------+---------------+----------------+---------------+--------------------------------+---------------------------------+
|File Name            |Product Version|Operating System|Time Zone      |Start Time[America/Buenos_Aires]|Finish Time[America/Buenos_Aires]|
+---------------------+---------------+----------------+---------------+--------------------------------+---------------------------------+
|/cluster1-locator.gz |GemFire 9.3.0  |Mac OS X 10.13.3|Europe/Dublin  |Mar 22, 2018 11:06:09 AM        |Mar 22, 2018 12:17:05 PM         |
+---------------------+---------------+----------------+---------------+--------------------------------+---------------------------------+
|/cluster2-locator.gz |GemFire 8.2.8  |Mac OS X 10.13.3|America/Chicago|Mar 22, 2018 11:06:24 AM        |Mar 22, 2018 12:17:04 PM         |
+---------------------+---------------+----------------+---------------+--------------------------------+---------------------------------+
|/sampleClient.gfs    |GemFire 9.1.0  |Mac OS X 10.13.3|Europe/Dublin  |Mar 22, 2018 11:06:51 AM        |Mar 22, 2018 12:07:08 PM         |
+---------------------+---------------+----------------+---------------+--------------------------------+---------------------------------+


$ geode-support-shell>show statistics metadata --path ./corrupted/unparseableFile.gfs 
+--------------------+-------------------------------+
|File Name           |Error Description              |
+--------------------+-------------------------------+
|/unparseableFile.gfs|Unexpected token byte value: 67|
+--------------------+-------------------------------+
```

##### Parameters:

| Name | Description |
| :--- | :--- |
|  --path | *Mandatory*. Statistics file, or directory to scan for statistics files. |
|  --timeZone | *Optional*. Time Zone Id to use when showing results. If not set, the default from the statistics file will be used. Useful when analyzing files written in different time zones (generally true for clusters replicating data over the WAN).|


### filter statistics by date-time

Scans the statistics files contained within the `sourceFolder`, and copies them to different 
directories depending on whether they match (`matchingFolder`) the time frame specified as the 
filter or not (`nonMatchingFolder`). The command aims to quickly setup an organized folder structure 
where files covering a time frame of interest are grouped together, allowing the user to start the 
proper analysis on a valid set instead of manually checking and moving the files one by one.

The result includes one or two tables, depending on whether the parsing of the different statistics 
files fails or succeeds.

The _Results_ table includes a list of the statistics files for which the parsing succeeded, along 
with the _File Name_ (relative to the original path), and the flag _Matches_, which states whether 
the file matches the specified time frame or not.
In general the analysis is executed on different members that belong to the same distributed system, 
so it's expected for all of them to be on the same _Time Zone_. As such, the time frame specified as 
the filter is compared against the time interval covered by the statistics file using the same _Time 
Zone_ as the one of the statistics file itself; in short: the user must set the the filter without 
doing any manual conversion between _Time Zones_. If, on the other hand, the files cover members from 
different _Time Zones_ (different clusters replicated through WAN probably), the user can specify 
a unique _Time Zone_ to use, in which case all timestamps from the statistics files will be converted 
prior to executing the comparision.

The _Errors_ table includes a list of the statistics files for which there was an error, either 
parsing the file or copying it to the corresponding folder, along with the _File Name_ (relative to 
the original path) and the _Error Description_.

##### Syntax:
```
$ tree
.
└── samples
    ├── cluster1-locator.gz
    ├── cluster2-locator.gz
    └── sampleClient.gfs

$ geode-support-shell>filter statistics by date-time --sourceFolder ./samples --matchingFolder ./relevant --nonMatchingFolder ./irrelevant --year 2018 --month 3 --day 22 --hour 15
+--------------------+-------+
|File Name           |Matches|
+--------------------+-------+
|/cluster1-locator.gz|true   |
+--------------------+-------+
|/cluster2-locator.gz|false  |
+--------------------+-------+
|/sampleClient.gfs   |true   |
+--------------------+-------+

$ tree
.
├── irrelevant
│   └── cluster2-locator.gz
├── relevant
│   ├── cluster1-locator.gz
│   └── sampleClient.gfs
└── samples
    ├── cluster1-locator.gz
    ├── cluster2-locator.gz
    └── sampleClient.gfs
----------------------

$ tree
.
└── samples
    ├── cluster1-locator.gz
    ├── cluster2-locator.gz
    └── sampleClient.gfs

$ geode-support-shell>filter statistics by date-time --sourceFolder ./samples --matchingFolder ./relevant --nonMatchingFolder ./irrelevant --year 2018 --month 3 --day 22 --hour 11 --minute 30 --timeZone "America/Buenos_Aires"
+--------------------+-------+
|File Name           |Matches|
+--------------------+-------+
|/cluster1-locator.gz|true   |
+--------------------+-------+
|/cluster2-locator.gz|true   |
+--------------------+-------+
|/sampleClient.gfs   |true   |
+--------------------+-------+

$ tree
.
├── relevant
│   ├── cluster1-locator.gz
│   ├── cluster2-locator.gz
│   └── sampleClient.gfs
└── samples
    ├── cluster1-locator.gz
    ├── cluster2-locator.gz
    └── sampleClient.gfs
```

##### Parameters:

| Name | Description |
| :--- | :--- |
|  --year | *Mandatory*. Year [2010 - ...] to look for within the statistics samples. |
|  --month | *Mandatory*. Month of Year [1 - 12] to look for within the statistics samples. |
|  --day | *Mandatory*. Day of Month [1 - 31] to look for within the statistics samples. |
|  --hour | *Optional*. Hour of day [00 - 23] to look for within the statistics samples. |
|  --minute | *Optional*. Minute of Hour [00 - 59] to look for within the statistics samples. |
|  --second | *Optional*. Second of minute [00 - 59] to look for within the statistics samples. |
|  --sourceFolder | *Mandatory*. Directory to scan for statistics. |
|  --matchingFolder | *Mandatory*. Directory where matching files should be copied to. |
|  --nonMatchingFolder | *Mandatory*. Directory where non matching files should be copied to. |
|  --timeZone | *Optional*. Time Zone Id to use when filtering. If not set, the Time Zone from the statistics file will be used. Useful when filtering a set of statistics files from different time zones. |


### start vsd
 
Starts the Visual Statistics Display Tool (VSD) and loads the specified statistics (decompress them 
if needed) into the tool for further analysis. The VSD tool is an external library which does not get 
shipped with this project and, as such, it should be installed on the local system before running 
this command, otherwise it will fail stating that the tool can not be found.

There are three different ways that can be used to configure the folder where VSD is installed:
* Using the `--vsdHome` command argument.
* Setting the system property `app.vsd.home` when starting the application.
* Setting the environment variable `VSD_HOME` before starting the application.

The result includes a message stating whether VSD process could be launched or not, along with an 
additional error table, depending on whether the decompression of the compressed statistics files 
fails or succeeds.

##### Syntax:
```
$ tree
.
└── samples
    ├── unparseableFile.gz
    ├── cluster1-locator.gfs
    ├── cluster2-locator.gz
    └── sampleClient.gfs

$ geode-support-shell>start vsd --sourcePath ./samples --vsdHome /apps/vsd --decompressionFolder ./decompressed --timeZone "America/Buenos_Aires"
 +-----------------------------+------------------+
 |File Name                    |Error Description |
 +-----------------------------+------------------+
 |/unparseableFile.gz          |Not in GZIP format|
 +-----------------------------+------------------+
 
 Visual Statistics Display Tool (VSD) successfully started.

$ tree
.
├── decompressed
│   └── cluster2-locator.gfs
└── samples
    ├── unparseableFile.gz
    ├── cluster1-locator.gfs
    ├── cluster2-locator.gz
    └── sampleClient.gfs
```

##### Parameters:

| Name | Description |
| :--- | :--- |
|  --sourcePath | *Optional*. Path to directory to scan for statistics files. |
|  --vsdHome | *Optional*. Path to the Visual Statistics Display Tool (VSD) Installation Directory. |
|  --decompressionFolder | *Optional*. Path to the folder where decompressed files should be located. If none is specified, compressed files are left alone and won't be loaded into VSD. |
|  --timeZone | *Optional*. Time Zone to set as system environment variable. This will be used by the Visual Statistics Display Tool (VSD) when showing data. If not set, none is used. |
