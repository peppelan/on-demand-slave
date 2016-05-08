package org.jenkinsci.plugins.ondemandslave;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Slave;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.NodeProperty;
import hudson.slaves.RetentionStrategy;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;

/**
 *
 */
public class OnDemandSlave extends Slave {

    @DataBoundConstructor
    public OnDemandSlave(@Nonnull String name,
                         String nodeDescription,
                         String startCommand,
                         String stopCommand,
                         String remoteFS,
                         int numExecutors,
                         Mode mode,
                         String labelString,
                         ComputerLauncher launcher,
                         RetentionStrategy retentionStrategy,
                         List<? extends NodeProperty<?>> nodeProperties)
            throws Descriptor.FormException, IOException
    {
        super(name,
                nodeDescription,
                remoteFS,
                numExecutors,
                mode,
                labelString,
                new OnDemandSlaveLauncher(launcher, startCommand, stopCommand),
                retentionStrategy,
                nodeProperties);

    }

    @Extension
    public static final class DescriptorImpl extends SlaveDescriptor {
        public String getDisplayName() {
            return "On-demand slave";
        }
    }
}
