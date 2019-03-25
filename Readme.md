# **Parallel-diff Project (POC)**

**Fetching**

`git clone https://github.com/Money-Honey/parallel-diff.git`

**Building**

Run next command:

`./gradlew clean build`
During the build tests will be run. There is test which run a diff between files from the task.

jar file will be accessible by the path `build/libs/parallel-diff-1.0-SNAPSHOT.jar`

**Running**

Just a copy the jar `parallel-diff-1.0-SNAPSHOT.jar` into the other (empty) directory and then run it, or specify path to this `jar`:

Example: 
`java -jar parallel-diff-1.0-SNAPSHOT.jar -i1 aus-28-2002 -i2 aws-28-2002 -o out.txt`

`aus-28-2002` and `aws-28-2002` files are expected to be in the same directory. 
`-i1` and `-i2` are required options and consumes relative paths to the files to diff. `-o` is optional and will be defaulted.

**Verifying**

`diff` directory will be created with diff chunks. 
`out.txt` or defaulter `eventsDiff.txt` will be created with merged chunks. 

Also it's possible to check the logs from the console output or `$HOME` file with detailed statistic and metrics.
Example:
`2019-03-25 01:33:00 INFO  DiffService:91 - Total diff result: DiffReport(leftSkippedRows=225, rightSkippedRows=222, timeElapsed=10744, metrics=DiffMetrics(newLeftRowsAmount=65, newRightRowsAmount=2, differentEventsRowsAmount=1480, sameLines=498903)). Within Threads: 7. Approximate diff time: 1534`
Where ` Within Threads: 7` means that 7 workers were used in parallel and `Approximate diff time: 1534` means that it took `1534ms`

Tested on MacOs with i7 processor