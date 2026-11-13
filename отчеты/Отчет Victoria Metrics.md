# Отчет Victoria Metrics

## Какие метрики использованы

- `pg_exporter_last_scrape_duration_seconds` - сколько секунд postgres-exporter ждал ответ от PostgreSQL при последнем опросе;

- `http_server_requests_seconds` сколько секунд длится HTTP-запрос;

- `up` - равен 1, когда vmagent получил ответ от пода `tracker-app`, и 0, когда под есть, но ответа нет.

## SLI и SLO

1. Время ответа Postgres.

SLI: Сколько секунд postgres-exporter ждал ответ базы при последнем опросе.

```metricsql
pg_exporter_last_scrape_duration_seconds{namespace="task-tracker"}
```

SLO: Ответ Postgres не дольше 0.05 с.

```metricsql
vector(0.05)
```

2. Время получения списка задач.

SLI: Время, быстрее которого уложились 95% GET запросов за последние 5 минут.

```metricsql
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{namespace="task-tracker",method="GET",uri="/api/v1/tasks"}[5m])) by (le))
```

SLO: Время полуения ответа от GET '/api/v1/tasks' не более 0.03 с.

```metricsql
vector(0.03)
```

3. Доля успешных ответов.

SLI: Доля ответов со статусом 500 среди всех запросов за последние 5 минут.

```metricsql
sum(rate(http_server_requests_seconds_count{namespace="task-tracker",status="500"}[5m]))
/
sum(rate(http_server_requests_seconds_count{namespace="task-tracker"}[5m]))
or vector(0)
```

SLO: Доля ответов со статусом 500 не более 0.05.

```metricsql
vector(0.05)
```

## SLA

`SLA(%) = (TWTp − TDTp) / TWTp × 100%`

`TWTp` - все минуты последнего часа. `TDTp` - минуты, в которые под `tracker-app` не отвечал на сбор метрик (`up` равен 0). Значение должно быть не ниже 99%.

```metricsql
(
  count_over_time((max(up{namespace="task-tracker",pod=~"tracker-app-.*"}) == 1)[1h:1m])
  /
  count_over_time(max(up{namespace="task-tracker",pod=~"tracker-app-.*"})[1h:1m])
) * 100 or vector(0)
```

## Пробер

Каждые 15 секунд пробер запрашивает список задач.

## Скринкаст

https://disk.360.yandex.ru/i/NaDgVUGX5okYHg

Чтобы при выключенном пробере метрики считались для заданных SLI, необходимо сделать любое действие с задачами: добавить/изменить задачу или просто обновить страницу. 
Тогда вызовется эндпоинт /tasks, который влияет на каждую из трех SLI.