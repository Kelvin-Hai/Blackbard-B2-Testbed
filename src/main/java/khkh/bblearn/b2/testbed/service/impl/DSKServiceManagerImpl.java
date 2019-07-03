package khkh.bblearn.b2.testbed.service.impl;

import blackboard.admin.data.datasource.DataSource;
import blackboard.admin.persist.datasource.DataSourceManager;
import blackboard.admin.persist.datasource.DataSourceManagerFactory;
import blackboard.data.ValidationException;
import blackboard.persist.PersistenceException;
import blackboard.platform.dataintegration.DataIntegration;
import blackboard.platform.dataintegration.LogLevel;
import blackboard.util.StringUtil;
import khkh.bblearn.b2.testbed.domain.DSK;
import khkh.bblearn.b2.testbed.domain.header.DSKFeedHeader;
import khkh.bblearn.b2.testbed.enums.FeedEnum;
import khkh.bblearn.b2.testbed.service.DSKServiceManager;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class DSKServiceManagerImpl extends FeedServiceManagerImpl implements DSKServiceManager {

    private static final Log logger = LogFactory.getLog(DSKServiceManagerImpl.class);
    private DataSourceManager dataSourceManager = DataSourceManagerFactory.getInstance();

    @Override
    public void createDataSourceKeys() {
        List<DSK> dskList = loadDataSourceKeyFeed();
        if (null != dskList && !dskList.isEmpty()) {
            dskList.forEach(dsk -> {
                DataSource dataSource = null;
                try {
                    dataSource = dataSourceManager.loadByBatchUid(dsk.getName());
                    if (dataSource != null) {
                        logger.info("Update Data Source: " + dsk.getName());
                        dataSource.setDescription(dsk.getDescription());
                        dataSourceManager.modify(dataSource);
                    }
                } catch (PersistenceException e) {
                    logger.warn(e.getFullMessageTrace());
                } catch (ValidationException e) {
                    logger.warn(e.getMessage());
                }


                if (dataSource == null) {
                    logger.info("Create Data Source: " + dsk.getName());
                    try {
                        dataSourceManager.create(dsk.getName(), dsk.getDescription());
                    } catch (PersistenceException e) {
                        logger.error(e.getFullMessageTrace());
                    } catch (ValidationException e) {
                        logger.error(e.getMessage());
                    }
                }
            });
        }
    }

    private List<DSK> loadDataSourceKeyFeed() {
        String feedAbsolutePath = getFeedAbsolutePath(feedFilePathMap.get(FeedEnum.DSK));
        if (null != feedAbsolutePath) {
            Reader in = null;
            try {
                in = new FileReader(feedAbsolutePath);
                Iterable<CSVRecord> records = CSVFormat.RFC4180
                        .withHeader(DSKFeedHeader.class)
                        .withIgnoreHeaderCase()
                        .withSkipHeaderRecord(true)
                        .withDelimiter(getDelimiter()).parse(in);
                if (records != null) {
                    List<DSK> dskList = new ArrayList<>();
                    records.forEach((record) -> {
                        String name = record.get(DSKFeedHeader.NAME);
                        if (StringUtil.notEmpty(name)) {
                            dskList.add(new DSK(name, record.get(DSKFeedHeader.DESCRIPTION)));
                        }
                    });
                    return dskList;
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
}
