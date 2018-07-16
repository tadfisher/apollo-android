package com.apollographql.apollo.compiler.java

import com.apollographql.apollo.compiler.CodeGenerationContext
import com.squareup.javapoet.TypeSpec

interface JavaCodeGenerator {
  fun toTypeSpec(context: CodeGenerationContext, abstract: Boolean = false): TypeSpec
}