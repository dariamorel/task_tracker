package ru.hse.tracker

import jakarta.servlet.http.HttpServletRequest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.*
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

class NotFoundException(message: String) : RuntimeException(message)

private fun Task.requireValid() {
    require(title.isNotBlank()) { "title must not be blank" }
    require(priority in PRIORITIES) { "priority must be one of $PRIORITIES" }
}

@RestController
class HealthController {
    @GetMapping("/health")
    fun health() = mapOf("status" to "ok")
}

@RestController
@RequestMapping("/api/v1/tags")
class TagController(private val repo: TagRepository) {

    @GetMapping
    fun list() = repo.findAll()

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long) = repo.findById(id) ?: throw NotFoundException("tag $id not found")

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody body: Tag): Tag {
        require(body.name.isNotBlank()) { "name must not be blank" }
        return repo.create(body.copy(name = body.name.trim()))
    }

    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody body: Tag): Tag {
        require(body.name.isNotBlank()) { "name must not be blank" }
        return repo.update(id, body.copy(name = body.name.trim()))
            ?: throw NotFoundException("tag $id not found")
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) {
        if (!repo.delete(id)) throw NotFoundException("tag $id not found")
    }
}

@RestController
@RequestMapping("/api/v1/tasks")
class TaskController(private val repo: TaskRepository) {

    @GetMapping
    fun list(@RequestParam(required = false) tagId: Long?) = repo.findAll(tagId)

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long) = repo.findById(id) ?: throw NotFoundException("task $id not found")

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody body: Task): Task {
        body.requireValid()
        return repo.create(body)
    }

    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody body: Task): Task {
        body.requireValid()
        return repo.update(id, body) ?: throw NotFoundException("task $id not found")
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) {
        if (!repo.delete(id)) throw NotFoundException("task $id not found")
    }
}

@RestControllerAdvice
class ErrorHandler {
    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun notFound(e: NotFoundException, request: HttpServletRequest): ErrorResponse {
        val message = e.message ?: "not found"
        logRequest(request, HttpStatus.NOT_FOUND.value(), message)
        return ErrorResponse(message)
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun badRequest(e: IllegalArgumentException, request: HttpServletRequest): ErrorResponse {
        val message = e.message ?: "bad request"
        logRequest(request, HttpStatus.BAD_REQUEST.value(), message)
        return ErrorResponse(message)
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun badParam(e: MethodArgumentTypeMismatchException, request: HttpServletRequest): ErrorResponse {
        val message = "${e.name} must be an integer"
        logRequest(request, HttpStatus.BAD_REQUEST.value(), message)
        return ErrorResponse(message)
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun badBody(e: HttpMessageNotReadableException, request: HttpServletRequest): ErrorResponse {
        val message = "invalid request body"
        logRequest(request, HttpStatus.BAD_REQUEST.value(), message)
        return ErrorResponse(message)
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun constraint(e: DataIntegrityViolationException, request: HttpServletRequest): ErrorResponse {
        val message = "constraint violation"
        logRequest(request, HttpStatus.BAD_REQUEST.value(), message)
        return ErrorResponse(message)
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun unexpected(e: Exception, request: HttpServletRequest): ErrorResponse {
        logRequest(request, HttpStatus.INTERNAL_SERVER_ERROR.value(), e.message ?: "internal error", e)
        return ErrorResponse("internal error")
    }
}
