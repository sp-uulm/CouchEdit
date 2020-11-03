package de.uulm.se.couchedit.serialization.di

import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.owlike.genson.Genson
import com.owlike.genson.GensonBuilder
import de.uulm.se.couchedit.export.di.ExportModule
import de.uulm.se.couchedit.serialization.controller.Persister
import de.uulm.se.couchedit.serialization.controller.impl.GensonPersister

class PersistenceModule(private val exportModule: ExportModule) : AbstractModule() {
    override fun configure() {
        install(exportModule)

        bind(Persister::class.java).to(GensonPersister::class.java)
    }

    @Provides
    private fun provideGenson(): Genson {
        val builder = GensonBuilder().apply {
            useClassMetadata(true)
            useRuntimeType(true)
            useIndentation(true)
        }

        return builder.create()
    }
}
