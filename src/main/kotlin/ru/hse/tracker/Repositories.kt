package ru.hse.tracker

import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.sql.ResultSet

@Repository
class TagRepository(private val jdbc: JdbcClient) {

    fun findAll(): List<Tag> =
        jdbc.sql("SELECT id, name FROM tags ORDER BY name")
            .query { rs, _ -> rs.toTag() }
            .list()

    fun findById(id: Long): Tag? =
        jdbc.sql("SELECT id, name FROM tags WHERE id = :id")
            .param("id", id)
            .query { rs, _ -> rs.toTag() }
            .optional().orElse(null)

    fun create(tag: Tag): Tag {
        val id = jdbc.sql("INSERT INTO tags (name) VALUES (:name) RETURNING id")
            .param("name", tag.name)
            .query(Long::class.java)
            .single()
        return tag.copy(id = id)
    }

    fun update(id: Long, tag: Tag): Tag? {
        val updated = jdbc.sql("UPDATE tags SET name = :name WHERE id = :id")
            .param("name", tag.name)
            .param("id", id)
            .update()
        return if (updated == 0) null else tag.copy(id = id)
    }

    fun delete(id: Long): Boolean =
        jdbc.sql("DELETE FROM tags WHERE id = :id").param("id", id).update() > 0

    private fun ResultSet.toTag() = Tag(getLong("id"), getString("name"))
}

@Repository
class TaskRepository(private val jdbc: JdbcClient) {

    fun findAll(tagId: Long?): List<Task> {
        val sql = buildString {
            append("SELECT id, title, done, priority, tag_id FROM tasks")
            if (tagId != null) append(" WHERE tag_id = :tagId")
            append(" ORDER BY CASE priority WHEN 'HIGH' THEN 0 WHEN 'MEDIUM' THEN 1 ELSE 2 END, id")
        }
        var spec = jdbc.sql(sql)
        if (tagId != null) spec = spec.param("tagId", tagId)
        return spec.query { rs, _ -> rs.toTask() }.list()
    }

    fun findById(id: Long): Task? =
        jdbc.sql("SELECT id, title, done, priority, tag_id FROM tasks WHERE id = :id")
            .param("id", id)
            .query { rs, _ -> rs.toTask() }
            .optional().orElse(null)

    fun create(t: Task): Task {
        val id = jdbc.sql(
            "INSERT INTO tasks (title, done, priority, tag_id) VALUES (:title, :done, :priority, :tagId) RETURNING id"
        )
            .param("title", t.title)
            .param("done", t.done)
            .param("priority", t.priority)
            .param("tagId", t.tagId)
            .query(Long::class.java)
            .single()
        return t.copy(id = id)
    }

    fun update(id: Long, t: Task): Task? {
        val updated = jdbc.sql(
            "UPDATE tasks SET title = :title, done = :done, priority = :priority, tag_id = :tagId WHERE id = :id"
        )
            .param("title", t.title)
            .param("done", t.done)
            .param("priority", t.priority)
            .param("tagId", t.tagId)
            .param("id", id)
            .update()
        return if (updated == 0) null else t.copy(id = id)
    }

    fun delete(id: Long): Boolean =
        jdbc.sql("DELETE FROM tasks WHERE id = :id").param("id", id).update() > 0

    private fun ResultSet.toTask(): Task {
        val tagId = getObject("tag_id") as Long?
        return Task(getLong("id"), getString("title"), getBoolean("done"), getString("priority"), tagId)
    }
}
