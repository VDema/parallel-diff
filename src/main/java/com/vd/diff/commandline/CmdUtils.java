package com.vd.diff.commandline;

import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

@Slf4j
public class CmdUtils {

    public static CommandLine getCmdOrExitWithHint(String[] args) {
        Options options = new Options();

        Option input1 = new Option("i1", "input first", true, "input first to cmp");
        input1.setRequired(true);
        options.addOption(input1);

        Option input2 = new Option("i2", "input second", true, "input last to cmp");
        input2.setRequired(true);
        options.addOption(input2);

        Option output = new Option("o", "output", true, "output diff result");
        output.setRequired(false);
        options.addOption(output);

        try {
            return new DefaultParser().parse(options, args);
        } catch (ParseException e) {
            log.error("Error on parsing arguments: " + Arrays.toString(args), e);
            new HelpFormatter().printHelp("parallel-diff", options);
            System.exit(1);
            throw new IllegalStateException("This line shouldn't be reached");
        }
    }
}
