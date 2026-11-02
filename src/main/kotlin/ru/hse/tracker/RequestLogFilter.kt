package ru.hse.tracker

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

private val log = LoggerFactory.getLogger(RequestLogFilter::class.java)
private val startedAt = ThreadLocal<Long>()
private val requestIdHolder = ThreadLocal<String>()
private val logged = ThreadLocal<Boolean>()

fun logRequest(
    request: HttpServletRequest,
    status: Int,
    errorMessage: String? = null,
    error: Throwable? = null,
) {
    val durationMs = startedAt.get()?.let { (System.nanoTime() - it) / 1000000 } ?: 0L
    val requestId = requestIdHolder.get()
        ?: request.getHeader("X-Request-Id")
        ?: UUID.randomUUID().toString()
    val event = when {
        status >= 500 -> log.atError()
        status >= 400 -> log.atWarn()
        else -> log.atInfo()
    }
    event
        .addKeyValue("request_id", requestId)
        .addKeyValue("http.method", request.method)
        .addKeyValue("http.route", request.requestURI)
        .addKeyValue("http.status_code", status)
        .addKeyValue("duration_ms", durationMs)
    if (!errorMessage.isNullOrBlank()) event.addKeyValue("error.message", errorMessage)
    if (error != null) event.setCause(error)
    val message = when {
        !errorMessage.isNullOrBlank() -> errorMessage
        status >= 400 -> "request failed"
        else -> "request completed"
    }
    event.log(message)
    logged.set(true)
}

@Component
class RequestLogFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        startedAt.set(System.nanoTime())
        requestIdHolder.set(request.getHeader("X-Request-Id") ?: UUID.randomUUID().toString())
        logged.set(false)
        try {
            filterChain.doFilter(request, response)
        } finally {
            if (logged.get() != true) logRequest(request, response.status)
            startedAt.remove()
            requestIdHolder.remove()
            logged.remove()
        }
    }
}
