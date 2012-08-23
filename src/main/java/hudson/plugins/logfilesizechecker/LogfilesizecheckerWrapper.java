package hudson.plugins.logfilesizechecker;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Executor;
import hudson.model.Result;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.triggers.SafeTimerTask;
import hudson.triggers.Trigger;

import java.io.IOException;
import java.util.logging.Logger;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * {@link BuildWrapper} that terminates a build if its log file size is too big.
 *
 * @author Stefan Brausch
 */
public class LogfilesizecheckerWrapper extends BuildWrapper {
    
    /** Set your own max size instaed of using the default */
    public boolean setOwn;

    /** If the log file for the build has more MB, it will be terminated. */
    public int maxLogSize;

    /** Fail the build rather than aborting it. */
    public boolean failBuild;
    
    private static final Logger LOG = Logger.getLogger(LogfilesizecheckerWrapper.class.getName());
    
    @DataBoundConstructor
    public LogfilesizecheckerWrapper(int maxLogSize, boolean failBuild, boolean setOwn) {
        this.maxLogSize = maxLogSize;
        this.failBuild = failBuild;
        this.setOwn = setOwn;
    }
    
    @Override
	public Environment setUp(final AbstractBuild build, Launcher launcher,
			final BuildListener listener) throws IOException,
			InterruptedException {
		class EnvironmentImpl extends Environment {
			private final TimeoutTimerTask logtask;
			private final int allowedLogSize;

            final class TimeoutTimerTask extends SafeTimerTask {
                private final AbstractBuild build;
                private final BuildListener listener;

                private TimeoutTimerTask(AbstractBuild build, BuildListener listener) {
                    this.build = build;
                    this.listener = listener;
                }

                public void doRun() {
                    Executor e = build.getExecutor();
                    if (e != null) {
                        if (build.getLogFile().length() > allowedLogSize * 1024L * 1024L) {
                            if (!e.isInterrupted()) {
                                listener
                                        .getLogger()
                                        .println(
                                                ">>> Max Log Size reached. Aborting <<<");
                                e.interrupt(failBuild? Result.FAILURE : Result.ABORTED);
                            }
                        }
                    }
                }
            }

            
            public EnvironmentImpl() {
                if (setOwn){
                    allowedLogSize = maxLogSize;
                } else {
                    allowedLogSize = DESCRIPTOR.getDefaultLogSize();
                }
                
                logtask = new TimeoutTimerTask(build, listener);
                if (allowedLogSize > 0) {
                    //TODO Periodenwert ändern!
                    Trigger.timer.scheduleAtFixedRate(logtask, 100L, 100L);
                }
            }
		    
			@Override
			public boolean tearDown(AbstractBuild build, BuildListener listener)
					throws IOException, InterruptedException {
                if (allowedLogSize > 0) {
					logtask.cancel();
                }
                listener.getLogger().println("erreicht: " + build.getLogFile().length());

                //TODO anpassen an Vorlage bei BuildTimeOut-Plugin
				return true;
			}
			
		}
		
		listener.getLogger().println(
				"Executor: " + build.getExecutor().getNumber());
		return new EnvironmentImpl();
	}

    @Override
	public Descriptor<BuildWrapper> getDescriptor() {
		return DESCRIPTOR;
	}

	@Extension
	public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

	public static final class DescriptorImpl extends BuildWrapperDescriptor {
	    
	    /** Meet the logger. */
	    private static final Logger LOG = Logger.getLogger(DescriptorImpl.class.getName());

	    /** If there is no job specific size set, this will be used. */
	    private int defaultLogSize;

		DescriptorImpl() {
			super(LogfilesizecheckerWrapper.class);
			load();
		}

		public String getDisplayName() {
			return "Abort the build if its log file size is too big";
		}

		public boolean isApplicable(AbstractProject<?, ?> item) {
			return true;
		}
		
		public int getDefaultLogSize(){
		    return defaultLogSize;
		}
		
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            String size = formData.getString("defaultLogSize");

            if (size != null) {
                defaultLogSize = Integer.parseInt(size);
            } else {
                defaultLogSize = 0;
            }
            save();
            return super.configure(req, formData);
        }

        @Override
		public BuildWrapper newInstance(StaplerRequest req, JSONObject formData)
				throws FormException {

            JSONObject newData = new JSONObject();
            newData.put("failBuild", formData.getString("failBuild"));
            
            if (formData.containsKey("setOwn")){
                newData.put("setOwn", true);
                JSONObject sizeObject = formData.getJSONObject("setOwn");
                newData.put("maxLogSize", sizeObject.getString("maxLogSize"));
            } else {
                newData.put("setOwn", false);
            }
            
            return super.newInstance(req, newData);
        }
	}
}
