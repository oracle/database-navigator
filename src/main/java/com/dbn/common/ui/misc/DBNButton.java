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

package com.dbn.common.ui.misc;

import com.dbn.common.ui.util.Borders;
import com.dbn.common.ui.util.Cursors;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.border.Border;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class DBNButton extends JLabel {
    public DBNButton(Icon image) {
        super(image);
        setBorder(Borders.buttonBorder());
        setCursor(Cursors.handCursor());
    }

    @Override
    public void setBorder(Border border) {
        super.setBorder(border);
    }

    @Override
    public synchronized void addMouseListener(MouseListener l) {
        super.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (isEnabled()) l.mouseClicked(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (isEnabled()) l.mousePressed(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (isEnabled()) l.mouseReleased(e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (isEnabled()) l.mouseEntered(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (isEnabled()) l.mouseEntered(e);
            }
        });
    }

    @Override
    public synchronized void addKeyListener(KeyListener l) {
        super.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (isEnabled()) l.keyTyped(e);
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (isEnabled()) l.keyPressed(e);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (isEnabled()) l.keyReleased(e);
            }
        });
    }
}
