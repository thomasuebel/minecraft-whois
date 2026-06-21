# Whois

Paper-Plugin für **Minecraft 26.1.2**, das pro Spieler-UUID einen vom Admin
hinterlegten **given-name** und die komplette **Nick-Historie** (`aka`) lokal
speichert. Alles UUID-basiert, kein Mojang-API-Aufruf, kein externes Backend.

Vollständige Spezifikation: [SPEC.md](SPEC.md).

---

## Für Spieler

Es gibt **nichts, was du selbst aufrufen kannst**. Die Befehle sind ausschließlich
für Admins gedacht.

Was das Plugin im Hintergrund macht, sobald du joinst:

- Es notiert deine aktuelle UUID und deinen aktuellen Nickname.
- Wenn du in Zukunft mit einem neuen Nick joinst, wird der zusätzlich gespeichert.
  Bekannte Nicks werden nicht doppelt erfasst.
- Ein Admin kann zu deiner UUID einen Klarnamen (oder Spitznamen, was auch immer)
  hinterlegen. Diesen Namen sehen **nur Admins** mit der Permission
  `whois.admin` — niemand sonst, weder im Chat noch über dem Kopf noch in der
  Tab-Liste.

---

## Für Server-Admins

### Voraussetzungen

- Paper **26.1.2** oder kompatibel
- Java **25** auf dem Server (Paper-Anforderung)

### Installation

1. `Whois-<version>.jar` aus `build/libs/` (oder dem GitHub-Release-Zip) in den
   `plugins/`-Ordner deines Servers kopieren.
2. Server starten oder `/reload confirm` (nicht empfohlen, lieber Restart).

Beim **ersten Start** importiert das Plugin alle UUID↔Name-Paare aus der
`usercache.json` im Server-Root, falls vorhanden. Im Log siehst du:

```
[Whois] usercache.json bootstrap: 18 candidates, 18 new, 0 skipped (from /pfad/usercache.json)
```

Danach entsteht `plugins/Whois/data.yml`.

### Permission

| Node          | Default | Wirkung                                            |
| ------------- | ------- | -------------------------------------------------- |
| `whois.admin` | `op`    | Beide Commands + Tab-Completion. Sonst nichts.     |

Wer die Permission **nicht** hat, bekommt beim Aufruf nur eine knappe Absage und
sieht weder Daten noch Online-Namen in der Auto-Completion.

### Commands

#### Lookup

```
/whois <name|uuid>
```

- `<name>` funktioniert **nur für aktuell online** Spieler (Online-Namen sind
  eindeutig).
- `<uuid>` funktioniert immer, **mit oder ohne Bindestriche**, auch für offline
  oder gebannte Spieler.
- Ausgabe als natürliche Sätze:
  - `<arg> is <given-name>` wenn ein given-name gesetzt ist, sonst `<arg> is <uuid>.`
  - Danach `<arg> is also known as a, b, c` — bis zu drei Aliase pro Zeile,
    wiederholt bis alle weiteren Aliase gelistet sind. Der Such-Name selbst
    erscheint in der Alias-Auflistung nicht.

#### Klarnamen setzen

```
/whois set <name|uuid> <given-name...>
```

- Letztes Argument ist der Rest der Zeile — Leerzeichen erlaubt, also
  `/whois set abc Max Mustermann` setzt den Klarnamen auf „Max Mustermann".
- Auflösung des Ziels via Online-Name oder UUID wie bei Lookup.

#### Tab-Completion

- Erstes Argument: `set` + alle Online-Spieler.
- Nach `/whois set `: alle Online-Spieler.
- Ohne `whois.admin`: leere Liste (kein Leak von Online-Namen).

### Datei-Layout

```
<server-root>/
├── usercache.json              # vanilla, wird beim ersten Boot eingelesen
└── plugins/
    └── Whois/
        ├── data.yml            # alle Records, UUID-indexiert
        └── data.yml.broken-*   # nur falls data.yml mal kaputt war
```

`data.yml` ist menschenlesbares YAML, Form:

```yaml
players:
  069a79f4-44e9-4726-a5be-fca90e38aaf5:
    given-name: "Max Mustermann"
    aka:
      - "xX_Steve_Xx"
      - "Steve"
```

### Robustheit

- Schreibvorgänge sind **asynchron** und **atomar** (Temp-Datei + `Files.move`).
  Der Server-Tick blockiert nie auf I/O.
- Bei Server-Shutdown wird ausstehender Write synchron abgeschlossen — kein
  Datenverlust bei normalem Stop.
- Bei korrupter `data.yml` (z. B. Stromausfall mitten im Schreiben): die
  defekte Datei wird in `data.yml.broken-<timestamp>` umbenannt, das Plugin
  startet leer, eine WARN-Zeile landet im Log. Importiert wird dann **nicht**
  erneut aus `usercache.json` — das geschieht nur bei wirklich erstem Start.

### Datenschutz

Klarnamen sind personenbezogene Daten. Das Plugin gibt sie **ausschließlich** an
Spieler mit `whois.admin` aus, nie an reguläre Spieler, nie über die Tab-Liste,
nie über dem Kopf. Außerhalb des Server-Storage werden sie nirgendwohin
geschickt.

---

## Für Entwickler

### Build

```sh
./gradlew build
```

Erzeugt `build/libs/Whois-<version>.jar`. Kein Shading, keine Reobf.

### Tests + Coverage

```sh
./gradlew check
```

Führt JUnit-5-Tests aus und prüft JaCoCo-Coverage (Gate: **80 %** LINE +
BRANCH, ohne die dünnen Bukkit-Adapter und den Composition Root).

### CI/Release

GitHub Actions baut + testet jeden Push und PR. Tag-Push `v*` erzeugt
zusätzlich ein Release-Zip (`Whois-<version>.jar` + `SPEC.md`) und hängt es an
einen GitHub Release.
