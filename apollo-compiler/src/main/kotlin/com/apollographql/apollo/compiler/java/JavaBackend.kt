package com.apollographql.apollo.compiler.java

import com.apollographql.apollo.compiler.Backend
import com.apollographql.apollo.compiler.formatPackageName
import com.apollographql.apollo.compiler.ir.CodeGenerationContext
import com.apollographql.apollo.compiler.ir.ScalarType
import com.apollographql.apollo.compiler.ir.TypeDeclaration
import com.apollographql.apollo.compiler.supportedTypeDeclarations
import com.squareup.javapoet.JavaFile
import java.io.File

class JavaBackend : Backend {
  override fun writeFiles(context: CodeGenerationContext, outputDir: File, outputPackageName: String?) {
    with (context.ir) {
      val javaContext = context.copy(customTypeMap = context.customTypeMap.supportedTypeMap(typesUsed))

      fragments.forEach {
        val typeSpec = FragmentGenerator(it).toTypeSpec(javaContext.copy())
        JavaFile.builder(javaContext.fragmentsPackage, typeSpec).build().writeTo(outputDir)
      }

      typesUsed.supportedTypeDeclarations().forEach {
        val typeSpec = TypeDeclarationGenerator(it).toTypeSpec(javaContext.copy())
        JavaFile.builder(javaContext.typesPackage, typeSpec).build().writeTo(outputDir)
      }

      if (javaContext.customTypeMap.isNotEmpty()) {
        val typeSpec = CustomEnumTypeSpecBuilder(javaContext.copy()).build()
        JavaFile.builder(javaContext.typesPackage, typeSpec).build().writeTo(outputDir)
      }

      operations.map { OperationTypeSpecBuilder(it, fragments, context.useSemanticNaming) }
          .forEach {
            val packageName = outputPackageName ?: it.operation.filePath.formatPackageName()
            val typeSpec = it.toTypeSpec(javaContext.copy())
            JavaFile.builder(packageName, typeSpec).build().writeTo(outputDir)
          }
    }
  }

  private fun Map<String, String>.supportedTypeMap(typeDeclarations: List<TypeDeclaration>): Map<String, String> {
    val idScalarTypeMap = ScalarType.ID.name to (this[ScalarType.ID.name] ?: ClassNames.STRING.toString())
    return typeDeclarations.filter { it.kind == TypeDeclaration.KIND_SCALAR_TYPE }
        .associate { it.name to (this[it.name] ?: ClassNames.OBJECT.toString()) }
        .plus(idScalarTypeMap)
  }
}
