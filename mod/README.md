# Plagun Mod (Fabric, 1.21.3) — base

Foundation Fabric mod for **Minecraft 1.21.3**. Designed as a starting point —
features will be expanded in follow-up iterations.

## Что уже сделано

| Команда (нужен OP / permission level 2) | Что делает |
| --- | --- |
| `/plagun version` | Версия мода |
| `/plagun heal [targets]` | Лечит игрока (себя, если без аргумента) + сердечки в воздух |
| `/plagun feed [targets]` | Восстанавливает голод и насыщение |
| `/plagun fly [targets]` | Переключает возможность полёта |
| `/plagun sparkle [targets]` | Включает / выключает цветной шлейф из частиц вокруг игрока |
| `/plagun pvp <true\|false>` | Глобальный переключатель PvP (запрет ближней атаки игрок→игрок) |

### Эффекты, которые сразу видны

- **Welcome-титр** при заходе на сервер: золотой `PLAGUN`, подзаголовок с ником,
  взрыв `END_ROD` частиц и тихий `note_block.chime`.
- **Death FX** — при смерти игрока вокруг тела появляется облако
  `SOUL_FIRE_FLAME` + `LARGE_SMOKE` и звук `ENTITY_WITHER_SPAWN` (тихий, высокий).
- **Sparkle trail** — после `/plagun sparkle` игрок оставляет за собой циклирующий
  шлейф из `END_ROD → WAX_ON → GLOW` каждые 3 тика. Видно всем рядом.
- **PvP-блок** — `AttackEntityCallback` отменяет удар игрок→игрок при выключенном PvP.

## Сборка

Нужен **JDK 21**. Gradle подтянется через wrapper:

```bash
cd mod
./gradlew build
```

> Если `gradlew` ещё не сгенерирован (нет бинаря wrapper), запусти один раз
> через системный Gradle 8.10+: `gradle wrapper`, после этого `./gradlew build`
> подтянет нужную версию автоматически.

JAR появится в `mod/build/libs/plagun-mod-1.0.0.jar` — кидай его в `mods/`
клиента/сервера Fabric 1.21.3 (Fabric API обязателен).

## Запуск дев-окружения

```bash
./gradlew runClient   # запустить Minecraft с модом
./gradlew runServer   # dedicated server для тестов
```

## Структура

```
mod/
├── build.gradle, settings.gradle, gradle.properties
└── src/main/
    ├── java/com/plagun/mod/
    │   ├── PlagunMod.java         — entrypoint
    │   ├── PlagunState.java       — runtime state (sparkle set, pvp flag)
    │   ├── commands/ModCommands.java
    │   ├── events/ModEvents.java  — join, death, pvp guard
    │   └── effects/SparkleManager.java
    └── resources/fabric.mod.json
```

В следующих итерациях планируется: жизни, команды-цвета, кастомные предметы и
блоки, кастомный HUD, дата-генерация ресурсов.
