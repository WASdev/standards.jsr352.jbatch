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
package com.ibm.batch.container.artifact.proxy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.batch.annotation.CheckpointListener;
import javax.batch.annotation.FlowListener;
import javax.batch.annotation.ItemProcessListener;
import javax.batch.annotation.ItemReadListener;
import javax.batch.annotation.ItemWriteListener;
import javax.batch.annotation.JobListener;
import javax.batch.annotation.RetryListener;
import javax.batch.annotation.SkipListener;
import javax.batch.annotation.SplitListener;
import javax.batch.annotation.StepListener;

import jsr352.batch.jsl.Flow;
import jsr352.batch.jsl.JSLJob;
import jsr352.batch.jsl.Listener;
import jsr352.batch.jsl.Listeners;
import jsr352.batch.jsl.Property;
import jsr352.batch.jsl.Split;
import jsr352.batch.jsl.Step;

public class ListenerFactory {

    private List<ListenerInfo> jobLevelListenerInfo = null;          

    private Map<String, List<ListenerInfo>> stepLevelListenerInfo =  
        new HashMap<String, List<ListenerInfo>>();

    private Map<String, List<ListenerInfo>> splitLevelListenerInfo =  
        new HashMap<String, List<ListenerInfo>>();

    private Map<String, List<ListenerInfo>> flowLevelListenerInfo =  
        new HashMap<String, List<ListenerInfo>>();
    
    /*
     * Build job-level ListenerInfo(s) up-front, but build step-level ones lazily.
     */
    public ListenerFactory(JSLJob jobModel) {     
        initJobLevelListeners(jobModel);
    }

    private void initJobLevelListeners(JSLJob jobModel) {
        jobLevelListenerInfo = new ArrayList<ListenerInfo>();

        Listeners jobLevelListeners = jobModel.getListeners();

        if (jobLevelListeners != null) {
            for (Listener listener : jobLevelListeners.getListenerList()) {
                ListenerInfo info = buildListenerInfo(listener);
                jobLevelListenerInfo.add(info);
            }
        }
    }

    /*
     * Does NOT throw an exception if a step-level listener is annotated with
     * @JobListener, even if that is the only type of listener annotation found. 
     */   
    private synchronized List<ListenerInfo> getStepListenerInfo(Step step) {
        if (!stepLevelListenerInfo.containsKey(step.getId())) {
            List<ListenerInfo> stepListenerInfoList = 
                new ArrayList<ListenerInfo>();
            stepLevelListenerInfo.put(step.getId(), stepListenerInfoList);

            Listeners stepLevelListeners = step.getListeners();
            if (stepLevelListeners != null) {
                for (Listener listener : stepLevelListeners.getListenerList()) {
                    ListenerInfo info = buildListenerInfo(listener);
                    stepListenerInfoList.add(info);
                }
            }

            return stepListenerInfoList;
        } else {
            return stepLevelListenerInfo.get(step.getId());
        }
    }

    /*
     * Does NOT throw an exception if a split-level listener is annotated with
     * @JobListener, even if that is the only type of listener annotation found. 
     */   
    private synchronized List<ListenerInfo> getSplitListenerInfo(Split split) {
        if (!splitLevelListenerInfo.containsKey(split.getId())) {
            List<ListenerInfo> splitListenerInfoList = 
                new ArrayList<ListenerInfo>();
            splitLevelListenerInfo.put(split.getId(), splitListenerInfoList);

            Listeners splitLevelListeners = split.getListeners();
            if (splitLevelListeners != null) {
                for (Listener listener : splitLevelListeners.getListenerList()) {
                    ListenerInfo info = buildListenerInfo(listener);
                    splitListenerInfoList.add(info);
                }
            }

            return splitListenerInfoList;
        } else {
            return splitLevelListenerInfo.get(split.getId());
        }
    }

    /*
     * Does NOT throw an exception if a flow-level listener is annotated with
     * @JobListener, even if that is the only type of listener annotation found. 
     */   
    private synchronized List<ListenerInfo> getFlowListenerInfo(Flow flow) {
        if (!flowLevelListenerInfo.containsKey(flow.getId())) {
            List<ListenerInfo> flowListenerInfoList = 
                new ArrayList<ListenerInfo>();
            flowLevelListenerInfo.put(flow.getId(), flowListenerInfoList);

            Listeners flowLevelListeners = flow.getListeners();
            if (flowLevelListeners != null) {
                for (Listener listener : flowLevelListeners.getListenerList()) {
                    ListenerInfo info = buildListenerInfo(listener);
                    flowListenerInfoList.add(info);
                }
            }

            return flowListenerInfoList;
        } else {
            return flowLevelListenerInfo.get(flow.getId());
        }
    }
    
