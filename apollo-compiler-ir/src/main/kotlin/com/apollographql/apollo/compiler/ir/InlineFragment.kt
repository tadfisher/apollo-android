@file:Suppress("unused")

package com.apollographql.apollo.compiler.ir

data class InlineFragment(
    val typeCondition: String,
    val possibleTypes: List<String>?,
    val fields: List<Field>,
    val fragmentSpreads: List<String>?
) {

  fun formatClassName(): String = "$INTERFACE_PREFIX${typeCondition.capitalize()}"

  companion object {
    private const val INTERFACE_PREFIX = "As"
  }
}
