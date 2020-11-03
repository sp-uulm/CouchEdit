package de.uulm.se.couchedit.client.view

import com.google.inject.Guice
import de.uulm.se.couchedit.client.di.FrontendModule
import de.uulm.se.couchedit.client.style.InPlaceEditStyleSheet
import de.uulm.se.couchedit.client.view.workspace.CouchWorkspace
import de.uulm.se.couchedit.processing.common.modbus.ModificationBusManager
import tornadofx.*
import kotlin.reflect.KClass

class CouchApplication : App(CouchWorkspace::class) {
    val guice = Guice.createInjector(FrontendModule())

    // Just initialize this here to trigger construction of the Processing object tree
    val processorManager = guice.getInstance(ModificationBusManager::class.java)

    init {
        FX.dicontainer = object : DIContainer {
            override fun <T : Any> getInstance(type: KClass<T>) = guice.getInstance(type.java)
        }

        importStylesheet(InPlaceEditStyleSheet::class)
    }
}
