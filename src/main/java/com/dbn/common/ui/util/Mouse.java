/*
 * Copyright 2024 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.common.ui.util;

import com.dbn.common.routine.Consumer;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Arrays;

import static com.dbn.common.dispose.Failsafe.guarded;

public class Mouse {
    private Mouse() {}

    public static void processMouseEvent(MouseEvent e, MouseListener listener) {
        int id = e.getID();
        switch (id) {
            case MouseEvent.MOUSE_PRESSED:
                listener.mousePressed(e);
                break;
            case MouseEvent.MOUSE_RELEASED:
                listener.mouseReleased(e);
                break;
            case MouseEvent.MOUSE_CLICKED:
                listener.mouseClicked(e);
                break;
            case MouseEvent.MOUSE_EXITED:
                listener.mouseExited(e);
                break;
            case MouseEvent.MOUSE_ENTERED:
                listener.mouseEntered(e);
                break;
        }
    }

    public static boolean isNavigationEvent(MouseEvent e) {
        int button = e.getButton();
        return button == MouseEvent.BUTTON2 || (e.isControlDown() && button == MouseEvent.BUTTON1);
    }

    public static void removeMouseListeners(JComponent root) {
        UserInterface.visitRecursively(root, component -> {
            MouseListener[] mouseListeners = component.getMouseListeners();
            for (MouseListener mouseListener : mouseListeners) {
                root.removeMouseListener(mouseListener);
            }
        });
    }

    public static Listener listener() {
        return new Listener();
    }

    public static boolean isMainSingleClick(MouseEvent e) {
        return e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 1;
    }

    public static boolean isMainDoubleClick(MouseEvent e) {
        return e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2;
    }

    /**
     * Adds a mouse listener to the specified component that triggers a consumer for mouse click events
     * matching the specified button and click count.
     *
     * @param component the JComponent to which the mouse listener is added
     * @param button the mouse button that should trigger the consumer, as defined in MouseEvent (e.g., MouseEvent.BUTTON1)
     * @param clickCount the number of clicks required to trigger the consumer
     * @param consumer the callback function to be executed when the mouse click event matches the given button and click count
     */
    public static void onMouseClick(JComponent component, int button, int clickCount, Consumer<MouseEvent> consumer) {
        component.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() != button) return;
                if (e.getClickCount() != clickCount) return;
                consumer.accept(e);
            }
        });
    }

    public static void onButtonRelease(JComponent component, int button, Consumer<MouseEvent> consumer) {
        component.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() != button) return;
                consumer.accept(e);
            }
        });
    }

    public static class Listener implements MouseListener, MouseMotionListener {
        private Consumer<MouseEvent> clickConsumer;
        private Consumer<MouseEvent> pressConsumer;
        private Consumer<MouseEvent> releaseConsumer;
        private Consumer<MouseEvent> enterConsumer;
        private Consumer<MouseEvent> exitConsumer;
        private Consumer<MouseEvent> moveConsumer;
        private Consumer<MouseEvent> dragConsumer;

        @Override
        public void mouseClicked(MouseEvent e) {
            consume(e, clickConsumer);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            consume(e, pressConsumer);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            consume(e, releaseConsumer);
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            consume(e, enterConsumer);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            consume(e, exitConsumer);
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            consume(e, dragConsumer);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            consume(e, moveConsumer);
        }

        public Listener onClick(Consumer<MouseEvent> consumer) {
            this.clickConsumer = consumer;
            return this;
        }

        public Listener onPress(Consumer<MouseEvent> consumer) {
            this.pressConsumer = consumer;
            return this;
        }

        public Listener onRelease(Consumer<MouseEvent> consumer) {
            this.releaseConsumer = consumer;
            return this;
        }

        public Listener onEnter(Consumer<MouseEvent> consumer) {
            this.enterConsumer = consumer;
            return this;
        }

        public Listener onExit(Consumer<MouseEvent> consumer) {
            this.exitConsumer = consumer;
            return this;
        }

        public Listener onMove(Consumer<MouseEvent> consumer) {
            this.moveConsumer = consumer;
            return this;
        }

        public Listener onDrag(Consumer<MouseEvent> consumer) {
            this.dragConsumer = consumer;
            return this;
        }

        private void consume(MouseEvent e, @Nullable Consumer<MouseEvent> consumer) {
            if (consumer == null) return;
            guarded(consumer, c -> c.accept(e));
        }
    }

    /**
     * Add the given mouse listener as first in the sequence for the component
     * @param component the {@link JComponent} to add the listener to
     * @param listener the {@link MouseListener} to be added
     */
    public static void insertMouseListener(JComponent component, MouseListener listener) {
        MouseListener[] mouseListeners = component.getMouseListeners();
        Arrays.stream(mouseListeners).forEach(l -> component.removeMouseListener(l));
        component.addMouseListener(listener);
        Arrays.stream(mouseListeners).forEach(l -> component.addMouseListener(l));
    }
}
