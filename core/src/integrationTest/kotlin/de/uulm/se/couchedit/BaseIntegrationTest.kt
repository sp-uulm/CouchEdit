package de.uulm.se.couchedit

import com.google.inject.Guice
import com.google.inject.Injector
import de.uulm.se.couchedit.di.CoreModule
import de.uulm.se.couchedit.processing.common.repository.ModelRepository
import de.uulm.se.couchedit.processing.common.repository.RelationCache
import de.uulm.se.couchedit.processing.common.repository.graph.RootGraphBasedModelRepository
import de.uulm.se.couchedit.processing.common.services.diff.Applicator
import de.uulm.se.couchedit.processing.common.services.diff.DiffCollectionFactory
import de.uulm.se.couchedit.processing.common.services.diff.VersionManager
import de.uulm.se.couchedit.testsuiteutils.controller.CouchEditTest
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class BaseIntegrationTest : CouchEditTest() {
    val guiceInjector: Injector by disposableLazy {
        Guice.createInjector(CoreModule())
    }

    /**
     * DiffCollectionFactory used as a dependency of the [testApplicator]
     */
    protected val testDiffCollectionFactory: DiffCollectionFactory by disposableLazy {
        guiceInjector.getInstance(DiffCollectionFactory::class.java)
    }


    /**
     * ModelRepository instance that can be used to store results of the system under test and then
     * make assertions on the resulting Element graph
     */
    protected val testModelRepository: ModelRepository by disposableLazy {
        RootGraphBasedModelRepository(testDiffCollectionFactory, RelationCache(), VersionManager("TestModelRepo"))
    }

    /**
     * Applicator instance that can be used to store results of the system under test in the [testModelRepository]
     * so that assertions can be made on the full Element graph
     */
    protected val testApplicator: Applicator by disposableLazy {
        Applicator(testModelRepository, testDiffCollectionFactory)
    }
}
