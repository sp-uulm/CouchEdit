package de.uulm.se.couchedit.export.di

import com.google.common.reflect.TypeToken
import com.google.inject.AbstractModule
import com.google.inject.TypeLiteral
import com.google.inject.multibindings.MapBinder
import de.uulm.se.couchedit.export.converters.AggregateConverter
import de.uulm.se.couchedit.export.converters.AggregateConverterImpl
import de.uulm.se.couchedit.export.converters.Converter
import de.uulm.se.couchedit.export.converters.attributes.AttributeBagConverter
import de.uulm.se.couchedit.export.converters.attributes.AttributeConverter
import de.uulm.se.couchedit.export.converters.attributes.AttributesForConverter
import de.uulm.se.couchedit.export.converters.basic.EnumConverter
import de.uulm.se.couchedit.export.converters.connection.ConnectionEndConverter
import de.uulm.se.couchedit.export.converters.element.ElementReferenceConverter
import de.uulm.se.couchedit.export.converters.element.ProbabilityConverter
import de.uulm.se.couchedit.export.converters.element.VectorTimestampConverter
import de.uulm.se.couchedit.export.converters.graphic.PrimitiveGraphicObjectConverter
import de.uulm.se.couchedit.export.converters.graphic.composition.ComponentOfConverter
import de.uulm.se.couchedit.export.converters.graphic.shapes.*
import de.uulm.se.couchedit.export.model.SerializableObject
import kotlin.reflect.jvm.javaMethod

open class ExportModule : AbstractModule() {
    override fun configure() {
        this.bind(AggregateConverter::class.java).to(AggregateConverterImpl::class.java)

        this.bindConverters()
    }

    private fun bindConverters() {
        val tlAnyClass = object : TypeLiteral<Class<*>>() {}
        val tlConverterClass = object : TypeLiteral<Converter<*, *>>() {}
        val tlSerObjectClass = object : TypeLiteral<Class<SerializableObject>>() {}

        val mapBinderToSerializable = MapBinder.newMapBinder(binder(), tlAnyClass, tlConverterClass)
        val mapBinderFromSerializable = MapBinder.newMapBinder(binder(), tlSerObjectClass, tlConverterClass)

        for (converter in getConverters()) {
            // use Guava TypeToken to retrieve the generic type so we don't have to repeat it every time.
            val typeToken = TypeToken.of(converter)

            /*
             * For the converter class, the first type argument is the converted type, while the second one is the
             * output SerializableObject.
             */
            val fromSerializableMethod = typeToken.method(Converter<*, *>::fromSerializable.javaMethod!!)
            val toSerializableMethod = typeToken.method(Converter<*, *>::toSerializable.javaMethod!!)

            val inputType = toSerializableMethod.parameters.first().type.rawType
            @Suppress("UNCHECKED_CAST") // ensured by the constraint in Converter class definition
            val serializableObjectType = fromSerializableMethod.parameters.first().type.rawType as Class<SerializableObject>

            mapBinderFromSerializable.addBinding(serializableObjectType).to(converter)
            mapBinderToSerializable.addBinding(inputType).to(converter)
        }
    }

    protected open fun getConverters(): List<Class<out Converter<*, *>>> {
        return listOf(
                EnumConverter::class.java,
                VectorTimestampConverter::class.java,
                ElementReferenceConverter::class.java,
                ProbabilityConverter::class.java
        ) + getGraphicConverters() + getAttributeConverters() + getConnectionConverters()
    }

    /**
     * Export converters for Attribute data model
     */
    private fun getAttributeConverters(): List<Class<out Converter<*, *>>> {
        return listOf(
                AttributeConverter::class.java,
                AttributeBagConverter::class.java,
                AttributesForConverter::class.java
        )
    }

    /**
     * Export converters for Graphic data model
     */
    private fun getGraphicConverters(): List<Class<out Converter<*, *>>> {
        return listOf(
                LabelConverter::class.java,
                PointConverter::class.java,
                PolygonConverter::class.java,
                RectangleConverter::class.java,
                RoundedRectangleConverter::class.java,
                StraightSegmentLineConverter::class.java,
                PrimitiveGraphicObjectConverter::class.java,
                ComponentOfConverter::class.java
        )
    }

    /**
     * Export converters for Connection data model
     */
    private fun getConnectionConverters(): List<Class<out Converter<*, *>>> {
        return listOf(ConnectionEndConverter::class.java)
    }
}
