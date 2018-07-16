package com.apollographql.apollo.compiler.ir

import com.apollographql.apollo.compiler.*
import com.apollographql.apollo.compiler.java.ClassNames
import com.apollographql.apollo.compiler.java.JavaTypeResolver
import com.apollographql.apollo.compiler.java.SchemaTypeSpecBuilder
import com.squareup.javapoet.*
import javax.lang.model.element.Modifier

data class Field(
    val responseName: String,
    val fieldName: String,
    val type: String,
    val args: List<Argument>? = null,
    val isConditional: Boolean = false,
    val fields: List<Field>? = null,
    val fragmentSpreads: List<String>? = null,
    val inlineFragments: List<InlineFragment>? = null,
    val description: String? = null,
    val isDeprecated: Boolean? = false,
    val deprecationReason: String? = null,
    val conditions: List<Condition>? = null
) {

  fun formatClassName() = responseName.capitalize().let { if (isList()) it.singularize() else it }

  fun isOptional(): Boolean = isConditional || !methodResponseType().endsWith("!")
      || (inlineFragments?.isNotEmpty() ?: false)

  fun isNonScalar() = hasFragments() || (fields?.any() ?: false)

  fun hasFragments() = (fragmentSpreads?.any() ?: false) || (inlineFragments?.any() ?: false)

  fun isList(): Boolean = type.removeSuffix("!").let { it.startsWith('[') && it.endsWith(']') }

  fun methodResponseType(): String {
    if (isNonScalar() || hasFragments()) {
      // For non scalar fields, we use the responseName as the method return type.
      // However, we need to also encode any extra information from the `type` field
      // eg, [lists], nonNulls!, [[nestedLists]], [nonNullLists]!, etc
      val normalizedName = formatClassName()
      return when {
        type.startsWith("[") -> // array type
          if (type.endsWith("!")) "[$normalizedName]!" else "[$normalizedName]"
        type.endsWith("!") -> // non-null type
          "$normalizedName!"
        else -> // nullable type
          normalizedName
      }
    } else {
      return type
    }
  }

  companion object {
    val TYPE_NAME_FIELD = Field(responseName = "__typename", fieldName = "__typename", type = "String!")
  }
}
