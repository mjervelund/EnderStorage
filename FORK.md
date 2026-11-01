# Unofficial fork of EnderStorage for the Modern Tekkit pack

This is **not** the official project. Upstream: https://github.com/TheCBProject/EnderStorage (branch `master`).
Please report problems with these branches here, not upstream.

We build every mod of a private Minecraft 26.3 / NeoForge 26.3.0.99 modpack ("Modern Tekkit") from source. This fork
holds our changes to EnderStorage, kept as small branches so they can be offered upstream one at a time:

- `public`: what we run: the branches below that are in our pack, merged together (merge-only, never rebased).
  Default branch of this fork.
- `port/*`: ports to newer Minecraft / NeoForge versions.
- `fix/*`: bug fixes, one change per branch, based on upstream's branch.
- `perf/*`: performance changes with measurements, one change per branch.
- `backport/*`: fixes rebuilt on the release tag we run.

The changes were made with AI assistance (Anthropic's Claude). Each commit message says what changed and why; not
every branch has been play-tested yet. The licence is unchanged: see [LICENSE.txt](LICENSE.txt).
