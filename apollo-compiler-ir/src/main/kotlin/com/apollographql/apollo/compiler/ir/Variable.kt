@file:Suppress("unused")

package com.apollographql.apollo.compiler.ir

data class Variable(
    val name: String,
    val type: String
) {
  fun isOptional(): Boolean = !type.endsWith(suffix = "!")
}