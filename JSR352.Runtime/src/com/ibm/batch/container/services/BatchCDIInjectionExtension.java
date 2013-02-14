package com.ibm.batch.container.services;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;

public class BatchCDIInjectionExtension implements Extension {

    private final static String sourceClass = BatchProducerBean.class.getName();
    private final static Logger logger = Logger.getLogger(sourceClass);


    BeanManager beanManager;

    void beforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd, BeanManager bm) {

        logger.log(Level.FINE, "BatchCDIInjectionExtension.beforeBeanDiscovery() bm=" + bm);

        beanManager = bm;

        AnnotatedType<BatchProducerBean> at = bm.createAnnotatedType(BatchProducerBean.class);
        bbd.addAnnotatedType(at);
        
        logger.log(Level.FINE, "BatchCDIInjectionExtension.beforeBeanDiscovery() added annotated type: " + BatchProducerBean.class.getName());
    }

    
    public void afterBeanDiscovery(@Observes AfterBeanDiscovery abd) {

        logger.log(Level.FINE, "BatchCDIInjectionExtension.afterBeanDiscovery() adding bean.");

        abd.addBean(new BatchProducerBean());

        logger.log(Level.FINE, "BatchCDIInjectionExtension.afterBeanDiscovery() added bean.");

    }
}
