/*
 * #%L
 * Service Activity Monitoring :: Server
 * %%
 * Copyright (C) 2011 - 2012 Talend Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.talend.esb.sam.server.persistence;

import com.ibatis.common.jdbc.ScriptRunner;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;

/**
 * The Class DBInitializer using for initializing persistence.
 */
public class DBInitializer implements InitializingBean {

    private static final Logger LOG = Logger.getLogger(DBInitializer.class.getName());

    private DataSource dataSource;
    private String createSql;
    private String createSqlInd;

    /**
     * Sets the data source.
     * 
     * @param dataSource
     *            the new data source
     */
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Sets the database dialect.
     * 
     * @param dialect
     *            the database dialect
     */
    public void setDialect(String dialect) {
        String[] scripts = createScripts.get(dialect);
        createSql = scripts[0];
        createSqlInd = scripts[1];
    }

    @SuppressWarnings("serial")
    private final Map<String, String[]> createScripts = new HashMap<String, String[]>() {
        {
            put("derbyDialect", new String[] { "create.sql", "create_ind.sql" });
            put("h2Dialect", new String[] { "create_h2.sql", "create_h2_ind.sql" });
            put("mysqlDialect", new String[] { "create_mysql.sql", "create_mysql_ind.sql" });
            put("oracleDialect", new String[] { "create_oracle.sql", "create_oracle_ind.sql" });
            put("DB2Dialect", new String[] { "create_db2.sql", "create_db2_ind.sql" });
            put("sqlServerDialect", new String[] { "create_sqlserver.sql", "create_sqlserver_ind.sql" });
        }
    };

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        boolean createTables = true;
        try {
            ResultSet rs = dataSource.getConnection().getMetaData()
                    .getTables(dataSource.getConnection().getCatalog(), null, "EVENTS_CUSTOMINFO", null);
            while (rs.next()) {
                createTables = false;
            }
        } catch (SQLException e) {
            LOG.warning("The create tables parameter has not been set. Tables and indexes will not be created.");
            createTables = false;
        }
        if (createTables) {
            ScriptRunner sr = new ScriptRunner(dataSource.getConnection(), false, false);
            sr.setLogWriter(null);
            sr.setErrorLogWriter(null);
            sr.runScript(new InputStreamReader(this.getClass().getResourceAsStream("/" + createSql)));
            if (createSqlInd != null && !createSqlInd.equals("")) {
                sr.runScript(new InputStreamReader(this.getClass().getResourceAsStream("/" + createSqlInd)));
            } else {
                LOG.warning("The script to create indexes has not been set. Indexes will not be created.");
            }
        }
    }

}
