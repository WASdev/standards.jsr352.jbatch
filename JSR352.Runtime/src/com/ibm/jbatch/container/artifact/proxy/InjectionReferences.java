package com.ibm.jbatch.container.artifact.proxy;

import java.util.List;

import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;

import com.ibm.jbatch.jsl.model.Property;



/**
 * This is a container class that holds on to the property and context injections
 * that should be injected into a batch artifact. 
 * 
 */
public class InjectionReferences {

    private final JobContext<?> jobContext;
    private final StepContext<?,?> stepContext;
    
    private List<Property> props;
    
    public InjectionReferences(JobContext<?> jobContext, StepContext<?, ?> stepContext, 
            List<Property> props) {

        this.jobContext = jobContext;
        this.stepContext = stepContext;
        this.props = props;
    }

    public JobContext<?> getJobContext() {
        return jobContext;
    }

    public StepContext<?, ?> getStepContext() {
        return stepContext;
    }

    public List<Property> getProps() {
        return props;
    }

    public void setProps(List<Property> props) {
        this.props = props;
    }
    
}
