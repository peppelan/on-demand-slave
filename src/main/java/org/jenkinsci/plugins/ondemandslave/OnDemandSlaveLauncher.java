package org.jenkinsci.plugins.ondemandslave;


import com.google.common.base.Strings;
import hudson.Extension;
import hudson.Functions;
import hudson.Launcher;
import hudson.model.Descriptor;
import hudson.model.TaskListener;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.DelegatingComputerLauncher;
import hudson.slaves.SlaveComputer;
import hudson.util.StreamTaskListener;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements the custom logic for an on-demand slave, executing commands before connecting and after disconnecting
 */
public class OnDemandSlaveLauncher extends DelegatingComputerLauncher {

    private final String startScript;
    private final String stopScript;

    @DataBoundConstructor
    public OnDemandSlaveLauncher(ComputerLauncher launcher,
                          String startScript,
                          String stopScript) {
        super(launcher);
        this.startScript = startScript;
        this.stopScript = stopScript;
    }

    /**
     * Executes a command on the master node, with a bit of tracing.
     */
    private void execute(String command, TaskListener listener) {
        Jenkins jenkins = Jenkins.getInstance();

        if (jenkins == null) {
            listener.getLogger().println("Jenkins is not ready... doing nothing");
            return;
        }

        if (Strings.isNullOrEmpty(command)) {
            listener.getLogger().println("No command to be executed for this on-demand slave.");
            return;
        }

        try {
            Launcher launcher = jenkins.getRootPath().createLauncher(listener);
            launcher.launch().cmdAsSingleString(command).stdout(listener).join();
        } catch (Exception e) {
            listener.getLogger().println("Failed executing command '" + command + "'");
            e.printStackTrace(listener.getLogger());
        }

    }

    /*
     * Getters for Jelly
     */
    public String getStartScript() {
        return startScript;
    }

    public String getStopScript() {
        return stopScript;
    }

    /*
     *  Delegated methods that plug the additional logic for on-demand slaves
     */

    @Override
    public void launch(SlaveComputer computer, TaskListener listener) throws IOException, InterruptedException {
        execute(startScript, listener);
        super.launch(computer, listener);
    }

    @Override
    public void afterDisconnect(SlaveComputer computer, TaskListener listener) {
        super.afterDisconnect(computer, listener);
        execute(stopScript, listener);
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<ComputerLauncher> {

        public String getDisplayName() {
            return "Start and stop this node on-demand";
        }

        /**
         * Returns the applicable nested computer launcher types.
         * The default implementation avoids all delegating descriptors, as that creates infinite recursion.
         */
        public List<Descriptor<ComputerLauncher>> getApplicableDescriptors() {
            List<Descriptor<ComputerLauncher>> r = new ArrayList<>();
            for (Descriptor<ComputerLauncher> d : Functions.getComputerLauncherDescriptors()) {
                if (DelegatingComputerLauncher.class.isAssignableFrom(d.getKlass().toJavaClass()))  continue;
                r.add(d);
            }
            return r;
        }
    }

}
