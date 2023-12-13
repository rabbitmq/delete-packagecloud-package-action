/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.rabbitmq.actions;

// import static com.rabbitmq.actions.PackagecloudDeletePackageAction.filterByTag;
// import static com.rabbitmq.actions.PackagecloudDeletePackageAction.filterForDeletion;
import static com.rabbitmq.actions.PackagecloudDeletePackageAction.*;
import static com.rabbitmq.actions.PackagecloudLogic.PackagecloudPackageAccess.filter;
import static java.util.Arrays.asList;
import static java.util.Collections.shuffle;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import com.rabbitmq.actions.Domain.PackageVersion;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

public class PackagecloudDeletePackageActionTest {

  @Test
  void filterPackagesVersion() {
    List<Domain.Package> packages =
        asList(
            p(null, "25.0"),
            p(null, "25.0.1"),
            p(null, "25.1.1"),
            p(null, "25.2.0"),
            p(null, "26.0"),
            p(null, "26.0.1"),
            p(null, "26.1.1"),
            p(null, "26.2.0"));

    assertThat(filter("^25\\.*", null, packages))
        .hasSize(4)
        .allMatch(p -> p.version().startsWith("25"));
    assertThat(filter("^25\\.1\\.*", null, packages))
        .hasSize(1)
        .allMatch(p -> p.version().startsWith("25.1"));
  }

  @Test
  void filterPackagesGlobs() {
    List<Domain.Package> packages =
        asList(
            p("erlang-25.0.1-1.el9.x86_64.rpm", null),
            p("erlang-debuginfo-25.0.1-1.el9.x86_64.rpm", null),
            p("erlang-25.0.1-1.el8.x86_64.rpm", null),
            p("erlang-debuginfo-25.0.1-1.el8.x86_64.rpm", null));

    assertThat(filter(null, "erlang-*-*.el8*.rpm", packages))
        .hasSize(2)
        .allMatch(p -> p.filename().contains("el8"));
  }

  @Test
  void filterVersionGlobs() {
    List<Domain.Package> packages =
        asList(
            p("erlang-25.0-1.el8.x86_64.rpm", "25.0"),
            p("erlang-debuginfo-25.0-1.el8.x86_64.rpm", "25.0"),
            p("erlang-25.0-1.el9.x86_64.rpm", "25.0"),
            p("erlang-debuginfo-25.0-1.el9.x86_64.rpm", "25.0"),
            p("erlang-25.0.1-1.el8.x86_64.rpm", "25.0.1"),
            p("erlang-debuginfo-25.0-1.el8.x86_64.rpm", "25.0.1"),
            p("erlang-25.0.1-1.el9.x86_64.rpm", "25.0.1"),
            p("erlang-debuginfo-25.0-1.el9.x86_64.rpm", "25.0.1"),
            p("erlang-25.1-1.el8.x86_64.rpm", "25.1"),
            p("erlang-debuginfo-25.1-1.el8.x86_64.rpm", "25.1"),
            p("erlang-25.1-1.el9.x86_64.rpm", "25.1"),
            p("erlang-debuginfo-25.1-1.el9.x86_64.rpm", "25.1"),
            p("erlang-25.1.1-1.el8.x86_64.rpm", "25.1.1"),
            p("erlang-debuginfo-25.1.1-1.el8.x86_64.rpm", "25.1.1"),
            p("erlang-25.1.1-1.el9.x86_64.rpm", "25.1.1"),
            p("erlang-debuginfo-25.1.1-1.el9.x86_64.rpm", "25.1.1"),
            p("erlang-26.0-1.el8.x86_64.rpm", "26.0"),
            p("erlang-debuginfo-26.0-1.el8.x86_64.rpm", "26.0"),
            p("erlang-26.0-1.el9.x86_64.rpm", "26.0"),
            p("erlang-debuginfo-26.0-1.el9.x86_64.rpm", "26.0"));

    assertThat(filter("^25\\.*", "erlang-*-*.el8*.rpm", packages))
        .hasSize(8)
        .allMatch(p -> p.filename().contains("el8"));

    assertThat(filter("^25\\.1\\.*", "erlang-*-*.el8*.rpm", packages))
        .hasSize(4)
        .allMatch(p -> p.filename().contains("el8"));

    assertThat(filter("^26\\.*", "erlang-*-*.el9*.rpm", packages))
        .hasSize(2)
        .allMatch(p -> p.filename().contains("el9"));
  }

