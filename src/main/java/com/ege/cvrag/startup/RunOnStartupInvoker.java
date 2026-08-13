package com.ege.cvrag.startup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

/**
 * Makes {@link RunOnStartup} work: once the application is fully started, scans
 * every bean for methods carrying the annotation and invokes them by reflection.
 *
 * This is how Spring's own method annotations (e.g. {@code @EventListener},
 * {@code @Scheduled}) are wired under the hood — an annotation is just metadata;
 * something has to find it and act on it.
 */
@Component
public class RunOnStartupInvoker {

    private static final Logger log = LoggerFactory.getLogger(RunOnStartupInvoker.class);

    private final ApplicationContext context;

    public RunOnStartupInvoker(ApplicationContext context) {
        this.context = context;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void invokeStartupMethods() {
        for (String beanName : context.getBeanDefinitionNames()) {
            Object bean;
            try {
                bean = context.getBean(beanName);
            } catch (RuntimeException ex) {
                continue; // e.g. a scoped bean not resolvable at startup — skip it
            }
            // Look at the real class behind any proxy so annotations are visible.
            Class<?> targetClass = AopProxyUtils.ultimateTargetClass(bean);
            ReflectionUtils.doWithMethods(
                    targetClass,
                    method -> invoke(bean, method, targetClass),
                    method -> method.isAnnotationPresent(RunOnStartup.class));
        }
    }

    private void invoke(Object bean, java.lang.reflect.Method method, Class<?> targetClass) {
        log.info("Invoking @RunOnStartup: {}.{}()", targetClass.getSimpleName(), method.getName());
        ReflectionUtils.makeAccessible(method);
        ReflectionUtils.invokeMethod(method, bean);
    }
}
