package org.jenkinsci.plugins.ondemandslave;


import com.google.common.base.Strings;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.Descriptor;
import hudson.model.TaskListener;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.SlaveComputer;
import hudson.util.StreamTaskListener;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Implements the custom logic for an on-demand slave, executing commands before connecting and after disconnecting
 */
public class OnDemandSlaveLauncher extends ComputerLauncher {

    private final ComputerLauncher delegate;
    private final String startCommand;
    private final String stopCommand;

    @DataBoundConstructor
    public OnDemandSlaveLauncher(ComputerLauncher delegate,
                          String startCommand,
                          String stopCommand) {

        this.delegate = delegate;
        this.startCommand = startCommand;
        this.stopCommand = stopCommand;
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
     *  Delegated methods that plug the additional logic for on-demand slaves
     */

    @Override
    public void launch(SlaveComputer computer, TaskListener listener) throws IOException, InterruptedException {
        execute(startCommand, listener);
        delegate.launch(computer, listener);
    }

    @Override
    @Deprecated
    public void launch(SlaveComputer computer, StreamTaskListener listener) throws IOException, InterruptedException {
        execute(startCommand, listener);
        delegate.launch(computer, listener);
    }

    @Override
    public void afterDisconnect(SlaveComputer computer, TaskListener listener) {
        delegate.afterDisconnect(computer, listener);
        execute(stopCommand, listener);
    }

    @Override
    @Deprecated
    public void afterDisconnect(SlaveComputer computer, StreamTaskListener listener) {
        delegate.afterDisconnect(computer, listener);
        execute(stopCommand, listener);
    }


    /*
     *  Purely delegated methods
     */
    @Override
    public boolean isLaunchSupported() {
        return delegate.isLaunchSupported();
    }

    @Override
    public void beforeDisconnect(SlaveComputer computer, TaskListener listener) {
        delegate.beforeDisconnect(computer, listener);
    }

    @Override
    @Deprecated
    public void beforeDisconnect(SlaveComputer computer, StreamTaskListener listener) {
        delegate.beforeDisconnect(computer, listener);
    }

    public static void checkJavaVersion(PrintStream logger, String javaCommand, BufferedReader r) throws IOException {
        ComputerLauncher.checkJavaVersion(logger, javaCommand, r);
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<ComputerLauncher> {

        public String getDisplayName() {
            return "Start and stop this node on-demand";
        }

        /* Todo: validation including delegating itself
        public FormValidation doCheckCommand(@QueryParameter String value) {
            if(Util.fixEmptyAndTrim(value)==null)
                return FormValidation.error(Messages.CommandLauncher_NoLaunchCommand());
            else
                return FormValidation.ok();
        }
        */

    }
}
