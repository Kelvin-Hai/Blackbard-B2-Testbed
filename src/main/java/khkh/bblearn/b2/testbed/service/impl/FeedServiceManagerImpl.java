package khkh.bblearn.b2.testbed.service.impl;

import blackboard.admin.persist.datasource.DataSourceManager;
import blackboard.admin.persist.datasource.DataSourceManagerFactory;
import blackboard.persist.PersistenceException;
import blackboard.util.StringUtil;
import khkh.bblearn.b2.testbed.enums.FeedEnum;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.Resource;
import java.util.Map;

public class FeedServiceManagerImpl extends ServiceManagerImpl {

    private static final Log logger = LogFactory.getLog(FeedServiceManagerImpl.class);
    private DataSourceManager dataSourceManager = DataSourceManagerFactory.getInstance();

    @Resource
    protected Map<FeedEnum, String> feedFilePathMap;

    @Resource
    protected Map<FeedEnum, String> feedConfgMap;

    protected String getFeedAbsolutePath(String feedRelativePath) {
        if (StringUtil.notEmpty(feedRelativePath)) {
            if (!feedRelativePath.startsWith("/")) {
                feedRelativePath = "/" + feedRelativePath;
            }
            return getB2WebappDirectortAbsolutePath() + feedRelativePath;
        }
        return null;
    }


    protected char getDelimiter() {
        String delimiter = feedConfgMap.get(FeedEnum.DELIMITER);
        if (StringUtil.notEmpty(delimiter)) {
            return delimiter.charAt(0);
        }
        logger.warn("Unable to locate the delimiter from sisFeedDefaultSettingMap. '|' will be used");
        return '|';
    }

    protected String getDataSourceBatchUid(String dataSourceBatchUid) {
        try {
            if(null!=dataSourceManager.loadByBatchUid(dataSourceBatchUid)){
                return dataSourceBatchUid;
            }
        } catch (PersistenceException e) {
            logger.error(e.getFullMessageTrace());
        }
        throw new IllegalArgumentException("Invalid DataSourceBatchUid specified: " + dataSourceBatchUid);
    }
}
