# Plagun Mod (Fabric, 1.21.3)

Полный порт Paper-плагина на Fabric: жизни, команды, голодные игры, модерация.
Все команды требуют `permission level 2` (OP). Состояние пишется в
`<config>/plagun/data.json`.

## Жизни (`/lives`)

| Команда | Действие |
| --- | --- |
| `/lives give <player> [N]` | Выдать жизни (по умолчанию 3) |
| `/lives set <player> <N>` | Установить точное количество |
| `/lives check [player]` | Узнать сколько жизней |
| `/lives revive <player>` | Воскресить «пермадед»-игрока |
| `/lives reset [player]` | Сбросить данные (всё или для одного) |
| `/lives list` | Список всех отслеживаемых |

При смерти жизнь списывается. Когда становится 0 — игрок переходит в
**SPECTATOR** (настраивается в JSON через ключ `config.death-gamemode`),
и при респавне снова форсится в spectator, пока админ не сделает
`/lives revive`.

## Команды-цвета (`/pteam`)

15 цветов: red, blue, yellow, green, aqua, purple, white, black, gold,
gray, dark_red, dark_blue, dark_green, dark_aqua, dark_purple.

| Команда | Действие |
| --- | --- |
| `/pteam create <name> <color>` | Создать команду |
| `/pteam delete <name>` | Удалить |
| `/pteam add <player> <team>` / `remove <player>` | Управление составом |
| `/pteam list` / `info <name>` / `clear` | Информация и очистка |
| `/pteam auto <numTeams> <playersPerTeam>` | Случайное распределение игроков |

Цвет применяется через scoreboard-team Майнкрафта: префикс `[RED] /
[BLUE]` перед ником, цвет имени над головой и в чате, friendly fire
отключён внутри команды.

## Голодные игры (`/hg`)

| Команда | Действие |
| --- | --- |
| `/hg setlobby` | Зафиксировать лобби в твоей текущей точке |
| `/hg setspawn` | Добавить точку спавна |
| `/hg clearspawns` | Очистить точки |
| `/hg start` | Старт игры — телепорт по случайным точкам, очистка инвентаря, обратный отсчёт 30с с инвулом |
| `/hg stop` | Остановить |
| `/hg tplobby [player]` / `tpall` | Телепорт в лобби |
| `/hg spectate` | Самому в режим наблюдателя |
| `/hg info` | Состояние |

## Модерация

- `/pfreeze <player>` / `/punfreeze <player>` — заморозка через тиковый телепорт назад
- `/pheal [player]` / `/pfeed [player]` — здоровье и голод
- `/pvanish` — статус-эффект Invisibility (`Integer.MAX_VALUE`, без иконки и частиц)
- `/ptp <player>` — телепорт к игроку
- `/pbroadcast <message>` — глобальное объявление

## Управление модом

- `/plagun version` — версия
- `/plagun reload` — перечитать JSON
- `/plagun save` — принудительно сохранить
- `/plagun info` — список корней команд

## Базовые «фишки» (остались с прошлой итерации)

| Команда | Что делает |
| --- | --- |
| `/plagun heal [targets]` | Лечит + сердечки |
| `/plagun feed [targets]` | Голод и сатурация на максимум |
| `/plagun fly [targets]` | Переключает возможность полёта |
| `/plagun sparkle [targets]` | Включает / выключает цветной шлейф из частиц |
| `/plagun pvp <true\|false>` | Глобальный переключатель PvP |

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
