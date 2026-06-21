# SPEC: Klarnamen — UUID-basierte Given-Names + Nick-Historie (Paper-Plugin)

Diese Datei ist die Implementierungs-Spec für ein Paper-Plugin. Sie ist als Vorgabe
für Claude Code (Plan Mode) gedacht. Prosa ist Deutsch; Bezeichner, Befehle und
Dateinamen sind englisch/standard.

## 1. Zweck

Ein Server-Admin-Tool, mit dem über die **UUID** eines Spielers ein **given-name**
(der vom Admin zugewiesene Name) hinterlegt und nachgeschlagen werden kann.
Zusätzlich wird die **Nick-Historie** (`aka`) pro Spieler automatisch gepflegt.
Der given-name ist ausschließlich für berechtigte Admins sichtbar.

## 2. Gelockte Entscheidungen

1. Persistenz: **Flatfile als YAML** über Bukkits `org.bukkit.configuration.file.YamlConfiguration`. Keine externen Dependencies, kein Shading.
2. Pro Spieler gespeichert: **given-name** (vom Admin gesetzt) + **aka** (deduplizierte Liste aller je gesehenen Nicks). Keine Zeitstempel, kein current-nick.
3. Nick-Erfassung **bei Join** (`PlayerJoinEvent`), nicht bei Pre-Login.
4. Ziel: **Paper 26.1.2** (paper-api `26.1.2`).

## 3. Nicht-Ziele (explizit ausgeschlossen für V1)

1. Kein Mojang-/Microsoft-API-Aufruf. Auflösung passiert rein lokal.
2. Kein in-game Rendering (kein Name in Tab-Liste/über dem Kopf, keine per-viewer Packet-Manipulation).
3. Kein Freitext-Notizfeld.
4. Kein SQLite / keine Datenbank.
5. **Kein** Lookup über historische `aka`-Nicks. Die `aka`-Liste ist reine Anzeige.
6. **Kein** Namens-Lookup für Offline-Spieler. Per Name auflösbar sind nur aktuell online Spieler; sonst über die UUID.
7. Keine Zeitstempel pro `aka`-Eintrag.

## 4. Zielplattform & Build

1. Paper **26.1.2**, `compileOnly` auf das paper-api-Artefakt `26.1.2` aus dem Paper-Repo `https://repo.papermc.io/repository/maven-public/`.
2. **Keine Reobfuscation**: ab Paper 26.1 entfällt das Remapping auf Spigot-Runtime-Mappings (Mojang-Mappings sind Standard). `paperweight reobfJar` NICHT verwenden. Der gebaute JAR aus `build/libs` läuft direkt.
3. Da keine externen Runtime-Dependencies vorhanden sind, ist **kein** Shadow/Shade-Setup nötig. Plain Gradle reicht.
4. Java-Toolchain gemäß 26.1.2 Dev-Bundle. Exakte Version aus den aktuellen Paper-Dev-Docs übernehmen (Stand: Java 21+). Nicht raten — verifizieren.
5. `plugin.yml` (klassisch) mit `api-version` passend zu 26.1, oder `paper-plugin.yml`. Eine Variante wählen und kurz begründen.

## 5. Datenmodell

Persistiert in `plugins/Klarnamen/data.yml`:

```yaml
players:
  <uuid-string>:
    given-name: "Max Mustermann"   # optional; vom Admin gesetzt, fehlt solange nicht gesetzt
    aka:                           # alle je gesehenen Nicks, dedupliziert, in Reihenfolge des ersten Auftretens
      - "xX_Steve_Xx"
      - "Steve"
```

1. Schlüssel ist die UUID als String. Der Store ist rein UUID-indexiert; kein Namens-Index.
2. `given-name` ist optional (nicht gesetzt = kein Eintrag bzw. `null`).
3. `aka` ist eine deduplizierte Liste reiner Nick-Strings, in der Reihenfolge ihres ersten Auftretens. Keine Zeitstempel.

## 6. Laufzeit-State & Persistenz

1. Beim `onEnable` wird `data.yml` geladen und in eine **In-Memory-Map** (`Map<UUID, PlayerRecord>`) überführt. Existiert die Datei nicht, leerer Store.
2. Schreibender Zugriff geht zuerst auf die In-Memory-Map, danach wird **asynchron** auf Platte geschrieben (Bukkit-Scheduler async task). Der Main-Thread blockiert nie auf File-I/O.
3. Schreiben **atomar**: in temporäre Datei (`data.yml.tmp`) schreiben, dann per `Files.move(..., ATOMIC_MOVE)` über `data.yml` ersetzen.
4. Bei `onDisable` einmal synchron flushen.
5. Nebenläufigkeit: Mutation im Main-Thread, dann **immutable Kopie** an den async write-Task übergeben — nicht über die Live-Map iterieren, während sie mutiert werden kann. Strategie im Plan benennen.

