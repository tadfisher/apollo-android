package com.apollographql.apollo.compiler

import com.apollographql.apollo.compiler.ir.CodeGenerationContext
import com.apollographql.apollo.compiler.ir.TypeDeclaration
import java.io.File

interface Backend {
  fun writeFiles(context: CodeGenerationContext, outputDir: File, outputPackageName: String? = null)
}
