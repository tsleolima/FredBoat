/*
 * MIT License
 *
 * Copyright (c) 2017-2018 Frederik Ar. Mikkelsen
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package fredboat.main;

import fredboat.audio.queue.MusicPersistenceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Created by napster on 23.02.18.
 */
@Component
public class ShutdownHandler {

    public static final int UNKNOWN_SHUTDOWN_CODE = -991023;

    private static final Logger log = LoggerFactory.getLogger(ShutdownHandler.class);
    private final MusicPersistenceHandler musicPersistenceHandler;

    private int shutdownCode = UNKNOWN_SHUTDOWN_CODE;//Used when specifying the intended code for shutdown hooks

    public ShutdownHandler(MusicPersistenceHandler musicPersistenceHandler) {
        this.musicPersistenceHandler = musicPersistenceHandler;
    }

    public void shutdown(int code) {
        log.info("Shutting down with exit code " + code);
        shutdownCode = code;
        preShutdown();
        System.exit(code);
    }

    private void preShutdown() {
        try {
            int code = shutdownCode != ShutdownHandler.UNKNOWN_SHUTDOWN_CODE ? shutdownCode : -1;
            musicPersistenceHandler.handlePreShutdown(code);
        } catch (Exception e) {
            log.error("Critical error while handling music persistence.", e);
        }
    }

    public int getShutdownCode() {
        return shutdownCode;
    }
}
