/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.rabbitmq.actions;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.rabbitmq.actions.PackagecloudLogic.PackagecloudPackageAccess;
import java.util.List;
import java.util.function.IntFunction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HttpTest {

  WireMockServer wireMockServer;

  @BeforeEach
  public void startMockServer() {
    wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
    wireMockServer.start();
    WireMock.configureFor(wireMockServer.port());
  }

  @AfterEach
  public void stopMockServer() {
    wireMockServer.stop();
  }

  @Test
  void findForDelete() {
    stubFor(get(urlPathMatching("/.*")).willReturn(aResponse().withBody("[]")));
    PackagecloudPackageAccess access = access();
    access.list();
    verify(exactly(1), getRequestedFor(urlEqualTo("/rabbitmq/erlang/packages.json?filter=deb")));
  }

  @Test
  void pagination() {
    String response = "[{\"filename\": \"some-package.deb\"}]";
    IntFunction<String> nextHeader =
        page -> "<" + baseUrl() + "/rabbitmq/erlang/page-" + page + ">; rel=\"next\"";
    stubFor(
        get(urlPathMatching("/rabbitmq/erlang/packages.*"))
            .willReturn(aResponse().withBody(response).withHeader("Link", nextHeader.apply(2))));
    stubFor(
        get(urlPathMatching("/rabbitmq/erlang/page-2"))
            .willReturn(aResponse().withBody(response).withHeader("Link", nextHeader.apply(3))));
    stubFor(
        get(urlPathMatching("/rabbitmq/erlang/page-3")).willReturn(aResponse().withBody(response)));
    PackagecloudPackageAccess access = access();
    List<Domain.Package> packages = access.list();
    assertThat(packages).hasSize(3);
  }

  PackagecloudPackageAccess access() {
    String username = "rabbitmq";
    String repository = "erlang";
    String token = "abcde";
    String type = "deb";
    String globs = null;
    String versionFilter = null;
    return new PackagecloudPackageAccess(
        baseUrl(), username, repository, token, type, globs, versionFilter);
  }

  String baseUrl() {
    return "http://localhost:" + wireMockServer.port();
  }
}
