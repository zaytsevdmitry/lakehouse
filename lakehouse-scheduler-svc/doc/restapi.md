#

## Вывод списка расписаний 
Метод запроса GET в возвратом body JSON
```
http://localhost:8081/v1_0/schedule
```
```bash
curl -X GET http://localhost:8081/v1_0/schedule |jq
```
> |jq не влияет на работу команды. Пример приведен для демонстрации вывода с форматированием JSON.

Пример вывода
```json
[
  {
    "id": 1,
    "configScheduleKeyName": "regular",
    "targetExecutionDateTime": "2025-01-02T00:00:00Z",
    "status": "RUNNING"
  },
  {
    "id": 2,
    "configScheduleKeyName": "generateSourceDict",
    "targetExecutionDateTime": "2025-01-02T00:00:00Z",
    "status": "RUNNING"
  },
  {
    "id": 3,
    "configScheduleKeyName": "initial",
    "targetExecutionDateTime": "2025-02-01T00:00:00Z",
    "status": "RUNNING"
  },
  {
    "id": 4,
    "configScheduleKeyName": "generateSource",
    "targetExecutionDateTime": "2025-01-02T00:00:00Z",
    "status": "RUNNING"
  }
]
```

## Удаление расписания
На пример требуется удалить расписание из предыдущего примера generateSourceDict.  Его id=2.
Тогда строка для удаления будет выглядеть так:
```
http://localhost:8081/v1_0/schedule/id=2
```
HTTP CODE 200 означает успешное удаление

Шедулер создаст новое расписание взамен удаленного, при наличии конфигурации.