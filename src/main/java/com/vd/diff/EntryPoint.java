package com.vd.diff;

import com.vd.diff.commandline.CmdUtils;
import com.vd.diff.compare.DiffService;
import java.io.File;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class EntryPoint {

    private static final String DEFAULT_DIFF_FILE_PATH = "eventsDiff.txt";

    public static void main(String[] args) throws IOException {
        CommandLine cmd = CmdUtils.getCmdOrExitWithHint(args);

        String inLeftFilePath = cmd.getOptionValue("i1");
        String inRightFilePath = cmd.getOptionValue("i2");
        String outFilePath = cmd.getOptionValue("output");

        File fileA = new File(inLeftFilePath);
        File fileB = new File(inRightFilePath);

        File diff = new DiffService().diffFiles(
                fileA,
                fileB,
                StringUtils.firstNonBlank(outFilePath, DEFAULT_DIFF_FILE_PATH)
        );
        log.info("The diff file path is: {}. Size of file is: {}", diff.getAbsolutePath(), diff.length());
    }
}
