package org.hjchanna.ejmp.mojo;

import com.lazerycode.jmeter.exceptions.DependencyResolutionException;
import com.lazerycode.jmeter.exceptions.IOException;
import com.lazerycode.jmeter.json.TestConfig;
import com.lazerycode.jmeter.properties.ConfigurationFiles;
import static com.lazerycode.jmeter.properties.ConfigurationFiles.GLOBAL_PROPERTIES;
import static com.lazerycode.jmeter.properties.ConfigurationFiles.JMETER_PROPERTIES;
import static com.lazerycode.jmeter.properties.ConfigurationFiles.REPORT_GENERATOR_PROPERTIES;
import static com.lazerycode.jmeter.properties.ConfigurationFiles.SAVE_SERVICE_PROPERTIES;
import static com.lazerycode.jmeter.properties.ConfigurationFiles.SYSTEM_PROPERTIES;
import static com.lazerycode.jmeter.properties.ConfigurationFiles.UPGRADE_PROPERTIES;
import static com.lazerycode.jmeter.properties.ConfigurationFiles.USER_PROPERTIES;
import static com.lazerycode.jmeter.properties.ConfigurationFiles.values;
import com.lazerycode.jmeter.properties.PropertiesFile;
import com.lazerycode.jmeter.properties.PropertiesMapping;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.apache.commons.io.FileUtils;
import static org.apache.commons.io.FileUtils.copyInputStreamToFile;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;

/**
 *
 * @author ChannaJ
 */
@Mojo(name = "configure", defaultPhase = LifecyclePhase.COMPILE)
public class RunEnhancedConfigureJMeterMojoProxy extends AbstractJMeterMojoProxy {

