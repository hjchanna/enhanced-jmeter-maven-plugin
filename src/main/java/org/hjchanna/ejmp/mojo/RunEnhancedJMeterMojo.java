package org.hjchanna.ejmp.mojo;

import com.lazerycode.jmeter.exceptions.IOException;
import com.lazerycode.jmeter.json.TestConfig;
import com.lazerycode.jmeter.mojo.RunJMeterMojo;
import java.io.File;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.hjchanna.ejmp.configuration.EnhancedConfiguration;
import org.hjchanna.ejmp.testrunner.EnhancedTestManager;

/**
 *
 * @author ChannaJ
 */
@Mojo(name = "enhanced-jmeter", defaultPhase = LifecyclePhase.INTEGRATION_TEST)
@Execute(goal = "configure")
public class RunEnhancedJMeterMojo extends RunJMeterMojo {

    @Parameter(defaultValue = "${enhancedConfiguration}")
    private EnhancedConfiguration enhancedConfiguration;

    @Override
    public void doExecute() throws MojoExecutionException, MojoFailureException {
        getLog().info(" ");
        getLog().info("-------------------------------------------------------");
        getLog().info(" E N H A N C E D    P E R F O R M A N C E    T E S T S ");
        getLog().info("-------------------------------------------------------");

        if (!testFilesDirectory.exists()) {
            getLog().info("<testFilesDirectory>" + testFilesDirectory.getAbsolutePath() + "</testFilesDirectory> does not exist...");
            getLog().info("Performance tests are skipped.");
            return;
        }

        TestConfig testConfig = new TestConfig(new File(testConfigFile));
        initialiseJMeterArgumentsArray(true, testConfig.getResultsOutputIsCSVFormat());

        if (null != remoteConfig) {
            remoteConfig.setPropertiesMap(propertiesMap);
        }

        CopyFilesInTestDirectory(testFilesDirectory, testFilesBuildDirectory);

        EnhancedTestManager jMeterTestManager
                = new EnhancedTestManager(testArgs,
                        testFilesBuildDirectory,
                        testFilesIncluded,
                        testFilesExcluded,
                        remoteConfig,
                        suppressJMeterOutput,
                        workingDirectory,
                        jMeterProcessJVMSettings,
                        runtimeJarName,
                        reportDirectory,
                        generateReports,
                        enhancedConfiguration);
        
        jMeterTestManager.setPostTestPauseInSeconds(postTestPauseInSeconds);
        getLog().info(" ");
        if (proxyConfig != null) {
            getLog().info(this.proxyConfig.toString());
        }

        testConfig.setResultsFileLocations(jMeterTestManager.executeTests());
        testConfig.writeResultFilesConfigTo(testConfigFile);
    }

    static void CopyFilesInTestDirectory(File sourceDirectory, File destinationDirectory) throws IOException {
        try {
            FileUtils.copyDirectory(sourceDirectory, destinationDirectory);
        } catch (java.io.IOException e) {
            throw new IOException(e.getMessage(), e);
        }
    }
}
