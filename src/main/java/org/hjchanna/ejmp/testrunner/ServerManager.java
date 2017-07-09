package org.hjchanna.ejmp.testrunner;

import com.lazerycode.jmeter.utility.UtilityFunctions;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author ChannaJ
 */
public class ServerManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerManager.class);

    //server start commands
    private final String[] startupCommands;

    //server host
    private final String serverHost;

    //server port
    private final int serverPort;

    //server availability check count
    private final int validationCount;

    //server availability check durations
    private final long validationInterval;

    //level of server output
    private final boolean suppressServerOutput;

    //current process of the server
    private Process currentServerProcess;

    public ServerManager(String[] startupCommands, String serverHost, int serverPort, int validationCount, long validationInterval, boolean suppressServerOutput) {
        this.startupCommands = startupCommands;
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.validationCount = validationCount;
        this.validationInterval = validationInterval;
        this.suppressServerOutput = suppressServerOutput;
    }

    public void startServer() throws IOException {
        LOGGER.info("[SERVER] Server is starting with the following command line arguments: {}",
                UtilityFunctions.humanReadableCommandLineOutput(Arrays.asList(startupCommands)));

        ProcessBuilder serverProcessBuilder = new ProcessBuilder(startupCommands);
        currentServerProcess = serverProcessBuilder.start();

        //shutdown hook for the server
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int exitCode = currentServerProcess.waitFor();
                    LOGGER.info("[SERVER] Server " + serverHost + " stopped, destroying server process");
                } catch (InterruptedException ex) {
                    LOGGER.info("[SERVER] Server " + serverHost + " interrupted");
                }
            }
        }));

        //consume out and error streams
        consumeStream("OUT", currentServerProcess.getInputStream());
        consumeStream("ERROR", currentServerProcess.getErrorStream());

        //wait until server start successfully
        waitForServer();
    }

    public void stopServer() {
        LOGGER.info("[SERVER] Server is stopping");
        currentServerProcess.destroy();
    }

    public boolean isServerAlive() {
        return currentServerProcess.isAlive();
    }

    private void consumeStream(String name, InputStream inputStream) {
        Thread streamConsumerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                BufferedReader bufferedReader;

                try {
                    bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        if (name.equalsIgnoreCase("ERRROR")) {
                            LOGGER.error("[SERVER]\t" + line);
                        } else {
                            if (suppressServerOutput) {
                                LOGGER.debug("[SERVER]\t" + line);
                            } else {
                                LOGGER.info("[SERVER]\t" + line);
                            }
                        }
                    }
                } catch (IOException ex) {
                    //unable to consume stream, server shoud stop
                    currentServerProcess.destroy();
                }
            }
        });
        streamConsumerThread.start();
    }

    private void waitForServer() throws IOException {
        int attemptCount = this.validationCount;
        for (int attemptIndex = 0; attemptIndex < attemptCount; attemptIndex++) {
            try {
                Thread.sleep(validationInterval);

                LOGGER.info("[SERVER] Validating server " + serverHost + "(" + serverPort + ")" + ", attempt [" + (attemptIndex + 1) + "]");
                Socket socket = new Socket(serverHost, serverPort);

                if (socket.isConnected()) {
                    //break and continue
                    LOGGER.info("[SERVER] Server validation success, attempt [" + (attemptIndex + 1) + "]");
                    attemptIndex = attemptCount;
                    return;
                }
            } catch (InterruptedException ex) {
                //shall we try once again
                attemptCount++;
            } catch (IOException ex) {
                //if attempt failes
                LOGGER.info("[SERVER] Server validation failed, attempt [" + (attemptIndex + 1) + "]");
            }
        }

        //failed to connect to the server, so let's stop the server process
        currentServerProcess.destroy();
        LOGGER.info("[SERVER] Server didn't start as expected.");
        throw new IOException("Server didn't start as expected.");
    }

}
