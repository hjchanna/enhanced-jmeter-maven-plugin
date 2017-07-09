package org.hjchanna.ejmp.mojo;

import com.lazerycode.jmeter.json.TestConfig;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.hjchanna.ejmp.configuration.EnhancedConfiguration;
import static org.hjchanna.ejmp.mojo.RunEnhancedConfigureJMeterMojoProxy.libDirectory;
import org.hjchanna.ejmp.testrunner.ReportManager;

/**
 *
 * @author ChannaJ
 */
@Mojo(name = "generate-reports", defaultPhase = LifecyclePhase.INTEGRATION_TEST)
@Execute(goal = "configure")
public class RunEnhancedGenerateReportsMojo extends AbstractJMeterMojoProxy {

    @Parameter(defaultValue = "${enhancedConfiguration}")
    private EnhancedConfiguration enhancedConfiguration;

    @Override
    protected void doExecute() throws MojoExecutionException, MojoFailureException {
        getLog().info(" ");
        getLog().info("-------------------------------------------------------");
        getLog().info(" E N H A N C E D    R E P O R T    G E N E R A T I O N ");
        getLog().info("-------------------------------------------------------");

        //read test results from config file
        File configFile = new File(testConfigFile);
        if (!configFile.exists()) {
            getLog().info("Test config file does not exists.");
            getLog().info("Performance tests are skipped.");
            return;
        }

        TestConfig testConfig = new TestConfig(configFile);
        List<String> resultFileLocations = testConfig.getResultsFileLocations();

        //concat result files to process
        List<File> resultFiles = concatResultFiles(resultFileLocations);

        ReportManager reportManager = new ReportManager(jMeterProcessJVMSettings, enhancedConfiguration, resultFiles, pluginsCmdJarFile);
        reportManager.setWorkingDirectory(libDirectory);

        reportManager.generateReports();

    }

    private List<File> concatResultFiles(List<String> resultFileLocations) {
        List<File> resultFiles = new ArrayList<>();
        for (String resultFileLocation : resultFileLocations) {
            resultFiles.add(new File(resultFileLocation));
        }
        return resultFiles;
    }

}
