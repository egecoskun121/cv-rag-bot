package com.ege.cvrag.startup;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a (no-arg) bean method to be invoked once the application is fully started.
 *
 * The three meta-annotations are what make it work:
 *   @Target(METHOD)            — it can only go on a method,
 *   @Retention(RUNTIME)        — it's kept in the .class file and visible via
 *                                reflection at runtime (a compile-time-only
 *                                retention would be invisible to our scanner),
 *   @Documented                — it shows up in generated Javadoc.
 *
 * {@code RunOnStartupInvoker} finds methods carrying this annotation and calls
 * them on {@code ApplicationReadyEvent}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RunOnStartup {
}
