# Логи Task Tracker в Victoria Logs

## UI

Снаружи UI открывается так:

```bash
kubectl port-forward -n task-tracker svc/victoria-logs 9428:9428
```

vmui отдается по адресу `http://127.0.0.1:9428/select/vmui/`

Манифесты: `k8s/victoria-logs.yaml`, `k8s/vector.yaml`. JSON лог: `src/main/resources/logback-spring.xml`.

## Запросы

### (а) Сырые записи

```logsql
_time:30m | sort by (_time) desc | limit 20
```

Показывает сами записи за последние 30 минут. Сортировка по `_time`, `limit 20` оставляет только последние 20.

![Снимок экрана от 2026-11-02 16-58-36.png](../../../%D0%98%D0%B7%D0%BE%D0%B1%D1%80%D0%B0%D0%B6%D0%B5%D0%BD%D0%B8%D1%8F/%D0%A1%D0%BD%D0%B8%D0%BC%D0%BA%D0%B8%20%D1%8D%D0%BA%D1%80%D0%B0%D0%BD%D0%B0/%D0%A1%D0%BD%D0%B8%D0%BC%D0%BE%D0%BA%20%D1%8D%D0%BA%D1%80%D0%B0%D0%BD%D0%B0%20%D0%BE%D1%82%202026-11-02%2016-58-36.png)

### (б) Фильтр по полю

```logsql
_time:30m level:ERROR
```

Показывает только те строки за 30 минут, у которых поле `level` равно `ERROR`.

![Снимок экрана от 2026-11-02 16-59-28.png](../../../%D0%98%D0%B7%D0%BE%D0%B1%D1%80%D0%B0%D0%B6%D0%B5%D0%BD%D0%B8%D1%8F/%D0%A1%D0%BD%D0%B8%D0%BC%D0%BA%D0%B8%20%D1%8D%D0%BA%D1%80%D0%B0%D0%BD%D0%B0/%D0%A1%D0%BD%D0%B8%D0%BC%D0%BE%D0%BA%20%D1%8D%D0%BA%D1%80%D0%B0%D0%BD%D0%B0%20%D0%BE%D1%82%202026-11-02%2016-59-28.png)

### (в) Статистика с группировкой

```logsql
_time:30m | stats by (service.name, level) count() logs
```

Показывает, сколько записей пришлось на каждую пару сервис + уровень за 30 минут.

![Снимок экрана от 2026-11-02 17-00-03.png](../../../%D0%98%D0%B7%D0%BE%D0%B1%D1%80%D0%B0%D0%B6%D0%B5%D0%BD%D0%B8%D1%8F/%D0%A1%D0%BD%D0%B8%D0%BC%D0%BA%D0%B8%20%D1%8D%D0%BA%D1%80%D0%B0%D0%BD%D0%B0/%D0%A1%D0%BD%D0%B8%D0%BC%D0%BE%D0%BA%20%D1%8D%D0%BA%D1%80%D0%B0%D0%BD%D0%B0%20%D0%BE%D1%82%202026-11-02%2017-00-03.png)

### (г) Агрегация: p95

```logsql
_time:30m | stats by (http.method) quantile(0.95, duration_ms) p95
```

Показывает 95-й перцентиль длительности запроса отдельно для каждого HTTP-метода. 

![Снимок экрана от 2026-11-02 17-01-28.png](../../../%D0%98%D0%B7%D0%BE%D0%B1%D1%80%D0%B0%D0%B6%D0%B5%D0%BD%D0%B8%D1%8F/%D0%A1%D0%BD%D0%B8%D0%BC%D0%BA%D0%B8%20%D1%8D%D0%BA%D1%80%D0%B0%D0%BD%D0%B0/%D0%A1%D0%BD%D0%B8%D0%BC%D0%BE%D0%BA%20%D1%8D%D0%BA%D1%80%D0%B0%D0%BD%D0%B0%20%D0%BE%D1%82%202026-11-02%2017-01-28.png)
