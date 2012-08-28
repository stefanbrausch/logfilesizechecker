
package hudson.plugins.logfilesizechecker;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.tasks.Shell;

import org.apache.commons.io.FileUtils;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.recipes.LocalData;

public class LogfilesizecheckerWrapperIntegrationTest extends HudsonTestCase {

/*    public void test1() throws Exception {
        FreeStyleProject project = createFreeStyleProject();
        project.getBuildersList().add(new Shell("echo hello"));

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        System.out.println(build.getDisplayName()+" completed");

        String s = FileUtils.readFileToString(build.getLogFile());
        assertTrue(s.contains("+ echo hello"));
    }
 */  
    @LocalData
    public void test2() throws Exception {
        FreeStyleProject project = (FreeStyleProject) hudson.getItem("1");
        System.out.println("geholt");
        if (project == null){
            System.out.println("Ist null!");
        }
        
        //in jobs/2/config.xml steht nun "echo hello"

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        System.out.println(build.getDisplayName()+" completed");

        String s = FileUtils.readFileToString(build.getLogFile());
        assertTrue(s.contains("hello"));
    }
}
