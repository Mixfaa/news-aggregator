package com.mixfa.naggr.utils

object EmptyMonoError : Throwable("No element") {
    private fun readResolve(): Any = EmptyMonoError
}