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

import com.ibm.batch.container.IBatchConfig;

public class BatchConfigImpl implements IBatchConfig {
	
	protected String batchContainerHome = "";
	protected boolean j2seMode = false;
	protected String jndiName = "";
	protected String jdbcDriver = "";
	protected String dbUser = "";
	protected String dbPassword = "";
	protected String jdbcUrl = "";
	protected String workManagerJndiName = null;
	
	
	public String getJdbcUrl() {
		return jdbcUrl;
	}

	public void setJdbcUrl(String jdbcUrl) {
		this.jdbcUrl = jdbcUrl;
	}

	public void setDbPassword(String dbPassword) {
		this.dbPassword = dbPassword;
	}

	public boolean isJ2seMode() {
		return j2seMode;
	}

	public void setJ2seMode(boolean j2seMode) {
		this.j2seMode = j2seMode;
	}

	public void setJdbcDriver(String jdbcDriver) {
		this.jdbcDriver = jdbcDriver;
	}

	public void setDbUser(String dbUser) {
		this.dbUser = dbUser;
	}
	
	public void setBatchContainerHome(String batchContainerHome) {
		this.batchContainerHome = batchContainerHome;
	}
	
	public void setJndiName(String jndiName) {
		this.jndiName = jndiName;
	}
	

	@Override
	public String getBatchContainerHome() {
		return this.batchContainerHome;
	}
	
	@Override
	public String getJndiName() {
		return this.jndiName;
	}

	@Override
	public String getJdbcDriver() {
		return this.jdbcDriver;
	}

	@Override
	public String getDbUser() {
		return this.dbUser;
	}

	@Override
	public String getDbPassword() {
		return this.dbPassword;
	}

	public String getWorkManagerJndiName() {
		return workManagerJndiName;
	}

	public void setWorkManagerJndiName(String workManagerJndiName) {
		this.workManagerJndiName = workManagerJndiName;
	}

}
