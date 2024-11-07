package com.dbn.diagnostics;

import com.dbn.common.state.PersistentStateElement;
import com.dbn.nls.NlsSupport;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import static com.dbn.common.notification.NotificationGroup.DIAGNOSTICS;
import static com.dbn.common.notification.NotificationSupport.sendInfoNotification;
import static com.dbn.common.options.setting.Settings.getBoolean;
import static com.dbn.common.options.setting.Settings.getInteger;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setBoolean;
import static com.dbn.common.options.setting.Settings.setInteger;

@Getter
@Setter
public class DeveloperMode implements PersistentStateElement, NlsSupport {
    private volatile boolean enabled;
    private volatile Timer timer;
    private volatile long timerStart;
    private int timeout = 10;

    private void start() {
        cancel();
        timer = new Timer("DBN - Developer Mode Disable Timer");
        timer.schedule(createTimerTask(), TimeUnit.MINUTES.toMillis(timeout));
        timerStart = System.currentTimeMillis();
    }

    private TimerTask createTimerTask() {
        return new TimerTask() {
            @Override
            public void run() {
                enabled = false;
                cancel();
            }
        };
    }

    private void cancel() {
        Timer timer = this.timer;
        if (timer != null) {
            timer.cancel();
            this.timer = null;
            this.timerStart = 0;
        }
    }

    public synchronized void setEnabled(boolean enabled) {
        boolean changed = this.enabled != enabled;
        this.enabled = enabled;
        cancel();

        if (enabled) {
            start();
            // just became active - notify activated
            sendInfoNotification(null, DIAGNOSTICS, txt("ntf.diagnostics.warning.DeveloperModeActivatedFor", timeout));
        } else if (changed) {
            // not active and changed just now -> notify deactivated
            sendInfoNotification(null, DIAGNOSTICS, txt("ntf.diagnostics.warning.DeveloperModeDeactivated"));
        }
    }

    public String getRemainingTime() {
        if (!enabled) return txt("app.shared.label.OneSecond");

        long lapsed = System.currentTimeMillis() - timerStart;
        long lapsedSeconds = TimeUnit.MILLISECONDS.toSeconds(lapsed);
        long remainingSeconds = Math.max(0, TimeUnit.MINUTES.toSeconds(timeout) - lapsedSeconds);
        return remainingSeconds < 60 ?
                txt("app.shared.label.MoreSeconds", remainingSeconds) :
                txt("app.shared.label.MoreMinutes", TimeUnit.SECONDS.toMinutes(remainingSeconds));
    }


    @Override
    public void readState(Element element) {
        Element developerMode = element.getChild("developer-mode");
        if (developerMode != null) {
            setTimeout(getInteger(developerMode, "timeout", timeout));
            setEnabled(getBoolean(developerMode, "enabled", enabled));
        }
    }

    @Override
    public void writeState(Element element) {
        Element developerMode = newElement(element, "developer-mode");
        setInteger(developerMode, "timeout", timeout);
        setBoolean(developerMode, "enabled", enabled);
    }
}
