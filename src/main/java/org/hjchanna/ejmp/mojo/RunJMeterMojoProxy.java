package org.hjchanna.ejmp.mojo;

import com.lazerycode.jmeter.mojo.RunJMeterMojo;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 *
 * @author ChannaJ
 */
@Mojo(name = "jmeter", defaultPhase = LifecyclePhase.INTEGRATION_TEST)
@Execute(goal = "configure")
public class RunJMeterMojoProxy extends RunJMeterMojo {

}
