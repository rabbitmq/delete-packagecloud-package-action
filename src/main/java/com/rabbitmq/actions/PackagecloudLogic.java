/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.rabbitmq.actions;

import static com.rabbitmq.actions.LogUtils.logIndent;
import static com.rabbitmq.actions.LogUtils.yellow;
import static java.util.stream.Collectors.toList;

import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

abstract class PackagecloudLogic {

  private static final String API_URL = "https://packagecloud.io/api/v1/repos";

  private PackagecloudLogic() {}

  interface PackageAccess {

    List<Domain.Package> list();

    void delete(Domain.Package p);
  }

  static class PackagecloudPackageAccess implements PackageAccess {

    private final HttpClient client =
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(60)).build();

    private final String baseUrl;
    private final String username, repository, token, type, globs, version;

    PackagecloudPackageAccess(
        String username,
        String repository,
        String token,
        String type,
        String globs,
        String version) {
      this(API_URL, username, repository, token, type, globs, version);
    }

    PackagecloudPackageAccess(
        String baseUrl,
        String username,
        String repository,
        String token,
        String type,
        String globs,
        String version) {
      this.baseUrl = baseUrl;
      this.username = username;
      this.repository = repository;
      this.token = token;
      this.type = type;
      this.globs = globs;
      this.version = version;
    }

    static String nextLink(String linkHeader) {
      String nextLink = null;
      for (String link : linkHeader.split(",")) {
        // e.g.
        // <https://api.github.com/repositories/343344332/releases?per_page=1&page=3>; rel="next"
        String[] urlRel = link.split(";");
        if ("rel=\"next\"".equals(urlRel[1].trim())) {
          String url = urlRel[0].trim();
          // removing the < and >
          nextLink = url.substring(1, url.length() - 1);
        }
      }
      return nextLink;
    }

    @Override
    public List<Domain.Package> list() {
      Map<String, String> queryParameters = new LinkedHashMap<>();
      if (this.type != null) {
        queryParameters.put("filter", this.type);
      }

      String path = "packages.json";

      if (!queryParameters.isEmpty()) {
        String parameters =
            queryParameters.entrySet().stream()
                .map(e -> e.getKey() + "=" + Utils.encodeHttpParameter(e.getValue()))
                .collect(Collectors.joining("&"));
        path = path + "?" + parameters;
      }

      HttpRequest request = requestBuilder(path).GET().build();
      try {
        Type type = TypeToken.getParameterized(List.class, Domain.Package.class).getType();
        List<Domain.Package> packages = new ArrayList<>();
        boolean hasMore = true;
        while (hasMore) {
          HttpResponse<String> response =
              client.send(request, HttpResponse.BodyHandlers.ofString());
          packages.addAll(Utils.GSON.fromJson(response.body(), type));
          Optional<String> link = response.headers().firstValue("link");
          String nextLink;
          if (link.isPresent() && (nextLink = nextLink(link.get())) != null) {
            request = requestBuilder().uri(URI.create(nextLink)).GET().build();
          } else {
            hasMore = false;
          }
        }
        return filter(this.version, this.globs, packages);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    static List<Domain.Package> filter(
        String version, String globs, List<Domain.Package> packages) {
      Predicate<Domain.Package> filter = null;
      if (globs != null) {
        filter = Utils.globPredicate(globs);
        packages = packages.stream().filter(Utils.globPredicate(globs)).collect(toList());
      }

      if (version != null) {
        filter =
            filter == null
                ? Utils.versionPredicate(version)
                : filter.and(Utils.versionPredicate(version));
      }

      if (filter != null) {
        packages = packages.stream().filter(filter).collect(toList());
      }
      return packages;
    }

    @Override
    public void delete(Domain.Package p) {
      // getting just the host to add it to the destroy URL
      String base = this.baseUrl.replace(URI.create(this.baseUrl).getPath(), "");
      HttpRequest request =
          requestBuilder().DELETE().uri(URI.create(base + p.destroy_url())).build();
      try {
        HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() != 200) {
          logIndent(yellow("Unexpected response code:" + response.statusCode()));
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    private HttpRequest.Builder requestBuilder() {
      return auth(HttpRequest.newBuilder());
    }

    private HttpRequest.Builder requestBuilder(String path) {
      String url = String.format("%s/%s/%s/%s", this.baseUrl, this.username, this.repository, path);
      return auth(HttpRequest.newBuilder().uri(URI.create(url)));
    }

    private HttpRequest.Builder auth(HttpRequest.Builder builder) {
      return builder.setHeader("Authorization", "Basic " + Utils.base64(this.token + ":"));
    }
  }
}
