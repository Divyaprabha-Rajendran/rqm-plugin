/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.jenkins.rqm.model;

import hudson.model.BuildListener;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.praqma.jenkins.rqm.model.exception.ClientCreationException;
import net.praqma.jenkins.rqm.model.exception.LoginException;
import net.praqma.jenkins.rqm.model.exception.RQMObjectParseException;
import net.praqma.jenkins.rqm.model.exception.RequestException;
import net.praqma.jenkins.rqm.request.RQMGetRequest;
import net.praqma.jenkins.rqm.request.RQMHttpClient;
import net.praqma.jenkins.rqm.request.RQMUtilities;
import net.praqma.jenkins.rqm.request.RqmParameterList;
import net.praqma.util.structure.Tuple;
import org.apache.commons.httpclient.NameValuePair;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Praqma
 * 
 * A test plan can contain both testCases and test suites.
 * 
 * 
 */
public class TestPlan extends RqmObject<TestPlan> {
    private final static String RESOURCE_RQM_NAME = "testplan";
    private static final Logger log = Logger.getLogger(TestPlan.class.getName());
    private String testPlanTitle;
    private SortedSet<TestCase> testCases = new TreeSet<TestCase>();
    private SortedSet<TestSuite> testSuites = new TreeSet<TestSuite>();
    
    public TestPlan() {}
    
    public TestPlan(String testPlanName) {
        this.testPlanTitle = testPlanName;
        testCases = new TreeSet<TestCase>();
        testSuites = new TreeSet<TestSuite>();
    }
    
    @Override
    public HashMap<String,String> attributes() {
        HashMap<String,String> attr = new HashMap<String, String>();
        attr.put("testplan_title",testPlanTitle);
        return attr;
    }
    
    
    /**
     * We initalize the list of test suites by their assocated TestPlanName
     * 
     * Two cases:
     * 
     * Case 1: 
     * TestCase directly in plan
     * 
     * Case 2:
     * TestCase
     * 
     * 
     * @return
     * @throws RQMObjectParseException 
     */
    @Override
    public TestPlan initializeSingleResource(String xml) throws RQMObjectParseException {
        try {
            Document doc = RqmObject.getDocumentReader(xml);
            
            NodeList list = doc.getElementsByTagName("ns4:title");     
            String title = null;
            for(int i=0; i<list.getLength(); i++) {
                Node elem = list.item(i);
                if(elem.getNodeType() == Node.ELEMENT_NODE) {                    
                     title = ((Element)elem).getTextContent();                    
                     setTestPlanTitle(title);
                }
            }
                        
            NodeList nlistForTestCases = doc.getElementsByTagName("testcase");
            for(int i=0; i<nlistForTestCases.getLength(); i++) {
                TestCase tc = new TestCase();
                
                Node elem = nlistForTestCases.item(i);
                if(elem.getNodeType() == Node.ELEMENT_NODE) {
                    Element elm = (Element)elem;
                    tc.setRqmObjectResourceUrl(elm.getAttribute("href"));
                }
                getTestCases().add(tc);
            }
            
            NodeList nlistForTestSuites = doc.getElementsByTagName("testsuite");
            for(int i=0; i<nlistForTestSuites.getLength(); i++) {
                TestSuite suite = new TestSuite(null,null);
                Node elem = nlistForTestSuites.item(i);
                if(elem.getNodeType() == Node.ELEMENT_NODE) {
                    Element elm = (Element)elem;
                    suite.setRqmObjectResourceUrl(elm.getAttribute("href"));
                }
                getTestSuites().add(suite);
            }
                        
            return this;
        } catch (RQMObjectParseException ex) {
            throw new RQMObjectParseException("Failed to parse TestPlan object", ex);
        } catch (DOMException ex) {
            throw new RQMObjectParseException("DOM Exception, malformed xml?", ex);
        }        
    }
    
    /**
     * Method that returns a basic list of feed data. We include everything for a given test plan.
     * @param host
     * @param port
     * @param context
     * @param project
     * @throws UnsupportedEncodingException
     * @return 
     */
    public String getFeedUrlForTestPlans(String host, int port, String context, String project) throws UnsupportedEncodingException {
        String request = String.format("%s:%s/%s/service/com.ibm.rqm.integration.service.IIntegrationService/resources/%s/testplan", host, port, context, URLEncoder.encode(project,"UTF-8"));        
        return request;
    }
    
