package com.mixfa.naggr.utils

import org.springframework.data.domain.Pageable

val defaultPageable = Pageable.ofSize(10)

object EmptyMonoError : Throwable("No element") {
    private fun readResolve(): Any = EmptyMonoError
}