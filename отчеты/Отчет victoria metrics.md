# Метрики Task Tracker в Victoria Metrics

## Стэк

Для сбора метрик в tracker-app использованы spring-boot-starter-actuator и micrometer-registry-prometheus.

spring-boot-starter-actuator открывает адрес /actuator/prometheus. micrometer-registry-prometheus переводит внутренние счетчики Spring и JVM в текст формата Prometheus, который можно получить на порту :8081.

Четыре бизнес-метрики пишутся той же библиотекой Micrometer и попадают в тот же ответ.

Для сбора метрик в Victoria Metrics используются vmagent. Для сбора метрик из postgres используется postgres-exporter.

## Экспортеры

Метрики собираются по pull-модели. 

postgres-exporter ходит в postgres по localhost:5432, берет оттуда метрики и публикует их на порту
9187 по адресу /metrics в формате Prometheus. vmagent раз в 15 секунд забирает эти метрики и складывает в Victoria Metrics.

Также в vmagent раз в 15 секунд забирает продуктовые метрики из tracker-app на порту :8081 по пути /actuator/prometheus.

vmagent ходит только по подам, которые помечены как prometheus.io/scrape: "true" (т.е. под с postgres и под с tracker-app).

## Бизнес-метрики

`tasks_total` - сколько задач успешно создано;

`tasks_open` - сколько задач сейчас не выполнено;

`task_save_duration_seconds` - сколько длится создание и обновление задачи;

`task_completion_duration_seconds` - сколько времени прошло от появления задачи до ее выполнения.


## Демонстрация

https://disk.360.yandex.ru/i/26tmsyyzG7sKTg 