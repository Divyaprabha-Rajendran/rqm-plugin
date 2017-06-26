/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.jenkins.rqm.model;

import hudson.model.BuildListener;
import java.io.IOException;
import java.net.MalformedURLException;
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Praqma
 */
public class TestSuite extends RqmObject<TestSuite> implements Comparable<TestSuite>{
    private static final Logger log = Logger.getLogger(TestSuite.class.getName());
    private static final String RESOURCE_RQM_NAME = "testsuite";
    private String testSuiteTitle;
    private SortedSet<TestCase> testcases;
    private Set<TestSuiteExecutionRecord> testSuiteExecutionRecords;
   
    public TestSuite() { }
    
    public TestSuite(String rqmObjectResourceUrl) {
        this(rqmObjectResourceUrl, null);
    }
    
    
    public TestSuite(String rqmObjectResourceUrl, String suiteName) {
        testcases = new TreeSet<TestCase>();
        this.testSuiteTitle = suiteName;
        this.rqmObjectResourceUrl = rqmObjectResourceUrl;
    }
    
    @Override
    public TestSuite initializeSingleResource(String xml) throws RQMObjectParseException {
        try {
            log.fine("Initializing test suite...");
            Document doc = RqmObject.getDocumentReader(xml);
            
            NodeList list = doc.getElementsByTagName("ns2:testsuite");

            for(int i=0; i<list.getLength(); i++) {
                
                Node elem = list.item(i);
                if(elem.getNodeType() == Node.ELEMENT_NODE) {
                    Element el = (Element)elem;
                    String title = el.getElementsByTagName("ns4:description").item(0).getTextContent();
                    
                    //Now find all suite elements
                    NodeList suiteElements = el.getElementsByTagName("ns2:suiteelement");
                    for(int selement = 0; selement<suiteElements.getLength(); selement++) {
                        
                        int executionOrder = Integer.parseInt(((Element)suiteElements.item(selement)).getAttribute("elementindex"))+1;
                        
                        if(suiteElements.item(selement).getNodeType() == Node.ELEMENT_NODE) {
                            Element suteElem = (Element)suiteElements.item(selement);
                            String testCaseHref = ((Element)suteElem.getElementsByTagName("ns2:testcase").item(0)).getAttribute("href");
                            
                            NodeList nlTestScript = suteElem.getElementsByTagName("ns2:remotescript");
                            TestCase tc = new TestCase(testCaseHref);
                            
                            if (nlTestScript.getLength() > 0) {
                                String testScriptHref = ((Element)nlTestScript.item(0)).getAttribute("href");
                                TestScript ts = new TestScript(testScriptHref);
                                tc.getScripts().add(ts);
                            } else {
                                log.warning(String.format("The testcase %s, has no test scripts", testCaseHref));
                            }

                            tc.setExecutionOrder(executionOrder);
                            getTestcases().add(tc);
                        }
                    }

                    setSuiteTitle(title);
                }
            }            
            return this;
        } catch (Exception ex) {
            throw new RQMObjectParseException("Failed to initialize TestSuite", ex);
        }
    }

    /**
     * @return the testcases
     */
    public SortedSet<TestCase> getTestcases() {
        return testcases;
    }

    /**
     * @param testcases the testcases to set
     */
    public void setTestcases(SortedSet<TestCase> testcases) {
        this.testcases = testcases;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(RqmObject.getDescriptor(this, getTestSuiteTitle()));
        if(getTestcases().size() > 0) {            
            builder.append( String.format( "Associated test cases:%n" ) );
            for(TestCase tc : getTestcases()) {
                builder.append(tc);            
            }
        }
        return builder.toString();
    }

    /**
     * @return the testSuiteTitle
     */
    public String getSuiteTitle() {
        return getTestSuiteTitle();
    }

    /**
     * @param suiteTitle the testSuiteTitle to set
     */
    public void setSuiteTitle(String suiteTitle) {
        this.setTestSuiteTitle(suiteTitle);
    }

    /**
     * @return the testSuiteTitle
     */
    public String getTestSuiteTitle() {
        return testSuiteTitle;
    }

    /**
     * @param testSuiteTitle the testSuiteTitle to set
     */
    public void setTestSuiteTitle(String testSuiteTitle) {
        this.testSuiteTitle = testSuiteTitle;
    }

    @Override
    public List<TestSuite> read(RqmParameterList parameters, BuildListener listener) throws IOException {
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
            Tuple<Integer,String> res = new RQMGetRequest(client, getRqmObjectResourceUrl(), null).executeRequest();            
            
            if(res.t1 != 200) {
                throw new RequestException("Failed to load test suite. Response written to log.", res);
            }
            
            log.fine(res.t2);
            
            TestSuite suite = this.initializeSingleResource(res.t2);
            for(TestCase tc : suite.getTestcases()) {
                log.fine(String.format( "Reading test case %s for suite %s", tc.getRqmObjectResourceUrl(), suite.getTestSuiteTitle()) );
                tc.read(parameters, listener);                
            }
            
            for(TestCase tc : suite.getTestcases()) {
                for(TestScript script : tc.getScripts()) {
                    script.read(parameters, listener).get(0);
                }
            }

            log.fine(suite.toString());
            return Arrays.asList(suite);                
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
    public List<TestSuite> createOrUpdate(RqmParameterList parameters, BuildListener listener) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public HashMap<String, String> attributes() {
        HashMap<String,String> attr = new HashMap<String, String>();
        try {
            attr.put("rqm_testsuite_title", testSuiteTitle);
            attr.put("rqm_testsuite_id", ""+getInternalId());            
        } catch (RQMObjectParseException ex) {
            log.log(Level.SEVERE, "Failed to add internal id for test suite", ex);
        } finally {
            return attr;
        }
    }

    /**
     * @return the testSuiteExecutionRecords
     */
    public Set<TestSuiteExecutionRecord> getTestSuiteExecutionRecords() {
        return testSuiteExecutionRecords;
    }

    /**
     * @param testSuiteExecutionRecords the testSuiteExecutionRecords to set
     */
    public void setTestSuiteExecutionRecords(Set<TestSuiteExecutionRecord> testSuiteExecutionRecords) {
        this.testSuiteExecutionRecords = testSuiteExecutionRecords;
    }

    @Override
    public String getResourceName() {
        return RESOURCE_RQM_NAME;
    }

    @Override
    public int compareTo(TestSuite t) {
        return t.getRqmObjectResourceUrl().compareTo(t.getRqmObjectResourceUrl());
    }
    
}
