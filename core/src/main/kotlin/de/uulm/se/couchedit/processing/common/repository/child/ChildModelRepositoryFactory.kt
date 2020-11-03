package de.uulm.se.couchedit.processing.common.repository.child

import de.uulm.se.couchedit.processing.common.repository.ModelRepository
import de.uulm.se.couchedit.processing.common.repository.ModelRepositoryRead
import de.uulm.se.couchedit.processing.common.repository.child.specification.ChildRepoSpec

/**
 * Interface for a service that provides a repository showing a defined subset of the element graph in a given
 * ModelRepository.
 */
interface ChildModelRepositoryFactory {
    fun provideChildRepository(modelRepository: ModelRepository, spec: ChildRepoSpec): ModelRepositoryRead
}
