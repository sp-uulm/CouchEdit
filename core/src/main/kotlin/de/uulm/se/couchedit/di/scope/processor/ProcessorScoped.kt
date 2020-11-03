package de.uulm.se.couchedit.di.scope.processor

import com.google.inject.ScopeAnnotation

/**
 * This annotation marks any classes that have only one instance in the object tree of one Processor.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ScopeAnnotation
annotation class ProcessorScoped
