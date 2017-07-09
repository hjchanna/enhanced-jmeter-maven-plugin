package org.hjchanna.ejmp.testrunner;

import com.lazerycode.jmeter.configuration.JMeterArgumentsArray;
import com.lazerycode.jmeter.configuration.JMeterProcessJVMSettings;
import com.lazerycode.jmeter.configuration.RemoteArgumentsArrayBuilder;
import com.lazerycode.jmeter.configuration.RemoteConfiguration;
import com.lazerycode.jmeter.testrunner.JMeterProcessBuilder;
import com.lazerycode.jmeter.utility.UtilityFunctions;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.tools.ant.DirectoryScanner;
import org.hjchanna.ejmp.configuration.EnhancedConfiguration;
import org.hjchanna.ejmp.configuration.ServerConfiguration;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author ChannaJ
 */
public class EnhancedTestManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(JMeterProcessBuilder.class);

    private static final String REPORT_DIR_DATE_FORMAT = "yyyyMMdd_HHmmss";
    private final JMeterArgumentsArray baseTestArgs;
    private final File binDir;
    private final File testFilesDirectory;
    private final String[] testFilesIncluded;
    private final String[] testFilesExcluded;
    private final boolean suppressJMeterOutput;
    private final RemoteConfiguration remoteServerConfiguration;
    private final JMeterProcessJVMSettings jMeterProcessJVMSettings;
    private long postTestPauseInSeconds;
    private final String runtimeJarName;
    private File reportDirectory;
    private boolean generateReports;
    //enhanced
    private final EnhancedConfiguration enhancedConfiguration;

    public EnhancedTestManager(
            JMeterArgumentsArray baseTestArgs,
            File testFilesDirectory,
            List<String> testFilesIncluded,
            List<String> testFilesExcluded,
            RemoteConfiguration remoteServerConfiguration,
            boolean suppressJMeterOutput,
            File binDir,
            JMeterProcessJVMSettings jMeterProcessJVMSettings,
            String runtimeJarName,
            File reportDirectory,
            boolean generateReports,
            EnhancedConfiguration enhancedConfiguration
    ) {

        this.binDir = binDir;
        this.baseTestArgs = baseTestArgs;
        this.testFilesDirectory = testFilesDirectory;
        this.remoteServerConfiguration = remoteServerConfiguration;
        this.suppressJMeterOutput = suppressJMeterOutput;
        this.jMeterProcessJVMSettings = jMeterProcessJVMSettings;
        this.runtimeJarName = runtimeJarName;
        this.testFilesExcluded = testFilesExcluded.toArray(new String[0]);
        this.reportDirectory = reportDirectory;
        this.generateReports = generateReports;
        if (testFilesIncluded.size() > 0) {
            this.testFilesIncluded = testFilesIncluded.toArray(new String[0]);
        } else {
            this.testFilesIncluded = new String[]{"**/*.jmx"};
        }
        this.enhancedConfiguration = enhancedConfiguration;
    }

    public void setPostTestPauseInSeconds(String postTestPauseInSeconds) {
        Long testPause = null;
        try {
            testPause = Long.parseLong(postTestPauseInSeconds);
        } catch (NumberFormatException ex) {
            LOGGER.error("Error parsing value {} of <postTestPauseInSeconds>, will default to 0",
                    postTestPauseInSeconds, ex);
        }
        if (null == testPause) {
            testPause = 0l;
        }

        this.postTestPauseInSeconds = testPause;
    }

    public List<String> executeTests() throws MojoExecutionException {
        JMeterArgumentsArray thisTestArgs = baseTestArgs;
        List<String> tests = generateTestList();
        List<String> results = new ArrayList<String>();
        DateTimeFormatter sdf = new DateTimeFormatterBuilder().appendPattern(REPORT_DIR_DATE_FORMAT).toFormatter();
        for (String file : tests) {
            if (generateReports) {
                thisTestArgs.setReportsDirectory(reportDirectory + "/"
                        + FilenameUtils.getBaseName(file) + "_"
                        + sdf.print(new DateTime()));
            }
            if (remoteServerConfiguration != null) {
                if ((remoteServerConfiguration.isStartServersBeforeTests() && tests.get(0).equals(file)) || remoteServerConfiguration.isStartAndStopServersForEachTest()) {
                    thisTestArgs.setRemoteStart();
                    thisTestArgs.setRemoteStartServerList(remoteServerConfiguration.getServerList());
                }
                if ((remoteServerConfiguration.isStopServersAfterTests() && tests.get(tests.size() - 1).equals(file)) || remoteServerConfiguration.isStartAndStopServersForEachTest()) {
                    thisTestArgs.setRemoteStop();
                }
            }
            results.add(executeSingleTest(new File(testFilesDirectory, file), thisTestArgs));
            try {
                TimeUnit.SECONDS.sleep(postTestPauseInSeconds);
            } catch (InterruptedException ignored) {
            }
        }
        return results;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private String executeSingleTest(File test, JMeterArgumentsArray testArgs) throws MojoExecutionException {
        //Start the server if needed
        ServerConfiguration serverConfiguration = enhancedConfiguration.getServerConfiguration();
        ServerManager serverManager = null;
        if (serverConfiguration.isEnabled()) {
            serverManager
                    = new ServerManager(serverConfiguration.getStartupCommands(),
                            serverConfiguration.getHost(),
                            serverConfiguration.getPort(),
                            serverConfiguration.getValidationCount(),
                            serverConfiguration.getValidationInterval(),
                            serverConfiguration.isSuppressOutput());
        }

        testArgs.setTestFile(test, testFilesDirectory);
        //Delete results file if it already exists
        new File(testArgs.getResultsLogFileName()).delete();
        List<String> argumentsArray = testArgs.buildArgumentsArray();
        argumentsArray.addAll(buildRemoteArgs(remoteServerConfiguration));

        LOGGER.debug("JMeter is called with the following command line arguments: {}", UtilityFunctions.humanReadableCommandLineOutput(argumentsArray));
        LOGGER.info("Executing test: {}", test.getName());
        //Start the test.
        JMeterProcessBuilder JMeterProcessBuilder = new JMeterProcessBuilder(jMeterProcessJVMSettings, runtimeJarName);
        JMeterProcessBuilder.setWorkingDirectory(binDir);
        JMeterProcessBuilder.addArguments(argumentsArray);

        try {
            //start and wait for server if needed
            if (serverConfiguration.isEnabled() && serverManager != null) {
                serverManager.startServer();
            }

            final Process process = JMeterProcessBuilder.startProcess();

            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    LOGGER.info("Shutdown detected, destroying JMeter process...");
                    process.destroy();
                }
            });

            try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (suppressJMeterOutput) {
                        LOGGER.debug(line);
                    } else {
                        LOGGER.info(line);
                    }
                }
                int jMeterExitCode = process.waitFor();
                if (jMeterExitCode != 0) {
                    throw new MojoExecutionException("Test failed with exit code:" + jMeterExitCode);
                }
                LOGGER.info("Completed Test: {}", test.getAbsolutePath());

                //stop server if needed
                if (serverConfiguration.isEnabled() && serverManager != null) {
                    serverManager.stopServer();
                }
            }
        } catch (InterruptedException ex) {
            LOGGER.info(" ");
            LOGGER.info("System Exit Detected!  Stopping Test...");
            LOGGER.info(" ");
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }

        return testArgs.getResultsLogFileName();
    }

    private List<String> buildRemoteArgs(RemoteConfiguration remoteConfig) {
        if (remoteConfig == null) {
            return Collections.emptyList();
        }
        return new RemoteArgumentsArrayBuilder().buildRemoteArgumentsArray(remoteConfig.getPropertiesMap());
    }

    private List<String> generateTestList() {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(this.testFilesDirectory);
        scanner.setIncludes(this.testFilesIncluded);
        scanner.setExcludes(this.testFilesExcluded);
        scanner.scan();

        return Arrays.asList(scanner.getIncludedFiles());
    }
}
