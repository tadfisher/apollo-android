package com.apollographql.apollo.compiler.java

import com.apollographql.apollo.compiler.escapeJavaReservedWord
import com.apollographql.apollo.compiler.ir.CodeGenerationContext
import com.apollographql.apollo.compiler.ir.TypeDeclaration
import com.apollographql.apollo.compiler.ir.TypeDeclaration.Companion.ENUM_SAFE_VALUE_OF
import com.apollographql.apollo.compiler.ir.TypeDeclaration.Companion.ENUM_UNKNOWN_CONSTANT
import com.apollographql.apollo.compiler.ir.TypeDeclaration.Companion.KIND_ENUM
import com.apollographql.apollo.compiler.ir.TypeDeclaration.Companion.KIND_INPUT_OBJECT_TYPE
import com.squareup.javapoet.*
import javax.lang.model.element.Modifier

class TypeDeclarationGenerator(val typeDeclaration: TypeDeclaration) : JavaCodeGenerator {
  override fun toTypeSpec(context: CodeGenerationContext, abstract: Boolean): TypeSpec {
    return when (typeDeclaration.kind) {
      KIND_ENUM -> enumTypeToTypeSpec()
      KIND_INPUT_OBJECT_TYPE -> inputObjectToTypeSpec(context)
      else -> throw UnsupportedOperationException("unsupported ${typeDeclaration.kind} type declaration")
    }
  }

  private fun enumTypeToTypeSpec(): TypeSpec {
    val enumConstants = typeDeclaration.values?.map { value ->
      value.name to TypeSpec.anonymousClassBuilder("\$S", value.name)
          .apply {
            if (!value.description.isNullOrEmpty()) {
              addJavadoc("\$L\n", value.description)
            }
          }
          .apply {
            if (value.isDeprecated == true) {
              addAnnotation(Annotations.DEPRECATED)
              if (!value.deprecationReason.isNullOrBlank()) {
                addJavadoc("@deprecated \$L\n", value.deprecationReason)
              }
            }
          }
          .build()
    }
    val unknownConstantTypeSpec = TypeSpec.anonymousClassBuilder("\$S", "\$UNKNOWN")
        .addJavadoc("\$L\n", "Auto generated constant for unknown enum values")
        .build()
    val safeValueOfMethodSpec = MethodSpec.methodBuilder(ENUM_SAFE_VALUE_OF)
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .addParameter(ParameterSpec.builder(ClassNames.STRING, "rawValue").build())
        .returns(ClassName.get("", typeDeclaration.name))
        .addCode(CodeBlock.builder()
            .beginControlFlow("for (\$L enumValue : values())", typeDeclaration.name)
            .beginControlFlow("if (enumValue.rawValue.equals(rawValue))")
            .addStatement("return enumValue")
            .endControlFlow()
            .endControlFlow()
            .addStatement("return \$L.\$L", typeDeclaration.name, ENUM_UNKNOWN_CONSTANT)
            .build()
        )
        .build()

    return TypeSpec.enumBuilder(typeDeclaration.name)
        .addAnnotation(Annotations.GENERATED_BY_APOLLO)
        .addModifiers(Modifier.PUBLIC)
        .addField(FieldSpec.builder(ClassNames.STRING, "rawValue", Modifier.PRIVATE, Modifier.FINAL).build())
        .addMethod(MethodSpec.constructorBuilder()
            .addParameter(ParameterSpec.builder(ClassNames.STRING, "rawValue").build())
            .addStatement("this.rawValue = rawValue")
            .build()
        )
        .addMethod(MethodSpec.methodBuilder("rawValue")
            .addModifiers(Modifier.PUBLIC)
            .returns(ClassNames.STRING)
            .addStatement("return rawValue")
            .build())
        .apply {
          enumConstants?.forEach { (name, typeSpec) ->
            addEnumConstant(name.escapeJavaReservedWord().toUpperCase(), typeSpec)
          }
        }
        .addEnumConstant(ENUM_UNKNOWN_CONSTANT, unknownConstantTypeSpec)
        .apply {
          if (!typeDeclaration.description.isNullOrEmpty()) {
            addJavadoc("\$L\n", typeDeclaration.description)
          }
        }
        .addMethod(safeValueOfMethodSpec)
        .build()
  }

  private fun inputObjectToTypeSpec(context: CodeGenerationContext) =
      InputTypeSpecBuilder(typeDeclaration.name, typeDeclaration.fields ?: emptyList(), context).build()
}