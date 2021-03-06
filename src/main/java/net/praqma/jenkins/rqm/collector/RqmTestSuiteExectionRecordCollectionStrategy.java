/*
 * The MIT License
 *
 * Copyright 2014 Mads.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.praqma.jenkins.rqm.collector;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.EnvironmentContributingAction;
import hudson.model.Result;
import hudson.model.Run;
import hudson.remoting.Future;
import hudson.tasks.BuildStep;
import hudson.util.Secret;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import net.praqma.jenkins.rqm.RqmBuilder;
import net.praqma.jenkins.rqm.RqmCollector;
import net.praqma.jenkins.rqm.RqmCollectorDescriptor;
import net.praqma.jenkins.rqm.RqmObjectCreator;
import net.praqma.jenkins.rqm.model.RqmObject;
import net.praqma.jenkins.rqm.model.TestCase;
import net.praqma.jenkins.rqm.model.TestScript;
import net.praqma.jenkins.rqm.model.TestSuiteExecutionRecord;
import net.praqma.jenkins.rqm.request.RqmParameterList;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Step 1 : Get a feed of all testsuiterecords within the plan
 * Step 2 : Select the test cases associated with testsuiteexecutionrecord. This has to be done though a testsuite 
 * 
 * 
 * @author Mads
 */
public class RqmTestSuiteExectionRecordCollectionStrategy extends RqmCollector {
    
    private static final Logger log = Logger.getLogger(RqmTestSuiteExectionRecordCollectionStrategy.class.getName());
    public final String executionRecordName;    
    public final String projectName;
    private String planName;
    
    public RqmTestSuiteExectionRecordCollectionStrategy() { 
        this("exrecor","planname","projname");
    }
    
    @DataBoundConstructor
    public RqmTestSuiteExectionRecordCollectionStrategy(final String executionRecordName, final String planName, final String projectName) {
        this.planName = planName;
        this.executionRecordName = executionRecordName;        
        this.projectName = projectName;
    }

    /**
     * @return the planName
     */
    public String getPlanName() {
        return planName;
    }

    /**
     * @param planName the planName to set
     */
    public void setPlanName(String planName) {
        this.planName = planName;
    }
    
    @Extension
    public static class RqmTestSuiteCollectionStrategyImpl extends RqmCollectorDescriptor {        
        @Override
        public String getDisplayName() {
            return "Test suite exection record selection stategy";
        }        
    }

    @Override
    public <T extends RqmObject> List<T> collect(BuildListener listener, AbstractBuild<?, ?> build) throws Exception {
        NameValuePair[] filterProperties = TestSuiteExecutionRecord.getFilteringProperties(executionRecordName, planName);
        String request = TestSuiteExecutionRecord.getResourceFeedUrl(getHostName(), getPort(), getContextRoot(), projectName);
        listener.getLogger().println( String.format ("Resource request feed is %s", request) );

        RqmParameterList list;
        if(!StringUtils.isBlank(credentialId) && !credentialId.equals("none")) {
            listener.getLogger().println("Using credentials");
            StandardUsernameCredentials usrName = CredentialsProvider.findCredentialById(credentialId, StandardUsernameCredentials.class, build, Collections.EMPTY_LIST);        
            UsernamePasswordCredentials userPasswordCreds = (UsernamePasswordCredentials)usrName;                        
            list = new RqmParameterList(getHostName(), getPort(), getContextRoot(), projectName, userPasswordCreds, request, filterProperties, "GET", null);
        } else {
            listener.getLogger().println("Using legacy");
            list = new RqmParameterList(getHostName(), getPort(), getContextRoot(), projectName, getUsrName(), getPasswd(), request, filterProperties, "GET", null); 
        }
        
        /*
            TODO:
            Get a list of all plans in the current project. We need to do this since the feed-url does NOT allow us to filter based on names of a test when looking for test
            suite execution records. 
        */
        
        
        RqmObjectCreator<TestSuiteExecutionRecord> object = new RqmObjectCreator<TestSuiteExecutionRecord>(TestSuiteExecutionRecord.class, list, listener);        
        Future<List<TestSuiteExecutionRecord>> result = build.getWorkspace().actAsync(object);
        return (List<T>) result.get(20, TimeUnit.MINUTES);
        
    }

