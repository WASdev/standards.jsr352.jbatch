/*
 * Copyright 2022 International Business Machines Corp.
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
package test.artifacts;

import jakarta.batch.api.Batchlet;
import jakarta.batch.runtime.context.JobContext;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import test.beans.MyBean;

@Dependent
@Named("weldArtifactFactoryBatchletDependent")
public class WeldArtifactFactoryDependentBatchlet implements Batchlet {

	@Inject MyBean bean;
	@Inject JobContext jobCtx;

	@Override
	public String process() throws Exception {
		jobCtx.setExitStatus(Integer.toString(bean.increment()));
		return null;
	}

	@Override
	public void stop() throws Exception {
		// TODO Auto-generated method stub

	}

}