    public NameValuePair[] getParametersForFeedUrlForTestPlans() {
        NameValuePair[] pairs = new NameValuePair[1];
        pairs[0] = new NameValuePair("fields", String.format("feed/entry/content/testplan[title='%s']/*", getTestPlanTitle()));
        return pairs;
    }
    

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(RqmObject.getDescriptor(this, getTestPlanTitle()));        
        builder.append(String.format(" %s testsuite(s)%n %s testcase(s)",getTestSuites().size(),getTestCases().size()));        
        builder.append("===Contents===%n");
        for(TestSuite suite : getTestSuites()) {
            builder.append(suite);
        }
        
        
        return builder.toString();
    }

    /**
     * @return the testCases
     */
    public SortedSet<TestCase> getTestCases() {
        return testCases;
    }

    /**
     * @param testCases the testCases to set
     */
    public void setTestCases(SortedSet<TestCase> testCases) {
        this.testCases = testCases;
    }

    /**
     * @return the testSuites
     */
    public Set<TestSuite> getTestSuites() {
        return testSuites;
    }

    /**
     * @param testSuites the testSuites to set
     */
    public void setTestSuites(SortedSet<TestSuite> testSuites) {
        this.testSuites = testSuites;
    }    

    /**
     * @return the testPlanTitle
     */
    public String getTestPlanTitle() {
        return testPlanTitle;
    }

    /**
     * @param testPlanTitle the testPlanTitle to set
     */
    public void setTestPlanTitle(String testPlanTitle) {
        this.testPlanTitle = testPlanTitle;
    }
    
    /**
     * Basic 'Read' operation (GET) for a test plan
     * @param parameters
     * @return
     * @throws IOException 
     */
    @Override
    public List<TestPlan> read(RqmParameterList parameters, BuildListener listener) throws IOException {
        RQMHttpClient client = null;
        try {            
            client = RQMUtilities.createClient(parameters);
        } catch (MalformedURLException ex) {
            log.logp(Level.SEVERE, this.getClass().getName(), "read", "Caught MalformedURLException in read throwing IO Exception",ex);
            throw new IOException("RqmMethodInvoker exception", ex);
        } catch (ClientCreationException cre) {
            log.logp(Level.SEVERE, this.getClass().getName(), "read", "Caught ClientCreationException in read throwing IO Exception", cre);
            throw new IOException("RqmMethodInvoker exception(ClientCreationException)", cre);
        }
        
        try {
            Tuple<Integer,String> res = new RQMGetRequest(client, parameters.requestString, parameters.parameterList).executeRequest();
            this.initializeSingleResource(res.t2);

            for(TestCase tc : getTestCases()) {
                parameters.requestString = tc.getRqmObjectResourceUrl();
                tc.read(parameters, listener);
                for(TestScript ts : tc.getScripts()) {
                    parameters.requestString = ts.getRqmObjectResourceUrl();
                    ts.read(parameters, listener);
                }                
            }
            
            for(TestSuite suites : getTestSuites()) {
                parameters.requestString = suites.getRqmObjectResourceUrl();
                suites.read(parameters, listener);                
                for(TestCase tc : suites.getTestcases()) {
                    parameters.requestString = tc.getRqmObjectResourceUrl();
                    tc.read(parameters, listener);                    
                    for(TestScript ts : tc.getScripts()) {
                        parameters.requestString = ts.getRqmObjectResourceUrl();
                        ts.read(parameters, listener);
                    }                    
                }                
            }
             
            return Arrays.asList(this);
 
        } catch (LoginException loginex) {
            log.logp(Level.SEVERE, this.getClass().getName(), "invoke", "Caught login exception in invoke");
            throw new IOException("RqmMethodInvoker exception(LoginException)",loginex);
        } catch (RequestException reqExeception) {
            log.logp(Level.SEVERE, this.getClass().getName(), "invoke", "Caught RequestException in invoke");
            throw new IOException("RqmMethodInvoker exception(RequestException)",reqExeception);
        } catch (Exception ex) {
            log.logp(Level.SEVERE, this.getClass().getName(), "invoke", "Caught Exception in invoke");
            throw new IOException("RqmMethodInvoker exception(Exception)", ex);
        }
    }

    @Override
    public String getResourceName() {
        return RESOURCE_RQM_NAME;
    }
    
    public static String getResourceFeedUrl(String host, int port, String context, String project) throws UnsupportedEncodingException {
        String request = String.format("%s:%s/%s/service/com.ibm.rqm.integration.service.IIntegrationService/resources/%s/%s", host, port, context, URLEncoder.encode(project,"UTF-8"), RESOURCE_RQM_NAME);                               
        return request;
    }

}
