# PlagunMinecraft

Плагин для Minecraft Paper **1.21.3** с системой жизней, командами по цветам, режимом голодных игр и набором модераторских команд.

## Возможности

### Жизни
- `/lives give <игрок> [кол-во]` — выдать жизни (по умолчанию 3 из `config.yml`).
- `/lives set <игрок> <кол-во>` — установить точное количество.
- `/lives check [игрок]` — посмотреть жизни.
- `/lives revive <игрок>` — админское воскрешение игрока, который умер навсегда.
- `/lives reset [игрок]` — сбросить данные о жизнях.
- `/lives list` — список всех отслеживаемых игроков.

После того как у игрока заканчиваются жизни, он автоматически переходит в режим
наблюдателя (настраивается в `config.yml`) и не может возродиться, пока админ
не воскресит его командой `/lives revive`.

### Команды (тимы)
- `/pteam create <name> <color>` — создать команду. Цвета: `red`, `blue`,
  `yellow`, `green`, `aqua`, `purple`, `white`, `black`, `gold`, `gray`,
  `dark_red`, `dark_blue`, `dark_green`, `dark_aqua`, `dark_purple`.
- `/pteam delete <name>` — удалить.
- `/pteam add <player> <team>` / `/pteam remove <player>` — управление составом.
- `/pteam list` / `/pteam info <name>` — информация.
- `/pteam clear` — очистить участников всех команд.
- `/pteam auto <numTeams> <playersPerTeam>` — авто-распределение игроков
  случайным образом по выбранному количеству команд (фиксированное число
  человек в каждой команде).

Цвет команды применяется через scoreboard — над головой и в чате имя игрока
отображается соответствующим цветом, плюс перед ником показывается префикс
`[RED]`, `[BLUE]` и т.д., чтобы все видели, против какой команды играют.
Friendly fire отключён внутри команды.

### Голодные игры
- `/hg setlobby` — установить лобби в текущей точке.
- `/hg setspawn` — добавить точку спавна (вызывайте по разу для каждой клетки).
- `/hg clearspawns` — очистить точки спавна.
- `/hg start` — старт игры: телепорт по случайным точкам, инвентарь
  очищается, обратный отсчёт.
- `/hg stop` — остановить игру.
- `/hg tplobby [player]` / `/hg tpall` — телепорт в лобби.
- `/hg spectate` — перейти в режим наблюдателя.
- `/hg info` — информация о состоянии.

### Модерация
- `/pfreeze <player>`, `/punfreeze <player>` — заморозить / разморозить.
- `/pheal [player]`, `/pfeed [player]` — вылечить / накормить.
- `/pvanish` — переключить невидимость.
- `/ptp <player>` — телепорт к игроку.
- `/pbroadcast <message>` — глобальное объявление.

### Управление плагином
- `/plagun reload` — перезагрузить конфиг и данные.
- `/plagun info` — версия и подсказки.
- `/plagun save` — принудительно сохранить данные.

## Права

| Право | По умолчанию | Описание |
| --- | --- | --- |
| `plagun.*` | op | все права |
| `plagun.admin` | op | модераторские команды и `/plagun` |
| `plagun.lives.use` | op | команды `/lives` |
| `plagun.team.use` | op | команды `/pteam` |
| `plagun.hg.use` | op | команды `/hg` |

## Конфиг

```yaml
default-lives: 3
death-gamemode: SPECTATOR
show-lives-actionbar: true
broadcast-final-death: true
hungergames:
  countdown-seconds: 30
  min-players: 2
  invulnerable-countdown: true
```

## Сборка

Требуется JDK 21 и Maven 3.9+. Проект использует репозиторий PaperMC.

```bash
mvn clean package
```

Готовый JAR появится в `target/plagun-minecraft-1.0.0.jar`. Положите его в
папку `plugins/` сервера Paper 1.21.3 и перезапустите.
