/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.rabbitmq.actions;

import static com.rabbitmq.actions.LogUtils.*;
import static java.util.stream.Collectors.*;

import com.rabbitmq.actions.Domain.PackageVersion;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class PackagecloudDeletePackageAction {

  public static void main(String[] args) {
    if (args.length == 1 && "test".equals(args[0])) {
      Utils.testSequence();
    }
    Map<String, String> envArguments = new LinkedHashMap<>();
    envArguments.put("INPUT_USERNAME", "username");
    envArguments.put("INPUT_REPOSITORY", "repository");
    envArguments.put("INPUT_TOKEN", "token");

    for (Entry<String, String> entry : envArguments.entrySet()) {
      try {
        checkParameter(entry.getKey(), entry.getValue());
      } catch (IllegalArgumentException e) {
        logRed(e.getMessage());
        System.exit(1);
      }
    }
    String username = System.getenv("INPUT_USERNAME");
    String repository = System.getenv("INPUT_REPOSITORY");
    String token = System.getenv("INPUT_TOKEN");

    String type = System.getenv("INPUT_TYPE");
    String globs = System.getenv("INPUT_GLOBS");
    String versionFilter = System.getenv("INPUT_VERSION_FILTER");
    String orderBy = System.getenv("INPUT_ORDER_BY");
    String keepLastNStr = System.getenv("INPUT_KEEP_LAST_N");
    String keepLastMinorPatchesStr = System.getenv("INPUT_KEEP_LAST_MINOR_PATCHES");
    String doDeleteStr = System.getenv("INPUT_DO_DELETE");

    boolean orderByVersion = true;
    if ("time".equals(orderBy)) {
      orderByVersion = false;
    }

    int keepLastN = 0;
    if (keepLastNStr != null) {
      try {
        keepLastN = Integer.parseInt(keepLastNStr);
      } catch (Exception e) {
        logYellow("Incorrect value for keep_last_n: " + keepLastNStr);
        logYellow("Using default value instead (" + keepLastN + ").");
      }
    }

    boolean keepLastMinorPatches = false;
    if (keepLastMinorPatchesStr != null) {
      keepLastMinorPatches = Boolean.parseBoolean(keepLastMinorPatchesStr);
    }

    boolean doDeleteTemp = false;
    if (doDeleteStr != null) {
      doDeleteTemp = Boolean.parseBoolean(doDeleteStr);
    }
    boolean doDelete = doDeleteTemp;

    PackagecloudLogic.PackageAccess access =
        new PackagecloudLogic.PackagecloudPackageAccess(
            username, repository, token, type, globs, versionFilter);

    List<Domain.Package> packages = access.list();

    Map<String, PackageVersion> versions =
        packages.stream()
            .reduce(
                new HashMap<>(),
                (packageVersions, aPackage) -> {
                  PackageVersion packageVersion =
                      packageVersions.computeIfAbsent(
                          aPackage.version(),
                          version -> {
                            PackageVersion pv = new PackageVersion(version);
                            pv.consider(aPackage);
                            return pv;
                          });
                  packageVersion.consider(aPackage);
                  return packageVersions;
                },
                (stringPackageVersionHashMap, stringPackageVersionHashMap2) -> {
                  stringPackageVersionHashMap.putAll(stringPackageVersionHashMap2);
                  return stringPackageVersionHashMap;
                });

    List<String> versionsToDelete = filterForDeletion(versions.values(), keepLastN, orderByVersion);

    Collection<String> deletionExceptions = Collections.emptySet();
    if (keepLastMinorPatches) {
      String latestMinor =
          latestMinor(versions.values().stream().map(v -> v.version).collect(toList()));
      deletionExceptions = lastMinorPatches(latestMinor, versionsToDelete);
      if (!orderByVersion) {
        logYellow("Warning: keep_last_minor_patches should only be used with order_by:version");
      }
    }

    DateTimeFormatter dateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mmO", Locale.ENGLISH);
    Function<String, String> formatVersion =
        version -> {
          PackageVersion packageVersion = versions.get(version);
          return String.format(
              "%s [%s]",
              packageVersion.version, dateTimeFormatter.format(packageVersion.lastPackageDate));
        };

    log(
        green("Version(s) detected: ")
            + versions.values().stream()
                .map(pv -> formatVersion.apply(pv.version))
                .collect(joining(", ")));
    log(
        green("Version(s) to delete: ")
            + versionsToDelete.stream().map(formatVersion).collect(joining(", ")));

    if (keepLastMinorPatches && !deletionExceptions.isEmpty()) {
      log(
          green("Deletion exception(s) (last minor patches): ")
              + deletionExceptions.stream().map(formatVersion).collect(joining(", ")));
    }

    Collection<String> exceptionsToDeletion = new HashSet<>(deletionExceptions);

    newLine();

    List<String> versionsToKeep =
        versions.values().stream()
            .filter(
                pv ->
                    !versionsToDelete.contains(pv.version)
                        || exceptionsToDeletion.contains(pv.version))
            .map(pv -> formatVersion.apply(pv.version))
            .toList();

    if (!versionsToKeep.isEmpty()) {
      log(green("Version(s) to keep: ") + String.join(", ", versionsToKeep));
    }

    newLine();

    AtomicInteger deletedCount = new AtomicInteger();
    logGreen("Packages:");
    packages.forEach(
        p -> {
          boolean shouldBeDeleted =
              versionsToDelete.contains(p.version()) && !exceptionsToDeletion.contains(p.version());
          if (shouldBeDeleted) {
            deletedCount.incrementAndGet();
          }
          if (shouldBeDeleted && doDelete) {
            try {
              access.delete(p);
              logIndent(red("deleting " + p.filename()));
            } catch (Exception e) {
              logRed("Error while trying to delete " + p.destroy_url() + ": " + e.getMessage());
            }
          } else {
            boolean isDeletionException = exceptionsToDeletion.contains(p.version());
            logIndent(
                shouldBeDeleted
                    ? (red("deleting " + p.filename()) + yellow(" (skipped)"))
                    : "keeping "
                        + p.filename()
                        + (isDeletionException ? " (latest minor patch)" : ""));
          }
        });

    newLine();
    logGreen("Deleted " + deletedCount.get() + " file(s)");
  }

  private static void checkParameter(String env, String arg) {
    if (System.getenv(env) == null) {
      throw new IllegalArgumentException("Parameter " + arg + " must be set");
    }
  }

  static List<String> filterForDeletion(
      Collection<PackageVersion> versions, int keepLastN, boolean orderByVersion) {
    if (versions.isEmpty()) {
      return Collections.emptyList();
    } else if (keepLastN <= 0) {
      // do not want to keep any, return all
      return versions.stream().map(v -> v.version).collect(toList());
    } else if (keepLastN >= versions.size()) {
      // we want to keep more than we have, so nothing to delete
      return Collections.emptyList();
    } else {
      class VersionWrapper {

        private final PackageVersion version;
        private final ComparableVersion comparableVersion;

        VersionWrapper(PackageVersion version) {
          this.version = version;
          this.comparableVersion =
              new ComparableVersion(
                  version.version.startsWith("1:")
                      ? version.version.substring(2)
                      : version.version);
        }
      }
      Comparator<VersionWrapper> comparator =
          orderByVersion
              ? Comparator.comparing(packageVersion -> packageVersion.comparableVersion)
              : Comparator.comparing(packageVersion -> packageVersion.version.lastPackageDate);
      return versions.stream()
          .map(VersionWrapper::new)
          .sorted(comparator)
          .limit(versions.size() - keepLastN)
          .map(w -> w.version.version)
          .collect(toList());
    }
  }

  static String latestMinor(List<String> versions) {
    if (versions == null || versions.isEmpty()) {
      return null;
    } else {
      return versions.stream()
          .map(PackagecloudDeletePackageAction::extractMinor)
          .distinct()
          .map(ComparableVersion::new)
          .max(Comparator.naturalOrder())
          .get()
          .toString();
    }
  }

  static String extractMinor(String version) {
    // e.g. 1:22.3.4.3-1, removing 1:
    String curatedVersion = version.startsWith("1:") ? version.substring(2) : version;
    // e.g. 22.3-1, removing -1
    curatedVersion =
        curatedVersion.contains("-")
            ? curatedVersion.substring(0, curatedVersion.lastIndexOf("-"))
            : curatedVersion;
    String[] digits = curatedVersion.split("\\.");
    if (digits == null || digits.length <= 1) {
      return curatedVersion;
    } else {
      return digits[0] + "." + digits[1];
    }
  }

  static List<String> lastMinorPatches(String minorToIgnore, List<String> versions) {
    if (versions == null || versions.isEmpty()) {
      return Collections.emptyList();
    }
    class VersionWrapper {

      private final String version;
      private final ComparableVersion comparableVersion;
      private final String minor;

      VersionWrapper(String version) {
        this.version = version;
        // e.g. 1:22.3.4.3-1, removing 1:
        String curatedVersion = version.startsWith("1:") ? version.substring(2) : version;
        this.comparableVersion = new ComparableVersion(curatedVersion);
        this.minor = extractMinor(version);
      }
    }

    Map<String, List<VersionWrapper>> minors =
        versions.stream()
            .map(VersionWrapper::new)
            .filter(v -> !v.minor.equals(minorToIgnore))
            .collect(groupingBy(versionWrapper -> versionWrapper.minor));

    Comparator<VersionWrapper> comparator =
        Comparator.comparing(wrapper -> wrapper.comparableVersion);
    return minors.values().stream()
        .map(
            patches -> {
              if (patches.size() == 1) {
                return patches.get(0).version;
              } else {
                return Collections.max(patches, comparator).version;
              }
            })
        .collect(toList());
  }
}
