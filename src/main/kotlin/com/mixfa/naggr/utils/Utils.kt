package com.mixfa.naggr.utils

import org.springframework.data.domain.Pageable

val defaultPageable = Pageable.ofSize(10)

class EmptyMonoException(message:String) : Throwable(message)
val EMPTY_MONO_EXCEPTION = EmptyMonoException("Mono produced 0 elements")