    private ListenerInfo buildListenerInfo(Listener listener) {

        String id = listener.getRef();

        List<Property> propList = (listener.getProperties() == null) ? null : listener.getProperties().getPropertyList();
        Object listenerArtifact = ProxyFactory.loadArtifact(id);
        if (listenerArtifact == null) {
            throw new IllegalArgumentException("Load of artifact id: " + id + " returned <null>.");
        }
        ListenerInfo info = new ListenerInfo(listenerArtifact, propList);
        return info;

    }

    public List<JobListenerProxy> getJobListeners() {
        List<JobListenerProxy> retVal = new ArrayList<JobListenerProxy>();
        for (ListenerInfo li : jobLevelListenerInfo) {
            if (li.isJobListener()) {
                JobListenerProxy proxy = new JobListenerProxy(li.getArtifact(), li.getPropList());
                retVal.add(proxy);
            }
        }
        return retVal;
    }

    public List<CheckpointListenerProxy> getCheckpointListeners(Step step) {

        List<ListenerInfo> stepListenerInfo = getStepListenerInfo(step);

        List<CheckpointListenerProxy> retVal = new ArrayList<CheckpointListenerProxy>();

        for (ListenerInfo li : jobLevelListenerInfo) {
            if (li.isCheckpointListener()) {
                CheckpointListenerProxy proxy = new CheckpointListenerProxy(li.getArtifact(), li.getPropList());
                retVal.add(proxy);
            }
        }
        for (ListenerInfo li : stepListenerInfo) {
            if (li.isCheckpointListener()) {
                CheckpointListenerProxy proxy = new CheckpointListenerProxy(li.getArtifact(), li.getPropList());
                retVal.add(proxy);
            }
        }

        return retVal;
    }

    public List<ItemProcessListenerProxy> getItemProcessListeners(Step step) {

        List<ListenerInfo> stepListenerInfo = getStepListenerInfo(step);

        List<ItemProcessListenerProxy> retVal = new ArrayList<ItemProcessListenerProxy>();

        for (ListenerInfo li : jobLevelListenerInfo) {
            if (li.isItemProcessListener()) {
                ItemProcessListenerProxy proxy = new ItemProcessListenerProxy(li.getArtifact(), li.getPropList());
                retVal.add(proxy);
            }
        }
        for (ListenerInfo li : stepListenerInfo) {
            if (li.isItemProcessListener()) {
                ItemProcessListenerProxy proxy = new ItemProcessListenerProxy(li.getArtifact(), li.getPropList());
                retVal.add(proxy);
            }
        }

        return retVal;
    }

    public List<ItemReadListenerProxy> getItemReadListeners(Step step) {

        List<ListenerInfo> stepListenerInfo = getStepListenerInfo(step);

        List<ItemReadListenerProxy> retVal = new ArrayList<ItemReadListenerProxy>();

        for (ListenerInfo li : jobLevelListenerInfo) {
            if (li.isItemReadListener()) {
                ItemReadListenerProxy proxy = new ItemReadListenerProxy(li.getArtifact(), li.getPropList());
                retVal.add(proxy);
            }
        }
        for (ListenerInfo li : stepListenerInfo) {
            if (li.isItemReadListener()) {
                ItemReadListenerProxy proxy = new ItemReadListenerProxy(li.getArtifact(), li.getPropList());
                retVal.add(proxy);
            }
        }

        return retVal;
    }

    public List<ItemWriteListenerProxy> getItemWriteListeners(Step step) {
        List<ListenerInfo> stepListenerInfo = getStepListenerInfo(step);

        List<ItemWriteListenerProxy> retVal = new ArrayList<ItemWriteListenerProxy>();

        for (ListenerInfo li : jobLevelListenerInfo) {
            if (li.isItemWriteListener()) {
                ItemWriteListenerProxy proxy = new ItemWriteListenerProxy(li.getArtifact(), li.getPropList());
                retVal.add(proxy);
            }
        }
        for (ListenerInfo li : stepListenerInfo) {
            if (li.isItemWriteListener()) {
                ItemWriteListenerProxy proxy = new ItemWriteListenerProxy(li.getArtifact(), li.getPropList());
                retVal.add(proxy);
            }
        }

        return retVal;
    }


    public List<RetryListenerProxy> getRetryListeners(Step step) {
        List<ListenerInfo> stepListenerInfo = getStepListenerInfo(step);

        List<RetryListenerProxy> retVal = new ArrayList<RetryListenerProxy>();

        for (ListenerInfo li : jobLevelListenerInfo) {
            if (li.isRetryListener()) {
                RetryListenerProxy proxy = new RetryListenerProxy(li.getArtifact(), li.getPropList());
                retVal.add(proxy);
            }
        }
        for (ListenerInfo li : stepListenerInfo) {
            if (li.isRetryListener()) {
                RetryListenerProxy proxy = new RetryListenerProxy(li.getArtifact(), li.getPropList());
                retVal.add(proxy);
            }
        }

        return retVal;
    }

