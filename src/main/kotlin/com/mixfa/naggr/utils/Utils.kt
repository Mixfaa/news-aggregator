package com.mixfa.naggr.utils

import org.springframework.data.domain.Pageable
import reactor.core.publisher.Mono

val defaultPageable = Pageable.ofSize(10)

val emptyMonoError = Throwable("No element")