  @Test
  void filterForDeletionShouldReturnVersionsToDelete() {
    List<PackageVersion> versions =
        asList(
                "1:22.0-1",
                "1:22.3-1",
                "1:22.3.4-1",
                "1:22.3.4.1-1",
                "1:22.3.4.1-2",
                "1:22.3.4.10-1",
                "1:22.3.4.11-1",
                "1:22.3.4.12-1",
                "1:22.3.4.13-1",
                "1:22.3.4.14-1",
                "1:22.3.4.15-1",
                "1:22.3.4.16-1", // latest
                "1:22.3.4.2-1",
                "1:22.3.4.3-1",
                "1:22.3.4.4-1",
                "1:22.3.4.5-1",
                "1:22.3.4.6-1",
                "1:22.3.4.7-1",
                "1:22.3.4.8-1",
                "1:22.3.4.9-1")
            .stream()
            .map(Domain.PackageVersion::new)
            .collect(toList());

    shuffle(versions);
    assertThat(filterForDeletion(versions, 2, true))
        .hasSize(versions.size() - 2)
        .containsExactlyInAnyOrder(
            "1:22.0-1",
            "1:22.3-1",
            "1:22.3.4-1",
            "1:22.3.4.1-1",
            "1:22.3.4.1-2",
            "1:22.3.4.10-1",
            "1:22.3.4.11-1",
            "1:22.3.4.12-1",
            "1:22.3.4.13-1",
            "1:22.3.4.14-1",
            "1:22.3.4.2-1",
            "1:22.3.4.3-1",
            "1:22.3.4.4-1",
            "1:22.3.4.5-1",
            "1:22.3.4.6-1",
            "1:22.3.4.7-1",
            "1:22.3.4.8-1",
            "1:22.3.4.9-1")
        .doesNotContain("1:22.3.4.15-1", "1:22.3.4.16-1");

    assertThat(filterForDeletion(versions, versions.size() - 1, true))
        .hasSize(1)
        .containsExactly("1:22.0-1");

    assertThat(filterForDeletion(versions, 0, true))
        .hasSameSizeAs(versions)
        .hasSameElementsAs(versions.stream().map(v -> v.version).collect(toList()));

    assertThat(filterForDeletion(versions, versions.size() + 1, true)).isEmpty();

    versions =
        asList(
                "22.2.4-1.el8",
                "22.3-1.el8",
                "22.3.4-1.el8",
                "22.3.4.1-1.el8",
                "22.3.4.10-1.el8",
                "22.3.4.11-1.el8",
                "22.3.4.12-1.el8",
                "22.3.4.16-1.el8", // latest
                "22.3.4.2-1.el8",
                "22.3.4.3-1.el8",
                "22.3.4.4-1.el8",
                "22.3.4.5-1.el8",
                "22.3.4.6-1.el8",
                "22.3.4.7-1.el8")
            .stream()
            .map(Domain.PackageVersion::new)
            .collect(toList());
    shuffle(versions);
    assertThat(filterForDeletion(versions, 2, true))
        .hasSize(versions.size() - 2)
        .containsExactlyInAnyOrder(
            "22.2.4-1.el8",
            "22.3-1.el8",
            "22.3.4-1.el8",
            "22.3.4.1-1.el8",
            "22.3.4.10-1.el8",
            "22.3.4.11-1.el8",
            "22.3.4.2-1.el8",
            "22.3.4.3-1.el8",
            "22.3.4.4-1.el8",
            "22.3.4.5-1.el8",
            "22.3.4.6-1.el8",
            "22.3.4.7-1.el8")
        .doesNotContain("22.3.4.12-1.el8", "22.3.4.16-1.el8");

    assertThat(filterForDeletion(versions, versions.size() - 1, true))
        .hasSize(1)
        .containsExactly("22.2.4-1.el8");

    assertThat(filterForDeletion(versions, 0, true))
        .hasSameSizeAs(versions)
        .hasSameElementsAs(versions.stream().map(v -> v.version).collect(toList()));

    assertThat(filterForDeletion(versions, versions.size() + 1, true)).isEmpty();
  }

  @Test
  void filterForDeletionShouldReturnVersionsToDeleteWhenUsingUploadingDate() {
    List<PackageVersion> versions =
        asList(
            pv("1.1", "2021-04-01"),
            pv("1.2", "2021-04-02"),
            pv("1.3", "2021-04-03"),
            pv("1.4", "2021-04-04"));

    shuffle(versions);

    assertThat(filterForDeletion(versions, 2, false))
        .hasSize(versions.size() - 2)
        .containsExactly("1.1", "1.2");

    assertThat(filterForDeletion(versions, 1, false))
        .hasSize(versions.size() - 1)
        .containsExactly("1.1", "1.2", "1.3");

    versions =
        asList(
            pv("1.1", "2021-04-01"),
            pv("1.3", "2021-04-02"),
            pv("1.2", "2021-04-03"), // uploaded after 1.3
            pv("1.4", "2021-04-04"));

    shuffle(versions);

    assertThat(filterForDeletion(versions, 2, false))
        .hasSize(versions.size() - 2)
        .containsExactly("1.1", "1.3");

    assertThat(filterForDeletion(versions, 2, true))
        .hasSize(versions.size() - 2)
        .containsExactly("1.1", "1.2");
  }

