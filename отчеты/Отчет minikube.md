# Развертывание Task Tracker в minikube

## Какие абстракции Kubernetes использованы для запуска

Namespace `task-tracker` - логически отделяет ресурсы приложения от остальных объектов кластера.

ConfigMap `tracker-config` - хранит несекретные параметры: порт приложения, имя и пользователя базы данных, JDBC-URL. В контейнеры эти значения попадают через `envFrom` и `configMapRef`.

Secret `tracker-secret` - хранит пароли PostgreSQL. Он подключается к контейнерам через `secretRef`.

PersistentVolumeClaim `postgres-pvc` - запрашивает постоянный том для данных PostgreSQL, чтобы данные базы переживали перезапуск пода.

Deployment описывает два контроллера. Deployment `postgres` - поддерживает одну реплику базы данных. Deployment `tracker-app` - запускает веб-приложение, для него задана стратегия `RollingUpdate` с `maxSurge` и `maxUnavailable`. Масштабирование UI и API выполняется командой `kubectl scale` по этому Deployment.

Service `postgres` типа ClusterIP - дает подам стабильное DNS-имя и порт для доступа к базе внутри кластера. Service `tracker-app` типа NodePort открывает HTTP-порт наружу, чтобы UI можно было открыть через `minikube service`.

Job `tracker-migrate` - один раз применяет миграции схемы через Flyway. У Job заданы `restartPolicy: Never` и `backoffLimit: 4`, как в примере Job из семинара.

Pod - фактические контейнеры, которые создают Deployment и Job. Образ приложения `task-tracker:1.0.0` собирается локально и загружается в minikube. Образ PostgreSQL берётся из Docker Hub.

## Взаимодействие между сервисами приложения

Архитектура совпадает с разделением процессов в docker-compose: база данных, одноразовые миграции и веб-приложение.

Пользователь открывает UI через Service `tracker-app`. Трафик попадает на один из подов Deployment `tracker-app`. В этом процессе одновременно отдается фронтенд и REST API.

Приложение подключается к PostgreSQL по JDBC-URL из ConfigMap. В URL указан хост `postgres` - имя Service внутри namespace. Kubernetes резолвит это имя в ClusterIP и балансирует соединения на под базы данных.

Перед тем как приложение начинает обслуживать запросы со схемой БД, Job `tracker-migrate` запускает тот же образ приложения, но с точкой входа `MigrateKt`, и применяет SQL-миграции к той же базе через тот же Service `postgres`. Пароль и параметры подключения берутся из Secret и ConfigMap.

Все реплики `tracker-app` работают без локального состояния: задачи и теги хранятся только в PostgreSQL. Поэтому при увеличении числа реплик они продолжают обращаться к одной базе через Service `postgres`.
