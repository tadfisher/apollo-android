package com.apollographql.apollo.compiler.java

import com.apollographql.apollo.compiler.escapeJavaReservedWord
import com.apollographql.apollo.compiler.CodeGenerationContext
import com.apollographql.apollo.compiler.ir.Field
import com.apollographql.apollo.compiler.ir.ScalarType
import com.apollographql.apollo.compiler.toJavaBeansSemanticNaming
import com.apollographql.apollo.compiler.withBuilder
import com.squareup.javapoet.*
import javax.lang.model.element.Modifier

class FieldGenerator(val field: Field) : JavaCodeGenerator {

  override fun toTypeSpec(context: CodeGenerationContext, abstract: Boolean): TypeSpec {
    val fields = if (field.isNonScalar()) field.fields!! else emptyList()
    return SchemaTypeSpecBuilder(
        typeName = field.formatClassName(),
        schemaType = field.type,
        fields = fields,
        fragmentSpreads = field.fragmentSpreads ?: emptyList(),
        inlineFragments = field.inlineFragments ?: emptyList(),
        context = context,
        abstract = abstract
    )
        .build(Modifier.PUBLIC, Modifier.STATIC)
        .let {
          if (context.generateModelBuilder) {
            it.withBuilder()
          } else {
            it
          }
        }
  }

  fun accessorMethodSpec(context: CodeGenerationContext): MethodSpec {
    val respName = field.responseName.escapeJavaReservedWord()
    val returnTypeName = toTypeName(field.methodResponseType(), context)
    val name = if (context.useJavaBeansSemanticNaming) {
      val isBooleanField = returnTypeName == TypeName.BOOLEAN || returnTypeName == TypeName.BOOLEAN.box()
      respName.toJavaBeansSemanticNaming(isBooleanField = isBooleanField)
    } else {
      respName
    }
    return MethodSpec.methodBuilder(name)
        .addModifiers(Modifier.PUBLIC)
        .returns(returnTypeName)
        .addStatement("return this.\$L", field.responseName.escapeJavaReservedWord())
        .let { if (field.description != null) it.addJavadoc("\$L\n", field.description) else it }
        .let {
          if (field.isDeprecated == true && !field.deprecationReason.isNullOrBlank()) {
            it.addJavadoc("@deprecated \$L\n", field.deprecationReason)
          } else {
            it
          }
        }
        .build()
  }

  fun fieldSpec(context: CodeGenerationContext): FieldSpec {
    return FieldSpec
        .builder(toTypeName(field.methodResponseType(), context), field.responseName.escapeJavaReservedWord())
        .addModifiers(Modifier.FINAL)
        .build()
  }

  fun argumentCodeBlock(): CodeBlock {
    val args = field.args.takeIf { it != null && it.isNotEmpty() } ?: return CodeBlock.of("null")

    val mapBuilderClass = ClassNames.parameterizedUnmodifiableMapBuilderOf(String::class.java, Any::class.java)
    return args
        .map { (name, value, type) ->
          @Suppress("UNCHECKED_CAST")
          when (value) {
            is Number -> {
              val scalarType = ScalarType.forName(type.removeSuffix("!"))
              when (scalarType) {
                is ScalarType.INT -> CodeBlock.of(".put(\$S, \$L)\n", name, value.toInt())
                is ScalarType.FLOAT -> CodeBlock.of(".put(\$S, \$Lf)\n", name, value.toDouble())
                else -> CodeBlock.of(".put(\$S, \$L)\n", name, value)
              }
            }
            is Boolean -> CodeBlock.of(".put(\$S, \$L)\n", name, value)
            is Map<*, *> -> CodeBlock.of(".put(\$S, \$L)\n", name, jsonMapToCodeBlock(value as Map<String, Any?>))
            else -> CodeBlock.of(".put(\$S, \$S)\n", name, value)
          }
        }
        .fold(CodeBlock.builder().add("new \$T(\$L)\n", mapBuilderClass, args.size), CodeBlock.Builder::add)
        .add(".build()")
        .build()
  }

  private fun jsonMapToCodeBlock(map: Map<String, Any?>): CodeBlock {
    val mapBuilderClass = ClassNames.parameterizedUnmodifiableMapBuilderOf(String::class.java, Any::class.java)
    return map
        .map { (key, value) ->
          if (value is Map<*, *>) {
            @Suppress("UNCHECKED_CAST")
            CodeBlock.of(".put(\$S, \$L)\n", key, jsonMapToCodeBlock(value as Map<String, Any?>))
          } else {
            CodeBlock.of(".put(\$S, \$S)\n", key, value)
          }
        }
        .fold(CodeBlock.builder().add("new \$T(\$L)\n", mapBuilderClass, map.size).indent(), CodeBlock.Builder::add)
        .add(".build()")
        .unindent()
        .build()
  }

  private fun toTypeName(responseType: String, context: CodeGenerationContext): TypeName {
    val packageName = if (field.isNonScalar()) "" else context.typesPackage
    return JavaTypeResolver(context, packageName, field.isDeprecated ?: false).resolve(responseType, field.isOptional())
  }
}