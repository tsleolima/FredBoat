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

import fredboat.config.property.BackendConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

/**
 * Created by napster on 16.03.18.
 */
@Configuration
public class RestTemplateConfiguration {

    private static final Logger log = LoggerFactory.getLogger(RestTemplateConfiguration.class);

    @Bean
    public RestTemplate quarterdeckRestTemplate(BackendConfig backendConfig, GsonHttpMessageConverter gson) {
        return new RestTemplateBuilder()
                .basicAuthorization(backendConfig.getQuarterdeck().getUser(), backendConfig.getQuarterdeck().getPass())
                .messageConverters(gson)
                .additionalInterceptors(((req, body, execution) -> {
                    log.debug("{} {} {}", req.getMethodValue(), req.getURI().toString(), new String(body));
                    return execution.execute(req, body);
                }))
                .build();
    }
}
