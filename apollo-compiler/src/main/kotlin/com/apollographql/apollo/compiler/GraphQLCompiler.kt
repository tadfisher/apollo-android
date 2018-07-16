package com.apollographql.apollo.compiler

import com.apollographql.apollo.compiler.ir.CodeGenerationContext
import com.apollographql.apollo.compiler.ir.CodeGenerationIR
import com.apollographql.apollo.compiler.java.JavaBackend
import com.squareup.moshi.Moshi
import java.io.File

class GraphQLCompiler {
  fun write(args: Arguments) {
    val context = args.toCodeGenerationContext()
    if (args.irPackageName.isNotEmpty()) {
      File(args.outputDir, args.irPackageName.replace('.', File.separatorChar)).deleteRecursively()
    }

    val backend = JavaBackend()
    backend.writeFiles(
        context = context,
        outputDir = args.outputDir,
        outputPackageName = args.outputPackageName
    )
  }

  companion object {
    const val FILE_EXTENSION = "graphql"
    @JvmField
    val OUTPUT_DIRECTORY = listOf("generated", "source", "apollo")
    const val APOLLOCODEGEN_VERSION = "0.19.1"
  }

  data class Arguments(
      val irFile: File,
      val outputDir: File,
      val customTypeMap: Map<String, String>,
      val nullableValueType: NullableValueType,
      val useSemanticNaming: Boolean,
      val generateModelBuilder: Boolean,
      val useJavaBeansSemanticNaming: Boolean,
      val outputPackageName: String?,
      val suppressRawTypesWarning: Boolean
  ) {
    internal val irPackageName: String by lazy {
      outputPackageName ?: irFile.absolutePath.formatPackageName()
    }
  }
}

internal fun GraphQLCompiler.Arguments.toCodeGenerationContext(): CodeGenerationContext {
  val irAdapter = Moshi.Builder().build().adapter(CodeGenerationIR::class.java)
  val ir = irAdapter.fromJson(irFile.readText())!!
  val fragmentsPackage = if (irPackageName.isNotEmpty()) "$irPackageName.fragment" else "fragment"
  val typesPackage = if (irPackageName.isNotEmpty()) "$irPackageName.type" else "type"
  return CodeGenerationContext(
      reservedTypeNames = emptyList(),
      typeDeclarations = ir.typesUsed,
      fragmentsPackage = fragmentsPackage,
      typesPackage = typesPackage,
      customTypeMap = customTypeMap,
      nullableValueType = nullableValueType,
      ir = ir,
      useSemanticNaming = useSemanticNaming,
      generateModelBuilder = generateModelBuilder,
      useJavaBeansSemanticNaming = useJavaBeansSemanticNaming,
      suppressRawTypesWarning = suppressRawTypesWarning
  )
}
