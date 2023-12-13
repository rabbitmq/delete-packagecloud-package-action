# Overview

This is a GitHub Action to delete packages on [Packagecloud](https://packagecloud.io/).
The action deletes older packages and lets a given number of packages left, based on version or creation time.

# Usage

## Configuration

* `username`: *Required.* The username for the Packagecloud account.
* `repository`: *Required.* The repository name.
* `token`: *Required.* Token used for all requests.
* `type`: *Optional*. Type of the packages in the repository (`deb`, `rpm`).
* `globs`: *Optional.* Comma-separated list of globs for files that will be uploaded/downloaded.
* `do_delete`: *Optional*. Flag to enforce deletion.
Allows to preview deleted packages with dry runs.
Default is `false` (do not delete).
* `version_filter`: *Optional*. Java regular expression to select the packages.
* `keep_last_n`: *Optional*. Number of versions to keep.
Default is 0.
* `keep_last_minor_patches`: *Optional*.
Do not delete last patch versions of identified minors.
Default is `false`.
* `order_by`: *Optional. One of [version, time]*.
Whether to sort packages by version (the default) or by time.

## Example

```yaml
- name: Delete old packages
  uses: docker://pivotalrabbitmq/delete-packagecloud-package-action:latest
  with:
    username: ${{ secrets.PACKAGECLOUD_USERNAME }}
    repository: erlang
    token: ${{ secrets.PACKAGECLOUD_TOKEN }}
    globs: erlang-*
    version_filter: '^25\.*'
    keep_last_n: 2
    keep_last_minor_patches: true
    do_delete: true
```

# License and Copyright

(c) 2023 Broadcom. All Rights Reserved.
The term Broadcom refers to Broadcom Inc. and/or its subsidiaries.

This package, the Packagecloud Delete Package GitHub Action, is licensed under the Mozilla Public License 2.0 ("MPL").

See [LICENSE](./LICENSE).