    public List<SkipListenerProxy> getSkipListeners(Step step) {
        List<ListenerInfo> stepListenerInfo = getStepListenerInfo(step);

        List<SkipListenerProxy> retVal = new ArrayList<SkipListenerProxy>();

        for (ListenerInfo li : jobLevelListenerInfo) {
            if (li.isSkipListener()) {
                SkipListenerProxy proxy = new SkipListenerProxy(li.getArtifact(), li.getPropList());
                retVal.add(proxy);
            }
        }
        for (ListenerInfo li : stepListenerInfo) {
            if (li.isSkipListener()) {
                SkipListenerProxy proxy = new SkipListenerProxy(li.getArtifact(), li.getPropList());
                retVal.add(proxy);
            }
        }

        return retVal;
    }

    public List<StepListenerProxy> getStepListeners(Step step) {
        List<ListenerInfo> stepListenerInfo = getStepListenerInfo(step);

        List<StepListenerProxy> retVal = new ArrayList<StepListenerProxy>();

        for (ListenerInfo li : jobLevelListenerInfo) {
            if (li.isStepListener()) {
                StepListenerProxy proxy = new StepListenerProxy(li.getArtifact(), li.getPropList());
                retVal.add(proxy);
            }
        }
        for (ListenerInfo li : stepListenerInfo) {
            if (li.isStepListener()) {
                StepListenerProxy proxy = new StepListenerProxy(li.getArtifact(), li.getPropList());
                retVal.add(proxy);
            }
        }

        return retVal;
    }

    public List<SplitListenerProxy> getSplitListeners(Split split) {
        List<ListenerInfo> splitListenerInfo = getSplitListenerInfo(split);

        List<SplitListenerProxy> retVal = new ArrayList<SplitListenerProxy>();

        for (ListenerInfo li : jobLevelListenerInfo) {
            if (li.isSplitListener()) {
                SplitListenerProxy proxy = new SplitListenerProxy(li.getArtifact(), li.getPropList());
                retVal.add(proxy);
            }
        }
        for (ListenerInfo li : splitListenerInfo) {
            if (li.isSplitListener()) {
                SplitListenerProxy proxy = new SplitListenerProxy(li.getArtifact(), li.getPropList());
                retVal.add(proxy);
            }
        }

        return retVal;
    }
    
    public List<FlowListenerProxy> getFlowListeners(Flow flow) {
        List<ListenerInfo> flowListenerInfo = getFlowListenerInfo(flow);

        List<FlowListenerProxy> retVal = new ArrayList<FlowListenerProxy>();

        for (ListenerInfo li : jobLevelListenerInfo) {
            if (li.isFlowListener()) {
                FlowListenerProxy proxy = new FlowListenerProxy(li.getArtifact(), li.getPropList());
                retVal.add(proxy);
            }
        }
        for (ListenerInfo li : flowListenerInfo) {
            if (li.isFlowListener()) {
                FlowListenerProxy proxy = new FlowListenerProxy(li.getArtifact(), li.getPropList());
                retVal.add(proxy);
            }
        }

        return retVal;
    }
    
    
    private class ListenerInfo {
        Object listenerArtifact = null;
        Class listenerArtifactClass = null;
        List<Property> propList = null;

        Object getArtifact() {
            return listenerArtifact;
        }

        private ListenerInfo(Object listenerArtifact, List<Property> propList) {
            this.listenerArtifact = listenerArtifact;
            this.listenerArtifactClass = listenerArtifact.getClass();
            this.propList = propList;
        }

        boolean isJobListener() {
            return listenerArtifactClass.isAnnotationPresent(JobListener.class);   
        }

        boolean isSplitListener() {
            return listenerArtifactClass.isAnnotationPresent(SplitListener.class);
        }
        
        boolean isFlowListener() {
            return listenerArtifactClass.isAnnotationPresent(FlowListener.class);
        }
        
        boolean isStepListener() {
            return listenerArtifactClass.isAnnotationPresent(StepListener.class);
        }

        boolean isCheckpointListener() {
            return listenerArtifactClass.isAnnotationPresent(CheckpointListener.class);
        }

        boolean isItemProcessListener() {
            return listenerArtifactClass.isAnnotationPresent(ItemProcessListener.class);
        }
        boolean isItemReadListener() {
            return listenerArtifactClass.isAnnotationPresent(ItemReadListener.class);
        }
        boolean isItemWriteListener() {
            return listenerArtifactClass.isAnnotationPresent(ItemWriteListener.class);
        }

        boolean isRetryListener() {
            return listenerArtifactClass.isAnnotationPresent(RetryListener.class);
        }
        boolean isSkipListener() {
            return listenerArtifactClass.isAnnotationPresent(SkipListener.class);
        }

        List<Property> getPropList() {
            return propList;
        }

        void setPropList(List<Property> propList) {
            this.propList = propList;
        }
    }
}
