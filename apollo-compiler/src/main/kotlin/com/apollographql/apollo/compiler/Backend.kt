package com.apollographql.apollo.compiler

import java.io.File

interface Backend {
  fun writeFiles(context: CodeGenerationContext, outputDir: File, outputPackageName: String? = null)
}
