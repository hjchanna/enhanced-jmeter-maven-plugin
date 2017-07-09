package org.hjchanna.ejmp.testrunner;

import com.lazerycode.jmeter.configuration.JMeterProcessJVMSettings;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;
import org.hjchanna.ejmp.configuration.EnhancedConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author ChannaJ
 */
public class ReportManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportManager.class);

    private int initialHeapSizeInMegaBytes;
    private int maximumHeapSizeInMegaBytes;
    private final String runtimeJarName;
    private String workingDirectory;
    private String javaRuntime;

    private final EnhancedConfiguration enhancedConfiguration;
    private final List<File> resultFiles;

    public ReportManager(JMeterProcessJVMSettings settings, EnhancedConfiguration enhancedConfiguration, List<File> resultFiles, String runtimeJarName) {
        if (null == settings) {
            settings = new JMeterProcessJVMSettings();
        }

        this.runtimeJarName = runtimeJarName;
        this.initialHeapSizeInMegaBytes = settings.getXms();
        this.maximumHeapSizeInMegaBytes = settings.getXmx();
        this.javaRuntime = settings.getJavaRuntime();

        this.enhancedConfiguration = enhancedConfiguration;
        this.resultFiles = resultFiles;
    }

    public void setWorkingDirectory(File workingDirectory) throws MojoExecutionException {
        try {
            this.workingDirectory = workingDirectory.getCanonicalPath();
        } catch (IOException ignored) {
            throw new MojoExecutionException("Unable to set working directory for JMeter process!");
        }
    }

    public void generateReports() {
        LOGGER.info("[REPORTS] Start generating report graphs");
        String[] reportTypes = this.enhancedConfiguration.getReportConfiguration().getReportTypes();

        for (String reportType : reportTypes) {
            for (File resultFile : resultFiles) {
                try {
                    generateSingleReport(reportType, resultFile);
                } catch (IOException ex) {
                    LOGGER.error("[REPORTS] Failed to generate graph " + reportType + "images for " + resultFile.getAbsolutePath());
                }
            }
        }
    }

    private void generateSingleReport(String reportType, File resultFile) throws IOException {
        String[] arguments = constructArgumentsList(reportType, resultFile);
        LOGGER.info("[REPORTS] Generating " + reportType + " graph for " + resultFile.getAbsolutePath() + " with:{}", Arrays.asList(arguments));

        ProcessBuilder processBuilder = new ProcessBuilder(arguments);
        processBuilder.redirectErrorStream(true);
        processBuilder.directory(new File(this.workingDirectory));
        Process currentProcess = processBuilder.start();

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int exitCode = currentProcess.waitFor();
                    LOGGER.info("[REPORTS] Process stopped, destroying process");
                } catch (InterruptedException ex) {
                    LOGGER.info("[REPORTS] Process interrupted");
                }
            }
        }));

        consumeStream(reportType, resultFile, currentProcess);
    }

    private String[] constructArgumentsList(String reportType, File resultFile) {
        List<String> argumentsList = new ArrayList<>();
        argumentsList.add(javaRuntime);
        argumentsList.add(MessageFormat.format("-Xms{0}M", String.valueOf(this.initialHeapSizeInMegaBytes)));
        argumentsList.add(MessageFormat.format("-Xmx{0}M", String.valueOf(this.maximumHeapSizeInMegaBytes)));

        argumentsList.add("-jar");
        argumentsList.add(runtimeJarName);
        argumentsList.add("--tool");
        argumentsList.add("Reporter");
        argumentsList.add("--generate-png");
        argumentsList.add(resultFile.getAbsolutePath() + "-" + reportType + ".png");
        argumentsList.add("--input-jtl");
        argumentsList.add(resultFile.getAbsolutePath());
        argumentsList.add("--plugin-type");
        argumentsList.add(reportType);

        return argumentsList.toArray(new String[argumentsList.size()]);
    }

    private void consumeStream(String reportType, File resultFile, Process process) {
        Thread streamConsumerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                BufferedReader bufferedReader;

                try {
                    bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        LOGGER.info("[REPORTS] " + reportType + " " + resultFile.getName() + " - " + line);
                    }
                } catch (IOException ex) {
                    //unable to consume stream, server shoud stop
                    process.destroy();
                }
            }
        });
        streamConsumerThread.start();
    }

}
