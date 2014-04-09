/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.jenkins.rqm.model;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
public class TestSuite extends RqmObject<TestSuite> {
    private static final Logger log = Logger.getLogger(TestSuite.class.getName());
    private static final String RESOURCE_RQM_NAME = "testsuite";
    private String testSuiteTitle;
    private Set<TestCase> testcases;
    private Set<TestSuiteExecutionRecord> testSuiteExecutionRecords;
   
    public TestSuite() { }
    
    public TestSuite(String rqmObjectResourceUrl) {
        this(rqmObjectResourceUrl, null);
    }
    
    
    public TestSuite(String rqmObjectResourceUrl, String suiteName) {
        testcases = new HashSet<TestCase>();
        this.testSuiteTitle = suiteName;
        this.rqmObjectResourceUrl = rqmObjectResourceUrl;
    }
    
    @Override
    public TestSuite initializeSingleResource(String xml) throws RQMObjectParseException {
        try {
            log.fine("Initializing test suite...");
            Document doc = RqmObject.getDocumentReader(xml);
            
            NodeList list = doc.getElementsByTagName("ns4:testsuite");

            for(int i=0; i<list.getLength(); i++) {
                
                Node elem = list.item(i);
                if(elem.getNodeType() == Node.ELEMENT_NODE) {
                    Element el = (Element)elem;
                    String title = el.getElementsByTagName("ns6:description").item(0).getTextContent();
                    
                    //Now find all suite elements
                    NodeList suiteElements = el.getElementsByTagName("ns4:suiteelement");
                    for(int selement = 0; selement<suiteElements.getLength(); selement++) {
                        if(suiteElements.item(selement).getNodeType() == Node.ELEMENT_NODE) {
                            Element suteElem = (Element)suiteElements.item(selement);
                            String testCaseHref = ((Element)suteElem.getElementsByTagName("ns4:testcase").item(0)).getAttribute("href");
                            //String testScriptHref = ((Element)suteElem.getElementsByTagName("ns4:remotescript")).getAttribute("href");
                            TestCase tc = new TestCase(testCaseHref);
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
    public Set<TestCase> getTestcases() {
        return testcases;
    }

    /**
     * @param testcases the testcases to set
     */
    public void setTestcases(Set<TestCase> testcases) {
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
    public List<TestSuite> read(RqmParameterList parameters) throws IOException {
        RQMHttpClient client = null;
        try {            
            client = RQMUtilities.createClient(parameters.hostName, parameters.port, parameters.contextRoot, parameters.projectName, parameters.userName, parameters.passwd);
        } catch (MalformedURLException ex) {
            log.logp(Level.SEVERE, this.getClass().getName(), "read", "Caught MalformedURLException in read throwing IO Exception",ex);
            throw new IOException("RqmMethodInvoker exception", ex);
        } catch (ClientCreationException cre) {
            log.logp(Level.SEVERE, this.getClass().getName(), "read", "Caught ClientCreationException in read throwing IO Exception", cre);
            throw new IOException("RqmMethodInvoker exception(ClientCreationException)", cre);
        }
        
        try {
            Tuple<Integer,String> res = new RQMGetRequest(client, getRqmObjectResourceUrl(), null).executeRequest();            
            log.fine(res.t2);
            
            TestSuite suite = this.initializeSingleResource(res.t2);
            for(TestCase tc : suite.getTestcases()) {
                log.fine(String.format( "Reading test case %s for suite %s", tc.getRqmObjectResourceUrl(), suite.getTestSuiteTitle()) );
                tc.read(parameters);                
            }
            
            for(TestCase tc : suite.getTestcases()) {
                for(TestScript script : tc.getScripts()) {
                    script.read(parameters).get(0);
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
    public List<TestSuite> createOrUpdate(RqmParameterList parameters) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public HashMap<String, String> attributes() {
        HashMap<String,String> attr = new HashMap<String, String>();
        attr.put("testsuite_title", testSuiteTitle);
        return attr;
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
    
}