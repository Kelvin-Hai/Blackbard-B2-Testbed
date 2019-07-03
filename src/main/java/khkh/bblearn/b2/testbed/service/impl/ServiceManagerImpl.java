package khkh.bblearn.b2.testbed.service.impl;

import blackboard.persist.PersistenceException;
import blackboard.platform.config.BbConfig;
import blackboard.platform.config.ConfigurationService;
import blackboard.platform.config.ConfigurationServiceFactory;
import blackboard.platform.context.ContextManager;
import blackboard.platform.context.ContextManagerFactory;
import blackboard.platform.context.UnsetContextException;
import blackboard.platform.intl.BbResourceBundle;
import blackboard.platform.intl.BundleManager;
import blackboard.platform.intl.BundleManagerFactory;
import blackboard.platform.plugin.PlugIn;
import blackboard.platform.plugin.PlugInManager;
import blackboard.platform.plugin.PlugInManagerFactory;
import blackboard.platform.vxi.service.VirtualSystemException;
import khkh.bblearn.b2.testbed.enums.B2Enum;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;

import javax.annotation.Resource;
import java.io.File;
import java.util.Locale;
import java.util.Map;


/**
 * @author Kelvin Hai {@link <a href="mailto:kelvin.hai@york.ac.uk">kelvin.hai@york.ac.uk</a>}
 * @version $Revision$ $Date$
 */

public class ServiceManagerImpl {
    private static final String WEBAPP_BASE = "/webapp";
    private static final Log logger = LogFactory.getLog(ServiceManagerImpl.class);
    protected static final PlugInManager pluginManager = PlugInManagerFactory.getInstance();
    protected static final ContextManager contextManager = ContextManagerFactory.getInstance();
    private Locale locale;
    private static BbResourceBundle bundle;

    @Autowired
    MessageSource messageSource;

    @Resource
    private Map<String, String> b2ConfigMap;

    public String getPlugInVendor() {
        return b2ConfigMap.get(B2Enum.VENDOR);
    }

    public String getPlugInHandle() {
        return b2ConfigMap.get(B2Enum.HANDLE);
    }

    public PlugIn getPlugIn() {
        return pluginManager.getPlugIn(getPlugInVendor(), getPlugInHandle());
    }

    public String getPlugInBaseDirectoryAbsolutePath() {
        File base = pluginManager.getPlugInDir(getPlugIn());
        return base.getAbsolutePath();
    }


    public String getB2WebappDirectortAbsolutePath() {
        return this.getPlugInBaseDirectoryAbsolutePath() + WEBAPP_BASE;
    }

    public String getContextPath() {
        return pluginManager.getContextPath(getPlugIn());
    }

    public String getBbBundleMessage(String key, String args) {
        return getBbBundle().getString(key, args);
    }

    public String getBbBundleMessage(String key) {
        return getBbBundle().getString(key);
    }

    public BbResourceBundle getBbBundle() {
        if (bundle == null) {
            BundleManager bm = BundleManagerFactory.getInstance();
            bundle = bm.getPluginBundle(getPlugIn().getId());
        }
        return bundle;
    }

    //Typically returns bblearn and bb_bb60: Default is bb_bb60
    protected String getVirtualInstallationBbUid() {
        try {
            return contextManager.getContext().getVirtualInstallation().getBbUid();
        } catch (VirtualSystemException e) {
            logger.error("[ERROR] " + e.getLocalizedMessage());
        } catch (PersistenceException e) {
            logger.error("[ERROR] " + e.getLocalizedMessage());
        } catch (UnsetContextException e) {
            logger.error("[ERROR] " + e.getLocalizedMessage());
        }
        return "bb_bb60";
    }

    protected int getHttpsPort(){
        ConfigurationService configurationService = ConfigurationServiceFactory.getInstance();
        return Integer.parseInt(configurationService.getBbProperty(BbConfig.APPSERVER_HTTPS_PORTNUMBER));
    }

    //Returns same result as getVirtualInstallationBbUid()
    protected String getBbViDatabaseSchema() {
        ConfigurationService configurationService = ConfigurationServiceFactory.getInstance();
        //Set default to "bb_bb60"
        return configurationService.getBbProperty(BbConfig.ANTARGS_DEFAULT_VI_DB_NAME, "bb_bb60");
    }

    protected String getServerHostFromBbContext() {
        return contextManager.getContext().getHostName();
    }

    protected String getServerIPFromBbContext() {
        return contextManager.getContext().getIPAddress();
    }

    protected String getClientHostFromBbContext() {
        return contextManager.getContext().getRequest().getRemoteHost();
    }

    protected String getClientIPFromBbContext() {
        return contextManager.getContext().getRequest().getRemoteAddr();
    }

    protected String getUserNameFromBbContext() {
        return contextManager.getContext().getSession().getUserName();
    }

}