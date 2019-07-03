package khkh.bblearn.b2.testbed.service.impl;

import blackboard.data.ExtendedData;
import blackboard.platform.dataintegration.DataIntegration;
import blackboard.platform.dataintegration.DataIntegrationManager;
import blackboard.platform.dataintegration.DataIntegrationManagerFactory;
import blackboard.platform.dataintegration.LogLevel;
import blackboard.platform.dataintegration.framework.DataIntegrationHandler;
import blackboard.platform.dataintegration.framework.DataIntegrationHandlerManager;
import blackboard.platform.dataintegration.framework.DataIntegrationHandlerManagerFactory;
import blackboard.util.StringUtil;
import khkh.bblearn.b2.testbed.domain.SIS;
import khkh.bblearn.b2.testbed.domain.SISFeedConfig;
import khkh.bblearn.b2.testbed.domain.header.SISFeedHeader;
import khkh.bblearn.b2.testbed.enums.FeedEnum;
import khkh.bblearn.b2.testbed.enums.SISEnum;
import khkh.bblearn.b2.testbed.service.SISServiceManager;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SISServiceManagerImpl extends FeedServiceManagerImpl implements SISServiceManager {

    private static final Log logger = LogFactory.getLog(SISServiceManagerImpl.class);

    private DataIntegrationManager dataIntegrationManager = DataIntegrationManagerFactory.getInstance();
    private DataIntegrationHandlerManager dataIntegrationHandlerManager = DataIntegrationHandlerManagerFactory.getInstance();

    private static Map<String, DataIntegration.IntegrationState> integrationStateMap = null;
    private static Map<String, LogLevel> logLevelMap = null;

    @Resource
    private Map<SISEnum, String> sisFeedDefaultSettingMap;

    public void createDataIntegrations() {
        logger.info("Kelvin Date Source Batch Uid: " + this.getDataSourceBatchUid(sisFeedDefaultSettingMap.get(SISEnum.DATA_SOURCE_BATCH_UID)));
        List<SIS> sisList = loadSISFeed();
        if (null != sisList && !sisList.isEmpty()) {
            sisList.forEach(sis -> {
                logger.info(sis.getName());
                DataIntegration dataIntegration = loadDataIntegrationByName(sis.getName());
                if (dataIntegration != null) {
                    logger.info("Loaded Data Integration: " + dataIntegration.toString());
                    dataIntegration.getExtendedData().getValues().forEach((k, v)
                            -> logger.info("Extended Key: " + k + " => Extended Value: " + v));
                    logger.info("Update Data Integration: " + sis.getName());
                    dataIntegration.setDescription(sis.getDescription());

                    // DAO can also be used to update Data Integration
                    //new DataIntegrationDAO().update(dataIntegration);
                } else {
                    dataIntegration = new DataIntegration();
                    // Begin of required properties
                    dataIntegration.setTypeHandle(getIntegrationTypeHandle(
                            sisFeedDefaultSettingMap.get(SISEnum.INTEGRATION_HANDLER_TYPE)));

                    dataIntegration.setName(sis.getName());
                    dataIntegration.setDescription(sis.getDescription());
                    dataIntegration.setAuthPassword(RandomStringUtils.randomAlphabetic(50));
                    dataIntegration.setIntegrationState(
                            getIntegrationState(sisFeedDefaultSettingMap.get(SISEnum.INTEGRATION_STATE)));
                    //dataIntegration.setDataSourceBatchUid(
                    //        getDataSourceBatchUid(sisFeedDefaultSettingMap.get(SISEnum.DATA_SOURCE_BATCH_UID)));

                    dataIntegration.setDataSourceBatchUid(sisFeedDefaultSettingMap.get(SISEnum.DATA_SOURCE_BATCH_UID));
                    dataIntegration.setLogLevel(
                            getIntegrationLogLevel(sisFeedDefaultSettingMap.get(SISEnum.INTEGRATION_LOG_LEVEL)));
                    // End of required properties

                    ExtendedData extendedData = new ExtendedData();
                    extendedData.setValue(SISEnum.DELIMITER.toLowerCase(),
                            String.valueOf(getDelimiter()));
                    //extendedData.setValue("selectedRootNode", "Top");
                    //extendedData.setValue("useSystemRootNode", "true");
                    dataIntegration.setExtendedData(extendedData);

                    // DAO can also be used to create Data Integration
                    //new DataIntegrationDAO().persist(dataIntegration);
                }
                dataIntegrationManager.saveDataIntegration(dataIntegration);
            });
        }
    }

    public void processAllFeed() {
        processUserFeed();
        processCourseFeed();
        processCourseEnrolementFeed();
        processOrganisationFeed();
        processOrganisationEnrolmentFeed();
    }

    public void processUserFeed() {
        postIntegrationDataFeed(FeedEnum.USER);
    }

    public void processCourseFeed() {
        postIntegrationDataFeed(FeedEnum.COURSE);
    }

    public void processCourseEnrolementFeed() {
        postIntegrationDataFeed(FeedEnum.COURSE_ENROLMENT);
    }

    public void processOrganisationFeed() {
        postIntegrationDataFeed(FeedEnum.ORGANISATION);
    }

    public void processOrganisationEnrolmentFeed() {
        postIntegrationDataFeed(FeedEnum.ORGANISATION_ENROLMENT);
    }

    private List<SIS> loadSISFeed() {
        String feedAbsolutePath = getFeedAbsolutePath(feedFilePathMap.get(FeedEnum.SIS));
        if (null != feedAbsolutePath) {
            Reader in = null;
            try {
                in = new FileReader(feedAbsolutePath);
                Iterable<CSVRecord> records = CSVFormat.RFC4180
                        .withHeader(SISFeedHeader.class)
                        .withIgnoreHeaderCase()
                        .withSkipHeaderRecord(true)
                        .withDelimiter(getDelimiter()).parse(in);
                if (records != null) {
                    List<SIS> sisList = new ArrayList<>();
                    records.forEach((record) -> {
                        String name = record.get(SISFeedHeader.NAME);
                        if (StringUtil.notEmpty(name)) {
                            sisList.add(new SIS(name, record.get(SISFeedHeader.DESCRIPTION)));
                        }
                    });

                    return sisList;
                }
            } catch (FileNotFoundException e) {
                logger.error(e.getMessage());
            } catch (IOException e) {
                logger.error(e.getMessage());
            } finally {
                try {
                    in.close();
                } catch (IOException e) {
                    logger.error(e.getMessage());
                }
            }
        }

        return null;
    }

    private DataIntegration loadDataIntegrationByName(String name) {
        DataIntegration dataIntegration = null;
        if (StringUtil.notEmpty(name)) {
            List<DataIntegration> dataIntegrations = dataIntegrationManager.loadAllDataIntegrations();
            for (DataIntegration d : dataIntegrations) {
                if (d.getName().equalsIgnoreCase(name)) {
                    dataIntegration = d;
                    return dataIntegration;
                }
            }
        }
        return dataIntegration;
    }


    private String getIntegrationTypeHandle(String typeHandleString) {
        List<DataIntegrationHandler> list = dataIntegrationHandlerManager.loadDataIntegrationHandlers();
        for (DataIntegrationHandler d : list) {
            if (d.getTypeHandle().equalsIgnoreCase(typeHandleString)) {
                return typeHandleString;
            }
        }
        throw new IllegalArgumentException("DataIntegrationHandler: invalid value specified " + typeHandleString
                + " : Ensure you have specified the correct value.");
    }

    private void setIntegrationStateMap() {
        if (integrationStateMap == null) {
            integrationStateMap = new HashMap<>();
            integrationStateMap.put(DataIntegration.IntegrationState.ACTIVE.name(),
                    DataIntegration.IntegrationState.ACTIVE);
            integrationStateMap.put(DataIntegration.IntegrationState.INACTIVE.name(),
                    DataIntegration.IntegrationState.INACTIVE);
            integrationStateMap.put(DataIntegration.IntegrationState.LOG_ONLY.name(),
                    DataIntegration.IntegrationState.LOG_ONLY);
        }
    }

    private DataIntegration.IntegrationState getIntegrationState(String stateString) {
        setIntegrationStateMap();
        if (StringUtil.notEmpty(stateString)) {
            DataIntegration.IntegrationState state = integrationStateMap.get(stateString);
            if (state != null) {
                return state;
            }
        }
        throw new IllegalArgumentException("DataIntegration.IntegrationState: invalid value specified "
                + stateString + " : Ensure you have specified the correct value.");
    }


    private void setLogLevelMap() {
        if (logLevelMap == null) {
            logLevelMap = new HashMap<>();
            logLevelMap.put(LogLevel.ERROR.name(), LogLevel.ERROR);
            logLevelMap.put(LogLevel.DEBUG.name(), LogLevel.DEBUG);
            logLevelMap.put(LogLevel.WARNING.name(), LogLevel.WARNING);
            logLevelMap.put(LogLevel.INFORMATION.name(), LogLevel.INFORMATION);
        }
    }

    private LogLevel getIntegrationLogLevel(String logLevelString) {
        setLogLevelMap();
        if (StringUtil.notEmpty(logLevelString)) {
            LogLevel logLevel = logLevelMap.get(logLevelString);
            if (logLevel != null) {
                return logLevel;
            }
        }
        throw new IllegalArgumentException("LogLevel: invalid value specified: " + logLevelString
                + " : Ensure you have specified the correct value.");
    }

    private String getIntegrationFeedAbsolutePath(FeedEnum feedEnum) {
        String feedFileAbsolutePath = null;
        if (feedEnum.equals(FeedEnum.USER)) {
            feedFileAbsolutePath = getFeedAbsolutePath(feedFilePathMap.get(FeedEnum.USER));
            logger.info("Feed Absolute Path : " + feedFileAbsolutePath);
        }

        if (feedEnum.equals(FeedEnum.COURSE)) {
            feedFileAbsolutePath = getFeedAbsolutePath(feedFilePathMap.get(FeedEnum.COURSE));
            logger.info("Feed Absolute Path : " + feedFileAbsolutePath);
        }

        if (feedEnum.equals(FeedEnum.COURSE_ENROLMENT)) {
            feedFileAbsolutePath = getFeedAbsolutePath(feedFilePathMap.get(FeedEnum.COURSE_ENROLMENT));
            logger.info("Feed Absolute Path : " + feedFileAbsolutePath);
        }

        if (feedEnum.equals(FeedEnum.ORGANISATION)) {
            feedFileAbsolutePath = getFeedAbsolutePath(feedFilePathMap.get(FeedEnum.ORGANISATION));
            logger.info("Feed Absolute Path : " + feedFileAbsolutePath);
        }


        if (feedEnum.equals(FeedEnum.ORGANISATION_ENROLMENT)) {
            feedFileAbsolutePath = getFeedAbsolutePath(feedFilePathMap.get(FeedEnum.ORGANISATION_ENROLMENT));
            logger.info("Feed Absolute Path : " + feedFileAbsolutePath);
        }

        return feedFileAbsolutePath;
    }

    private URI getIntegrationURI(FeedEnum feedEnum) {
        URI uri = null;
        String schema = this.getBbViDatabaseSchema();
        if (feedEnum.equals(FeedEnum.USER)) {
            uri = getIntegrationEndpointFullURI(this.getBbBundleMessage(SISFeedConfig.USER_ENDPOINT_SUFFIX, schema));
            logger.info("URI: " + uri);
        }

        if (feedEnum.equals(FeedEnum.COURSE)) {
            uri = getIntegrationEndpointFullURI(this.getBbBundleMessage(
                    SISFeedConfig.COURSE_ENDPOINT_SUFFIX, schema));
            logger.info("URI: " + uri);
        }

        if (feedEnum.equals(FeedEnum.COURSE_ENROLMENT)) {
            uri = getIntegrationEndpointFullURI(this.getBbBundleMessage(
                    SISFeedConfig.COURSE_ENROLMENT_ENDPOINT_SUFFIX, schema));
            logger.info("URI: " + uri);
        }

        if (feedEnum.equals(FeedEnum.ORGANISATION)) {
            uri = getIntegrationEndpointFullURI(this.getBbBundleMessage(
                    SISFeedConfig.ORGANISATION_ENDPOINT_SUFFIX, schema));
            logger.info("URI: " + uri);
        }


        if (feedEnum.equals(FeedEnum.ORGANISATION_ENROLMENT)) {
            uri = getIntegrationEndpointFullURI(this.getBbBundleMessage(
                    SISFeedConfig.ORGANISATION_ENROLMENT_ENDPOINT_SUFFIX, schema));
            logger.info("URI: " + uri);
        }
        return uri;
    }

    private String postIntegrationDataFeed(FeedEnum feedEnum) {
        DataIntegration dataIntegration = loadDataIntegrationByName(sisFeedDefaultSettingMap.get(SISEnum.DEFAULT_INTEGRATION));
        CredentialsProvider credentialsProvider = getCredentialsProvider(dataIntegration);

        URI uri = this.getIntegrationURI(feedEnum);
        String feedFileAbsolutePath = this.getIntegrationFeedAbsolutePath(feedEnum);

        if (feedEnum != null && credentialsProvider != null && dataIntegration != null) {
            File file = new File(feedFileAbsolutePath);
            if (!file.exists()) {
                logger.error("[ERROR] feed file does not exist: " + feedFileAbsolutePath);
                return null;
            }

            CloseableHttpClient httpclient = HttpClients.custom().setDefaultCredentialsProvider(credentialsProvider).build();
            try {

                ContentType contentType = ContentType.create(ContentType.TEXT_PLAIN.getMimeType());
                HttpEntity entity = new FileEntity(file, contentType);
                HttpPost httpPost = new HttpPost(uri);
                httpPost.setEntity(entity);
                CloseableHttpResponse response = httpclient.execute(httpPost);

                try {

                    StatusLine statusLine = response.getStatusLine();
                    // Get output of response and cast to a string
                    String responseString = EntityUtils.toString(response.getEntity());
                    //Release all resources
                    EntityUtils.consume(response.getEntity());
                    logger.info("[INFO] response string: " + responseString);
                    logger.info("[INFO] Status Code: " + statusLine.getStatusCode());
                    if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
                        logger.error("Error occurred while trying to post the data feed");
                        return null;
                    }
                    return responseString;
                } finally {
                    try {
                        response.close();
                    } catch (Exception e) {
                    }
                }
            } catch (IOException e) {
                logger.error("[ERROR] post exception: " + e.getMessage());
            } finally {
                try {
                    httpclient.close();
                } catch (IOException e) {
                }
            }
        }
        return null;
    }

    private CredentialsProvider getCredentialsProvider(DataIntegration dataIntegration) {
        if (dataIntegration != null) {
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
//            AuthScope authscope = new AuthScope(this.getServerHostFromBbContext(), getHttpsPort());
            AuthScope authscope = new AuthScope(this.getServerHostFromBbContext(), 8080);
            UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(
                    dataIntegration.getGuid(), dataIntegration.getAuthPassword());
            credentialsProvider.setCredentials(authscope, credentials);
            return credentialsProvider;
        }
        return null;
    }

    private URI getIntegrationEndpointFullURI(String endPointSuffix) {
        if (StringUtil.notEmpty(endPointSuffix)) {
            URI uri = null;
            try {
                uri = new URIBuilder().setScheme("http")
                        .setHost(this.getServerHostFromBbContext())
                        .setPort(8080)
                        .setPath(endPointSuffix).build();
                return uri;
            } catch (URISyntaxException e) {
                logger.error("[ERROR] unable to build URI: " + e.getLocalizedMessage());
            }
        }
        return null;
    }
}