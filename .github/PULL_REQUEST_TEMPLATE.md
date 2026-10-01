## Summary

<!-- What does this change and why? -->

## Targets touched

<!-- Tick what this change concerns. A change in common code usually concerns the three Minecraft versions. -->

- [ ] common code (`common/<mcversion>/`)
- [ ] neoforge-1.21.1
- [ ] forge-1.21.1
- [ ] fabric-1.21.1
- [ ] neoforge-1.20.1
- [ ] forge-1.20.1
- [ ] fabric-1.20.1
- [ ] neoforge-26.1.2
- [ ] forge-26.1.2
- [ ] fabric-26.1.2
- [ ] docs or tools only

## How it was tested

<!-- Which targets you built, which self tests you ran, what you checked in game. -->

## Checklist

- [ ] Every touched project builds with `./gradlew build`
- [ ] `./gradlew runSelftest` ends with `[SELFTEST] RESULT SUCCESS` on each loader whose folder changed (at least one loader for a common code change)
- [ ] Tested in game on at least one target
- [ ] A change in `common/1.21.1` is carried to the other Minecraft versions present on `main`
- [ ] Common code only calls vanilla methods and the `Platform` interface
- [ ] New text exists in all 18 languages (interface and dialogue files) and `python tools/check_lang.py --strict` passes
- [ ] `CHANGELOG.md` updated (English and French)
- [ ] Guides in `docs/` updated (English and French) if behavior, options or commands changed
- [ ] No version number changed, no personal information in files or logs
