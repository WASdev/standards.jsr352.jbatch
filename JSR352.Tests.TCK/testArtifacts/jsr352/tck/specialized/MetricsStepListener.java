/**
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
package jsr352.tck.specialized;

import java.util.logging.Logger;

import javax.batch.annotation.BatchProperty;
import javax.batch.api.AbstractStepListener;
import javax.batch.runtime.Metric;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

@javax.inject.Named("metricsStepListener")
public class MetricsStepListener extends AbstractStepListener {

    @Inject
	StepContext stepCtx;
	
    @Inject
	JobContext JobCtx;
	
    @Inject    
    @BatchProperty(name="numberOfSkips")
    String skipNumberString;
	
    @Inject    
    @BatchProperty(name="ReadProcessWrite")
    String RPWString;
		
    private final static String sourceClass = MetricsStepListener.class.getName();
    private final static Logger logger = Logger.getLogger(sourceClass);
    
    public static final String GOOD_EXIT_STATUS_READ = "GOOD READ METRICS RESULT";
    public static final String GOOD_EXIT_STATUS_PROCESS = "GOOD PROCESS METRICS RESULT";
    public static final String GOOD_EXIT_STATUS_WRITE = "GOOD PROCESS METRICS RESULT";

    public static final String GOOD_EXIT_STATUS = "GOOD METRICS RESULT";
	public static final String BAD_EXIT_STATUS = "BAD RESULT";
	
	int skipNum;

    @Override
    public void beforeStep() {
        logger.finer("In before()");
        skipNum = Integer.parseInt(skipNumberString);
    }
    
    @Override
	public void afterStep() {
		logger.finer("In after()");

		Metric[] metrics = stepCtx.getMetrics();
		for (int i = 0; i < metrics.length; i++) {
			if (RPWString.equals("READ")) {
				if (metrics[i].getName().equals(Metric.MetricName.READCOUNT)) {
					if (metrics[i].getValue() == 30) {

						JobCtx.setExitStatus(GOOD_EXIT_STATUS_READ);
					} else {
						JobCtx.setExitStatus(BAD_EXIT_STATUS);
					}
				}
			} else if (RPWString.equals("READ_SKIP")) {
					if (metrics[i].getName().equals(Metric.MetricName.READSKIPCOUNT)) {
						if (metrics[i].getValue() == skipNum) {

							JobCtx.setExitStatus(GOOD_EXIT_STATUS_READ);
						} else {
							JobCtx.setExitStatus(BAD_EXIT_STATUS);
						}
					}
			} else if (RPWString.equals("PROCESS")) {
				if (metrics[i].getName().equals(Metric.MetricName.PROCESSSKIPCOUNT)) {
					if (metrics[i].getValue() == (skipNum)) {

						JobCtx.setExitStatus(GOOD_EXIT_STATUS_PROCESS);
					} else {
						JobCtx.setExitStatus(BAD_EXIT_STATUS);
					}
				}
			} else if (RPWString.equals("WRITE")) {
				if (metrics[i].getName().equals(Metric.MetricName.WRITECOUNT)) {
					if (metrics[i].getValue() == (30 - skipNum)) {

						JobCtx.setExitStatus(GOOD_EXIT_STATUS);
					} else {
						JobCtx.setExitStatus(BAD_EXIT_STATUS);
					}
				}
			
		} else if (RPWString.equals("WRITE_SKIP")) {
			if (metrics[i].getName().equals(Metric.MetricName.WRITESKIPCOUNT)) {
				if (metrics[i].getValue() == skipNum) {

					JobCtx.setExitStatus(GOOD_EXIT_STATUS);
				} else {
					JobCtx.setExitStatus(BAD_EXIT_STATUS);
				}
			}
		}
		}
	}
}