## 7. Nick-Erfassung (PlayerJoinEvent)

Bei jedem Join:

1. `uuid = player.getUniqueId()`, `nick = player.getName()`.
2. Record zur UUID holen (oder neu anlegen).
3. Ist `nick` noch nicht in `aka`? Dann anhängen (Set-Semantik, keine Duplikate). Andernfalls nichts tun.
4. Async persistieren.

Hinweis: Vergleich auf den vom Client gemeldeten Namen. Dedup case-insensitiv, gespeichert wird die gemeldete Schreibweise des ersten Auftretens.

## 8. Commands

1. `/whois <name|uuid>` — Lookup. Auflösung lokal (siehe 9). Ausgabe als natürliche Sätze: Zeile 1 ist `<arg> is <given-name>` bzw. `<arg> is <uuid>.` wenn kein given-name gesetzt ist. Danach folgt — sofern weitere Aliase bekannt sind — eine oder mehrere Zeilen `<arg> is also known as a, b, c` mit bis zu drei Aliasen pro Zeile. Der Such-Name wird in der Alias-Auflistung ausgespart.
2. `/whois set <name|uuid> <given-name...>` — given-name setzen/ändern. `<given-name...>` ist der Rest der Zeile (darf Leerzeichen enthalten). Auflösung per Name funktioniert nur für aktuell online Spieler; sonst die UUID angeben.
3. Tab-Completion für das Argument aus Online-Spielern (nice-to-have).

## 9. Lokale Auflösung `<name|uuid>`

1. Sieht das Argument wie eine UUID aus (mit oder ohne Bindestriche)? → direkt als UUID verwenden. Funktioniert für jeden bekannten Record, auch wenn der Spieler offline ist.
2. Sonst case-insensitiv gegen die **Online-Spieler** matchen (z. B. `Bukkit.getPlayerExact`). Treffer → dessen UUID verwenden. Online-Namen sind eindeutig, daher kein Mehrdeutigkeitsfall.
3. Kein Treffer (UUID unbekannt bzw. Name nicht online) → klare Meldung, kein Mojang-API-Fallback.

## 10. Permissions

1. Eine Node: `klarnamen.admin`, Default **OP only**.
2. Beide Commands erfordern `klarnamen.admin`. Keine getrennten Lese-/Schreib-Nodes.

## 11. Ausgabe-Format

1. Ausgabe über Adventure-Components (in Paper gebündelt), nicht über Legacy-`§`-Codes.
2. Kompakt: Kopfzeile (UUID + given-name), darunter die `aka`-Liste.

## 12. Optionale Erweiterung (V1.1, NICHT Teil von V1)

1. Beim ersten `onEnable` den Store aus `usercache.json` (und optional `whitelist.json`) vorbefüllen, damit die letzten bekannten UUID↔Name-Paare sofort vorhanden sind. Klar als optional markieren, nur auf explizite Anforderung umsetzen.

## 13. Risiken & Edge-Cases (im Plan adressieren)

1. File-I/O auf dem Main-Thread → ausschließlich async, plus atomarer Write (6.3).
2. Race zwischen In-Memory-Mutation und async Serialisierung → immutable Kopie zum Schreiben (6.5).
3. Korrupte/teilweise geschriebene `data.yml` → durch atomaren Write vermieden; defensiv laden (bei Parsefehler: Backup der kaputten Datei, leer starten, Warnung loggen).
4. Datenschutz: given-names sind personenbezogene Daten. Im Familienkontext unkritisch, bei Drittspielern relevant. Keine Funktionalität, die given-names an normale Spieler ausgibt.

## 14. Akzeptanzkriterien

1. Joint ein Spieler erstmals, entsteht ein Record mit `aka` = genau diesem einen Nick.
2. Joint derselbe Spieler (gleiche UUID) mit neuem Nick, wird der neue Nick an `aka` angehängt; der alte bleibt erhalten.
3. Joint er erneut mit einem bereits bekannten Nick, entsteht kein Duplikat in `aka`.
4. `/whois set <uuid> Max Mustermann` setzt den given-name; `/whois <uuid>` zeigt ihn.
5. `/whois <online-spieler-name>` findet den Spieler und zeigt given-name + `aka`.
6. `/whois <name-eines-offline-spielers>` löst **nicht** per Name auf; stattdessen `/whois <uuid>` verwenden.
7. Ohne `klarnamen.admin` sind beide Commands gesperrt.
8. Nach Server-Neustart sind alle Daten erhalten.
9. `data.yml`-Schreibvorgänge blockieren den Main-Thread nicht.
