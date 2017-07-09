package org.hjchanna.ejmp.mojo;

import com.lazerycode.jmeter.mojo.RunJMeterGUIMojo;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 *
 * @author ChannaJ
 */
@Mojo(name = "gui", defaultPhase = LifecyclePhase.TEST)
@Execute(goal = "configure")
public class RunJMeterGUIMojoProxy extends RunJMeterGUIMojo {

}