  @Test
  void testLastMinorPatches() {
    List<String> versions =
        asList(
                "1:22.0-1",
                "1:22.1.5-1",
                "1:22.1.4-1",
                "1:22.1.7-1",
                "1:22.1.6-1",
                "1:22.3-1",
                "1:22.3.4-1",
                "1:22.3.4.1-1",
                "1:22.3.4.1-2",
                "1:22.3.4.2-1",
                "1:22.3.4.3-1")
            .stream()
            .collect(toList());

    assertThat(lastMinorPatches("22.3", versions))
        .hasSize(2)
        .containsExactlyInAnyOrder("1:22.1.7-1", "1:22.0-1");

    versions =
        asList(
            "1:24.0.2-1",
            "1:24.0.3-1",
            "1:24.0.4-1",
            "1:24.0.5-1",
            "1:24.0.6-1",
            "1:24.1-1",
            "1:24.1.1-1",
            "1:24.1.2-1",
            "1:24.1.3-1",
            "1:24.1.4-1",
            "1:24.1.5-1",
            "1:24.1.6-1",
            "1:24.1.7-1",
            "1:24.2-1",
            "1:24.2.1-1",
            "1:24.2.2-1",
            "1:24.3-1",
            "1:24.3.1-1");

    assertThat(lastMinorPatches("24.3", versions))
        .hasSize(3)
        .containsExactlyInAnyOrder("1:24.2.2-1", "1:24.1.7-1", "1:24.0.6-1");
  }

  @Test
  void testLatestMinor() {
    List<String> versions =
        asList(
            "1:24.1.1-1",
            "1:24.1.3-1",
            "1:24.1.2-1",
            "1:24.3.1-1",
            "1:24.3.3-1",
            "1:24.3.2-1",
            "1:24.1.5-1",
            "1:24.1.4-1",
            "1:24.1.7-1",
            "1:24.1.6-1",
            "1:24.0.4-1",
            "1:24.0.3-1",
            "1:24.0.2-1",
            "1:24.3-1",
            "1:24.2-1",
            "1:24.2.2-1",
            "1:24.1-1",
            "1:24.2.1-1",
            "1:24.0.6-1",
            "1:24.0.5-1");
    shuffle(versions);
    assertThat(latestMinor(versions)).isEqualTo("24.3");
  }

