package de.uulm.se.couchedit

import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

/**
 * This class is to be used for all tests involving RxJava Observables
 * Ensures that all actions that subscribers do are executed in the test runner thread, so assertions can be made
 * after the function call returns.
 *
 * https://www.infoq.com/articles/Testing-RxJava2
 */
abstract class RxJavaTestCase {
    @BeforeEach
    fun setSynchronizedSchedulers() {
        RxJavaPlugins.setIoSchedulerHandler { _ -> Schedulers.trampoline() }
        RxJavaPlugins.setComputationSchedulerHandler { _ -> Schedulers.trampoline() }
    }

    @AfterEach
    fun resetRxJavaSchedulers() {
        RxJavaPlugins.reset()
    }
}
