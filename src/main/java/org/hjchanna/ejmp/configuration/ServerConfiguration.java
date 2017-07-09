package org.hjchanna.ejmp.configuration;

/**
 *
 * @author ChannaJ
 */
public class ServerConfiguration {

    //server start commands
    private String[] startupCommands = new String[]{};
    //server host
    private String host = "localhost";
    //server port
    private int port = 8080;
    //server availability check count
    private int validationCount = Integer.MAX_VALUE;
    //server availability check durations
    private long validationInterval = 1000L;
    //log level of server output
    private boolean suppressOutput = false;
    //
    private boolean enabled = false;

    public ServerConfiguration() {
    }

    public String[] getStartupCommands() {
        return startupCommands;
    }

    public void setStartupCommands(String[] startupCommands) {
        this.startupCommands = startupCommands;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getValidationCount() {
        return validationCount;
    }

    public void setValidationCount(int validationCount) {
        this.validationCount = validationCount;
    }

    public long getValidationInterval() {
        return validationInterval;
    }

    public void setValidationInterval(long validationInterval) {
        this.validationInterval = validationInterval;
    }

    public boolean isSuppressOutput() {
        return suppressOutput;
    }

    public void setSuppressOutput(boolean suppressOutput) {
        this.suppressOutput = suppressOutput;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return "ServerConfiguration{" + "startupCommands=" + startupCommands + ", host=" + host + ", port=" + port + ", validationCount=" + validationCount + ", validationInterval=" + validationInterval + ", suppressOutput=" + suppressOutput + ", enabled=" + enabled + '}';
    }

}
