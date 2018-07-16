@file:Suppress("unused")

package com.apollographql.apollo.compiler.ir

data class TypeDeclaration(
    val kind: String,
    val name: String,
    val description: String?,
    val values: List<TypeDeclarationValue>?,
    val fields: List<TypeDeclarationField>?
) {
  companion object {
    const val KIND_INPUT_OBJECT_TYPE: String = "InputObjectType"
    const val KIND_ENUM: String = "EnumType"
    const val KIND_SCALAR_TYPE: String = "ScalarType"
    const val ENUM_UNKNOWN_CONSTANT: String = "\$UNKNOWN"
    const val ENUM_SAFE_VALUE_OF: String = "safeValueOf"
  }
}