    @Component
    private RepositorySystem repositorySystem;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repositorySystemSession;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true)
    private List<RemoteRepository> repositoryList;

    private final String baseConfigFile = "/config.json";

    @Parameter(defaultValue = "3.2")
    private String jmeterVersion;

    @Parameter
    private List<String> jmeterArtifacts = new ArrayList<>();

    @Parameter
    private List<String> ignoredArtifacts = new ArrayList<>();

    @Parameter(defaultValue = "true")
    protected boolean downloadExtensionDependencies;

    @Parameter
    protected List<String> jmeterExtensions = new ArrayList<>();

    @Parameter(defaultValue = "false")
    protected boolean downloadJMeterDependencies;

    @Parameter(defaultValue = "false")
    protected boolean downloadOptionalDependencies;

    @Parameter(defaultValue = "true")
    protected boolean downloadLibraryDependencies;

    @Parameter
    protected List<String> junitLibraries = new ArrayList<>();

    @Parameter
    protected Map<String, String> propertiesJMeter = new HashMap<>();

    @Parameter
    protected Map<String, String> propertiesSaveService = new HashMap<>();

    @Parameter
    protected Map<String, String> propertiesReportGenerator = new HashMap<>();

    @Parameter
    protected Map<String, String> propertiesUpgrade = new HashMap<>();

    @Parameter
    protected Map<String, String> propertiesUser = new HashMap<>();

    @Parameter
    protected Map<String, String> propertiesGlobal = new HashMap<>();

    /**
     * (Java) System properties set for the test run. Properties are merged with
     * precedence into default JMeter file system.properties
     */
    @Parameter
    protected Map<String, String> propertiesSystem = new HashMap<>();

    @Parameter(defaultValue = "${basedir}/src/test/jmeter")
    protected File propertiesFilesDirectory;

    @Parameter(defaultValue = "true")
    protected boolean propertiesReplacedByCustomFiles;

    @Parameter(defaultValue = "xml")
    protected String resultsFileFormat;
    protected boolean resultsOutputIsCSVFormat = false;

    public static final String JMETER_CONFIG_ARTIFACT_NAME = "ApacheJMeter_config";
    private static final String JMETER_GROUP_ID = "org.apache.jmeter";

    protected static Artifact jmeterConfigArtifact;
    protected static File customPropertiesDirectory;
    protected static File libDirectory;
    protected static File libExtDirectory;
    protected static File libJUnitDirectory;

    @Override
    public void doExecute() throws MojoExecutionException, MojoFailureException {
        getLog().info("");
        getLog().info("-------------------------------------------------------");
        getLog().info(" C O N F I G U R I N G    E N V I R O N M E N T ");
        getLog().info("-------------------------------------------------------");

        generateJMeterDirectoryTree();
        configureJMeterArtifacts();
        populateJMeterDirectoryTree();
        copyExplicitLibraries(jmeterExtensions, libExtDirectory, downloadExtensionDependencies);
        copyExplicitLibraries(junitLibraries, libJUnitDirectory, downloadLibraryDependencies);
        configurePropertiesFiles();
        generateTestConfig();
    }

    protected void generateJMeterDirectoryTree() {
        workingDirectory = new File(jmeterDirectory, "bin");
        workingDirectory.mkdirs(); //TODO remove this, it's covered in extractConfigSettings()
        customPropertiesDirectory = new File(jmeterDirectory, "custom_properties");
        customPropertiesDirectory.mkdirs();
        libDirectory = new File(jmeterDirectory, "lib");
        libExtDirectory = new File(libDirectory, "ext");
        libExtDirectory.mkdirs();
        libJUnitDirectory = new File(libDirectory, "junit");
        libJUnitDirectory.mkdirs();
        testFilesBuildDirectory.mkdirs();
        resultsDirectory.mkdirs();
        if (generateReports) {
            reportDirectory.mkdirs();
        }
        logsDirectory.mkdirs();
    }

    protected void configurePropertiesFiles() throws MojoExecutionException, MojoFailureException {
        propertiesMap.put(JMETER_PROPERTIES, new PropertiesMapping(propertiesJMeter));
        propertiesMap.put(SAVE_SERVICE_PROPERTIES, new PropertiesMapping(propertiesSaveService));
        propertiesMap.put(UPGRADE_PROPERTIES, new PropertiesMapping(propertiesUpgrade));
        propertiesMap.put(SYSTEM_PROPERTIES, new PropertiesMapping(propertiesSystem));
        propertiesMap.put(REPORT_GENERATOR_PROPERTIES, new PropertiesMapping(propertiesReportGenerator));
        propertiesMap.put(USER_PROPERTIES, new PropertiesMapping(propertiesUser));
        propertiesMap.put(GLOBAL_PROPERTIES, new PropertiesMapping(propertiesGlobal));

        setJMeterResultFileFormat();

        for (ConfigurationFiles configurationFile : values()) {
            File suppliedPropertiesFile = new File(propertiesFilesDirectory, configurationFile.getFilename());
            File propertiesFileToWrite = new File(workingDirectory, configurationFile.getFilename());

            PropertiesFile somePropertiesFile = new PropertiesFile(jmeterConfigArtifact, configurationFile);
            somePropertiesFile.loadProvidedPropertiesIfAvailable(suppliedPropertiesFile, propertiesReplacedByCustomFiles);
            somePropertiesFile.addAndOverwriteProperties(propertiesMap.get(configurationFile).getAdditionalProperties());
            somePropertiesFile.writePropertiesToFile(propertiesFileToWrite);

            propertiesMap.get(configurationFile).setPropertiesFile(somePropertiesFile);
        }

        for (File customPropertiesFile : customPropertiesFiles) {
            PropertiesFile customProperties = new PropertiesFile(customPropertiesFile);
            String customPropertiesFilename = FilenameUtils.getBaseName(customPropertiesFile.getName()) + "-" + UUID.randomUUID().toString() + FilenameUtils.getExtension(customPropertiesFile.getName());
            customProperties.writePropertiesToFile(new File(customPropertiesDirectory, customPropertiesFilename));
        }

        setDefaultPluginProperties(workingDirectory.getAbsolutePath());
    }

    protected void generateTestConfig() throws MojoExecutionException {
        TestConfig testConfig;
        File configFile = new File(testConfigFile);
        if (!configFile.exists()) {
            try (InputStream configStream = this.getClass().getResourceAsStream(baseConfigFile)) {
                testConfig = new TestConfig(configStream);
            } catch (java.io.IOException ex) {
                throw new MojoExecutionException("Exception creating TestConfig", ex);
            }
        } else {
            testConfig = new TestConfig(configFile);
        }
        testConfig.setResultsOutputIsCSVFormat(resultsOutputIsCSVFormat);
        testConfig.setResultsFileLocations(testConfig.getResultsFileLocations());
        testConfig.writeResultFilesConfigTo(testConfigFile);
    }

    protected void setJMeterResultFileFormat() {
        if (generateReports || resultsFileFormat.toLowerCase().equals("csv")) {
            propertiesJMeter.put("jmeter.save.saveservice.output_format", "csv");
            resultsOutputIsCSVFormat = true;
        } else {
            propertiesJMeter.put("jmeter.save.saveservice.output_format", "xml");
            resultsOutputIsCSVFormat = false;
        }
    }

    public void setDefaultPluginProperties(String userDirectory) {
        //JMeter uses the system property "user.dir" to set its base working directory
        System.setProperty("user.dir", userDirectory);
        //Prevent JMeter from throwing some System.exit() calls
        System.setProperty("jmeterengine.remote.system.exit", "false");
        System.setProperty("jmeterengine.stopfail.system.exit", "false");
    }

    private void configureJMeterArtifacts() {
        if (jmeterArtifacts.isEmpty()) {
            jmeterArtifacts.add(JMETER_GROUP_ID + ":ApacheJMeter:" + jmeterVersion);
            jmeterArtifacts.add(JMETER_GROUP_ID + ":ApacheJMeter_components:" + jmeterVersion);
            jmeterArtifacts.add(JMETER_GROUP_ID + ":ApacheJMeter_config:" + jmeterVersion);
            jmeterArtifacts.add(JMETER_GROUP_ID + ":ApacheJMeter_core:" + jmeterVersion);
            jmeterArtifacts.add(JMETER_GROUP_ID + ":ApacheJMeter_ftp:" + jmeterVersion);
            jmeterArtifacts.add(JMETER_GROUP_ID + ":ApacheJMeter_functions:" + jmeterVersion);
            jmeterArtifacts.add(JMETER_GROUP_ID + ":ApacheJMeter_http:" + jmeterVersion);
            jmeterArtifacts.add(JMETER_GROUP_ID + ":ApacheJMeter_java:" + jmeterVersion);
            jmeterArtifacts.add(JMETER_GROUP_ID + ":ApacheJMeter_jdbc:" + jmeterVersion);
            jmeterArtifacts.add(JMETER_GROUP_ID + ":ApacheJMeter_jms:" + jmeterVersion);
            jmeterArtifacts.add(JMETER_GROUP_ID + ":ApacheJMeter_junit:" + jmeterVersion);
            jmeterArtifacts.add(JMETER_GROUP_ID + ":ApacheJMeter_ldap:" + jmeterVersion);
            jmeterArtifacts.add(JMETER_GROUP_ID + ":ApacheJMeter_mail:" + jmeterVersion);
            jmeterArtifacts.add(JMETER_GROUP_ID + ":ApacheJMeter_mongodb:" + jmeterVersion);
            jmeterArtifacts.add(JMETER_GROUP_ID + ":ApacheJMeter_native:" + jmeterVersion);
            jmeterArtifacts.add(JMETER_GROUP_ID + ":ApacheJMeter_tcp:" + jmeterVersion);
            //enhanced libs
            jmeterArtifacts.add("kg.apc" + ":jmeter-plugins-cmd:" + "2.1");
            jmeterArtifacts.add("kg.apc" + ":cmdrunner:" + "2.0");
            jmeterArtifacts.add("kg.apc" + ":jmeter-plugins-standard:" + "1.4.0");
        }
    }

    private void populateJMeterDirectoryTree() throws DependencyResolutionException, IOException {
        if (jmeterArtifacts.isEmpty()) {
            throw new DependencyResolutionException("No JMeter dependencies specified!, check jmeterArtifacts and jmeterVersion elements");
        }
        for (String desiredArtifact : jmeterArtifacts) {
            Artifact returnedArtifact = getArtifactResult(new DefaultArtifact(desiredArtifact));
            switch (returnedArtifact.getArtifactId()) {
                case JMETER_CONFIG_ARTIFACT_NAME:
                    jmeterConfigArtifact = returnedArtifact;
                    //TODO Could move the below elsewhere if required.
                    extractConfigSettings(jmeterConfigArtifact);
                    break;
                case "ApacheJMeter":
                    runtimeJarName = returnedArtifact.getFile().getName();
                    copyArtifact(returnedArtifact, workingDirectory);
                    copyTransitiveRuntimeDependenciesToLibDirectory(returnedArtifact, downloadJMeterDependencies);
                    break;
                case "cmdrunner":
                    pluginsCmdJarFile = returnedArtifact.getFile().getName();
                    copyArtifact(returnedArtifact, libDirectory);
                    copyTransitiveRuntimeDependenciesToLibDirectory(returnedArtifact, downloadJMeterDependencies);
                    break;
                default:
                    copyArtifact(returnedArtifact, libExtDirectory);
                    copyTransitiveRuntimeDependenciesToLibDirectory(returnedArtifact, downloadJMeterDependencies);
            }
        }

        if (confFilesDirectory.exists()) {
            CopyFilesInTestDirectory(confFilesDirectory, new File(jmeterDirectory, "bin"));
        }
    }

    private void copyExplicitLibraries(List<String> desiredArtifacts, File destination, boolean downloadDependencies) throws DependencyResolutionException, IOException {
        for (String desiredArtifact : desiredArtifacts) {
            Artifact returnedArtifact = getArtifactResult(new DefaultArtifact(desiredArtifact));
            copyArtifact(returnedArtifact, destination);
            if (downloadDependencies) {
                copyTransitiveRuntimeDependenciesToLibDirectory(returnedArtifact, true);
            }
        }
    }

    private Artifact getArtifactResult(Artifact desiredArtifact) throws DependencyResolutionException {
        ArtifactRequest artifactRequest = new ArtifactRequest();
        artifactRequest.setArtifact(desiredArtifact);
        artifactRequest.setRepositories(repositoryList);
        try {
            return repositorySystem.resolveArtifact(repositorySystemSession, artifactRequest).getArtifact();
        } catch (ArtifactResolutionException e) {
            throw new DependencyResolutionException(e.getMessage(), e);
        }
    }

    private void copyTransitiveRuntimeDependenciesToLibDirectory(Artifact artifact, boolean getDependenciesOfDependency) throws DependencyResolutionException, IOException {
        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(new Dependency(artifact, JavaScopes.RUNTIME));
        collectRequest.setRepositories(repositoryList);
        DependencyFilter dependencyFilter = DependencyFilterUtils.classpathFilter();
        DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, dependencyFilter);

        try {
            List<DependencyNode> artifactDependencyNodes = repositorySystem.resolveDependencies(repositorySystemSession, dependencyRequest).getRoot().getChildren();
            for (DependencyNode dependencyNode : artifactDependencyNodes) {
                if (getLog().isDebugEnabled()) {
                    getLog().debug("Dependency name: " + dependencyNode.toString());
                    getLog().debug("Dependency request trace: " + dependencyRequest.getCollectRequest().getTrace().toString());
                    getLog().debug("-------------------------------------------------------");
                }
                if (downloadOptionalDependencies || !dependencyNode.getDependency().isOptional()) {
                    Artifact returnedArtifact = getArtifactResult(dependencyNode.getArtifact());
                    if (!returnedArtifact.getArtifactId().startsWith("ApacheJMeter_")) {
                        copyArtifact(returnedArtifact, libDirectory);
                    }

                    if (getDependenciesOfDependency) {
                        copyTransitiveRuntimeDependenciesToLibDirectory(returnedArtifact, true);
                    }
                }
            }
        } catch (org.eclipse.aether.resolution.DependencyResolutionException e) {
            throw new DependencyResolutionException(e.getMessage(), e);
        }
    }

    private void copyArtifact(Artifact artifact, File destinationDirectory) throws IOException, DependencyResolutionException {
        for (String ignoredArtifact : ignoredArtifacts) {
            Artifact artifactToIgnore = getArtifactResult(new DefaultArtifact(ignoredArtifact));
            if (artifact.getFile().getName().equals(artifactToIgnore.getFile().getName())) {
                getLog().debug(artifact.getFile().getName() + " has not been copied over because it is in the ignore list.");
                return;
            }
        }
        try {
            File artifactToCopy = new File(destinationDirectory + File.separator + artifact.getFile().getName());
            getLog().debug("Checking: " + artifactToCopy.getAbsolutePath() + "...");
            if (!artifactToCopy.exists()) {
                getLog().debug("Copying: " + artifactToCopy.getAbsolutePath() + "...");
                FileUtils.copyFileToDirectory(artifact.getFile(), destinationDirectory);
            }
        } catch (java.io.IOException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    private void extractConfigSettings(Artifact artifact) throws IOException {
        try (JarFile configSettings = new JarFile(artifact.getFile())) {
            Enumeration<JarEntry> entries = configSettings.entries();
            while (entries.hasMoreElements()) {
                JarEntry jarFileEntry = entries.nextElement();
                // Only interested in files in the /bin directory that are not properties files
                if (!jarFileEntry.isDirectory() && jarFileEntry.getName().startsWith("bin") && !jarFileEntry.getName().endsWith(".properties")) {
                    File fileToCreate = new File(jmeterDirectory, jarFileEntry.getName());
                    copyInputStreamToFile(configSettings.getInputStream(jarFileEntry), fileToCreate);
                } else if (!jarFileEntry.isDirectory() && jarFileEntry.getName().startsWith("bin/report-template")) {
                    File fileToCreate = new File(jmeterDirectory, jarFileEntry.getName());
                    copyInputStreamToFile(configSettings.getInputStream(jarFileEntry), fileToCreate);
                }
            }
        } catch (java.io.IOException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    static void CopyFilesInTestDirectory(File sourceDirectory, File destinationDirectory) throws IOException {
        try {
            FileUtils.copyDirectory(sourceDirectory, destinationDirectory);
        } catch (java.io.IOException e) {
            throw new IOException(e.getMessage(), e);
        }

    }
}