  @Test
  void extractLatestMinorThenGetLastMinorPatches() {
    List<String> detected =
        versions(
            "1:24.1.1-1, 1:24.1.3-1, 1:24.1.2-1, 1:24.3.1-1, 1:24.3.3-1, "
                + "1:24.3.2-1, 1:24.1.5-1, 1:24.1.4-1, 1:24.1.7-1, 1:24.1.6-1, "
                + "1:24.0.4-1, 1:24.0.3-1, 1:24.0.2-1, 1:24.3-1, 1:24.2-1, "
                + "1:24.2.2-1, 1:24.1-1, 1:24.2.1-1, 1:24.0.6-1, 1:24.0.5-1");
    List<String> toDelete =
        versions(
            "1:24.0.2-1, 1:24.0.3-1, 1:24.0.4-1, 1:24.0.5-1, 1:24.0.6-1, 1:24.1-1, "
                + "1:24.1.1-1, 1:24.1.2-1, 1:24.1.3-1, 1:24.1.4-1, 1:24.1.5-1, "
                + "1:24.1.6-1, 1:24.1.7-1, 1:24.2-1, 1:24.2.1-1, 1:24.2.2-1, 1:24.3-1, 1:24.3.1-1");

    String latestMinor = latestMinor(detected);
    assertThat(latestMinor).isEqualTo("24.3");
    assertThat(lastMinorPatches(latestMinor, toDelete))
        .containsExactlyInAnyOrder("1:24.0.6-1", "1:24.1.7-1", "1:24.2.2-1");

    detected =
        versions(
            "1:24.1.3-1, 1:24.3.1-1, 1:24.3.3-1, 1:24.3.4.1-1, 1:24.3.2-1, 1:24.3.4.3-1, "
                + "1:24.3.4-1, 1:24.3.4.2-1, 1:24.1.5-1, 1:24.1.4-1, 1:24.1.7-1, "
                + "1:24.1.6-1, 1:24.3-1, 1:24.2-1, 1:24.2.2-1, 1:24.2.1-1");
    toDelete =
        versions(
            "1:24.1.3-1, 1:24.1.4-1, 1:24.1.5-1, 1:24.1.6-1, 1:24.1.7-1, 1:24.2-1, "
                + "1:24.2.1-1, 1:24.2.2-1, 1:24.3-1, 1:24.3.1-1, 1:24.3.2-1, 1:24.3.3-1, "
                + "1:24.3.4-1, 1:24.3.4.1-1");

    latestMinor = latestMinor(detected);
    assertThat(latestMinor).isEqualTo("24.3");
    assertThat(lastMinorPatches(latestMinor, toDelete))
        .containsExactlyInAnyOrder("1:24.1.7-1", "1:24.2.2-1");

    detected =
        versions(
            "1:24.3.4.8-1, 1:24.3.4.5-1, 1:24.3.4.4-1, 1:24.3.4.7-1, 1:24.3.4.6-1, "
                + "1:24.3.4.1-1, 1:24.3.4.3-1, 1:24.3.4-1, 1:24.3.4.2-1");
    toDelete =
        versions(
            "1:24.3.4-1, 1:24.3.4.1-1, 1:24.3.4.2-1, 1:24.3.4.3-1, 1:24.3.4.4-1, 1:24.3.4.5-1, 1:24.3.4.6-1");

    latestMinor = latestMinor(detected);
    assertThat(latestMinor).isEqualTo("24.3");
    assertThat(lastMinorPatches(latestMinor, toDelete)).isEmpty();

    detected =
        versions(
            "1:25.2-1, 1:25.2.2-1, 1:25.0.4-1, 1:25.1-1, 1:25.0-1, 1:25.0.1-1, "
                + "1:25.0.3-1, 1:25.0.2-1, 1:25.1.2-1, 1:25.1.1-1, 1:25.2.1-1");
    toDelete =
        versions(
            "1:25.0-1, 1:25.0.1-1, 1:25.0.2-1, 1:25.0.3-1, 1:25.0.4-1, 1:25.1-1, 1:25.1.1-1, "
                + "1:25.1.2-1, 1:25.2-1");

    latestMinor = latestMinor(detected);
    assertThat(latestMinor).isEqualTo("25.2");
    assertThat(lastMinorPatches(latestMinor, toDelete))
        .containsExactlyInAnyOrder("1:25.0.4-1", "1:25.1.2-1");

    detected =
        versions(
            "25.1-1.el8, 25.1.1-1.el8, 25.0.1-1.el8, 25.0.2-1.el8, 25.1.2-1.el8, "
                + "25.0.3-1.el8, 25.0-1.el8, 25.0.4-1.el8, 25.1.1-2.el8");
    toDelete =
        versions(
            "25.0-1.el8, 25.0.1-1.el8, 25.0.2-1.el8, 25.0.3-1.el8, 25.0.4-1.el8, "
                + "25.1-1.el8, 25.1.1-1.el8, 25.1.1-2.el8");

    latestMinor = latestMinor(detected);
    assertThat(latestMinor).isEqualTo("25.1");
    assertThat(lastMinorPatches(latestMinor, toDelete)).containsExactlyInAnyOrder("25.0.4-1.el8");

    detected =
        versions(
            "25.1-1.el8, 25.1.1-1.el8, 25.0.1-1.el8, 25.0.2-1.el8, 25.1.2-1.el8, "
                + "25.0.3-1.el8, 25.0-1.el8, 25.0.4-1.el8, 25.2-1.el8, 25.1.1-2.el8");
    toDelete =
        versions(
            "25.0-1.el8, 25.0.1-1.el8, 25.0.2-1.el8, 25.0.3-1.el8, 25.0.4-1.el8, "
                + "25.1-1.el8, 25.1.1-1.el8, 25.1.1-2.el8");

    latestMinor = latestMinor(detected);
    assertThat(latestMinor).isEqualTo("25.2");
    assertThat(lastMinorPatches(latestMinor, toDelete))
        .containsExactlyInAnyOrder("25.0.4-1.el8", "25.1.1-2.el8");
  }

  static List<String> versions(String line) {
    List<String> versions = Arrays.stream(line.split(",")).map(String::trim).collect(toList());
    Collections.shuffle(versions);
    return versions;
  }

  static PackageVersion pv(String version, String date) {
    PackageVersion pv = new PackageVersion(version);
    pv.lastPackageDate =
        ZonedDateTime.parse(date + "T12:58:11.418817Z", DateTimeFormatter.ISO_ZONED_DATE_TIME);
    return pv;
  }

  private static Domain.Package p(String filename, String version) {
    Domain.Package p = new Domain.Package();
    p.setFilename(filename);
    p.setVersion(version);
    return p;
  }
}
