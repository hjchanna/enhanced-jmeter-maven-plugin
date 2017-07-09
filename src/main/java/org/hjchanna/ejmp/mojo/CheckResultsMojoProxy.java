package org.hjchanna.ejmp.mojo;

import com.lazerycode.jmeter.mojo.CheckResultsMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 *
 * @author ChannaJ
 */
@Mojo(name = "results", defaultPhase = LifecyclePhase.VERIFY)
public class CheckResultsMojoProxy extends CheckResultsMojo {

}
