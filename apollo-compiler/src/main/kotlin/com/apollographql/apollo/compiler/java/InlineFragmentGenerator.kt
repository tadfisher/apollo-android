package com.apollographql.apollo.compiler.java

import com.apollographql.apollo.compiler.CodeGenerationContext
import com.apollographql.apollo.compiler.ir.InlineFragment
import com.apollographql.apollo.compiler.withBuilder
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

class InlineFragmentGenerator(val inlineFragment: InlineFragment) : JavaCodeGenerator {

  override fun toTypeSpec(context: CodeGenerationContext, abstract: Boolean): TypeSpec {
    return SchemaTypeSpecBuilder(
        typeName = inlineFragment.formatClassName(),
        fields = inlineFragment.fields,
        fragmentSpreads = inlineFragment.fragmentSpreads ?: emptyList(),
        inlineFragments = emptyList(),
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

  fun fieldSpec(context: CodeGenerationContext, publicModifier: Boolean = false): FieldSpec {
    return FieldSpec.builder(typeName(context), inlineFragment.formatClassName().decapitalize())
        .let { if (publicModifier) it.addModifiers(Modifier.PUBLIC) else it }
        .addModifiers(Modifier.FINAL)
        .build()
  }

  private fun typeName(context: CodeGenerationContext) =
      JavaTypeResolver(context, "").resolve(inlineFragment.formatClassName(), true)
}