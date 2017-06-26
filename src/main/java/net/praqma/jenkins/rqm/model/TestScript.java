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
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Praqma
 */
public class TestScript extends RqmObject<TestScript> {

    private final static String RESOURCE_RQM_MANUAL_NAME = "testscript";
    private final static String RESOURCE_RQM_NONMANUAL_NAME = "remotescript";    
    private static final Logger log = Logger.getLogger(TestScript.class.getName());       
    private String scriptTitle = "No script type defined";
    private boolean manual = false;
    private boolean executionSuccess = true;
    public HashMap<String,String> customAttributes = new HashMap<String, String>();
    
    public boolean isManual() {
        return this.manual;
    }
    
    public void setManual(boolean manual) {
        this.manual = manual;
    }
    
    /**
     * @return the scriptTitle
     */
    public String getScriptTitle() {
        return scriptTitle;
    }

    /**
     * @param scriptTitle the scriptTitle to set
     */
    public void setScriptTitle(String scriptTitle) {
        this.scriptTitle = scriptTitle;
    }

    @Override
    public List<TestScript> read(RqmParameterList parameters, BuildListener listener) throws IOException {
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
            Tuple<Integer,String> res = new RQMGetRequest(client, getRqmObjectResourceUrl(), parameters.parameterList).executeRequest();
            this.initializeSingleResource(res.t2);                
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
    public List<TestScript> createOrUpdate(RqmParameterList parameters, BuildListener listener) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public TestScript() { }
    
    public TestScript(String rqmObjectResourceUrl) throws RQMObjectParseException {
        this();
        this.rqmObjectResourceUrl = rqmObjectResourceUrl;
    }
    
    @Override
    public HashMap<String, String> attributes() {
        HashMap<String,String> attr = new HashMap<String, String>();
        attr.putAll(customAttributes);
        attr.put("rqm_testscript_title", scriptTitle);
        try {
            attr.put("rqm_testscript_id", ""+getInternalId());
        } catch (Exception ex) {
            log.log(Level.SEVERE, "Failed to add internal id for test script", ex);
        }
        return attr;
    }

    @Override
    public TestScript initializeSingleResource(String xml) throws RQMObjectParseException {       
        try {            
            customAttributes.clear();
            Document doc = RqmObject.getDocumentReader(xml);            
            NodeList list = doc.getElementsByTagName("ns4:title");     
            String title = null;
            for(int i=0; i<list.getLength(); i++) {
                Node elem = list.item(i);
                if(elem.getNodeType() == Node.ELEMENT_NODE) {                    
                     title = ((Element)elem).getTextContent();                    
                     setScriptTitle(title);
                }
            }
            
            NodeList inputs = doc.getElementsByTagName("ns4:inputParameter");
            for(int i=0; i<inputs.getLength(); i++) {
                Node n = inputs.item(i);
                if(n.getNodeType() == Node.ELEMENT_NODE) {                    
                    Element en = (Element)n;
                    String key = en.getElementsByTagName("ns4:name").item(0).getTextContent();
                    String value = en.getElementsByTagName("ns4:value").item(0).getTextContent();
                    log.fine("Key: "+key);
                    log.fine("Value: "+value);
                    customAttributes.put(key, value);
                }
            }
            
        } catch (RQMObjectParseException ex) {
            throw new RQMObjectParseException(String.format("Failed to parse %s", this.getClass().getSimpleName()), ex);
        } catch (DOMException ex) {
            throw new RQMObjectParseException(String.format("Failed to parse %s", this.getClass().getSimpleName()), ex);
        }
        return this;
    }
    
    public String getDescriptor() {
        return RqmObject.getDescriptor(this,getScriptTitle());
    }

    @Override
    public String getResourceName() {
        if(isManual()) {
            return RESOURCE_RQM_MANUAL_NAME;
        }
        return RESOURCE_RQM_NONMANUAL_NAME;
    }

    /**
     * @return the executionSuccess
     */
    public boolean isExecutionSuccess() {
        return executionSuccess;
    }

    /**
     * @param executionSuccess the executionSuccess to set
     */
    public void setExecutionSuccess(boolean executionSuccess) {
        this.executionSuccess = executionSuccess;
    }

    @Override
    public String toString() {
        return String.format("%s, Attributes = %s%nLink to this script: %s", scriptTitle, customAttributes, rqmObjectResourceUrl);
    }
}
