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

package fredboat.config;

import org.springframework.stereotype.Component;

/**
 * Created by napster on 23.02.18.
 * <p>
 * Proxy calls to our reloading property configs
 */
@Component
public class FilePropertyConfigProvider implements PropertyConfigProvider {

    public FilePropertyConfigProvider() {
    }

    @Override
    public AppConfig getAppConfig() {
        return FileConfig.get();
    }

    @Override
    public AudioSourcesConfig getAudioSourcesConfig() {
        return FileConfig.get();
    }

    @Override
    public Credentials getCredentials() {
        return FileConfig.get();
    }

    @Override
    public DatabaseConfig getDatabaseConfig() {
        return FileConfig.get();
    }

    @Override
    public EventLoggerConfig getEventLoggerConfig() {
        return FileConfig.get();
    }

    @Override
    public LavalinkConfig getLavalinkConfig() {
        return FileConfig.get();
    }
}
