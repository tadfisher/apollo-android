package com.apollographql.apollo.compiler.java

import com.apollographql.apollo.compiler.java.ClassNames.parameterizedGuavaOptional
import com.apollographql.apollo.compiler.java.ClassNames.parameterizedInputType
import com.apollographql.apollo.compiler.java.ClassNames.parameterizedJavaOptional
import com.apollographql.apollo.compiler.java.ClassNames.parameterizedOptional
import com.apollographql.apollo.compiler.NullableValueType
import com.apollographql.apollo.compiler.CodeGenerationContext
import com.apollographql.apollo.compiler.ir.ScalarType
import com.apollographql.apollo.compiler.toJavaType
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName

class JavaTypeResolver(
    private val context: CodeGenerationContext,
    private val packageName: String,
    private val deprecated: Boolean = false
) {
  fun resolve(typeName: String, isOptional: Boolean = !typeName.endsWith("!"),
      nullableValueType: NullableValueType? = null): TypeName {
    val normalizedTypeName = typeName.removeSuffix("!")
    val isList = normalizedTypeName.startsWith('[') && normalizedTypeName.endsWith(']')
    val customScalarType = context.customTypeMap[normalizedTypeName]
    val javaType = when {
      isList -> ClassNames.parameterizedListOf(
          resolve(normalizedTypeName.removeSurrounding("[", "]"), false))
      normalizedTypeName == ScalarType.STRING.name -> ClassNames.STRING
      normalizedTypeName == ScalarType.INT.name -> if (isOptional) TypeName.INT.box() else TypeName.INT
      normalizedTypeName == ScalarType.BOOLEAN.name -> if (isOptional) TypeName.BOOLEAN.box() else TypeName.BOOLEAN
      normalizedTypeName == ScalarType.FLOAT.name -> if (isOptional) TypeName.DOUBLE.box() else TypeName.DOUBLE
      customScalarType != null -> customScalarType.toJavaType()
      else -> ClassName.get(packageName, normalizedTypeName)
    }

    return when {
      javaType.isPrimitive -> javaType
      isOptional -> when (nullableValueType ?: context.nullableValueType) {
        NullableValueType.APOLLO_OPTIONAL -> parameterizedOptional(javaType)
        NullableValueType.GUAVA_OPTIONAL -> parameterizedGuavaOptional(javaType)
        NullableValueType.JAVA_OPTIONAL -> parameterizedJavaOptional(javaType)
        NullableValueType.INPUT_TYPE -> parameterizedInputType(javaType)
        else -> javaType.annotated(Annotations.NULLABLE)
      }
      else -> javaType.annotated(Annotations.NONNULL)
    }.let {
      if (deprecated) it.annotated(Annotations.DEPRECATED) else it
    }
  }
}