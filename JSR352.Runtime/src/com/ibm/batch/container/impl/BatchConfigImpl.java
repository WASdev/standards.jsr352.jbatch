/*
 * Copyright 2012 International Business Machines Corp.
 * 
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.ibm.batch.container.impl;

import com.ibm.batch.container.config.DatabaseConfigurationBean;
import com.ibm.batch.container.config.IBatchConfig;
import com.ibm.batch.container.config.GlassfishThreadPoolConfigurationBean;

public class BatchConfigImpl implements IBatchConfig {
	
	protected String batchContainerHome = "";
	protected boolean j2seMode = false;
	protected String workManagerJndiName = null;

	protected GlassfishThreadPoolConfigurationBean glassfishThreadPoolConfigBean;
	protected DatabaseConfigurationBean databaseConfigBean;
	
	public boolean isJ2seMode() {
		return j2seMode;
	}

	public void setJ2seMode(boolean j2seMode) {
		this.j2seMode = j2seMode;
	}
	
	@Override
	public String getBatchContainerHome() {
		return this.batchContainerHome;
	}
	
	public void setBatchContainerHome(String batchContainerHome) {
		this.batchContainerHome = batchContainerHome;
	}
	
	public String getWorkManagerJndiName() {
		return workManagerJndiName;
	}

	public void setWorkManagerJndiName(String workManagerJndiName) {
		this.workManagerJndiName = workManagerJndiName;
	}

	@Override
	public GlassfishThreadPoolConfigurationBean getGlassfishThreadPoolConfigurationBean() {
		return glassfishThreadPoolConfigBean;
	}

	@Override
	public void setGlassfishThreadPoolConfigurationBean(GlassfishThreadPoolConfigurationBean threadPoolConfigBean) {
		this.glassfishThreadPoolConfigBean = threadPoolConfigBean;
	}
	
	@Override
	public DatabaseConfigurationBean getDatabaseConfigurationBean() {
		return databaseConfigBean;
	}

	@Override
	public void setDatabaseConfigurationBean(DatabaseConfigurationBean databaseConfigBean) {
		this.databaseConfigBean = databaseConfigBean;
	}
}
