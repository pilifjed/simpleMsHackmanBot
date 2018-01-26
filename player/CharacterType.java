package player;

/**
 * player.CharacterType
 *
 * [description]
 *
 * @author Jim van Eeden - jim@riddles.io
 */
public enum CharacterType {
    BIXIE,
    BIXIETTE;

    @Override
    public String toString() {
        return this.name().toLowerCase();
    }
}
