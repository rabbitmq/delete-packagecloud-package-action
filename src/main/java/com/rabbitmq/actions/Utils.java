/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.rabbitmq.actions;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;

abstract class Utils {

  static final Gson GSON =
      new GsonBuilder()
          .registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeDeserializer())
          .create();

  private Utils() {}

  static String encodeHttpParameter(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  static String base64(String in) {
    return Base64.getEncoder().encodeToString(in.getBytes(StandardCharsets.UTF_8));
  }

  static Predicate<Domain.Package> globPredicate(String globs) {
    globs = globs == null || globs.isBlank() ? "*" : globs;
    return Arrays.stream(globs.split(","))
        .map(String::trim)
        .map(g -> "glob:" + g)
        .map(g -> FileSystems.getDefault().getPathMatcher(g))
        .map(
            pathMatcher ->
                (Predicate<Domain.Package>)
                    p -> pathMatcher.matches(Path.of(p.filename()).getFileName()))
        .reduce(aPackage -> false, Predicate::or);
  }

  static Predicate<Domain.Package> versionPredicate(String version) {
    Pattern pattern = Pattern.compile(version, Pattern.CASE_INSENSITIVE);
    return p -> pattern.matcher(p.version()).find();
  }

  static void testSequence() {
    Consumer<String> display = m -> LogUtils.logGreen(m);
    String message;
    int exitCode = 0;
    try {
      String testUri = "https://www.wikipedia.org/";
      LogUtils.logYellow("Starting test sequence, trying to reach " + testUri);
      HttpRequest request = HttpRequest.newBuilder().uri(new URI(testUri)).GET().build();
      HttpResponse<Void> response =
          HttpClient.newBuilder()
              .connectTimeout(Duration.ofSeconds(60))
              .build()
              .send(request, HttpResponse.BodyHandlers.discarding());
      int statusClass = response.statusCode() - response.statusCode() % 100;
      message = "Response code is " + response.statusCode();
      if (statusClass != 200) {
        display = LogUtils::logRed;
        exitCode = 1;
      }
    } catch (Exception e) {
      message = "Error during test sequence: " + e.getMessage();
      display = LogUtils::logRed;
      exitCode = 1;
    }
    display.accept(message);
    System.exit(exitCode);
  }

  static class ZonedDateTimeDeserializer implements JsonDeserializer<ZonedDateTime> {

    @Override
    public ZonedDateTime deserialize(
        JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {
      return ZonedDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_ZONED_DATE_TIME);
    }
  }
}