    @Override
    public boolean execute(AbstractBuild<?, ?> build, BuildListener listener, Launcher launcher, List<BuildStep> preBuildSteps, List<BuildStep> postBuildSteps, List<BuildStep> iterativeTestCaseBuilders, List<? extends RqmObject> results) throws Exception {
        
        int executionCounter = 0;
        int totalNumberOfScripts = 0;
        boolean success = true;
        
        List<TestSuiteExecutionRecord> records = (List<TestSuiteExecutionRecord>)results;
        
        for(TestSuiteExecutionRecord tser : records) {            
            for(TestCase tc : tser.getAllTestCases()) {
                totalNumberOfScripts += tc.getScripts().size();
            }
        }
        
        listener.getLogger().println(String.format("Found %s test cases", totalNumberOfScripts));
        
        if(preBuildSteps != null) {
            listener.getLogger().println(String.format("Performing pre build step"));
            for (BuildStep bs : preBuildSteps) {                
                success &= bs.perform(build, launcher, listener);
            }
        }
        
        for(final TestSuiteExecutionRecord rec : records) {            
            listener.getLogger().println(String.format( "Test Suite %s [%s] ", rec.getTestSuite().getTestSuiteTitle(), rec.getTestSuite().getRqmObjectResourceUrl()  ));
            listener.getLogger().println(String.format( "Test Suite Execution Record %s [%s]",rec.getTestSuiteExecutionRecordTitle(), rec.getRqmObjectResourceUrl()) );
            for(final TestCase tc : rec.getTestSuite().getTestcases()) {
                listener.getLogger().println(String.format( " Test Case %s(%s) [%s]",tc.getTestCaseTitle(), tc.getExecutionOrder(), tc.getRqmObjectResourceUrl()) );
                
                if(tc.getScripts().isEmpty()) {
                    listener.getLogger().println("Test case %s does not contain any scripts, setting result to unstable");
                    build.setResult(Result.UNSTABLE);
                }
                
                for(final TestScript ts : tc.getScripts()) {
                    boolean tsSuccess = true;
                    listener.getLogger().println(String.format( " * Test Script %s [%s]", ts.getScriptTitle(), ts.getRqmObjectResourceUrl()) );
                    for(BuildStep bstep : iterativeTestCaseBuilders) {
                        
                        final EnvironmentContributingAction envAction = new EnvironmentContributingAction() {
                            @Override
                            public void buildEnvVars(AbstractBuild<?, ?> ab, EnvVars ev) {
                                RqmBuilder.addToEnvironment(ev, rec.getTestSuite().attributes());
                                if(rec.getTestPlan() != null) {
                                    RqmBuilder.addToEnvironment(ev, rec.getTestPlan().attributes());
                                }
                                RqmBuilder.addToEnvironment(ev, tc.attributes());                         
                                RqmBuilder.addToEnvironment(ev, ts.attributes());
                            }

                            @Override
                            public String getIconFileName() {
                                return null;
                            }

                            @Override
                            public String getDisplayName() {
                                return null;
                            }

                            @Override
                            public String getUrlName() {
                                return null;
                            }
                        };
                        
                        build.addAction(envAction);                        
                        tsSuccess &= bstep.perform(build, launcher, listener);                        
                        build.getActions().remove(envAction);
                    }
                    
                    success &= tsSuccess;
                    
                    if(!tsSuccess) {
                        listener.getLogger().println( String.format( "Non-zero exit code for test script: %s", ts.getScriptTitle() ) );
                        ts.setExecutionSuccess(false);
                        build.setResult(Result.FAILURE);
                    } else {
                        executionCounter++;
                    }
                }
            }
        }
        
        if(postBuildSteps != null) {
            listener.getLogger().println(String.format("Performing post build step"));
            for(BuildStep bs : postBuildSteps) {
                success &= bs.perform(build, launcher, listener);
            }
        }
        
        listener.getLogger().println( String.format("Successfully executed %s out of %s test scripts", executionCounter, totalNumberOfScripts) );
        if(executionCounter != totalNumberOfScripts) {
            listener.getLogger().println( "Listing test cases which failed executing. Have you remembered to add the proper fields to your test scripts?" );
            for(TestSuiteExecutionRecord tser : records) {
                for(TestCase tc : tser.getAllTestCases()) {
                    for(TestScript tscript : tc.getScripts()) {
                        if(!tscript.isExecutionSuccess()) {
                            listener.getLogger().println(tscript);
                        }
                    }
                }
            }
        }
        return true;
    }

}
