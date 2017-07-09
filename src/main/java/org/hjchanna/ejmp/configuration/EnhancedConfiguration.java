package org.hjchanna.ejmp.configuration;

/**
 *
 * @author ChannaJ
 */
public class EnhancedConfiguration {

    private ServerConfiguration serverConfiguration = new ServerConfiguration();

    private ReportConfiguration reportConfiguration = new ReportConfiguration();

    public EnhancedConfiguration() {
    }

    public ServerConfiguration getServerConfiguration() {
        return serverConfiguration;
    }

    public void setServerConfiguration(ServerConfiguration serverConfiguration) {
        this.serverConfiguration = serverConfiguration;
    }

    public ReportConfiguration getReportConfiguration() {
        return reportConfiguration;
    }

    public void setReportConfiguration(ReportConfiguration reportConfiguration) {
        this.reportConfiguration = reportConfiguration;
    }

    @Override
    public String toString() {
        return "EnhancedConfiguration{" + "serverConfiguration=" + serverConfiguration + ", reportConfiguration=" + reportConfiguration + '}';
    }

}
