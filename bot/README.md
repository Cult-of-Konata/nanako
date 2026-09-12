# Nanako Kuroi

Nanako Kuroi is a Java-based Discord bot built with JDA.

## Overview

This project contains the bot entry point, command listeners, moderation helpers, configuration management, and worker tasks used to maintain status and runtime behavior.

## Main packages

- `cult.konata.nanako` — application entry point and top-level runtime setup
- `cult.konata.nanako.commands` — user-facing slash-style or message commands
- `cult.konata.nanako.commands.moderation` — moderation commands such as ban, kick, report, and config
- `cult.konata.nanako.config` — persistence and configuration helpers
- `cult.konata.nanako.listeners` — event listeners
- `cult.konata.nanako.worker` — background worker and status task logic

## Notes

This page is used as the Doxygen main page so the generated documentation opens with a useful project overview instead of a blank landing page.
