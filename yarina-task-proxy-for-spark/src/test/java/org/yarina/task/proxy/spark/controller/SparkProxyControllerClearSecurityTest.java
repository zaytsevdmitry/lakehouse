/*
 * "Yarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/yarina
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.yarina.task.proxy.spark.controller;

import org.yarina.task.proxy.spark.dto.SubmissionResponse;
import org.yarina.task.proxy.spark.exception.ForbiddenException;
import org.yarina.task.proxy.spark.service.SparkProxyService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The proxy itself must reject the destructive {@code /clear} operation with HTTP 403
 * (via {@link GlobalExceptionHandler}) when the caller has no {@code <domain>:CONTROL}
 * authority, regardless of what any frontend shows.
 */
class SparkProxyControllerClearSecurityTest {

    private static Authentication authOf(String... authorities) {
        return new UsernamePasswordAuthenticationToken(
                "u", null,
                List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList());
    }

    @Test
    void clearIsRejectedWithoutControlRole() {
        SparkProxyService proxy = mock(SparkProxyService.class);
        SparkProxyController controller = new SparkProxyController(proxy);

        assertThatThrownBy(() -> controller.clearCompleted(authOf("ROLE_ANALYTICS:EDITOR", "ROLE_VIEWER")))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> controller.clearCompleted(authOf()))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> controller.clearCompleted(null))
                .isInstanceOf(ForbiddenException.class);

        verify(proxy, never()).clear();
    }

    @Test
    void clearIsAllowedWithControlInAnyDomain() {
        SparkProxyService proxy = mock(SparkProxyService.class);
        SparkProxyController controller = new SparkProxyController(proxy);
        SubmissionResponse response = new SubmissionResponse("ClearResponse", "ok", null, null, true);
        when(proxy.clear()).thenReturn(response);

        assertThatCode(() -> controller.clearCompleted(authOf("ROLE_ANALYTICS:CONTROL")))
                .doesNotThrowAnyException();
        assertThatCode(() -> controller.clearCompleted(authOf("ROLE_PLATFORM:CONTROL")))
                .doesNotThrowAnyException();

        verify(proxy, org.mockito.Mockito.times(2)).clear();
    }

    @Test
    void forbiddenExceptionIsMappedToHttp403() {
        SubmissionResponse body = new GlobalExceptionHandler()
                .handleForbidden(new ForbiddenException("Access denied"))
                .getBody();

        assertThat(new GlobalExceptionHandler()
                .handleForbidden(new ForbiddenException("Access denied"))
                .getStatusCode().value()).isEqualTo(403);
        assertThat(body).isNotNull();
        assertThat(body.success()).isFalse();
    }
}
