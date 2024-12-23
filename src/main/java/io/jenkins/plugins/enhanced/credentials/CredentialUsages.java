package io.jenkins.plugins.enhanced.credentials;

import com.cloudbees.plugins.credentials.Credentials;
import hudson.Extension;
import hudson.model.*;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Class for saving and loading credential usages
 */
public class CredentialUsages extends AbstractDescribableImpl<CredentialUsages> {

    private static final Logger LOGGER = Logger.getLogger(CredentialRuleSupporter.class.getName());

    private HashMap<String, CredentialUsage> credentialUsageMap;

    @DataBoundConstructor
    public CredentialUsages(HashMap<String, CredentialUsage> credentialUsageMap) {
        this.credentialUsageMap = credentialUsageMap;
    }

    @DataBoundSetter
    public void setCredentialUsageList(HashMap<String, CredentialUsage> credentialUsageMap) {
        this.credentialUsageMap = credentialUsageMap;
    }

    public HashMap<String, CredentialUsage> getCredentialUsageMap() {
        return credentialUsageMap;
    }

    /**
     * Returns the descriptor
     * @return
     */
    private static CredentialUsages.CredentialUsageDescriptor getCredentialUsageReport() {
        Descriptor descriptor = Jenkins.get().getDescriptorOrDie(CredentialUsages.class);
        return (CredentialUsages.CredentialUsageDescriptor) descriptor;
    }

    /**
     * Loads credential usages
     * @return
     */
    protected static CredentialUsages loadCredentialUsageReport() {
        getCredentialUsageReport().load();
        return getCredentialUsageReport().getCredentialUsageReport();
    }

    /**
     * Saves the credential usages
     * @param credentialUsages
     * @return
     */
    private static CredentialUsages saveCredentialUsageReport(CredentialUsages credentialUsages) {
        getCredentialUsageReport().credentialUsageMap = credentialUsages.getCredentialUsageMap();
        getCredentialUsageReport().save();
        return getCredentialUsageReport().getCredentialUsageReport();
    }


    /**
     * Increments the credential usage for the given credential
     * @param credentials Credential
     * @param callerObject Caller Object Job/FreeStyle
     */
    public static synchronized void incrementCredentialUsage(Credentials credentials, Object callerObject) {
        String credentialId = CredentialRuleSupporter.callGetId(credentials);
        LOGGER.fine(String.format("Incrementing usage count for Credential:%s", credentialId));
        CredentialUsages credentialUsages = loadCredentialUsageReport();
        CredentialUsage credentialUsage = credentialUsages.credentialUsageMap.getOrDefault(credentialId, new CredentialUsage(credentialId));
        if (callerObject instanceof Run) {
            String itemName = ((Run) callerObject).getParent().getFullName();
            LOGGER.fine(String.format("Incrementing usage count for Credential:%s and item:%s", credentialId,itemName));
            credentialUsage = credentialUsage.incrementItemUsage(itemName);
        } else if (callerObject instanceof Node) {
            String nodeName = ((Node) callerObject).getNodeName();
            LOGGER.fine(String.format("Incrementing usage count for Credential:%s and node:%s", credentialId,nodeName));
            credentialUsage = credentialUsage.incrementNodeUsage(nodeName);
        } else if (callerObject instanceof Item) {
            String itemName = ((Item) callerObject).getFullName();
            LOGGER.fine(String.format("Incrementing usage count for Credential:%s and item:%s", credentialId,itemName));
            credentialUsage = credentialUsage.incrementItemUsage(itemName);
        }
        credentialUsages.credentialUsageMap.put(credentialId, credentialUsage);
        saveCredentialUsageReport(credentialUsages);
    }

    /**
     * Clears the credential usage
     */
    public static synchronized void clearUsageData(){
        LOGGER.info("Clearing Usage Data");
        CredentialUsages credentialUsages = loadCredentialUsageReport();
        credentialUsages.credentialUsageMap = new HashMap<>();
        saveCredentialUsageReport(credentialUsages);
    }

    @Extension
    public static final class CredentialUsageDescriptor extends Descriptor<CredentialUsages> {

        private HashMap<String, CredentialUsage> credentialUsageMap = new HashMap<>();

        public CredentialUsages getCredentialUsageReport() {
            return new CredentialUsages(this.credentialUsageMap);
        }

    }

}
