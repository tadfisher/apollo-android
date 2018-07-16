package com.apollographql.apollo.compiler.java

import com.apollographql.apollo.compiler.Util
import com.apollographql.apollo.compiler.flatten
import com.apollographql.apollo.compiler.CodeGenerationContext
import com.apollographql.apollo.compiler.ir.Fragment
import com.apollographql.apollo.compiler.withBuilder
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.TypeSpec
import java.util.*
import javax.lang.model.element.Modifier

class FragmentGenerator(val fragment: Fragment) : JavaCodeGenerator {

  /** Returns the Java interface that represents this Fragment object. */
  override fun toTypeSpec(context: CodeGenerationContext, abstract: Boolean): TypeSpec {
    return SchemaTypeSpecBuilder(
        typeName = fragment.formatClassName(),
        schemaType = fragment.typeCondition,
        fields = fragment.fields,
        fragmentSpreads = fragment.fragmentSpreads,
        inlineFragments = fragment.inlineFragments,
        context = context,
        abstract = abstract
    )
        .build(Modifier.PUBLIC)
        .toBuilder()
        .addSuperinterface(ClassNames.FRAGMENT)
        .addAnnotation(Annotations.GENERATED_BY_APOLLO)
        .addFragmentDefinitionField()
        .addTypeConditionField()
        .build()
        .flatten(excludeTypeNames = listOf(
            Util.RESPONSE_FIELD_MAPPER_TYPE_NAME,
            (SchemaTypeSpecBuilder.FRAGMENTS_FIELD.type as ClassName).simpleName(),
            ClassNames.BUILDER.simpleName()
        ))
        .let {
          if (context.generateModelBuilder) {
            it.withBuilder()
          } else {
            it
          }
        }
  }

  private fun TypeSpec.Builder.addFragmentDefinitionField(): TypeSpec.Builder =
      addField(FieldSpec.builder(ClassNames.STRING, FRAGMENT_DEFINITION_FIELD_NAME)
          .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
          .initializer("\$S", fragment.source)
          .build())

  @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
  private fun TypeSpec.Builder.addTypeConditionField(): TypeSpec.Builder =
      addField(FieldSpec.builder(ClassNames.parameterizedListOf(java.lang.String::class.java), POSSIBLE_TYPES_VAR)
          .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
          .initializer(possibleTypesInitCode())
          .build())

  private fun possibleTypesInitCode(): CodeBlock {
    val initBuilder = CodeBlock.builder().add("\$T.unmodifiableList(\$T.asList(", Collections::class.java,
        Arrays::class.java)
    return fragment.possibleTypes.foldIndexed(initBuilder)
        { i, builder, type ->
          if (i > 0) {
            builder.add(",")
          }
          builder.add(" \$S", type)
        }
        .add("))").build()
  }

  companion object {
    const val FRAGMENT_DEFINITION_FIELD_NAME: String = "FRAGMENT_DEFINITION"
    const val POSSIBLE_TYPES_VAR: String = "POSSIBLE_TYPES"
  }
}