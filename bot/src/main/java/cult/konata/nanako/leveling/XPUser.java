package cult.konata.nanako.leveling;

public record XPUser(
        long guildId,
        long userId,
        long xp,
        long messages
) {
}
