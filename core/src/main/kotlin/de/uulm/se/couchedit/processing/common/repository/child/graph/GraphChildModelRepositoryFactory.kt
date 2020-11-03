package de.uulm.se.couchedit.processing.common.repository.child.graph

import com.google.inject.Singleton
import de.uulm.se.couchedit.processing.common.repository.ModelRepository
import de.uulm.se.couchedit.processing.common.repository.ModelRepositoryRead
import de.uulm.se.couchedit.processing.common.repository.child.ChildModelRepositoryFactory
import de.uulm.se.couchedit.processing.common.repository.child.specification.ChildRepoSpec
import de.uulm.se.couchedit.processing.common.repository.graph.RootGraphBasedModelRepository

@Singleton
class GraphChildModelRepositoryFactory : ChildModelRepositoryFactory {
    override fun provideChildRepository(modelRepository: ModelRepository, spec: ChildRepoSpec): ModelRepositoryRead {
        if(modelRepository is RootGraphBasedModelRepository) {
            return GraphCalculatingChildModelRepository(modelRepository, spec)
        }

        throw IllegalArgumentException("Cannot create child from ${modelRepository::class.java.name}")
    }
}
