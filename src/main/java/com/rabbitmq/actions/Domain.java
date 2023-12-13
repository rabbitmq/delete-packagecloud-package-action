/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.rabbitmq.actions;

import java.time.ZonedDateTime;
import java.util.Objects;

abstract class Domain {

  private Domain() {}

  static class Package {

    private String name;
    private ZonedDateTime created_at;
    private String destroy_url;
    private String package_url;
    private String filename;
    private String version;

    Package() {}

    public void setFilename(String filename) {
      this.filename = filename;
    }

    public void setVersion(String version) {
      this.version = version;
    }

    String filename() {
      return this.filename;
    }

    String version() {
      return this.version;
    }

    public String destroy_url() {
      return destroy_url;
    }

    public ZonedDateTime created_at() {
      return created_at;
    }

    @Override
    public String toString() {
      return "Package{"
          + "name='"
          + name
          + '\''
          + ", filename='"
          + filename
          + '\''
          + ", version='"
          + version
          + '\''
          + ", created_at='"
          + created_at
          + '\''
          + ", destroy_url='"
          + destroy_url
          + '\''
          + '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Package aPackage = (Package) o;
      return Objects.equals(package_url, aPackage.package_url);
    }

    @Override
    public int hashCode() {
      return Objects.hash(package_url);
    }
  }

  static class PackageVersion {

    final String version;
    ZonedDateTime lastPackageDate;

    PackageVersion(String version) {
      this.version = version;
    }

    void consider(Domain.Package p) {
      if (lastPackageDate == null) {
        lastPackageDate = p.created_at();
      } else {
        lastPackageDate =
            lastPackageDate.isBefore(p.created_at()) ? p.created_at() : lastPackageDate;
      }
    }
  }
}
