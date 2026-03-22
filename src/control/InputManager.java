package control;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles all the key presses that the player performs.
 * Uses a held-key set so multiple keys can be active simultaneously.
 *
 * @version 2.0.0
 */
public class InputManager implements KeyListener {
    private final GameEngine engine;
    private final Set<Integer> heldKeys = ConcurrentHashMap.newKeySet();

    public InputManager(GameEngine engine) {
        this.engine = engine;
    }

    /**
     * Called every game tick by GameEngine to process all currently held keys.
     */
    public void pollInput() {
        GameStatus status = engine.getGameStatus();

        if (status == GameStatus.START_SCREEN || status == GameStatus.LEADERBOARDS) {
            return; // handled by keyPressed events only
        }

        if (status == GameStatus.RUNNING) {
            // Movement — check all held keys every tick
            boolean left  = heldKeys.contains(KeyEvent.VK_LEFT)  || heldKeys.contains(KeyEvent.VK_A);
            boolean right = heldKeys.contains(KeyEvent.VK_RIGHT) || heldKeys.contains(KeyEvent.VK_D);
            boolean jump  = heldKeys.contains(KeyEvent.VK_UP)    || heldKeys.contains(KeyEvent.VK_W) || heldKeys.contains(KeyEvent.VK_SPACE);
            boolean run   = heldKeys.contains(KeyEvent.VK_X);
            boolean crouch= heldKeys.contains(KeyEvent.VK_DOWN)  || heldKeys.contains(KeyEvent.VK_S);
            boolean fire  = heldKeys.contains(KeyEvent.VK_Z);

            if (run)    engine.receiveInput(ButtonAction.RUN);
            if (right)  engine.receiveInput(ButtonAction.M_RIGHT);
            if (left)   engine.receiveInput(ButtonAction.M_LEFT);
            if (jump)   engine.receiveInput(ButtonAction.JUMP);
            if (crouch) engine.receiveInput(ButtonAction.CROUCH);
            if (fire)   engine.receiveInput(ButtonAction.FIRE);

            if (!left && !right && !crouch)
                engine.receiveInput(ButtonAction.ACTION_COMPLETED);
        }
    }

    private void notifyInput(ButtonAction input) {
        if (input != ButtonAction.NO_ACTION) engine.receiveInput(input);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        heldKeys.add(code);

        // One-shot actions (menus, enter, escape, cheat)
        ButtonAction action = ButtonAction.NO_ACTION;
        GameStatus status = engine.getGameStatus();

        if (status == GameStatus.START_SCREEN) {
            if (code == KeyEvent.VK_UP   || code == KeyEvent.VK_W) action = ButtonAction.SELECTION_UP;
            if (code == KeyEvent.VK_DOWN || code == KeyEvent.VK_S) action = ButtonAction.SELECTION_DOWN;
        } else if (status == GameStatus.LEADERBOARDS) {
            if (code == KeyEvent.VK_UP   || code == KeyEvent.VK_W) action = ButtonAction.SELECTION_UP;
            if (code == KeyEvent.VK_DOWN || code == KeyEvent.VK_S) action = ButtonAction.SELECTION_DOWN;
        } else if (status == GameStatus.MULTIPLAYER_LOBBY) {
            if (code == KeyEvent.VK_H) action = ButtonAction.SELECTION_UP;
            if (code == KeyEvent.VK_J) action = ButtonAction.SELECTION_DOWN;
            if (code >= KeyEvent.VK_0 && code <= KeyEvent.VK_9) engine.receiveIpInput(String.valueOf(code - 48));
            if (code >= KeyEvent.VK_NUMPAD0 && code <= KeyEvent.VK_NUMPAD9) engine.receiveIpInput(String.valueOf(code - 96));
            if (code == KeyEvent.VK_PERIOD || code == KeyEvent.VK_DECIMAL) engine.receiveIpInput(".");
            if (code == KeyEvent.VK_BACK_SPACE) engine.receiveIpInput("\b");
        } else if (status == GameStatus.USERNAME_SCREEN) {
            if (code >= KeyEvent.VK_0 && code <= KeyEvent.VK_9) engine.receiveUsernameInput(String.valueOf(code - 48));
            if (code >= KeyEvent.VK_NUMPAD0 && code <= KeyEvent.VK_NUMPAD9) engine.receiveUsernameInput(String.valueOf(code - 96));
            if (code >= KeyEvent.VK_A && code <= KeyEvent.VK_Z) engine.receiveUsernameInput(String.valueOf((char) code));
            if (code == KeyEvent.VK_BACK_SPACE) engine.receiveUsernameInput("\b");
        } else if (status == GameStatus.RUNNING) {
            if (code == KeyEvent.VK_C) action = ButtonAction.CHEAT;
        }

        if (code == KeyEvent.VK_ENTER)  action = ButtonAction.ENTER;
        if (code == KeyEvent.VK_ESCAPE) action = ButtonAction.ESCAPE;

        notifyInput(action);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        heldKeys.remove(e.getKeyCode());
    }

    @Override
    public void keyTyped(KeyEvent e) {}
}
